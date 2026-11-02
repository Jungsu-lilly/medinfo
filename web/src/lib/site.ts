// 서비스 이름은 아직 미정 → 정해지면 여기 한 곳만 바꾸면 된다 (헤더, 제목, manifest)
export const SITE_NAME = "[서비스명]";
export const SITE_DESCRIPTION =
  "MRI·예방접종·1인실 같은 비급여 진료비를 병원별로 비교하고, 근처 응급실 남은 병상을 실시간으로 확인하세요.";

/** 실제 주소 (https://도메인). 빌드 때 SITE_URL로 넣는다. 공유 미리보기·robots.txt에 쓰임 */
export const SITE_URL = process.env.SITE_URL ?? "http://localhost:3000";

/**
 * 검색엔진 노출 여부. 공개(오픈) 전에는 false → robots.txt 전체 차단 + 모든 페이지 noindex.
 * 오픈할 때 SITE_INDEXING=true로 다시 빌드
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";
