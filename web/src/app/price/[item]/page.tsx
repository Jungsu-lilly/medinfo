import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { ChevronDown, InfoIcon } from "@/components/Icons";
import SampleBanner from "@/components/SampleBanner";
import SiteFooter from "@/components/SiteFooter";
import { manwon, won } from "@/lib/format";
import { getPriceItem, KEYWORDS, POPULAR } from "@/lib/sample-data";
import PriceList from "./PriceList";
import styles from "./price.module.css";

// 많이 찾는 항목은 빌드 때 미리 만든다. 나머지는 첫 방문 때 만들어 캐시 (ISR)
export function generateStaticParams() {
  const slugs = new Set([...POPULAR.map((p) => p.slug), ...KEYWORDS.map((k) => k.slug)]);
  return [...slugs].map((item) => ({ item }));
}

export async function generateMetadata(props: PageProps<"/price/[item]">): Promise<Metadata> {
  const { item } = await props.params;
  const data = getPriceItem(item);
  const prices = data.rows.map((r) => r.price);
  return {
    title: `${data.name} 가격 비교`,
    description: `${data.name} 비급여 가격: 최저 ${won(Math.min(...prices))}, 최고 ${won(Math.max(...prices))}. 병원 ${data.rows.length}곳 가격을 싼 순으로 비교하세요.`,
  };
}

export default function PricePage(props: PageProps<"/price/[item]">) {
  return (
    <Suspense fallback={<div style={{ height: 52 }} />}>
      <PriceContent params={props.params} />
    </Suspense>
  );
}

function median(sorted: number[]): number {
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 ? sorted[mid] : Math.round((sorted[mid - 1] + sorted[mid]) / 2);
}

async function PriceContent({ params }: { params: Promise<{ item: string }> }) {
  const { item } = await params;
  const data = getPriceItem(item);
  const prices = data.rows.map((r) => r.price).sort((a, b) => a - b);
  const low = prices[0];
  const high = prices[prices.length - 1];
  const mid = median(prices);
  const midPct = high === low ? 0 : Math.round(((mid - low) / (high - low)) * 100);

  return (
    <>
      <BackHeader
        backHref="/"
        title={data.name}
        right={
          <button type="button" className={styles.regionBtn}>
            {data.region}
            <ChevronDown />
          </button>
        }
      />
      <SampleBanner />

      <section aria-label="가격 요약" className={styles.summary}>
        <p className="meta" style={{ marginBottom: 10 }}>
          {data.path.join(" › ")} · 병원 {data.rows.length}곳
        </p>
        <div className={styles.lowLine}>
          <span className={styles.lowLabel}>최저</span>
          <span className={styles.lowPrice}>{won(low)}</span>
        </div>
        <p className={styles.midLine}>
          중간 {won(mid)} · 최고 {won(high)}
        </p>

        <div aria-hidden="true" className={styles.strip}>
          <div className={styles.track} />
          <div className={styles.fill} style={{ width: `${midPct}%` }} />
          <div className={styles.tick} style={{ left: 0, background: "var(--accent)" }} />
          <div className={styles.tick} style={{ left: `${midPct}%`, background: "var(--ink)" }} />
          <div className={styles.tick} style={{ right: 0, background: "var(--muted)" }} />
        </div>
        <div aria-hidden="true" className={styles.stripLabels}>
          <span style={{ left: 0 }}>{manwon(low)}</span>
          <span style={{ left: `${midPct}%`, transform: "translateX(-50%)", color: "var(--ink)", fontWeight: 600 }}>
            중간 {manwon(mid)}
          </span>
          <span style={{ right: 0 }}>{manwon(high)}</span>
        </div>

        <p className={styles.note}>
          <InfoIcon />
          <span>{data.note}</span>
        </p>
      </section>

      <div className="band" />

      <PriceList rows={data.rows} median={mid} />

      <SiteFooter>
        2026년 9월 3일 적용 가격 · 출처 건강보험심사평가원 비급여진료비정보.
        <br />
        병원 종류는 상급종합·종합병원·병원만 포함하며, 의원급 가격은 공개되지 않습니다.
      </SiteFooter>
    </>
  );
}
