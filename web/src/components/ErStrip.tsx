"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { DEFAULT_POINT, erQuery, type ErStatus } from "@/lib/er";
import { ChevronRight } from "./Icons";

/** 홈의 응급실 한 줄 요약. 홈은 정적 페이지로 두고 이 부분만 브라우저에서 불러온다 */
export default function ErStrip() {
  const [summary, setSummary] = useState<{ near: number; open: number; minutes: number | null } | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(erQuery(DEFAULT_POINT.lat, DEFAULT_POINT.lng, 10))
      .then((r) => (r.ok ? (r.json() as Promise<ErStatus>) : null))
      .then((data) => {
        if (cancelled || !data) return;
        const near = data.items.filter((i) => i.distanceMeters <= 5000);
        const open = near.filter((i) => (i.available ?? 0) > 0 && !i.stale).length;
        const minutes = data.lastCollectedAt
          ? Math.max(0, Math.round((Date.now() - new Date(data.lastCollectedAt).getTime()) / 60000))
          : null;
        setSummary({ near: near.length, open, minutes });
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <Link
      href="/er"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        margin: "0 16px 20px",
        padding: "12px 14px",
        border: "1px solid #f0d6d1",
        borderRadius: 8,
        background: "#fdf6f4",
      }}
    >
      <span aria-hidden="true" style={{ width: 8, height: 8, borderRadius: 4, background: "var(--red)", flex: "none" }} />
      <span style={{ flex: 1, fontSize: 14, lineHeight: "20px" }}>
        {summary && summary.near > 0 ? (
          <>
            근처 응급실{" "}
            <b style={{ fontWeight: 600 }}>
              {summary.near}곳 중 {summary.open}곳
            </b>{" "}
            지금 병상 있음
          </>
        ) : (
          "근처 응급실 남은 병상 보기"
        )}
      </span>
      {summary?.minutes != null && <span className="meta">{summary.minutes === 0 ? "방금" : `${summary.minutes}분 전`}</span>}
      <ChevronRight color="#6b7079" />
    </Link>
  );
}
