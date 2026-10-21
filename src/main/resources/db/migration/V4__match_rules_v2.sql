-- V4: 기관명 정규화에 "부속" 제거 추가 (V3는 이미 적용됐으므로 수정하지 않고 새로 정의)
--   재단법인 아산사회복지재단 부속 보령아산병원 → 보령아산병원 (V3에선 "부속보령아산병원"이 돼서 매칭 실패)
CREATE OR REPLACE FUNCTION norm_hosp_name(n text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT regexp_replace(
           regexp_replace(
             regexp_replace(
               regexp_replace(
                 regexp_replace(coalesce(n, ''), '\([^)]*\)', '', 'g'),
                 '(의료법인|재단법인|학교법인|사회복지법인|사단법인|특수법인)', '', 'g'),
               '^.*?(유지재단|의료재단|복지재단|재단|학원)', ''),
             '\s', '', 'g'),
           '^부속', '')
$$;

-- 매칭 방식 구분값이 늘어남: auto(1단계) / auto_far(2단계) / auto_cont(3단계) / manual
COMMENT ON COLUMN er_hospital.match_method IS 'auto: 300m+유사도 0.5 / auto_far: 2km+이름 완전일치 / auto_cont: 100m+이름 포함관계 / manual: 사람이 지정';
