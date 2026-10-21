-- V1: 서비스 스키마 (설계 문서 claude/의료-DB-스키마.sql v0.3 기준)
-- v0.3: er_hospital에 응급기관 분류·대표전화·매칭 방식 추가 (목록 API 실제 응답 반영)
-- 근거: 실제 API 응답 (비급여 요약/상세, 응급실 실시간 가용병상)
-- RDS for PostgreSQL에서 PostGIS 확장 사용 가능

CREATE EXTENSION IF NOT EXISTS postgis;

-- ─────────────────────────────────────────────
-- 1. 병원 (심평원 기준)
--    ykiho는 암호화 문자열이라 길다(80자+, 명세상 최대 400) → 대리키 id를 PK로
--    location: "내 위치 반경 N km" 검색용 (PostGIS geography)
-- ─────────────────────────────────────────────
CREATE TABLE hospital (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ykiho       VARCHAR(400) NOT NULL UNIQUE,
  name        VARCHAR(200) NOT NULL,              -- yadmNm
  cl_cd       CHAR(2)      NOT NULL,              -- 01 상급종합 ...
  cl_cd_nm    VARCHAR(50)  NOT NULL,
  sido_cd     CHAR(6)      NOT NULL,
  sido_nm     VARCHAR(40)  NOT NULL,
  sggu_cd     CHAR(6)      NOT NULL,
  sggu_nm     VARCHAR(40)  NOT NULL,
  addr        VARCHAR(500),                       -- 병원정보서비스에서 보강
  location    GEOGRAPHY(POINT, 4326),             -- 병원정보서비스 좌표로 보강
  nonpay_url  VARCHAR(500),                       -- urlAddr (병원 비급여 안내 페이지)
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_hospital_region   ON hospital (sido_cd, sggu_cd);
CREATE INDEX idx_hospital_location ON hospital USING GIST (location);

-- ─────────────────────────────────────────────
-- 2. 비급여 항목 (코드 + 분류 계층)
--    요약 API 응답의 중분류/소분류/상세분류로 채움
-- ─────────────────────────────────────────────
CREATE TABLE nonpay_item (
  npay_cd     VARCHAR(50)  PRIMARY KEY,           -- ABZ010001
  kor_nm      VARCHAR(400) NOT NULL,              -- 상급병실료/1인실
  mdiv_cd     VARCHAR(20),                        -- 1010A
  mdiv_nm     VARCHAR(200),                       -- 상급병실료
  sdiv_cd     VARCHAR(20),
  sdiv_nm     VARCHAR(200),
  dtl_div_cd  VARCHAR(20),
  dtl_div_nm  VARCHAR(200)
);
CREATE INDEX idx_item_mdiv ON nonpay_item (mdiv_cd);

-- ─────────────────────────────────────────────
-- 3. 비급여 가격 요약 (병원 × 항목, 최소~최대) ← 가격 비교 화면(기능 1)의 원천
--    출처: getNonPaymentItemHospList2 (시도 단위로 대량 수집 가능, 서울만 33,078건)
--    지역 컬럼을 일부러 비정규화: "항목 X, 지역 Y, 싼 순" 조회를 조인 없이 처리
--    → 장애 시나리오 1(커넥션 풀 고갈)의 before/after 비교 대상
-- ─────────────────────────────────────────────
CREATE TABLE nonpay_price_summary (
  hospital_id  BIGINT      NOT NULL REFERENCES hospital (id),
  npay_cd      VARCHAR(50) NOT NULL REFERENCES nonpay_item (npay_cd),
  adt_fr_dd    DATE        NOT NULL,              -- 적용개시일 (2026년분 = 2026-09-03)
  adt_end_dd   DATE        NOT NULL,              -- 9999-12-31 = 현재 적용 중
  min_prc      INTEGER     NOT NULL,
  max_prc      INTEGER     NOT NULL,
  sido_cd      CHAR(6)     NOT NULL,              -- 비정규화
  sggu_cd      CHAR(6)     NOT NULL,              -- 비정규화
  PRIMARY KEY (hospital_id, npay_cd, adt_fr_dd)
);
CREATE INDEX idx_summary_compare ON nonpay_price_summary (npay_cd, sido_cd, sggu_cd, min_prc);

-- ─────────────────────────────────────────────
-- 4. 비급여 가격 상세 (병원별 세부 항목, 단일 금액) ← 병원 상세 화면(기능 2)
--    출처: getNonPaymentItemHospDtlList (ykiho 필수 → 병원별 호출, 상급종합 1곳 약 1,300건)
--    같은 npay_cd라도 병원 자체 항목명(yadmNpayCdNm)별로 여러 행 → sno로 구분
-- ─────────────────────────────────────────────
CREATE TABLE nonpay_price_detail (
  hospital_id      BIGINT       NOT NULL REFERENCES hospital (id),
  sno              INTEGER      NOT NULL,         -- 병원 내 일련번호
  adt_fr_dd        DATE         NOT NULL,
  adt_end_dd       DATE         NOT NULL,
  npay_cd          VARCHAR(50)  NOT NULL,
  yadm_npay_cd_nm  VARCHAR(400),                  -- "1인실 입원료(일반병동)"
  cur_amt          INTEGER      NOT NULL,
  PRIMARY KEY (hospital_id, sno, adt_fr_dd)
);
CREATE INDEX idx_detail_item ON nonpay_price_detail (npay_cd);

-- ─────────────────────────────────────────────
-- 5. 응급의료기관 (국립중앙의료원 hpid 기준) + 심평원 병원 매핑
--    출처: getEgytListInfoInqire (전국 530곳, 1회 호출) — 실시간 병상(415곳)보다 많음
--          → 병상 정보 없는 기관도 목록·전화번호는 표시
--    hospital_id는 매핑 배치가 채움: 좌표 100m 이내 후보 → 기관명 정규화 + 유사도(pg_trgm)
--    자동 매칭 실패·오매칭은 match_method='manual'로 직접 지정 (배치가 덮어쓰지 않음)
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE er_hospital (
  hpid          VARCHAR(10)  PRIMARY KEY,         -- A1700023
  name          VARCHAR(200) NOT NULL,            -- dutyName "(의)내경의료재단울산제일병원"
  emcls_cd      VARCHAR(10),                      -- dutyEmcls G009
  emcls_nm      VARCHAR(50),                      -- dutyEmclsName 응급실운영신고기관
  main_tel      VARCHAR(20),                      -- dutyTel1
  er_tel        VARCHAR(20),                      -- dutyTel3
  addr          VARCHAR(500),                     -- dutyAddr
  location      GEOGRAPHY(POINT, 4326),           -- wgs84Lon, wgs84Lat
  hospital_id   BIGINT REFERENCES hospital (id),  -- 매핑 결과 (NULL = 미매칭)
  match_method  VARCHAR(10),                      -- auto / manual
  match_score   REAL,                             -- 이름 유사도 (auto일 때)
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_er_hospital_map      ON er_hospital (hospital_id);
CREATE INDEX idx_er_hospital_location ON er_hospital USING GIST (location);

-- ─────────────────────────────────────────────
-- 6. 응급실 가용병상 스냅샷 (2분마다 전국 415곳 → 하루 약 30만 행)
--    collected_at 기준 일 단위 파티션 → 보관기간 지난 파티션은 DROP으로 즉시 정리
--    hvidate      = 병원이 마지막으로 입력한 시각 → "데이터 신선도" 알람 근거
--    collected_at = 우리가 수집한 시각            → "수집 지연" 알람 근거
-- ─────────────────────────────────────────────
CREATE TABLE er_bed_snapshot (
  id            BIGINT GENERATED ALWAYS AS IDENTITY,
  hpid          VARCHAR(10) NOT NULL REFERENCES er_hospital (hpid),
  collected_at  TIMESTAMPTZ NOT NULL,
  hvidate       TIMESTAMPTZ NOT NULL,
  hvec          INTEGER,                          -- 응급실 가용 (음수 가능성 → 그대로 저장, 표시는 앱에서)
  hvs01         INTEGER,                          -- 응급실 기준병상 → "가용/기준" 표시용
  hvoc          INTEGER,                          -- 수술실 가용
  hvgc          INTEGER,                          -- 입원실 가용
  hvicc         INTEGER,                          -- 일반중환자실 가용
  PRIMARY KEY (id, collected_at)                  -- 파티션 키 포함 필수
) PARTITION BY RANGE (collected_at);
CREATE INDEX idx_snapshot_latest ON er_bed_snapshot (hpid, collected_at DESC);

-- 파티션은 애플리케이션(SnapshotPartitionManager)이 기동 시·매시간 생성/삭제한다

-- 화면 조회용: 기관별 최신 1건 (DISTINCT ON은 PostgreSQL 전용 문법)
CREATE VIEW er_bed_latest AS
SELECT DISTINCT ON (hpid) *
FROM er_bed_snapshot
ORDER BY hpid, collected_at DESC;

-- ─────────────────────────────────────────────
-- 7. 외부 API 일일 호출량 (한도 관리 → 장애 시나리오 3)
--    배치 실행 이력 자체는 Spring Batch 메타 테이블(BATCH_JOB_EXECUTION 등)을 그대로 사용
-- ─────────────────────────────────────────────
CREATE TABLE api_usage_daily (
  api_name     VARCHAR(100) NOT NULL,             -- 오퍼레이션 단위 (한도가 오퍼레이션별)
  use_date     DATE         NOT NULL,
  call_count   INTEGER      NOT NULL DEFAULT 0,
  daily_limit  INTEGER      NOT NULL,             -- 비급여 10000, 응급실 1000
  PRIMARY KEY (api_name, use_date)
);

-- 참고: 응급실 응답의 Y/N 필드는 'N1' 같은 값도 옴 (hvventisoayn) → 저장 시 boolean이 아닌 VARCHAR
