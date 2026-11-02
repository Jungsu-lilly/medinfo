"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronDown, LocateIcon, PhoneIcon } from "@/components/Icons";
import { DEFAULT_POINT, erQuery, type ErItem, type ErStatus } from "@/lib/er";
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

  const locateMe = () => {
    if (!("geolocation" in navigator)) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const p = { lat: pos.coords.latitude, lng: pos.coords.longitude, label: "내 위치" };
        setPoint(p);
        setLocating(false);
        void load(p);
      },
      () => setLocating(false),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 },
    );
  };

  const items = (data?.items ?? []).filter((i) => !onlyOpen || ((i.available ?? 0) > 0 && !i.stale));

  return (
    <>
      <div className={styles.toolbar}>
        <button type="button" className={styles.where} onClick={locateMe} disabled={locating}>
          {point.label} 기준 가까운 순
          <ChevronDown />
        </button>
        <label className={styles.toggle}>
          <input type="checkbox" checked={onlyOpen} onChange={(e) => setOnlyOpen(e.target.checked)} />
          병상 있는 곳만
        </label>
      </div>

      {point.label !== "내 위치" && (
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
