"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { LocateIcon } from "@/components/Icons";

/** 내 위치로 다시 찾기 → 주소창에 좌표를 넣고 서버가 다시 그린다 */
export default function LocateButton({ active }: { active: boolean }) {
  const router = useRouter();
  const [state, setState] = useState<"idle" | "locating" | "denied">("idle");

  const locate = () => {
    if (!("geolocation" in navigator)) return setState("denied");
    setState("locating");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setState("idle");
        router.push(`/hospital?lat=${pos.coords.latitude.toFixed(4)}&lng=${pos.coords.longitude.toFixed(4)}`);
      },
      () => setState("denied"),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 },
    );
  };

  return (
    <button
      type="button"
      onClick={locate}
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
