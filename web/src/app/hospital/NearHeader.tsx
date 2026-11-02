"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ChevronDown } from "@/components/Icons";
import LocationPicker from "@/components/location/LocationPicker";
import { areaQuery, coordsQuery, loadArea, loadLocation } from "@/lib/location";

/**
 * 가까운 병원 머리줄: "OO 기준 ▾"을 누르면 내 위치 / 시군구 고르기.
 * 고르면 주소창에 좌표를 넣고 서버가 다시 그린다.
 * 주소창에 위치가 없으면 기억해 둔 내 위치(30분)·지역으로 바로 바꿔 연다
 */
export default function NearHeader({ label, hasPoint }: { label: string; hasPoint: boolean }) {
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (hasPoint) return;
    const c = loadLocation();
    const a = loadArea();
    if (c) router.replace(`${pathname}?${coordsQuery(c)}`);
    else if (a) router.replace(`${pathname}?${areaQuery(a)}`);
  }, [hasPoint, pathname, router]);

  return (
    <>
      <div className="section-head">
        <h2 id="near" className="section-title">
          가까운 병원
        </h2>
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          aria-controls="near-picker"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 2,
            border: 0,
            background: "none",
            padding: 0,
            fontSize: 14,
            fontWeight: 600,
            color: "var(--accent)",
            cursor: "pointer",
          }}
        >
          {label} 기준
          <ChevronDown />
        </button>
      </div>
      {open && (
        <div style={{ marginBottom: 12 }}>
          <LocationPicker
            id="near-picker"
            onCoords={(c) => {
              setOpen(false);
              router.push(`${pathname}?${coordsQuery(c)}`);
            }}
            onArea={(a) => {
              setOpen(false);
              router.push(`${pathname}?${areaQuery(a)}`);
            }}
          />
        </div>
      )}
    </>
  );
}
