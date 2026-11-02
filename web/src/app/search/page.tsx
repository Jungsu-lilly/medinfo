import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { SearchIcon } from "@/components/Icons";
import { manwon } from "@/lib/format";
import { KEYWORDS, POPULAR } from "@/lib/sample-data";
import styles from "../home.module.css";

export const metadata: Metadata = { title: "검색", robots: { index: false } };

export default function SearchPage(props: PageProps<"/search">) {
  return (
    <>
      <BackHeader backHref="/" title="검색" />
      <Suspense fallback={null}>
        <Results searchParams={props.searchParams} />
      </Suspense>
    </>
  );
}

async function Results({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const raw = (await searchParams).q;
  const q = (Array.isArray(raw) ? raw[0] : raw ?? "").trim();
  // 임시: 예시 항목 이름에서 찾기. 항목 검색 API(/api/items?q=)가 생기면 교체
  const pool = [
    ...POPULAR,
    ...KEYWORDS.filter((k) => !POPULAR.some((p) => p.slug === k.slug)).map((k) => ({ slug: k.slug, name: k.label, count: 0, low: 0, high: 0 })),
  ];
  const results = q ? pool.filter((p) => p.name.toLowerCase().includes(q.toLowerCase())) : [];

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
      <section className="section" style={{ paddingTop: 4 }}>
        {q && results.length === 0 && <p className="meta">&quot;{q}&quot;에 맞는 항목이 없어요.</p>}
        <ul>
          {results.map((p) => (
            <li key={p.slug} className={styles.popRow}>
              <Link href={`/price/${p.slug}`} className={styles.popLink}>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{p.name}</span>
                  {p.count > 0 && <span className="meta">병원 {p.count}곳</span>}
                </span>
                {p.count > 0 && (
                  <span className={styles.range}>
                    {manwon(p.low)}
                    <span className={styles.rangeHigh}> ~ {manwon(p.high)}원</span>
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
