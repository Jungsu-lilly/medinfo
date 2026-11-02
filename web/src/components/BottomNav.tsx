"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NAV_TABS } from "./navTabs";
import styles from "./BottomNav.module.css";

/** 모바일 하단 탭. 데스크톱(960px 이상)에서는 숨기고 TopNav가 대신한다 */
export default function BottomNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="주 메뉴" className={styles.nav}>
      {NAV_TABS.map(({ href, label, icon: Icon, match, emergency }) => {
        const active = match(pathname);
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
