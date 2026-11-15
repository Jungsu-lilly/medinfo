import Link from "next/link";
import type { PriceList as Prices } from "@/lib/api";
import { won } from "@/lib/format";
import { GUIDES } from "@/lib/guides";
import { itemGuideFor } from "@/lib/itemGuides";
import styles from "./price.module.css";

/** 싼 쪽에서 25% 지점 가격. 목록이 잘리지 않았고 4곳 이상일 때만 */
function lowerQuartile(data: Prices): number | null {
  const n = data.stats.hospitalCount;
  if (n < 4 || data.rows.length !== n) return null;
  const sorted = data.rows.map((r) => r.minPrice).sort((a, b) => a - b);
  return sorted[Math.floor((n - 1) * 0.25)];
}

/** 데이터로 계산한 요약 문장 */
function summaryLines(data: Prices, where: string): string[] {
  const { stats, item } = data;
  const n = stats.hospitalCount;
  if (n === 0) return [];
  if (n === 1) {
    return [
      `${where}에서 ${item.name} 가격을 신고한 병원은 1곳(${won(stats.low)})뿐이에요. 다른 지역과도 비교해 보세요.`,
    ];
  }
  if (stats.low === stats.high) {
    return [`${where}에서 ${item.name} 가격을 신고한 병원 ${n}곳이 모두 ${won(stats.low)}이에요.`];
  }
  const lines = [`${where}에서 ${item.name} 가격을 신고한 병원은 ${n}곳이에요.`];
  const ratio = stats.low > 0 ? stats.high / stats.low : null;
  lines.push(
    `가장 싼 곳은 ${won(stats.low)}, 가장 비싼 곳은 ${won(stats.high)}` +
      (ratio !== null && ratio >= 1.1 ? `으로 ${ratio.toFixed(1)}배 차이가 나요.` : `이에요.`),
  );
  lines.push(`병원 절반은 ${won(stats.median)} 이하로 받아요.`);
  const q1 = lowerQuartile(data);
  if (q1 !== null && q1 < stats.median) lines.push(`싼 쪽 4곳 중 1곳은 ${won(q1)} 이하예요.`);
  return lines;
}

/** 가격 목록 아래 설명: 데이터 요약 + 이 항목 가격 읽는 법 + 병원에 물어볼 것 + 관련 가이드 */
export default function PriceInsight({ data, where }: { data: Prices; where: string }) {
  const guide = itemGuideFor(data.item.fullName);
  const lines = summaryLines(data, where);
  const related = guide.guides.map((slug) => GUIDES.find((g) => g.slug === slug)).filter((g) => g !== undefined);

  return (
    <section aria-labelledby="insight-title" className={styles.insight}>
      <h2 id="insight-title" className={styles.insightTitle}>
        {data.item.name} 가격, 이렇게 보세요
      </h2>
      {lines.length > 0 && <p className={styles.insightP}>{lines.join(" ")}</p>}
      {guide.about.map((t) => (
        <p key={t} className={styles.insightP}>
          {t}
        </p>
      ))}

      <h3 className={styles.insightH3}>병원에 물어볼 것</h3>
      <ul className={styles.insightUl}>
        {guide.ask.map((t) => (
          <li key={t}>{t}</li>
        ))}
      </ul>

      {related.length > 0 && (
        <>
          <h3 className={styles.insightH3}>함께 읽으면 좋은 글</h3>
          <ul className={styles.insightLinks}>
            {related.map((g) => (
              <li key={g.slug}>
                <Link href={`/guide/${g.slug}`}>{g.title}</Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
