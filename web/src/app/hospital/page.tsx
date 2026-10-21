import type { Metadata } from "next";
import Link from "next/link";
import { SearchIcon } from "@/components/Icons";
import SampleBanner from "@/components/SampleBanner";
import SiteFooter from "@/components/SiteFooter";
import { distance } from "@/lib/format";
import { getPriceItem } from "@/lib/sample-data";
import styles from "../home.module.css";

export const metadata: Metadata = { title: "병원 찾기" };

export default function HospitalIndexPage() {
  const nearby = [...getPriceItem("mri-knee").rows].sort((a, b) => a.distanceM - b.distanceM);
  return (
    <>
      <header className={styles.header}>
        <h1 style={{ fontSize: 20, fontWeight: 700 }}>병원 찾기</h1>
        <span className="meta">마포구 기준</span>
      </header>
      <SampleBanner />

      <form role="search" action="/search" className={styles.searchWrap} style={{ paddingTop: 12 }}>
        <label htmlFor="hq" className="sr-only">
          병원 이름 검색
        </label>
        <div className={styles.search}>
          <SearchIcon size={20} />
          <input id="hq" name="q" type="search" placeholder="병원 이름" autoComplete="off" />
        </div>
      </form>

      <section aria-labelledby="near" className="section" style={{ paddingTop: 8 }}>
        <h2 id="near" className="section-title">
          가까운 병원
        </h2>
        <ul>
          {nearby.map((h) => (
            <li key={h.hospitalId} className={styles.popRow}>
              <Link href={`/hospital/${h.hospitalId}`} className={styles.popLink}>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{h.name}</span>
                  <span className="meta">{h.type}</span>
                </span>
                <span className="meta">{distance(h.distanceM)}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <SiteFooter />
    </>
  );
}
