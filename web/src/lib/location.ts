"use client";

import { useCallback, useState } from "react";
import { beforeLocationPrompt } from "./locationNotice";

/**
 * 기준 위치 (브라우저 안에서만 기억, 서버로 따로 보내지 않음). 두 가지:
 *  - 내 위치(좌표): 30분 동안 기억. 다른 항목을 열어도 내 위치 기준으로 보여준다
 *  - 직접 고른 지역(시군구): 계속 기억. 응급실·병원 찾기·가격 비교가 같이 쓴다
 * 저장이 막힌 브라우저(사생활 보호 모드 등)에서는 그냥 기억 없이 동작.
 */
const KEY = "medinfo.location";
const AREA_KEY = "medinfo.area";
const TTL_MS = 30 * 60 * 1000;

/**
 * 브라우저 위치 오차(m)가 이보다 크면 부정확한 위치로 본다.
 * PC(유선)는 GPS·와이파이 정보가 없어 IP로 추정 → 오차 수 km
 */
export const INACCURATE_METERS = 1000;

export type Coords = { lat: number; lng: number };

/** GET /api/areas (Area.java): 시도·시군구 + 시군구 중심 좌표 */
export type AreaSggu = { code: string; name: string; lat: number; lng: number };
export type AreaSido = { code: string; name: string; sggu: AreaSggu[] };
export type SavedArea = {
  sido: string;
  sggu: string;
  lat: number;
  lng: number;
  label: string;
};

export function saveLocation(c: Coords) {
  try {
    localStorage.setItem(KEY, JSON.stringify({ ...c, at: Date.now() }));
  } catch {
    /* 저장 못 해도 동작에는 문제없음 */
  }
}

export function loadLocation(): Coords | null {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const v = JSON.parse(raw) as { lat: number; lng: number; at: number };
    if (Date.now() - v.at > TTL_MS) return null;
    return { lat: v.lat, lng: v.lng };
  } catch {
    return null;
  }
}

function clearLocation() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* 무시 */
  }
}

/** 지역을 직접 고르면 30분짜리 내 위치보다 우선하게, 기억해 둔 좌표는 지운다 */
export function saveArea(a: SavedArea) {
  try {
    localStorage.setItem(AREA_KEY, JSON.stringify(a));
  } catch {
    /* 저장 못 해도 동작에는 문제없음 */
  }
  clearLocation();
}

export function loadArea(): SavedArea | null {
  try {
    const raw = localStorage.getItem(AREA_KEY);
    if (!raw) return null;
    const v = JSON.parse(raw) as SavedArea;
    return typeof v.lat === "number" && typeof v.lng === "number" && v.label && v.sido ? v : null;
  } catch {
    return null;
  }
}

export function coordsQuery(c: Coords): string {
  return `lat=${c.lat.toFixed(4)}&lng=${c.lng.toFixed(4)}`;
}

/** 지역 기준 주소 뒷부분 (병원 찾기·응급실처럼 좌표로 찾는 화면용). area = 화면에 보일 이름 */
export function areaQuery(a: SavedArea): string {
  return `${coordsQuery(a)}&area=${encodeURIComponent(a.label)}`;
}

let areasCache: Promise<AreaSido[]> | null = null;

/** 시도·시군구 목록. 한 번 받으면 이 페이지 안에서는 다시 안 부른다 */
export function fetchAreas(): Promise<AreaSido[]> {
  areasCache ??= fetch("/api/areas").then((res) => {
    if (!res.ok) throw new Error(String(res.status));
    return res.json() as Promise<AreaSido[]>;
  });
  areasCache.catch(() => {
    areasCache = null; // 실패는 기억하지 않고 다음에 다시 시도
  });
  return areasCache;
}

export type LocateState = "idle" | "locating" | "denied" | "rough";

/**
 * 위치 권한을 받아 좌표를 넘겨준다. 정확한 위치면 30분 기억까지.
 * 오차가 크면(PC 등) state가 "rough"가 되고 roughMeters에 오차가 남는다.
 *   allowRough: true면 부정확해도 onFound를 부른다(응급실처럼 대략이라도 쓰는 화면), 기본은 부르지 않음
 */
export function useLocate() {
  const [state, setState] = useState<LocateState>("idle");
  const [roughMeters, setRoughMeters] = useState<number | null>(null);

  const locate = (onFound: (c: Coords, roughMeters: number | null) => void, opts: { allowRough?: boolean } = {}) => {
    if (!("geolocation" in navigator)) return setState("denied");
    setState("locating");

    const request = () =>
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const c = { lat: pos.coords.latitude, lng: pos.coords.longitude };
          const rough = pos.coords.accuracy > INACCURATE_METERS ? Math.round(pos.coords.accuracy) : null;
          setRoughMeters(rough);
          setState(rough === null ? "idle" : "rough");
          if (rough === null) saveLocation(c);
          if (rough === null || opts.allowRough) onFound(c, rough);
        },
        () => setState("denied"),
        // 핸드폰은 GPS까지 쓴다. GPS가 없는 PC에선 효과 없음
        { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
      );

    // 카카오톡 인앱 브라우저면 처음 한 번 안내 후 권한을 묻는다 (취소하면 아무것도 안 함)
    void beforeLocationPrompt().then((ok) => (ok ? request() : setState("idle")));
  };

  /** 다른 방법(지역 선택 등)으로 위치를 정했을 때 안내 상태 지우기 */
  const reset = useCallback(() => {
    setState("idle");
    setRoughMeters(null);
  }, []);

  return { state, roughMeters, locate, reset };
}
