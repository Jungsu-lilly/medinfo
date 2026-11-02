"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { coordsQuery, loadLocation } from "@/lib/location";

/** 항목 가격 페이지 링크. 최근에 "내 위치"를 골랐으면 내 위치 기준으로 연다 */
export default function PriceLink({
  code,
  className,
  children,
}: {
  code: string;
  className?: string;
  children: React.ReactNode;
}) {
  const base = `/price/${code}`;
  const [href, setHref] = useState(base);
  useEffect(() => {
    const c = loadLocation();
    if (c) setHref(`${base}?${coordsQuery(c)}`); // eslint-disable-line react-hooks/set-state-in-effect -- 저장소 값은 화면이 뜬 뒤에만 읽을 수 있음
  }, [base]);
  return (
    <Link href={href} className={className}>
      {children}
    </Link>
  );
}
