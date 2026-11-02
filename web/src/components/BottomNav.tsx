"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NAV_TABS } from "./navTabs";
import styles from "./BottomNav.module.css";

/** 모바일 하단 탭. 데스크톱(960px 이상)에서는 숨기고 TopNav가 대신한다 */
export default function BottomNav() {
  return <BottomNavView pathname={usePathname()} />;
}

/** 주소를 모르는 빌드 시점에는 pathname=null(활성 표시 없음)로 그려 둔다. TopNavView 주석 참고 */
export function BottomNavView({ pathname }: { pathname: string | null }) {
  return (
    <nav aria-label="주 메뉴" className={styles.nav}>
      {NAV_TABS.map(({ href, label, icon: Icon, match, emergency }) => {
        const active = pathname !== null && match(pathname);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={`${styles.tab} ${active ? (emergency ? styles.activeRed : styles.active) : ""}`}
          >
            <Icon size={22} />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
