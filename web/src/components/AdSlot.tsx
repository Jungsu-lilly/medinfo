"use client";

import { useEffect, useRef } from "react";
import { ADSENSE_CLIENT } from "@/lib/site";

declare global {
  interface Window {
    adsbygoogle?: unknown[];
  }
}

/**
 * 애드센스 광고 단위 한 칸. "광고" 표시를 붙여 병원 순위와 구분한다 (순위에는 섞지 않음).
 * slot이 비어 있으면(광고 단위를 아직 안 만듦) 아무것도 그리지 않는다. 응급실 화면에는 쓰지 않는다.
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
    <aside aria-label="광고" style={{ margin: "12px 0" }}>
      <span style={{ fontSize: 11, color: "var(--muted)" }}>광고</span>
      <ins
        className="adsbygoogle"
        style={{ display: "block" }}
        data-ad-client={ADSENSE_CLIENT}
        data-ad-slot={slot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </aside>
  );
}
