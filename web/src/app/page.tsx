import Link from "next/link";
import ErStrip from "@/components/ErStrip";
import { PinIcon, ChevronDown, SearchIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { GUIDES, KEYWORDS, POPULAR } from "@/lib/sample-data";
import { manwon } from "@/lib/format";
import { SITE_NAME } from "@/lib/site";
import styles from "./home.module.css";

export default function HomePage() {
  return (
    <>
      <header className={styles.header}>
        <Link href="/" className={styles.logo}>
          {SITE_NAME}
        </Link>
        <button type="button" className={styles.region}>
          <PinIcon />
          서울 마포구
          <ChevronDown />
        </button>
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
          <Link key={k.slug} href={`/price/${k.slug}`} className="chip">
            {k.label}
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
        <ol>
          {POPULAR.map((p, i) => (
            <li key={p.slug} className={styles.popRow}>
              <Link href={`/price/${p.slug}`} className={styles.popLink}>
                <span className={styles.rank}>{i + 1}</span>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{p.name}</span>
                  <span className="meta">병원 {p.count}곳</span>
                </span>
                <span className={styles.popPrice}>
                  <span className={styles.range}>
                    {manwon(p.low)}
                    <span className={styles.rangeHigh}> ~ {manwon(p.high)}원</span>
                  </span>
                  <span className={styles.rangeLabel}>최저 ~ 최고</span>
                </span>
              </Link>
            </li>
          ))}
        </ol>
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
