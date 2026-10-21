-- V3: 병원정보 수집 + 응급기관 매핑 준비

-- 병원 대표 전화·홈페이지 (병원정보서비스 telno, hospUrl)
ALTER TABLE hospital ADD COLUMN tel VARCHAR(30);
ALTER TABLE hospital ADD COLUMN hosp_url VARCHAR(500);

-- 기관명 정규화: 법인명·괄호·공백 제거
--   재단법인예수병원유지재단예수병원 → 예수병원
--   학교법인을지학원대전을지대학교병원 → 대전을지대학교병원
--   (의)내경의료재단울산제일병원 → 울산제일병원
-- hpid↔ykiho 매칭과 화면 표시명 정리에 같이 쓴다. IMMUTABLE이라 인덱스·생성 컬럼에도 사용 가능.
CREATE OR REPLACE FUNCTION norm_hosp_name(n text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT regexp_replace(
           regexp_replace(
             regexp_replace(
               regexp_replace(coalesce(n, ''), '\([^)]*\)', '', 'g'),
               '(의료법인|재단법인|학교법인|사회복지법인|사단법인|특수법인)', '', 'g'),
             '^.*?(유지재단|의료재단|복지재단|재단|학원)', ''),
           '\s', '', 'g')
$$;
