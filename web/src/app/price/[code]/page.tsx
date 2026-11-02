import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { InfoIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { coord, DEFAULT_SIDO, getPrices, getRegions, regionCode, type PriceList as Prices, type PriceQuery } from "@/lib/api";
import { koreanDate, manwon, won } from "@/lib/format";
import PriceList from "./PriceList";
import RegionPicker from "./RegionPicker";
import styles from "./price.module.css";

type Props = PageProps<"/price/[code]">;

/** 주소창: /price/ABZ010001?sido=110000&sggu=110016 또는 ?lat=..&lng=.. (내 위치) */
async function resolve(props: Props): Promise<{ code: string; q: PriceQuery; sort: "price" | "distance" }> {
  const [{ code }, sp] = await Promise.all([props.params, props.searchParams]);
  const lat = coord(sp.lat);
  const lng = coord(sp.lng);
  const sort = sp.sort === "distance" ? "distance" : "price";
  if (lat !== undefined && lng !== undefined) return { code, q: { lat, lng }, sort };
  return { code, q: { sido: regionCode(sp.sido) ?? DEFAULT_SIDO, sggu: regionCode(sp.sggu) }, sort };
}

export async function generateMetadata(props: Props): Promise<Metadata> {
  const { code, q } = await resolve(props);
  const { data } = await getPrices(code, q);
  if (!data) return { title: "가격 비교" };
  const where = regionLabel(data);
  const s = data.stats;
  return {
    title: `${data.item.name} 가격 비교 (${where})`,
    description:
      s.hospitalCount > 0
        ? `${where} ${data.item.fullName} 비급여 가격: 최저 ${won(s.low)}, 중간 ${won(s.median)}, 최고 ${won(s.high)}. 병원 ${s.hospitalCount}곳을 싼 순으로 비교하세요.`
        : `${data.item.fullName} 비급여 가격 비교`,
    // 내 위치 기준 결과는 사람마다 달라 검색 결과에 올리지 않는다
    robots: q.lat !== undefined ? { index: false } : undefined,
  };
}

export default function PricePage(props: Props) {
  return (
    <Suspense fallback={<div style={{ height: 52 }} />}>
      <PriceContent {...props} />
    </Suspense>
  );
}

function regionLabel(data: Prices): string {
  if (data.point) return `내 위치 ${data.point.radiusMeters / 1000}km`;
  const r = data.region;
  if (!r) return "";
  return [r.sidoName, r.sgguName ?? "전체"].filter(Boolean).join(" ");
}

async function PriceContent(props: Props) {
  const { code, q, sort } = await resolve(props);
  const [{ data, error }, regions] = await Promise.all([getPrices(code, q), getRegions()]);

  if (!data) {
    return (
      <>
        <BackHeader backHref="/" title="가격 비교" />
        <p className={styles.empty}>
          {error ? "가격 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요." : "찾는 비급여 항목이 없어요."}
        </p>
      </>
    );
  }

  const { item, stats, rows } = data;
  const midPct = stats.high === stats.low ? 0 : Math.round(((stats.median - stats.low) / (stats.high - stats.low)) * 100);

  return (
    <>
      <BackHeader backHref="/" title={item.name} />
      <RegionPicker
        regions={regions.data ?? []}
        sido={data.region?.sido ?? null}
        sggu={data.region?.sggu ?? null}
        nearMe={data.point !== null}
      />

      <section aria-label="가격 요약" className={styles.summary}>
        <p className="meta" style={{ marginBottom: 10 }}>
          {item.path.join(" › ")} · {regionLabel(data)} 병원 {stats.hospitalCount}곳
        </p>
        {stats.hospitalCount > 0 ? (
          <>
            <div className={styles.lowLine}>
              <span className={styles.lowLabel}>최저</span>
              <span className={styles.lowPrice}>{won(stats.low)}</span>
            </div>
            <p className={styles.midLine}>
              중간 {won(stats.median)} · 최고 {won(stats.high)}
            </p>
            {stats.hospitalCount > 1 && (
              <>
                <div aria-hidden="true" className={styles.strip}>
                  <div className={styles.track} />
                  <div className={styles.fill} style={{ width: `${midPct}%` }} />
                  <div className={styles.tick} style={{ left: 0, background: "var(--accent)" }} />
                  <div className={styles.tick} style={{ left: `${midPct}%`, background: "var(--ink)" }} />
                  <div className={styles.tick} style={{ right: 0, background: "var(--muted)" }} />
                </div>
                <div aria-hidden="true" className={styles.stripLabels}>
                  <span style={{ left: 0 }}>{manwon(stats.low)}</span>
                  <span style={{ left: `${midPct}%`, transform: "translateX(-50%)", color: "var(--ink)", fontWeight: 600 }}>
                    중간 {manwon(stats.median)}
                  </span>
                  <span style={{ right: 0 }}>{manwon(stats.high)}</span>
                </div>
              </>
            )}
          </>
        ) : (
          <p className={styles.midLine}>이 지역에는 이 항목 가격을 신고한 병원이 없어요. 위에서 지역을 바꿔 보세요.</p>
        )}

        <p className={styles.note}>
          <InfoIcon />
          <span>
            병원이 신고한 가격이에요. 병원마다 검사 범위·부위·약제가 달라 같은 항목도 차이가 날 수 있어요. 병원 안에서
            가격이 여러 개면 가장 낮은 가격 기준이에요.
          </span>
        </p>
      </section>

      <div className="band" />

      {/* 지역 ↔ 내 위치가 바뀌면 key가 바뀌어 정렬 상태를 새로 잡는다 */}
      {rows.length > 0 && (
        <PriceList
          key={data.point ? `near-${sort}` : "region"}
          rows={rows}
          median={stats.median}
          byDistance={data.point !== null}
          initialSort={sort}
        />
      )}

      <SiteFooter>
        {item.fullName} · 출처 건강보험심사평가원 비급여진료비정보
        {data.collectedOn ? ` (${koreanDate(data.collectedOn)} 수집)` : ""}.
        <br />
        병원급(상급종합·종합병원·병원 등)만 포함하며, 의원급 가격은 공개되지 않습니다.
      </SiteFooter>
    </>
  );
}
