"use client";

import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { LocateIcon } from "@/components/Icons";
import type { Region } from "@/lib/api";
import styles from "./price.module.css";

/** 지역 고르기: 시도 → 시군구(선택). 또는 내 위치 반경 5km. 고르면 주소창이 바뀌고 서버가 다시 그린다 */
export default function RegionPicker({
  regions,
  sido,
  sggu,
  nearMe,
}: {
  regions: Region[];
  sido: string | null;
  sggu: string | null;
  nearMe: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [locating, setLocating] = useState(false);
  const [locError, setLocError] = useState(false);
  const current = regions.find((r) => r.code === sido);

  const go = (params: Record<string, string>) => router.push(`${pathname}?${new URLSearchParams(params)}`);

  const locate = () => {
    if (!("geolocation" in navigator)) return setLocError(true);
    setLocating(true);
    setLocError(false);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false);
        go({ lat: pos.coords.latitude.toFixed(4), lng: pos.coords.longitude.toFixed(4) });
      },
      () => {
        setLocating(false);
        setLocError(true);
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 },
    );
  };

  return (
    <div className={styles.picker}>
      <label className="sr-only" htmlFor="sido">
        시도
      </label>
      <select
        id="sido"
        className={styles.select}
        value={nearMe ? "" : (sido ?? "")}
        onChange={(e) => e.target.value && go({ sido: e.target.value })}
      >
        {nearMe && <option value="">내 위치 5km</option>}
        {regions.map((r) => (
          <option key={r.code} value={r.code}>
            {r.name}
          </option>
        ))}
      </select>
      {!nearMe && current && (
        <>
          <label className="sr-only" htmlFor="sggu">
            시군구
          </label>
          <select
            id="sggu"
            className={styles.select}
            value={sggu ?? ""}
            onChange={(e) => go(e.target.value ? { sido: current.code, sggu: e.target.value } : { sido: current.code })}
          >
            <option value="">전체</option>
            {current.sggu.map((g) => (
              <option key={g.code} value={g.code}>
                {g.name}
              </option>
            ))}
          </select>
        </>
      )}
      <button type="button" className={styles.locateBtn} onClick={locate} disabled={locating} aria-pressed={nearMe}>
        <LocateIcon />
        {locating ? "확인 중…" : locError ? "위치 권한 필요" : "내 위치"}
      </button>
    </div>
  );
}
