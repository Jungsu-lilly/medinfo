"use client";

import { useState } from "react";
import { ChevronDown, ChevronUp, SearchIcon } from "@/components/Icons";
import { diffFromMedian, won } from "@/lib/format";
import type { HospitalGroup } from "@/lib/sample-data";
import styles from "./hospital.module.css";

const PREVIEW = 4;

export default function NonpayGroups({ groups, itemCount }: { groups: HospitalGroup[]; itemCount: number }) {
  const [open, setOpen] = useState<string | null>(groups[0]?.name ?? null);
  const [query, setQuery] = useState("");

  const q = query.trim();
  const matches = q
    ? groups.flatMap((g) => (g.items ?? []).filter((i) => i.name.includes(q) || i.std.includes(q)))
    : [];

  return (
    <section aria-labelledby="np" className={styles.nonpay}>
      <div className="section-head" style={{ marginBottom: 12 }}>
        <h2 id="np" className="section-title">
          비급여 가격
        </h2>
        <span className="meta">{itemCount}개 항목</span>
      </div>

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
        <ul className={styles.items}>
          {matches.length === 0 && <li className={styles.empty}>&quot;{q}&quot;에 맞는 항목이 없어요.</li>}
          {matches.map((i) => (
            <Item key={i.name} {...i} />
          ))}
        </ul>
      ) : (
        groups.map((g) => {
          const expanded = open === g.name;
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
                  {g.count}개
                </span>
                {expanded ? <ChevronUp size={18} /> : <ChevronDown size={18} color="#6b7079" />}
              </button>
              {expanded && (
                <>
                  <ul className={styles.items}>
                    {(g.items ?? []).slice(0, PREVIEW).map((i) => (
                      <Item key={i.name} {...i} />
                    ))}
                    {!g.items && <li className={styles.empty}>예시 데이터에는 이 분류 항목이 없어요.</li>}
                  </ul>
                  {g.count > PREVIEW && g.items && (
                    <button type="button" className={styles.more}>
                      {g.name} {g.count - PREVIEW}개 더 보기
                    </button>
                  )}
                </>
              )}
            </div>
          );
        })
      )}
    </section>
  );
}

function Item({ name, std, price, regionMedian }: { name: string; std: string; price: number; regionMedian: number }) {
  const d = diffFromMedian(price, regionMedian);
  return (
    <li className={styles.item}>
      <span className={styles.itemMain}>
        <span className={styles.itemName}>{name}</span>
        <span className={styles.itemStd}>{std}</span>
      </span>
      <span className={styles.itemPrice}>
        <span className={styles.price}>{won(price)}</span>
        <span className={d.lower ? styles.cmpLow : styles.cmp}>{d.text.replace("중간", "지역 중간")}</span>
      </span>
    </li>
  );
}
