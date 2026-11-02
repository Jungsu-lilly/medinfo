import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { SearchIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { coord, getHospitals } from "@/lib/api";
import { distance } from "@/lib/format";
import NearHeader from "./NearHeader";
import styles from "../home.module.css";

export const metadata: Metadata = {
  title: "병원 찾기",
  description: "병원 이름이나 가까운 병원으로 찾아 비급여 진료비를 확인하세요.",
};

type Props = PageProps<"/hospital">;

export default function HospitalIndexPage(props: Props) {
  return (
    <>
      <header className={styles.header}>
        <h1 style={{ fontSize: 20, fontWeight: 700 }}>병원 찾기</h1>
        <span className="meta">이름으로 찾기 · 가까운 병원</span>
      </header>
      <Suspense
        fallback={
          <p className={styles.loading} style={{ padding: 16 }}>
            불러오는 중…
          </p>
        }
      >
        <Results searchParams={props.searchParams} />
      </Suspense>
      <SiteFooter />
    </>
  );
}

async function Results({ searchParams }: { searchParams: Props["searchParams"] }) {
  const sp = await searchParams;
  const raw = Array.isArray(sp.q) ? sp.q[0] : sp.q;
  const q = (raw ?? "").trim();
  const lat = coord(sp.lat);
  const lng = coord(sp.lng);
  const nearMe = lat !== undefined && lng !== undefined;
  // 지역을 직접 골랐으면 그 이름, 좌표만 있으면 내 위치
  const areaRaw = Array.isArray(sp.area) ? sp.area[0] : sp.area;
  const area = areaRaw && areaRaw.length <= 20 ? areaRaw : undefined;
  const where = nearMe ? (area ?? "내 위치") : "마포구";
  const { data, error } = await getHospitals(q ? { q } : nearMe ? { lat, lng } : {});

  return (
    <>
      <form role="search" action="/hospital" className={styles.searchWrap} style={{ paddingTop: 12 }}>
        <label htmlFor="hq" className="sr-only">
          병원 이름 검색
        </label>
        <div className={styles.search}>
          <SearchIcon size={20} />
          <input id="hq" name="q" type="search" defaultValue={q} placeholder="병원 이름" autoComplete="off" />
        </div>
      </form>

      <section aria-labelledby="near" className="section" style={{ paddingTop: 8 }}>
        {q ? (
          <div className="section-head">
            <h2 id="near" className="section-title">
              &quot;{q}&quot; 검색 결과
            </h2>
          </div>
        ) : (
          <NearHeader label={where} hasPoint={nearMe} />
        )}
        {error && <p className={styles.loading}>병원 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
        {data && data.length === 0 && (
          <p className={styles.loading}>
            {q ? "이름이 맞는 병원이 없어요." : "반경 5km 안에 비급여 가격이 있는 병원이 없어요."}
          </p>
        )}
        {!q && data && data.length > 0 && (
          <p className="meta" style={{ marginBottom: 4 }}>
            비급여 가격이 있는 병원만 보여요. 의원은 이름으로 찾아 주세요.
          </p>
        )}
        <ul>
          {(data ?? []).map((h) => (
            <li key={h.id} className={styles.popRow}>
              <Link href={`/hospital/${h.id}`} className={styles.popLink}>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{h.name}</span>
                  <span className="meta">
                    {h.kind}
                    {!h.hasPrices && " · 가격 정보 없음"}
                    {h.addr ? ` · ${h.addr}` : ""}
                  </span>
                </span>
                {h.distanceMeters !== null && <span className="meta">{distance(h.distanceMeters)}</span>}
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
