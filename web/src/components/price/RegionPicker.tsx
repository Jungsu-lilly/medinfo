"use client";

import { usePathname, useRouter } from "next/navigation";
import { LocateIcon } from "@/components/Icons";
import type { Region } from "@/lib/api";
import { RoughNotice } from "@/components/location/LocationPicker";
import { coordsQuery, useLocate } from "@/lib/location";
import styles from "./price.module.css";

/** 지역 고르기: 시도 → 시군구(선택). 또는 내 위치 반경 5km(가까운 순으로 열림). 고르면 주소창이 바뀌고 서버가 다시 그린다 */
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
  const { state, roughMeters, locate } = useLocate();
  const current = regions.find((r) => r.code === sido);

  const go = (params: Record<string, string>) => router.push(`${pathname}?${new URLSearchParams(params)}`);

  return (
    <>
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
              onChange={(e) =>
                go(e.target.value ? { sido: current.code, sggu: e.target.value } : { sido: current.code })
              }
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
        <button
          type="button"
          className={styles.locateBtn}
          onClick={() => locate((c) => router.push(`${pathname}?${coordsQuery(c)}&sort=distance`))}
          disabled={state === "locating"}
          aria-pressed={nearMe}
        >
          <LocateIcon />
          {state === "locating" ? "확인 중…" : state === "denied" ? "위치 권한 필요" : "내 위치"}
        </button>
      </div>
      {state === "rough" && roughMeters !== null && (
        <div style={{ padding: "0 16px 4px" }}>
          <RoughNotice meters={roughMeters} hint="위에서 시도·시군구를 골라 주세요." />
        </div>
      )}
    </>
  );
}
