import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import PriceView, { regionLabel } from "@/components/price/PriceView";
import styles from "@/components/price/price.module.css";
import { coord, DEFAULT_SIDO, getPrices, getRegions, regionCode, type PriceQuery } from "@/lib/api";
import { won } from "@/lib/format";

type Props = PageProps<"/price/[code]">;

/** 주소창: /price/ABZ010001?sido=110000&sggu=110016 또는 ?lat=..&lng=.. (내 위치, 시군구 중심이면 &area=마포구) */
async function resolve(
  props: Props,
): Promise<{ code: string; q: PriceQuery; sort: "price" | "distance"; area: string | null }> {
  const [{ code }, sp] = await Promise.all([props.params, props.searchParams]);
  const lat = coord(sp.lat);
  const lng = coord(sp.lng);
  const sort = sp.sort === "distance" ? "distance" : "price";
  // area = 시군구 중심을 기준점으로 쓴 경우 그 이름 (화면 표시용)
  const area = typeof sp.area === "string" && sp.area.length <= 20 ? sp.area : null;
  if (lat !== undefined && lng !== undefined) return { code, q: { lat, lng }, sort, area };
  return { code, q: { sido: regionCode(sp.sido) ?? DEFAULT_SIDO, sggu: regionCode(sp.sggu) }, sort, area: null };
}

export async function generateMetadata(props: Props): Promise<Metadata> {
  const { code, q, area } = await resolve(props);
  const { data } = await getPrices(code, q);
  if (!data) return { title: "가격 비교" };
  const where = regionLabel(data, area);
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

async function PriceContent(props: Props) {
  const { code, q, sort, area } = await resolve(props);
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
  return <PriceView data={data} regions={regions.data ?? []} sort={sort} backHref="/" area={area} />;
}
