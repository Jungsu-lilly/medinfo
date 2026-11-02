import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { SearchIcon } from "@/components/Icons";
import PriceLink from "@/components/PriceLink";
import { itemHref, searchItems } from "@/lib/api";
import { manwon } from "@/lib/format";
import { KEYWORDS } from "@/lib/guides";
import styles from "../home.module.css";

export const metadata: Metadata = { title: "검색", robots: { index: false } };

type Props = PageProps<"/search">;

export default function SearchPage(props: Props) {
  return (
    <>
      <BackHeader backHref="/" title="검색" />
      <Suspense fallback={null}>
        <Results searchParams={props.searchParams} />
      </Suspense>
    </>
  );
}

async function Results({ searchParams }: { searchParams: Props["searchParams"] }) {
  const raw = (await searchParams).q;
  const q = (Array.isArray(raw) ? raw[0] : (raw ?? "")).trim();
  const { data, error } = q ? await searchItems(q) : { data: null, error: false };

  return (
    <>
      <form role="search" action="/search" className={styles.searchWrap} style={{ paddingTop: 12 }}>
        <label htmlFor="q" className="sr-only">
          비급여 항목 검색
        </label>
        <div className={styles.search}>
          <SearchIcon size={20} />
          <input id="q" name="q" type="search" defaultValue={q} placeholder="MRI, 대상포진 예방접종, 1인실" />
        </div>
      </form>

      {!q && (
        <nav aria-label="자주 찾는 검색어" className={styles.keywords}>
          {KEYWORDS.map((k) => (
            <Link key={k} href={`/search?q=${encodeURIComponent(k)}`} className="chip">
              {k}
            </Link>
          ))}
        </nav>
      )}

      <section className="section" style={{ paddingTop: 4 }}>
        {error && <p className="meta">검색하지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
        {data && data.length === 0 && <p className="meta">&quot;{q}&quot;에 맞는 항목이 없어요.</p>}
        {data && data.length > 0 && <p className="meta">전국 병원급 기준 · 병원이 많은 순</p>}
        <ul>
          {(data ?? []).map((p) => (
            <li key={p.vaccine ?? p.code} className={styles.popRow}>
              <PriceLink href={itemHref(p)} className={styles.popLink}>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{p.name}</span>
                  <span className="meta">
                    {p.vaccine && p.group ? `${p.group} · ` : ""}병원 {p.stats.hospitalCount}곳
                  </span>
                </span>
                <span className={styles.popPrice}>
                  <span className={styles.range}>
                    {manwon(p.stats.low)}
                    {p.stats.high > p.stats.low && <span className={styles.rangeHigh}> ~ {manwon(p.stats.high)}</span>}
                    <span className={styles.rangeHigh}>원</span>
                  </span>
                </span>
              </PriceLink>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
