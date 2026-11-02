-- V7: 가격·병원 조회 API용
--
-- 1) nonpay_price_current: 병원×항목별 "지금 적용 중인" 가격 1건
--    같은 병원·항목에 적용개시일이 다른 행이 여럿 남아 있을 수 있어 가장 최근 것만 쓴다
CREATE VIEW nonpay_price_current AS
SELECT DISTINCT ON (hospital_id, npay_cd)
       hospital_id, npay_cd, adt_fr_dd, adt_end_dd, min_prc, max_prc, sido_cd, sggu_cd
  FROM nonpay_price_summary
 WHERE adt_end_dd >= current_date
 ORDER BY hospital_id, npay_cd, adt_fr_dd DESC;

-- 2) nonpay_price_stats: 항목별 병원 수·최저·중간·최고 (시도별 + 전국 '000000')
--    홈 "많이 찾는 비급여", 항목 검색, 병원 상세의 "지역 중간보다 N원 낮음"에 쓴다.
--    요청마다 19만 행을 집계하지 않도록 미리 계산 → 요약 적재가 끝날 때마다 REFRESH (NonpayLoadLauncher)
--    가격 기준은 병원별 최저가(min_prc). 병원 안에서 최소~최대 범위가 있는 경우가 있어 하나로 통일
CREATE MATERIALIZED VIEW nonpay_price_stats AS
SELECT npay_cd,
       coalesce(sido_cd, '000000')::char(6)                          AS sido_cd,
       count(*)::int                                                  AS hospital_count,
       min(min_prc)                                                   AS low,
       round(percentile_cont(0.5) WITHIN GROUP (ORDER BY min_prc))::int AS median,
       max(min_prc)                                                   AS high
  FROM nonpay_price_current
 GROUP BY GROUPING SETS ((npay_cd, sido_cd), (npay_cd));

-- CONCURRENTLY 갱신(갱신 중에도 조회 가능)에 고유 인덱스가 필요
CREATE UNIQUE INDEX ux_price_stats ON nonpay_price_stats (npay_cd, sido_cd);
CREATE INDEX idx_price_stats_rank ON nonpay_price_stats (sido_cd, hospital_count DESC);

-- 3) 병원 이름 검색 (ILIKE '%이름%'도 인덱스를 탄다)
CREATE INDEX idx_hospital_name_trgm ON hospital USING gin (name gin_trgm_ops);
