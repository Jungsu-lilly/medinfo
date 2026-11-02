/** Spring GET /api/er 응답 (ErStatus.java와 같은 모양) */
export type ErItem = {
  hpid: string;
  name: string;
  kind: string | null;
  erTel: string | null;
  addr: string | null;
  lat: number;
  lng: number;
  distanceMeters: number;
  hospitalId: number | null;
  available: number | null;
  total: number | null;
  inputAt: string | null;
  minutesSinceInput: number | null;
  stale: boolean;
};

export type ErStatus = {
  generatedAt: string;
  lastCollectedAt: string | null;
  items: ErItem[];
};

/** 위치 권한이 없을 때 기준점: 서울 마포구청 (백엔드 기본값과 같음) */
export const DEFAULT_POINT = { lat: 37.5663, lng: 126.9019, label: "마포구" };

export function erQuery(lat: number, lng: number, limit = 30): string {
  return `/api/er?lat=${lat.toFixed(5)}&lng=${lng.toFixed(5)}&limit=${limit}`;
}

/** 서버 컴포넌트에서 Spring을 직접 부를 때 (브라우저는 /api 상대 경로 → next.config rewrites) */
export async function fetchErFromServer(lat: number, lng: number): Promise<ErStatus | null> {
  const base = process.env.BACKEND_URL ?? "http://localhost:8080";
  try {
    const res = await fetch(`${base}${erQuery(lat, lng)}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(3000), // 백엔드가 느려도 화면은 3초 안에 (스레드 고갈 교훈과 같은 원칙)
    });
    if (!res.ok) return null;
    return (await res.json()) as ErStatus;
  } catch {
    return null;
  }
}
