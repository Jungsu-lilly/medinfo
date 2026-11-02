"use client";

import { useState } from "react";

/**
 * 내 위치 기억하기 (브라우저 안에서만, 서버로 따로 보내지 않음).
 * 한 번 "내 위치"를 고르면 30분 동안 다른 항목을 열어도 내 위치 기준으로 보여준다.
 * 저장이 막힌 브라우저(사생활 보호 모드 등)에서는 그냥 기억 없이 동작.
 */
const KEY = "medinfo.location";
const TTL_MS = 30 * 60 * 1000;

export type Coords = { lat: number; lng: number };

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

export function coordsQuery(c: Coords): string {
  return `lat=${c.lat.toFixed(4)}&lng=${c.lng.toFixed(4)}`;
}

/** 위치 권한을 받아 좌표를 넘겨준다. 성공하면 기억까지 */
export function useLocate() {
  const [state, setState] = useState<"idle" | "locating" | "denied">("idle");

  const locate = (onFound: (c: Coords) => void) => {
    if (!("geolocation" in navigator)) return setState("denied");
    setState("locating");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const c = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        saveLocation(c);
        setState("idle");
        onFound(c);
      },
      () => setState("denied"),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 },
    );
  };

  return { state, locate };
}
