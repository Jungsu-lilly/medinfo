# web — Next.js 프론트

모바일 웹앱(PWA). 시안 v0.1(홈 · 가격 비교 · 병원 상세 · 응급실)을 옮긴 것.

- 폭 960px 미만: 모바일 레이아웃 (하단 탭, 본문 480px)
- 폭 960px 이상: 데스크톱 레이아웃 (상단 메뉴, 본문 720px). 화면별 2단 배치는 오픈 후

## 실행

Node.js **20.9 이상** 필요 (Next.js 16 요구사항). 권장은 26 LTS (`.nvmrc`).
`.npmrc`의 `engine-strict=true` 때문에 낮은 버전에서는 `npm install`이 바로 실패한다.

LTS를 설치했는데도 `node -v`가 예전 버전이면 Node가 두 군데 깔린 것. Windows는 `where.exe node`로 확인 →
맨 위 경로가 실제로 쓰이는 Node (nvm-windows의 `C:\nvm4w\nodejs`가 앞에 있으면 `nvm use 26` 또는 그 경로 제거).

```bash
cd web
npm install          # 처음 한 번
npm run dev          # http://localhost:3000
```

Spring(8080)이 떠 있어야 화면에 데이터가 나온다 (가격·병원·응급실 모두 실데이터). `/api/*` 요청은 Next가 Spring으로 넘긴다(`next.config.ts`의 rewrites).
Spring 주소가 다르면 `BACKEND_URL=http://주소:포트 npm run dev`.

휴대폰으로 보려면 PC와 같은 와이파이에서 `http://PC의IP:3000` (PC IP는 `ipconfig`).

## 구조

```
src/app/
├── page.tsx              홈 (정적) — 응급실 한 줄 요약만 브라우저에서 불러옴
├── price/[code]/         가격 비교 (/price/ABZ010001?sido=110000&sggu=… 또는 ?lat=&lng= 내 위치)
├── hospital/[id]/        병원 상세 (비급여 항목 전체, 지역 중간 대비)
├── er/                   응급실 — 첫 화면은 서버가 채우고, 2분마다 브라우저가 갱신
├── search/  guide/  about/  privacy/  offline/
└── manifest.ts           PWA (홈 화면에 추가)
src/lib/
├── api.ts                가격·병원 API 타입·호출 (Next 서버 캐시 10분)
├── guides.ts             가이드 글 목록, 홈 검색어
├── er.ts                 응급실 API 타입·호출
└── site.ts               서비스 이름 (미정 → 여기만 바꾸면 됨)
public/sw.js              서비스 워커: 오프라인 안내 화면만 (데이터는 캐시 안 함)
```

## 알아둘 것

- **Next.js 16 + Cache Components**: 데이터를 쓰는 부분은 `'use cache'`로 캐시하거나 `<Suspense>` 안에서 요청마다 읽는다. 빌드 결과의 ○(정적) / ◐(부분 정적) 표시로 확인
- 폰트는 Pretendard 가변 폰트(`pretendard` 패키지, dynamic subset)를 직접 포함 → 외부 CDN 없음, 쓰인 글자 조각만 내려받음
- `output: "standalone"` → 컨테이너 이미지용 최소 서버가 `.next/standalone`에 생김
- 서비스 워커는 production 빌드(`npm run build && npm start`)에서만 등록됨
