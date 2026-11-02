"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import AdSlot from "@/components/AdSlot";
import type { PriceRow } from "@/lib/api";
import { diffFromMedian, distance, won } from "@/lib/format";
import styles from "./price.module.css";

const PAGE = 10;
const AD_AFTER = 3; // 광고는 3번째 결과 뒤 고정 칸. 병원 순위에는 섞지 않는다
/** 종류 필터 순서. 데이터에 있는 종류만 보여준다 */
const KIND_ORDER = ["상급종합", "종합병원", "병원", "요양병원", "치과병원", "한방병원", "정신병원"];

export default function PriceList({ rows, median, byDistance }: { rows: PriceRow[]; median: number; byDistance: boolean }) {
  const [sort, setSort] = useState<"price" | "distance">("price");
  const [kind, setKind] = useState("전체");
  const [shown, setShown] = useState(PAGE);

  const kinds = useMemo(() => {
    const present = new Set(rows.map((r) => r.kind));
    const ordered = KIND_ORDER.filter((k) => present.has(k));
    const rest = [...present].filter((k) => !KIND_ORDER.includes(k)).sort();
    const all = [...ordered, ...rest];
    return all.length > 1 ? ["전체", ...all] : [];
  }, [rows]);

  const sorted = useMemo(() => {
    const filtered = kind === "전체" ? rows : rows.filter((r) => r.kind === kind);
    return [...filtered].sort((a, b) =>
      sort === "distance" ? (a.distanceMeters ?? 0) - (b.distanceMeters ?? 0) : a.minPrice - b.minPrice,
    );
  }, [rows, sort, kind]);

  const min = rows.reduce((m, r) => Math.min(m, r.minPrice), Infinity);
  const max = rows.reduce((m, r) => Math.max(m, r.minPrice), 0);
  const visible = sorted.slice(0, shown);
  const rest = sorted.length - visible.length;

  return (
    <>
      <div className={styles.controls}>
        {byDistance && (
          <div role="group" aria-label="정렬" className={styles.segment}>
            <button type="button" aria-pressed={sort === "price"} onClick={() => setSort("price")}>
              가격 낮은 순
            </button>
            <button type="button" aria-pressed={sort === "distance"} onClick={() => setSort("distance")}>
              가까운 순
            </button>
          </div>
        )}
        {kinds.length > 0 && (
          <div role="group" aria-label="병원 종류" className={styles.chips}>
            {kinds.map((k) => (
              <button
                key={k}
                type="button"
                className="chip"
                aria-pressed={kind === k}
                onClick={() => {
                  setKind(k);
                  setShown(PAGE);
                }}
              >
                {k}
              </button>
            ))}
          </div>
        )}
      </div>

      <ol className={styles.list}>
        {visible.map((r, i) => {
          const diff = diffFromMedian(r.minPrice, median);
          const tag = rows.length > 1 && r.minPrice === min ? "최저" : rows.length > 1 && r.minPrice === max ? "최고" : "";
          return (
            <li key={r.hospitalId} className={styles.rowItem}>
              {i === AD_AFTER && <AdSlot />}
              <Link href={`/hospital/${r.hospitalId}`} className="row-link">
                <span className={styles.rowMain}>
                  <span className={styles.rowName}>
                    {r.name}
                    {tag && <span className={tag === "최저" ? styles.tagLow : styles.tagHigh}>{tag}</span>}
                  </span>
                  <span className="meta">
                    {r.kind}
                    {r.distanceMeters !== null && ` · ${distance(r.distanceMeters)}`}
                  </span>
                </span>
                <span className={styles.rowPrice}>
                  <span className={styles.price}>
                    {won(r.minPrice)}
                    {r.maxPrice > r.minPrice && <span className={styles.rangeMax}> ~ {won(r.maxPrice)}</span>}
                  </span>
                  {rows.length > 2 && <span className={diff.lower ? styles.diffLow : styles.diff}>{diff.text}</span>}
                </span>
              </Link>
            </li>
          );
        })}
      </ol>
      {visible.length > 0 && visible.length <= AD_AFTER && <AdSlot />}
      {visible.length === 0 && <p className={styles.empty}>이 조건에 맞는 병원이 없어요.</p>}

      {rest > 0 && (
        <div style={{ padding: 16 }}>
          <button type="button" className={`btn-outline ${styles.more}`} onClick={() => setShown((s) => s + PAGE)}>
            {Math.min(rest, PAGE)}곳 더 보기 ({rest}곳 남음)
          </button>
        </div>
      )}
    </>
  );
}
