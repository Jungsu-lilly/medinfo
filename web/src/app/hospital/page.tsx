import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { SearchIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { coord, getHospitals } from "@/lib/api";
import { distance } from "@/lib/format";
import LocateButton from "./LocateButton";
import styles from "../home.module.css";

export const metadata: Metadata = {
  title: "병원 찾기",
  description: "가까운 병원이나 병원 이름으로 찾아 비급여 진료비를 확인하세요.",
};

type Props = PageProps<"/hospital">;

export default function HospitalIndexPage(props: Props) {
  return (
    <>
      <header className={styles.header}>
        <h1 style={{ fontSize: 20, fontWeight: 700 }}>병원 찾기</h1>
        <span className="meta">비급여 가격이 있는 병원</span>
      </header>
      <Suspense fallback={<p className={styles.loading} style={{ padding: 16 }}>불러오는 중…</p>}>
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
        <div className="section-head">
          <h2 id="near" className="section-title">
            {q ? `"${q}" 검색 결과` : nearMe ? "내 위치에서 가까운 병원" : "가까운 병원 (마포구 기준)"}
          </h2>
          {!q && <LocateButton active={nearMe} />}
        </div>
        {error && <p className={styles.loading}>병원 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
        {data && data.length === 0 && (
          <p className={styles.loading}>{q ? "이름이 맞는 병원이 없어요." : "반경 5km 안에 비급여 가격이 있는 병원이 없어요."}</p>
        )}
        <ul>
          {(data ?? []).map((h) => (
            <li key={h.id} className={styles.popRow}>
              <Link href={`/hospital/${h.id}`} className={styles.popLink}>
                <span className={styles.popMain}>
                  <span className={styles.popName}>{h.name}</span>
                  <span className="meta">
                    {h.kind}
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
