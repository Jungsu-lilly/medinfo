"use client";

import { useRouter } from "next/navigation";
import { LocateIcon } from "@/components/Icons";
import { coordsQuery, useLocate } from "@/lib/location";

/** 내 위치로 다시 찾기 → 주소창에 좌표를 넣고 서버가 다시 그린다 */
export default function LocateButton({ active }: { active: boolean }) {
  const router = useRouter();
  const { state, locate } = useLocate();

  return (
    <button
      type="button"
      onClick={() => locate((c) => router.push(`/hospital?${coordsQuery(c)}`))}
      disabled={state === "locating"}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 4,
        border: 0,
        background: "none",
        padding: 0,
        fontSize: 14,
        fontWeight: active ? 700 : 500,
        color: "var(--accent)",
        cursor: "pointer",
      }}
    >
      <LocateIcon />
      {state === "locating" ? "확인 중…" : state === "denied" ? "위치 권한 필요" : "내 위치"}
    </button>
  );
}
