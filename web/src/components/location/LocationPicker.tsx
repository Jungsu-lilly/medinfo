"use client";

import { useEffect, useState } from "react";
import { LocateIcon } from "@/components/Icons";
import { distance } from "@/lib/format";
import { type AreaSido, type Coords, fetchAreas, loadArea, type SavedArea, saveArea, useLocate } from "@/lib/location";
import styles from "./location.module.css";

/**
 * 기준 위치 고르기 창: [내 위치 사용] 또는 시도·시군구 선택.
 * 시군구를 고르면 기억해 두고(응급실·병원 찾기·가격 비교 공통) onArea를 부른다.
 * 내 위치가 부정확하면(PC 등) 안내를 띄우고, 부정확한 좌표로는 onCoords를 부르지 않는다
 */
export default function LocationPicker({
  id,
  onArea,
  onCoords,
}: {
  id: string;
  onArea: (a: SavedArea) => void;
  onCoords: (c: Coords) => void;
}) {
  const { state, roughMeters, locate } = useLocate();
  const [areas, setAreas] = useState<AreaSido[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [sido, setSido] = useState("");

  useEffect(() => {
    const saved = loadArea();
    if (saved) setSido(saved.sido); // eslint-disable-line react-hooks/set-state-in-effect -- 저장소 값은 화면이 뜬 뒤에만 읽을 수 있음
    fetchAreas().then(setAreas, () => setFailed(true));
  }, []);

  const current = areas?.find((a) => a.code === sido);

  return (
    <div id={id} className={styles.picker}>
      {state === "rough" && roughMeters !== null && <RoughNotice meters={roughMeters} />}
      <button type="button" className={styles.locate} onClick={() => locate(onCoords)} disabled={state === "locating"}>
        <LocateIcon />
        {state === "locating" ? "위치 확인 중…" : state === "denied" ? "위치 권한이 필요해요" : "내 위치 사용"}
      </button>
      <div className={styles.selects}>
        <label className="sr-only" htmlFor={`${id}-sido`}>
          시도
        </label>
        <select
          id={`${id}-sido`}
          className={styles.select}
          value={sido}
          onChange={(e) => setSido(e.target.value)}
          disabled={!areas}
        >
          <option value="">{areas ? "시도 선택" : failed ? "불러오지 못했어요" : "불러오는 중…"}</option>
          {(areas ?? []).map((a) => (
            <option key={a.code} value={a.code}>
              {a.name}
            </option>
          ))}
        </select>
        <label className="sr-only" htmlFor={`${id}-sggu`}>
          시군구
        </label>
        <select
          id={`${id}-sggu`}
          className={styles.select}
          value=""
          onChange={(e) => {
            const g = current?.sggu.find((x) => x.code === e.target.value);
            if (!g) return;
            const a = {
              sido,
              sggu: g.code,
              lat: g.lat,
              lng: g.lng,
              label: g.name,
            };
            saveArea(a);
            onArea(a);
          }}
          disabled={!current}
        >
          <option value="">시군구 선택</option>
          {(current?.sggu ?? []).map((g) => (
            <option key={g.code} value={g.code}>
              {g.name}
            </option>
          ))}
        </select>
      </div>
      <p className={styles.note}>시군구를 고르면 그 지역 중심에서 가까운 순으로 보여줘요.</p>
    </div>
  );
}

/** 브라우저 위치 오차가 클 때 안내 */
export function RoughNotice({ meters, hint = "아래에서 지역을 골라 주세요." }: { meters: number; hint?: string }) {
  return (
    <p className={styles.rough} role="status">
      위치가 정확하지 않아요 (오차 약 {distance(meters)}). 컴퓨터에서는 위치가 몇 km씩 틀리기도 해요. {hint}
    </p>
  );
}
