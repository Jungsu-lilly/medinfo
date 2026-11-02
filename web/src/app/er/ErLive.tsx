"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronDown, LocateIcon, PhoneIcon } from "@/components/Icons";
import {
  DEFAULT_POINT,
  erQuery,
  INACCURATE_METERS,
  loadArea,
  saveArea,
  type ErArea,
  type ErItem,
  type ErRegion,
  type ErStatus,
} from "@/lib/er";
import { distance, hhmm } from "@/lib/format";
import styles from "./er.module.css";

const REFRESH_MS = 120_000; // 수집 주기와 같은 2분

type Point = { lat: number; lng: number; label: string };

export default function ErLive({ initial }: { initial: ErStatus | null }) {
  const [data, setData] = useState<ErStatus | null>(initial);
  const [error, setError] = useState(initial === null);
  const [point, setPoint] = useState<Point>(DEFAULT_POINT);
  const [onlyOpen, setOnlyOpen] = useState(false);
  const [locating, setLocating] = useState(false);
  /** 기준 위치 고르기 창 */
  const [picking, setPicking] = useState(false);
  const [regions, setRegions] = useState<ErRegion[] | null>(null);
  const [regionsError, setRegionsError] = useState(false);
  const [sido, setSido] = useState("");
  /** 브라우저 위치가 부정확할 때(오차 m). PC는 IP로 추정해서 수 km 틀리기도 한다 */
  const [roughMeters, setRoughMeters] = useState<number | null>(null);
  const pointRef = useRef(point);
  useEffect(() => {
    pointRef.current = point;
  }, [point]);

  const load = useCallback(async (p: Point) => {
    try {
      const res = await fetch(erQuery(p.lat, p.lng));
      if (!res.ok) throw new Error(String(res.status));
      setData((await res.json()) as ErStatus);
      setError(false);
    } catch {
      setError(true); // 이전 데이터는 그대로 두고 오류만 표시
    }
  }, []);

  // 전에 직접 고른 지역이 있으면 그 기준으로 다시 연다
  useEffect(() => {
    const saved = loadArea();
    if (!saved) return;
    const p = { lat: saved.lat, lng: saved.lng, label: saved.label };
    setPoint(p); // eslint-disable-line react-hooks/set-state-in-effect -- 저장소 값은 화면이 뜬 뒤에만 읽을 수 있음
    setSido(saved.sido);
    void load(p);
  }, [load]);

  // 2분마다 갱신. 탭이 백그라운드면 건너뛰고, 다시 보이면 바로 한 번 갱신
  useEffect(() => {
    const tick = () => {
      if (document.visibilityState === "visible") void load(pointRef.current);
    };
    const id = window.setInterval(tick, REFRESH_MS);
    document.addEventListener("visibilitychange", tick);
    return () => {
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [load]);

  const openPicker = async () => {
    setPicking(true);
    if (regions) return;
    try {
      const res = await fetch("/api/er/regions");
      if (!res.ok) throw new Error(String(res.status));
      setRegions((await res.json()) as ErRegion[]);
      setRegionsError(false);
    } catch {
      setRegionsError(true);
    }
  };

  const locateMe = () => {
    if (!("geolocation" in navigator)) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const rough = pos.coords.accuracy > INACCURATE_METERS;
        const p = { lat: pos.coords.latitude, lng: pos.coords.longitude, label: rough ? "내 위치(대략)" : "내 위치" };
        setPoint(p);
        setLocating(false);
        setRoughMeters(rough ? Math.round(pos.coords.accuracy) : null);
        if (rough) void openPicker();
        else setPicking(false);
        void load(p);
      },
      () => setLocating(false),
      // 응급실은 거리가 중요해서 GPS까지 쓴다 (핸드폰). GPS가 없는 PC에선 효과 없음
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
    );
  };

  const pickArea = (area: ErArea) => {
    const p = { lat: area.lat, lng: area.lng, label: area.name };
    setPoint(p);
    setRoughMeters(null);
    setPicking(false);
    saveArea({ sido, sggu: area.code, lat: area.lat, lng: area.lng, label: area.name });
    void load(p);
  };

  const items = (data?.items ?? []).filter((i) => !onlyOpen || ((i.available ?? 0) > 0 && !i.stale));
  const sidoRegion = regions?.find((r) => r.code === sido);

  return (
    <>
      <div className={styles.toolbar}>
        <button
          type="button"
          className={styles.where}
          onClick={() => (picking ? setPicking(false) : void openPicker())}
          aria-expanded={picking}
          aria-controls="er-picker"
        >
          {point.label} 기준 가까운 순
          <ChevronDown />
        </button>
        <label className={styles.toggle}>
          <input type="checkbox" checked={onlyOpen} onChange={(e) => setOnlyOpen(e.target.checked)} />
          병상 있는 곳만
        </label>
      </div>

      {roughMeters !== null && (
        <p className={styles.rough} role="status">
          위치가 정확하지 않아요 (오차 약 {distance(roughMeters)}). 컴퓨터에서는 위치가 몇 km씩 틀리기도 해요. 아래에서
          지역을 골라 주세요.
        </p>
      )}

      {picking && (
        <div id="er-picker" className={styles.picker}>
          <button type="button" className={styles.pickerLocate} onClick={locateMe} disabled={locating}>
            <LocateIcon />
            {locating ? "위치 확인 중…" : "내 위치 사용"}
          </button>
          <div className={styles.pickerSelects}>
            <label className="sr-only" htmlFor="er-sido">
              시도
            </label>
            <select
              id="er-sido"
              className={styles.select}
              value={sido}
              onChange={(e) => setSido(e.target.value)}
              disabled={!regions}
            >
              <option value="">{regions ? "시도 선택" : regionsError ? "불러오지 못했어요" : "불러오는 중…"}</option>
              {(regions ?? []).map((r) => (
                <option key={r.code} value={r.code}>
                  {r.name}
                </option>
              ))}
            </select>
            <label className="sr-only" htmlFor="er-sggu">
              시군구
            </label>
            <select
              id="er-sggu"
              className={styles.select}
              value=""
              onChange={(e) => {
                const area = sidoRegion?.sggu.find((g) => g.code === e.target.value);
                if (area) pickArea(area);
              }}
              disabled={!sidoRegion}
            >
              <option value="">시군구 선택</option>
              {(sidoRegion?.sggu ?? []).map((g) => (
                <option key={g.code} value={g.code}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>
          <p className={styles.pickerNote}>시군구를 고르면 그 지역 중심에서 가까운 순으로 보여줘요.</p>
        </div>
      )}

      {!picking && !point.label.startsWith("내 위치") && (
        <button type="button" className={styles.locate} onClick={locateMe} disabled={locating}>
          <LocateIcon />
          {locating ? "위치 확인 중…" : "내 위치로 다시 찾기"}
        </button>
      )}

      {error && (
        <p className={styles.error} role="alert">
          {data ? "최신 정보를 불러오지 못했어요. 아래는 마지막으로 받은 정보예요." : "응급실 정보를 불러오지 못했어요."}{" "}
          <button type="button" onClick={() => void load(point)}>
            다시 시도
          </button>
        </p>
      )}

      {data && (
        <p className={styles.updated}>
          {data.lastCollectedAt ? `${hhmm(data.lastCollectedAt)} 기준` : "최근 30분 동안 병상 정보가 수집되지 않았어요"}
        </p>
      )}

      <ul className={styles.list}>
        {items.map((item) => (
          <ErRow key={item.hpid} item={item} />
        ))}
        {data && items.length === 0 && <li className={styles.empty}>조건에 맞는 응급실이 없어요.</li>}
      </ul>
    </>
  );
}

function ErRow({ item }: { item: ErItem }) {
  const { available, total } = item;
  const known = available !== null && total !== null && total > 0;
  const full = known && available! <= 0;
  // 음수는 기준 병상보다 환자를 더 받은 상태(정원 초과). 숫자는 0으로 보여주고 문구로 알린다
  const over = known && available! < 0 ? -available! : 0;
  const shown = known ? Math.max(0, available!) : null;
  const pct = known ? Math.max(0, Math.min(100, Math.round((available! / total!) * 100))) : 0;

  let note: { text: string; tone: "normal" | "red" | "amber" };
  if (!known) note = { text: "병상 정보 없음 · 전화로 확인", tone: "normal" };
  else if (item.stale) note = { text: `${item.minutesSinceInput}분째 갱신 안 됨 · 전화 확인 필요`, tone: "amber" };
  else if (over > 0) note = { text: `정원 초과(${over}명) · ${minutesText(item.minutesSinceInput)}`, tone: "red" };
  else if (full) note = { text: `지금 병상 없음 · ${minutesText(item.minutesSinceInput)}`, tone: "red" };
  else note = { text: minutesText(item.minutesSinceInput), tone: "normal" };

  const nameBlock = (
    <>
      <span className={styles.name}>{item.name}</span>
      <span className="meta">
        {item.kind ? `${item.kind} · ` : ""}
        {distance(item.distanceMeters)}
      </span>
    </>
  );

  return (
    <li className={styles.row}>
      <div className={styles.rowTop}>
        {item.hospitalId ? (
          <Link href={`/hospital/${item.hospitalId}`} className={styles.rowMain}>
            {nameBlock}
          </Link>
        ) : (
          <div className={styles.rowMain}>{nameBlock}</div>
        )}
        <div className={styles.beds}>
          <span className={styles.bedsLabel}>남은 병상</span>
          {known ? (
            <span className={styles.bedsNum}>
              <b className={full ? styles.numRed : item.stale ? styles.numStale : styles.numOk}>{shown}</b> / {total}
            </span>
          ) : (
            <span className={styles.bedsNum}>-</span>
          )}
        </div>
      </div>
      <div aria-hidden="true" className={styles.bar}>
        <div className={full ? styles.barRed : styles.barFill} style={{ width: `${pct}%` }} />
      </div>
      <div className={styles.rowBottom}>
        <span className={note.tone === "red" ? styles.noteRed : note.tone === "amber" ? styles.noteAmber : styles.note}>
          {note.text}
        </span>
        {item.erTel && (
          <a href={`tel:${item.erTel}`} className={styles.callBtn}>
            <PhoneIcon size={14} />
            응급실 전화
          </a>
        )}
      </div>
    </li>
  );
}

function minutesText(m: number | null): string {
  if (m === null) return "입력 시각 모름";
  return m === 0 ? "방금 병원 입력" : `${m}분 전 병원 입력`;
}
