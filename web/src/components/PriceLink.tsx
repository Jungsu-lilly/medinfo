"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coordsQuery, loadArea, loadLocation } from "@/lib/location";

/**
 * 가격 페이지 링크(/price/…, /vaccine/…).
 * 최근(30분)에 "내 위치"를 썼으면 내 위치 기준, 아니면 직접 고른 지역(응급실·병원 찾기에서 고른 시군구) 기준으로 연다
 */
export default function PriceLink({
  href: base,
  className,
  children,
}: {
  href: string;
  className?: string;
  children: React.ReactNode;
}) {
  const [href, setHref] = useState(base);
  useEffect(() => {
    const c = loadLocation();
    const a = loadArea();
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 저장소 값은 화면이 뜬 뒤에만 읽을 수 있음
    if (c) setHref(`${base}?${coordsQuery(c)}`);
    else if (a) setHref(`${base}?sido=${a.sido}&sggu=${a.sggu}`);
  }, [base]);
  return (
    <Link href={href} className={className}>
      {children}
    </Link>
  );
}
