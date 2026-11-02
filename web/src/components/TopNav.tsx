"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { SITE_NAME } from "@/lib/site";
import { SearchIcon } from "./Icons";
import { NAV_TABS } from "./navTabs";
import styles from "./TopNav.module.css";

/** 데스크톱 상단 메뉴. 모바일에서는 CSS로 숨기고 BottomNav를 쓴다 */
export default function TopNav() {
  return <TopNavView pathname={usePathname()} />;
}

/**
 * 주소(pathname)를 받아 그리기만 하는 부분. 주소를 모르는 빌드 시점(동적 경로의 틀)에는
 * pathname=null로 그려 두고(활성 표시 없음), 요청 때 TopNav가 실제 주소로 바꾼다 (layout의 Suspense).
 */
export function TopNavView({ pathname }: { pathname: string | null }) {
  return (
    <header className={styles.bar}>
      <div className={styles.inner}>
        <Link href="/" className={styles.logo}>
          {SITE_NAME}
        </Link>
        <nav aria-label="주 메뉴" className={styles.links}>
          {NAV_TABS.map(({ href, label, match, emergency }) => {
            const active = pathname !== null && match(pathname);
            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? "page" : undefined}
                className={`${styles.link} ${active ? (emergency ? styles.activeRed : styles.active) : ""}`}
              >
                {label}
              </Link>
            );
          })}
        </nav>
        {/* 홈에는 큰 검색창이 있으니 다른 화면에서만 */}
        {pathname !== null && pathname !== "/" && (
          <form role="search" action="/search" className={styles.search}>
            <label htmlFor="top-q" className="sr-only">
              비급여 항목 검색
            </label>
            <SearchIcon size={18} />
            <input id="top-q" name="q" type="search" placeholder="비급여 항목 검색" autoComplete="off" />
          </form>
        )}
      </div>
    </header>
  );
}
