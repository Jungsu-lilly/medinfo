import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import PriceView, { regionLabel } from "@/components/price/PriceView";
import styles from "@/components/price/price.module.css";
import { coord, DEFAULT_SIDO, getRegions, getVaccine, regionCode, type PriceQuery } from "@/lib/api";
import { won } from "@/lib/format";

type Props = PageProps<"/vaccine/[slug]">;

/**
 * 예방접종 질병별 묶음: /vaccine/flu?sido=110000 또는 ?lat=..&lng=.. (내 위치).
 * 같은 질병 백신 제품을 합쳐 병원마다 가장 싼 백신 가격으로 비교한다
 */
async function resolve(props: Props): Promise<{ slug: string; q: PriceQuery; sort: "price" | "distance" }> {
  const [{ slug }, sp] = await Promise.all([props.params, props.searchParams]);
  const lat = coord(sp.lat);
  const lng = coord(sp.lng);
  const sort = sp.sort === "distance" ? "distance" : "price";
  if (lat !== undefined && lng !== undefined) return { slug, q: { lat, lng }, sort };
  return { slug, q: { sido: regionCode(sp.sido) ?? DEFAULT_SIDO, sggu: regionCode(sp.sggu) }, sort };
}

export async function generateMetadata(props: Props): Promise<Metadata> {
  const { slug, q } = await resolve(props);
  const { data } = await getVaccine(slug, q);
  if (!data) return { title: "예방접종 가격 비교" };
  const where = regionLabel(data);
  const s = data.stats;
  const kinds = data.products?.length ?? 0;
  return {
    title: `${data.item.name} 가격 비교 (${where})`,
    description:
      s.hospitalCount > 0
        ? `${where} ${data.item.name} 비급여 가격: 최저 ${won(s.low)}, 중간 ${won(s.median)}, 최고 ${won(s.high)}. 백신 ${kinds}종, 병원 ${s.hospitalCount}곳을 싼 순으로 비교하세요.`
        : `${data.item.name} 비급여 가격 비교`,
    robots: q.lat !== undefined ? { index: false } : undefined,
  };
}

export default function VaccinePage(props: Props) {
  return (
    <Suspense fallback={<div style={{ height: 52 }} />}>
      <VaccineContent {...props} />
    </Suspense>
  );
}

async function VaccineContent(props: Props) {
  const { slug, q, sort } = await resolve(props);
  const [{ data, error }, regions] = await Promise.all([getVaccine(slug, q), getRegions()]);

  if (!data) {
    return (
      <>
        <BackHeader backHref="/" title="예방접종 가격 비교" />
        <p className={styles.empty}>
          {error ? "가격 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요." : "찾는 예방접종이 없어요."}
        </p>
      </>
    );
  }
  return <PriceView data={data} regions={regions.data ?? []} sort={sort} backHref="/" />;
}
