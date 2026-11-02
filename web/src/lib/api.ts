import { cacheLife } from "next/cache";

/**
 * Spring 가격·병원 조회 API (PriceApiController.java · ApiModels.java와 같은 모양).
 * 서버 컴포넌트에서만 부른다. 브라우저에서 부를 땐 /api 상대 경로(next.config rewrites).
 */

export type Stats = { hospitalCount: number; low: number; median: number; high: number };
export type ItemSummary = { code: string; name: string; fullName: string; group: string | null; stats: Stats };
export type ItemInfo = { code: string; name: string; fullName: string; path: string[] };
export type RegionRef = { sido: string; sidoName: string | null; sggu: string | null; sgguName: string | null };
export type PointRef = { lat: number; lng: number; radiusMeters: number };
export type PriceRow = {
  hospitalId: number;
  name: string;
  kind: string;
  minPrice: number;
  maxPrice: number;
  distanceMeters: number | null;
};
export type PriceList = {
  item: ItemInfo;
  region: RegionRef | null;
  point: PointRef | null;
  stats: Stats;
  rows: PriceRow[];
};
export type HospitalItem = { code: string; name: string; std: string | null; price: number; regionMedian: number | null };
export type ItemGroup = { name: string; items: HospitalItem[] };
export type HospitalPage = {
  id: number;
  name: string;
  kind: string;
  addr: string | null;
  tel: string | null;
  homepage: string | null;
  nonpayUrl: string | null;
  sidoName: string;
  sgguName: string;
  lat: number | null;
  lng: number | null;
  detailed: boolean;
  itemCount: number;
  groups: ItemGroup[];
};
export type HospitalListItem = { id: number; name: string; kind: string; addr: string | null; distanceMeters: number | null };
export type Region = { code: string; name: string; sggu: { code: string; name: string }[] };

export const DEFAULT_SIDO = "110000"; // 서울

const BASE = process.env.BACKEND_URL ?? "http://localhost:8080";

/**
 * Next 서버 캐시에 담아 같은 요청이 몰려도 Spring은 10분에 한 번만 부른다 (데이터는 월 1회 적재).
 * 404는 "없음"으로 캐시하고, 그 밖의 실패는 throw → 캐시되지 않아 다음 요청에서 다시 시도한다.
 */
async function getJson<T>(path: string): Promise<T | null> {
  "use cache";
  cacheLife({ stale: 300, revalidate: 600, expire: 3600 });
  const res = await fetch(`${BASE}${path}`, { signal: AbortSignal.timeout(3000) });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`API ${res.status} ${path}`);
  return (await res.json()) as T;
}

/** 실패해도 화면은 그리도록: data가 null이면 error로 "없음"과 "실패"를 구분 */
async function attempt<T>(path: string): Promise<{ data: T | null; error: boolean }> {
  try {
    return { data: await getJson<T>(path), error: false };
  } catch {
    return { data: null, error: true };
  }
}

function query(params: Record<string, string | number | null | undefined>): string {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined && v !== "") q.set(k, String(v));
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

export const getFeatured = (sido = DEFAULT_SIDO, limit = 8) =>
  attempt<ItemSummary[]>(`/api/items/featured${query({ sido, limit })}`);

export const searchItems = (q: string) => attempt<ItemSummary[]>(`/api/items${query({ q })}`);

export type PriceQuery = { sido?: string; sggu?: string; lat?: number; lng?: number };
export const getPrices = (code: string, p: PriceQuery) =>
  attempt<PriceList>(
    `/api/prices/${encodeURIComponent(code)}${query({
      sido: p.sido,
      sggu: p.sggu,
      lat: p.lat?.toFixed(4),
      lng: p.lng?.toFixed(4),
    })}`,
  );

export const getHospital = (id: string) => attempt<HospitalPage>(`/api/hospitals/${encodeURIComponent(id)}`);

export const getHospitals = (p: { q?: string; lat?: number; lng?: number }) =>
  attempt<HospitalListItem[]>(`/api/hospitals${query({ q: p.q, lat: p.lat?.toFixed(4), lng: p.lng?.toFixed(4) })}`);

export const getRegions = () => attempt<Region[]>(`/api/regions`);

/** 주소창 값 정리: 숫자 6자리 지역 코드만 */
export function regionCode(v: string | string[] | undefined): string | undefined {
  const s = Array.isArray(v) ? v[0] : v;
  return s && /^\d{6}$/.test(s) ? s : undefined;
}

export function coord(v: string | string[] | undefined): number | undefined {
  const s = Array.isArray(v) ? v[0] : v;
  const n = s === undefined ? NaN : Number(s);
  return Number.isFinite(n) ? n : undefined;
}
