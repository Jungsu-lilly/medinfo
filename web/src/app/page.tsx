import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";
import ErStrip from "@/components/ErStrip";
import { SearchIcon } from "@/components/Icons";
import PriceLink from "@/components/PriceLink";
import SiteFooter from "@/components/SiteFooter";
import { DEFAULT_SIDO, getFeatured, itemHref } from "@/lib/api";
import { manwon } from "@/lib/format";
import { GUIDES, KEYWORDS } from "@/lib/guides";
import { SITE_NAME } from "@/lib/site";
import styles from "./home.module.css";

export default function HomePage() {
  return (
    <>
      <header className={styles.header}>
        <Link href="/" className={styles.logo}>
          {SITE_NAME}
        </Link>
        <span className="meta">병원급 비급여 가격</span>
      </header>

      <form role="search" action="/search" className={styles.searchWrap}>
        <label htmlFor="q" className="sr-only">
          비급여 항목 검색
        </label>
        <div className={styles.search}>
          <SearchIcon size={20} />
          <input id="q" name="q" type="search" placeholder="MRI, 대상포진 예방접종, 1인실" autoComplete="off" />
        </div>
      </form>

      <nav aria-label="자주 찾는 검색어" className={styles.keywords}>
        {KEYWORDS.map((k) => (
          <Link key={k} href={`/search?q=${encodeURIComponent(k)}`} className="chip">
            {k}
          </Link>
        ))}
      </nav>

      <ErStrip />

      <div className="band" />

      <section aria-labelledby="popular" className="section">
        <div className="section-head">
          <h2 id="popular" className="section-title">
            많이 찾는 비급여
          </h2>
          <span className="meta">서울 · 병원급</span>
        </div>
        {/* 가격 목록만 요청 때 채운다. 페이지 틀은 빌드 때 만들어 둠 */}
        <Suspense fallback={<p className={styles.loading}>불러오는 중…</p>}>
          <Popular />
        </Suspense>
      </section>

      <div className="band" style={{ marginTop: 12 }} />

      <section aria-labelledby="guide" className="section" style={{ paddingBottom: 4 }}>
        <h2 id="guide" className="section-title">
          알고 가면 덜 내는 법
        </h2>
        <ul>
          {GUIDES.map((g) => (
            <li key={g.slug} className={styles.popRow}>
              <Link href={`/guide/${g.slug}`} className={styles.guideLink}>
                <span className={styles.guideTitle}>{g.title}</span>
                <span className="meta">가이드 · {g.minutes}분</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <SiteFooter />
    </>
  );
}

async function Popular() {
  await connection();
  const { data, error } = await getFeatured(DEFAULT_SIDO, 8);
  if (error) return <p className={styles.loading}>가격 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>;
  if (!data || data.length === 0) return <p className={styles.loading}>가격 정보를 준비하고 있어요.</p>;
  return (
    <ol>
      {data.map((p, i) => (
        <li key={p.vaccine ?? p.code} className={styles.popRow}>
          <PriceLink href={itemHref(p)} className={styles.popLink}>
            <span className={styles.rank}>{i + 1}</span>
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
              <span className={styles.rangeLabel}>{p.stats.high > p.stats.low ? "최저 ~ 최고" : "가격"}</span>
            </span>
          </PriceLink>
        </li>
      ))}
    </ol>
  );
}
