// 서비스 이름 (2026-11-02 확정). 헤더·페이지 제목·홈 화면 앱 이름(manifest)이 모두 여기서 나온다
export const SITE_NAME = "병원값";
export const SITE_DESCRIPTION =
  "MRI·예방접종·1인실 같은 비급여 진료비를 병원별로 비교하고, 근처 응급실 남은 병상을 실시간으로 확인하세요.";

/**
 * 실제 주소. 빌드 때 SITE_URL로 넣는다 (공유 미리보기·robots.txt).
 * 한글 도메인 병원값.com은 퓨니코드로: https://xn--639ao13amqh.com (DNS·인증서·설정은 전부 이 형태)
 */
export const SITE_URL = process.env.SITE_URL ?? "http://localhost:3000";

/**
 * 검색엔진 노출 여부. 공개(오픈) 전에는 false → robots.txt 전체 차단 + 모든 페이지 noindex.
 * 오픈할 때 SITE_INDEXING=true로 다시 빌드
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";

/**
 * 구글 애드센스 게시자 ID (공개 값). 모든 페이지 <head>에 애드센스 스크립트를 넣고, /ads.txt에도 같은 값
 * 광고 단위 ID는 애드센스 → 광고 → 광고 단위에서 만든 뒤 넣는다. 비어 있으면 그 자리는 그리지 않음
 */
export const ADSENSE_CLIENT = "ca-pub-2979099793904209";
export const ADSENSE_SLOTS = {
  /** 가격 비교 목록 3번째 아래 */
  priceList: "2008786392", // price-list-inline (디스플레이, 반응형)
} as const;
