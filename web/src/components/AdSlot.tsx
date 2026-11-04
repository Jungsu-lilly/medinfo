"use client";

import { useEffect, useRef } from "react";
import { ADSENSE_CLIENT } from "@/lib/site";
import styles from "./AdSlot.module.css";

declare global {
  interface Window {
    adsbygoogle?: unknown[];
  }
}

/**
 * 애드센스 광고 단위 한 칸. "광고" 표시를 붙여 병원 순위와 구분한다 (순위에는 섞지 않음).
 * slot이 비어 있으면(광고 단위를 아직 안 만듦) 아무것도 그리지 않는다. 응급실 화면에는 쓰지 않는다.
 * 높이는 CSS로 고정 (data-ad-format="auto"는 모바일에서 화면 절반 넘게 차지함)
 */
export default function AdSlot({ slot }: { slot: string }) {
  const pushed = useRef(false);

  useEffect(() => {
    if (!slot || pushed.current) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch {
      /* 광고 차단기 등 → 빈 칸으로 둠 */
    }
  }, [slot]);

  if (!slot) return null;
  return (
    <aside aria-label="광고" className={styles.ad}>
      <span className={styles.label}>광고</span>
      <ins className={`adsbygoogle ${styles.unit}`} data-ad-client={ADSENSE_CLIENT} data-ad-slot={slot} />
    </aside>
  );
}
