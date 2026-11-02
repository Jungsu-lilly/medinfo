import type { Metadata, Viewport } from "next";
// Pretendard 가변 폰트를 npm 패키지로 직접 포함 → 빌드·접속 때 외부 CDN에 의존하지 않음
// dynamic-subset: 글자 범위별로 92개 조각으로 나뉘어 있어 화면에 쓰인 글자 조각만 내려받음
import "pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css";
import "./globals.css";
import BottomNav from "@/components/BottomNav";
import ServiceWorkerRegister from "@/components/ServiceWorkerRegister";
import TopNav from "@/components/TopNav";
import { SITE_DESCRIPTION, SITE_NAME } from "@/lib/site";

export const metadata: Metadata = {
  title: { default: `${SITE_NAME} — 비급여 진료비 비교·응급실 현황`, template: `%s | ${SITE_NAME}` },
  description: SITE_DESCRIPTION,
  applicationName: SITE_NAME,
  appleWebApp: { capable: true, title: SITE_NAME, statusBarStyle: "default" },
  formatDetection: { telephone: false },
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
      <body>
        <TopNav />
        <div className="app">{children}</div>
        <BottomNav />
        <ServiceWorkerRegister />
      </body>
    </html>
  );
}
