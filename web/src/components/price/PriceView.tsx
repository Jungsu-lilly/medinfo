import Link from "next/link";
import BackHeader from "@/components/BackHeader";
import { InfoIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import type { PriceList as Prices, Region } from "@/lib/api";
import { koreanDate, manwon, won } from "@/lib/format";
import { noticeFor } from "@/lib/itemNotices";
import PriceList from "./PriceList";
import RegionPicker from "./RegionPicker";
import styles from "./price.module.css";

/** area = 지역 중심을 기준점으로 쓴 경우 그 지역 이름 (주소창 area) */
export function regionLabel(data: Prices, area?: string | null): string {
  if (data.point) return `${area ? `${area} 중심` : "내 위치"} ${data.point.radiusMeters / 1000}km`;
  const r = data.region;
  if (!r) return "";
  return [r.sidoName, r.sgguName ?? "전체"].filter(Boolean).join(" ");
}

/** 같은 지역(또는 내 위치) 그대로 다른 가격 페이지로 갈 때 붙이는 주소 뒷부분 */
function sameWhere(data: Prices, area: string | null): string {
  if (data.point)
    return `?lat=${data.point.lat}&lng=${data.point.lng}${area ? `&area=${encodeURIComponent(area)}` : ""}`;
  const r = data.region;
  if (!r) return "";
  return `?sido=${r.sido}${r.sggu ? `&sggu=${r.sggu}` : ""}`;
}

/** 가격 비교 화면 (항목 1개 /price/…, 예방접종 묶음 /vaccine/… 공통) */
export default function PriceView({
  data,
  regions,
  sort,
  backHref,
  area = null,
}: {
  data: Prices;
  regions: Region[];
  sort: "price" | "distance";
  backHref: string;
  /** 시군구 중심을 기준점으로 쓴 경우 그 이름 */
  area?: string | null;
}) {
  const { item, stats, rows, products } = data;
  const isGroup = products !== null;
  const notice = noticeFor(item.fullName);
  const midPct =
    stats.high === stats.low ? 0 : Math.round(((stats.median - stats.low) / (stats.high - stats.low)) * 100);

  return (
    <>
      <BackHeader backHref={backHref} title={item.name} />
      <RegionPicker
        regions={regions}
        sido={data.region?.sido ?? null}
        sggu={data.region?.sggu ?? null}
        nearMe={data.point !== null}
        nearLabel={regionLabel(data, area)}
        areaBased={area !== null}
      />

      {notice && (
        <p className={styles.itemNotice} role="note">
          {notice.text}{" "}
          <Link href={notice.href} className={styles.itemNoticeLink}>
            {notice.linkText}
          </Link>
        </p>
      )}

      <section aria-label="가격 요약" className={styles.summary}>
        <p className="meta" style={{ marginBottom: 10 }}>
          {item.path.join(" › ")} · {regionLabel(data, area)} 병원 {stats.hospitalCount}곳
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
                  <span
                    style={{ left: `${midPct}%`, transform: "translateX(-50%)", color: "var(--ink)", fontWeight: 600 }}
                  >
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
            {isGroup
              ? "병원이 신고한 가격이에요. 백신 제품을 합쳐서, 병원마다 가장 싼 백신 가격으로 비교해요. 같은 병원도 백신에 따라 가격이 달라요."
              : "병원이 신고한 가격이에요. 병원마다 검사 범위·부위·약제가 달라 같은 항목도 차이가 날 수 있어요. 병원 안에서 가격이 여러 개면 가장 낮은 가격 기준이에요."}
          </span>
        </p>

        {products && products.length > 0 && (
          <details className={styles.products}>
            <summary>
              백신별로 보기 ({products.length}종{data.point ? " · 전국 기준" : ""})
            </summary>
            <ul>
              {products.map((p) => (
                <li key={p.code}>
                  <Link href={`/price/${p.code}${sameWhere(data, area)}`} className={styles.productLink}>
                    <span className={styles.productName}>{p.name}</span>
                    <span className="meta">
                      최저 {manwon(p.stats.low)}원 · 병원 {p.stats.hospitalCount}곳
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </details>
        )}
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
          region={data.region ? { sido: data.region.sido, sggu: data.region.sggu } : null}
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
