"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import AdSlot from "@/components/AdSlot";
import { diffFromMedian, distance, won } from "@/lib/format";
import type { PriceRow } from "@/lib/sample-data";
import styles from "./price.module.css";

const TYPES = ["전체", "상급종합", "종합병원", "병원"] as const;
const PAGE = 7;
const AD_AFTER = 3; // 광고는 3번째 결과 뒤 고정 칸. 병원 순위에는 섞지 않는다

export default function PriceList({ rows, median }: { rows: PriceRow[]; median: number }) {
  const [sort, setSort] = useState<"price" | "distance">("price");
  const [type, setType] = useState<(typeof TYPES)[number]>("전체");
  const [shown, setShown] = useState(PAGE);

  const sorted = useMemo(() => {
    const filtered = type === "전체" ? rows : rows.filter((r) => r.type === type);
    return [...filtered].sort((a, b) => (sort === "price" ? a.price - b.price : a.distanceM - b.distanceM));
  }, [rows, sort, type]);

  const min = Math.min(...rows.map((r) => r.price));
  const max = Math.max(...rows.map((r) => r.price));
  const visible = sorted.slice(0, shown);
  const rest = sorted.length - visible.length;

  return (
    <>
      <div className={styles.controls}>
        <div role="group" aria-label="정렬" className={styles.segment}>
          <button type="button" aria-pressed={sort === "price"} onClick={() => setSort("price")}>
            가격 낮은 순
          </button>
          <button type="button" aria-pressed={sort === "distance"} onClick={() => setSort("distance")}>
            가까운 순
          </button>
        </div>
        <div role="group" aria-label="병원 종류" className={styles.chips}>
          {TYPES.map((t) => (
            <button
              key={t}
              type="button"
              className="chip"
              aria-pressed={type === t}
              onClick={() => {
                setType(t);
                setShown(PAGE);
              }}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      <ol className={styles.list}>
        {visible.map((r, i) => {
          const diff = diffFromMedian(r.price, median);
          const tag = r.price === min ? "최저" : r.price === max ? "최고" : "";
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
                    {r.type} · {distance(r.distanceM)}
                  </span>
                </span>
                <span className={styles.rowPrice}>
                  <span className={styles.price}>{won(r.price)}</span>
                  <span className={diff.lower ? styles.diffLow : styles.diff}>{diff.text}</span>
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
            {Math.min(rest, PAGE)}곳 더 보기
          </button>
        </div>
      )}
    </>
  );
}
