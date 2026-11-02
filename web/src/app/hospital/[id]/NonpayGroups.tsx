"use client";

import { useState } from "react";
import { ChevronDown, ChevronUp, SearchIcon } from "@/components/Icons";
import type { HospitalItem, ItemGroup } from "@/lib/api";
import { diffFromMedian, won } from "@/lib/format";
import styles from "./hospital.module.css";

const PREVIEW = 5;
const SEARCH_LIMIT = 50;

export default function NonpayGroups({
  groups,
  itemCount,
  detailed,
}: {
  groups: ItemGroup[];
  itemCount: number;
  detailed: boolean;
}) {
  const [open, setOpen] = useState<string | null>(groups[0]?.name ?? null);
  const [all, setAll] = useState<Set<string>>(new Set());
  const [query, setQuery] = useState("");

  const q = query.trim().toLowerCase();
  const matches = q
    ? groups.flatMap((g) => g.items.filter((i) => i.name.toLowerCase().includes(q) || (i.std ?? "").toLowerCase().includes(q)))
    : [];

  return (
    <section aria-labelledby="np" className={styles.nonpay}>
      <div className="section-head" style={{ marginBottom: 12 }}>
        <h2 id="np" className="section-title">
          비급여 가격
        </h2>
        <span className="meta">{itemCount}개 항목</span>
      </div>

      {!detailed && itemCount > 0 && (
        <p className={styles.notice}>병원별 세부 항목을 준비하고 있어요. 지금은 표준 항목 기준 가장 낮은 가격을 보여드려요.</p>
      )}

      {itemCount === 0 ? (
        <p className={styles.empty}>이 병원이 신고한 비급여 가격이 없어요.</p>
      ) : (
        <>
          <label htmlFor="hq" className="sr-only">
            이 병원 항목 검색
          </label>
          <div className={styles.search}>
            <SearchIcon size={18} color="#5b616b" />
            <input
              id="hq"
              type="search"
              placeholder="이 병원에서 찾기 (예: 초음파)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>

          {q ? (
            <ul>
              {matches.length === 0 && <li className={styles.empty}>&quot;{query.trim()}&quot;에 맞는 항목이 없어요.</li>}
              {matches.slice(0, SEARCH_LIMIT).map((i, idx) => (
                <Item key={`${i.code}-${idx}`} item={i} />
              ))}
              {matches.length > SEARCH_LIMIT && (
                <li className={styles.empty}>
                  {matches.length}개 중 {SEARCH_LIMIT}개만 보여드려요. 검색어를 더 자세히 입력해 보세요.
                </li>
              )}
            </ul>
          ) : (
            groups.map((g) => {
              const expanded = open === g.name;
              const showAll = all.has(g.name);
              const items = showAll ? g.items : g.items.slice(0, PREVIEW);
              return (
                <div key={g.name}>
                  <button
                    type="button"
                    aria-expanded={expanded}
                    className={expanded ? styles.groupOpen : styles.group}
                    onClick={() => setOpen(expanded ? null : g.name)}
                  >
                    <span className={styles.groupName}>{g.name}</span>
                    <span className="meta" style={{ marginRight: 6 }}>
                      {g.items.length}개
                    </span>
                    {expanded ? <ChevronUp size={18} /> : <ChevronDown size={18} color="#6b7079" />}
                  </button>
                  {expanded && (
                    <>
                      <ul>
                        {items.map((i, idx) => (
                          <Item key={`${i.code}-${idx}`} item={i} />
                        ))}
                      </ul>
                      {!showAll && g.items.length > PREVIEW && (
                        <button type="button" className={styles.more} onClick={() => setAll(new Set(all).add(g.name))}>
                          {g.name} {g.items.length - PREVIEW}개 더 보기
                        </button>
                      )}
                    </>
                  )}
                </div>
              );
            })
          )}
        </>
      )}
    </section>
  );
}

function Item({ item }: { item: HospitalItem }) {
  const d = item.regionMedian !== null ? diffFromMedian(item.price, item.regionMedian) : null;
  return (
    <li className={styles.item}>
      <span className={styles.itemMain}>
        <span className={styles.itemName}>{item.name}</span>
        {item.std && item.std !== item.name && <span className={styles.itemStd}>{item.std}</span>}
      </span>
      <span className={styles.itemPrice}>
        <span className={styles.price}>{won(item.price)}</span>
        {d && <span className={d.lower ? styles.cmpLow : styles.cmp}>{d.text.replace("중간", "지역 중간")}</span>}
      </span>
    </li>
  );
}
