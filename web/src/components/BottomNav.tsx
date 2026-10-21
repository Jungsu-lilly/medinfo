"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HospitalIcon, ListIcon, PulseIcon } from "./Icons";
import styles from "./BottomNav.module.css";

const TABS = [
  { href: "/", label: "가격 비교", icon: ListIcon, match: (p: string) => p === "/" || p.startsWith("/price") },
  { href: "/hospital", label: "병원 찾기", icon: HospitalIcon, match: (p: string) => p.startsWith("/hospital") },
  { href: "/er", label: "응급실", icon: PulseIcon, match: (p: string) => p.startsWith("/er"), emergency: true },
];

export default function BottomNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="주 메뉴" className={styles.nav}>
      {TABS.map(({ href, label, icon: Icon, match, emergency }) => {
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
