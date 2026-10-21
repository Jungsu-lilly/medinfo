-- V5
-- 1) 기관명 정규화 버그 수정: "학원"·"재단"으로 끝나는 이름이 통째로 지워지던 문제
--    동남권원자력의학원 → (V4) "" → (V5) 동남권원자력의학원
--    법인 접두어 제거는 뒤에 병원 이름이 남을 때만 한다 (?=.)
CREATE OR REPLACE FUNCTION norm_hosp_name(n text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT regexp_replace(
           regexp_replace(
             regexp_replace(
               regexp_replace(
                 regexp_replace(coalesce(n, ''), '\([^)]*\)', '', 'g'),
                 '(의료법인|재단법인|학교법인|사회복지법인|사단법인|특수법인)', '', 'g'),
               '^.*?(유지재단|의료재단|복지재단|재단|학원)(?=.)', ''),
             '\s', '', 'g'),
           '^부속', '')
$$;

-- 2) 수동 매칭 목록: 자동으로 못 붙는 응급기관을 코드로 관리 (내용은 R__er_manual_match.sql)
--    DB에 직접 UPDATE하면 로컬에만 남고 운영 DB엔 없으므로, 목록 자체를 저장소에 둔다.
--    FK를 걸지 않는 이유: 마이그레이션 시점엔 응급기관·병원 데이터가 아직 없을 수 있음
CREATE TABLE er_manual_match (
  hpid        VARCHAR(10)  PRIMARY KEY,
  ykiho       VARCHAR(400) NOT NULL,
  note        VARCHAR(200),
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON COLUMN er_hospital.match_method IS 'manual: er_manual_match 목록 / auto: 300m+유사도 0.5 / auto_far: 2km+이름 완전일치 / auto_cont: 100m+이름 포함관계';
