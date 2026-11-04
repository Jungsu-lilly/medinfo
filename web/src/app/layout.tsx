import type { Metadata, Viewport } from "next";
// Pretendard 가변 폰트를 npm 패키지로 직접 포함 → 빌드·접속 때 외부 CDN에 의존하지 않음
// dynamic-subset: 글자 범위별로 92개 조각으로 나뉘어 있어 화면에 쓰인 글자 조각만 내려받음
import "pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css";
import "./globals.css";
import { Suspense } from "react";
import BottomNav, { BottomNavView } from "@/components/BottomNav";
import LocationNoticeHost from "@/components/LocationNoticeHost";
import ServiceWorkerRegister from "@/components/ServiceWorkerRegister";
import TopNav, { TopNavView } from "@/components/TopNav";
import {
  ADSENSE_CLIENT,
  GA_ENABLED,
  GA_MEASUREMENT_ID,
  SITE_DESCRIPTION,
  SITE_INDEXING,
  SITE_NAME,
  SITE_URL,
} from "@/lib/site";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  // 공개 전에는 모든 페이지 noindex (robots.ts와 같이)
  robots: SITE_INDEXING ? undefined : { index: false, follow: false },
  title: { default: `${SITE_NAME} — 비급여 진료비 비교·응급실 현황`, template: `%s | ${SITE_NAME}` },
  description: SITE_DESCRIPTION,
  applicationName: SITE_NAME,
  appleWebApp: { capable: true, title: SITE_NAME, statusBarStyle: "default" },
  formatDetection: { telephone: false },
  // 네이버 서치어드바이저 소유확인 (구글 서치 콘솔은 public/google…html 파일로 확인). 지우면 소유확인 풀림
  verification: { other: { "naver-site-verification": "57a8886aca11b303f7efe138b966a5bb578b24c2" } },
};

export const viewport: Viewport = {
  themeColor: "#ffffff",
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko">
      <head>
        {/* GA4: 구글이 안내한 gtag 코드 그대로 <head> 위쪽에 */}
        {GA_ENABLED && (
          <>
            <script async src={`https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`} />
            <script
              dangerouslySetInnerHTML={{
                __html: `window.dataLayer=window.dataLayer||[];function gtag(){dataLayer.push(arguments);}gtag('js',new Date());gtag('config','${GA_MEASUREMENT_ID}');`,
              }}
            />
          </>
        )}
        {/* 구글 애드센스: 애드센스가 안내한 그대로 <head>에 직접 (사이트 확인이 HTML에서 이 태그를 찾음) */}
        <script
          async
          src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT}`}
          crossOrigin="anonymous"
        />
      </head>
      <body>
        <Suspense fallback={<TopNavView pathname={null} />}>
          <TopNav />
        </Suspense>
        <div className="app">{children}</div>
        <Suspense fallback={<BottomNavView pathname={null} />}>
          <BottomNav />
        </Suspense>
        <ServiceWorkerRegister />
        <LocationNoticeHost />
      </body>
    </html>
  );
}
