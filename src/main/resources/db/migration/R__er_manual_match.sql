-- 수동 매칭 목록 (Flyway repeatable migration)
-- 이 파일 내용이 바뀌면 다음 기동 때 Flyway가 다시 실행하고, 매칭기가 목록을 그대로 반영한다.
-- 한 줄 추가 = 응급기관 1곳 수동 지정. 이유(note)는 꼭 남긴다.
--
-- ykiho 찾는 법: SELECT name, addr, ykiho FROM hospital WHERE addr LIKE '%도로명%';
-- hpid 찾는 법:  SELECT hpid, name, addr FROM er_hospital WHERE name LIKE '%병원명%';

DELETE FROM er_manual_match;

-- 예시
-- INSERT INTO er_manual_match (hpid, ykiho, note) VALUES
--   ('A0000000', 'JDQ4MT...', '병원명 변경으로 심평원 이름과 다름');
