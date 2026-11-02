import { HospitalIcon, ListIcon, PulseIcon } from "./Icons";

/** 하단 탭(모바일)과 상단 메뉴(데스크톱)가 같이 쓰는 주 메뉴 */
export const NAV_TABS = [
  { href: "/", label: "가격 비교", icon: ListIcon, match: (p: string) => p === "/" || p.startsWith("/price") },
  { href: "/hospital", label: "병원 찾기", icon: HospitalIcon, match: (p: string) => p.startsWith("/hospital") },
  { href: "/er", label: "응급실", icon: PulseIcon, match: (p: string) => p.startsWith("/er"), emergency: true },
];
