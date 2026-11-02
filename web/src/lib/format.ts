/** 320000 → "320,000원" */
export function won(n: number): string {
  return `${n.toLocaleString("ko-KR")}원`;
}

/** 320000 → "32만", 482500 → "48만", 65000 → "6.5만" (10만 이상은 만 단위 반올림) */
export function manwon(n: number): string {
  const v = n / 10000;
  return `${v >= 10 ? Math.round(v) : Math.round(v * 10) / 10}만`;
}

/** 850 → "850m", 1234 → "1.2km" */
export function distance(m: number): string {
  return m < 1000 ? `${m}m` : `${(m / 1000).toFixed(1)}km`;
}

/** 중간값 대비 차이 문구. 높은 가격도 빨강으로 겁주지 않는다(시안 원칙) */
export function diffFromMedian(price: number, median: number): { text: string; lower: boolean } {
  const d = price - median;
  if (Math.abs(d) < 10000) return { text: "중간 가격", lower: false };
  const amount = manwon(Math.abs(d));
  return d < 0 ? { text: `중간보다 ${amount}원 낮음`, lower: true } : { text: `중간보다 ${amount}원 높음`, lower: false };
}

export function hhmm(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false, timeZone: "Asia/Seoul" });
}
