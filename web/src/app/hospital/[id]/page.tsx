import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { ExternalIcon, NavigateIcon, PhoneIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { getHospital } from "@/lib/api";
import NonpayGroups from "./NonpayGroups";
import styles from "./hospital.module.css";

type Props = PageProps<"/hospital/[id]">;

export async function generateMetadata(props: Props): Promise<Metadata> {
  const { id } = await props.params;
  const { data } = await getHospital(id);
  if (!data) return { title: "병원" };
  return {
    title: `${data.name} 비급여 가격`,
    description: `${data.sidoName} ${data.sgguName} ${data.name}(${data.kind})의 비급여 진료비 ${data.itemCount}개 항목과 지역 중간 가격 비교.`,
  };
}

export default function HospitalPage(props: Props) {
  return (
    <Suspense fallback={<div style={{ height: 52 }} />}>
      <HospitalContent params={props.params} />
    </Suspense>
  );
}

async function HospitalContent({ params }: { params: Props["params"] }) {
  const { id } = await params;
  const { data: h, error } = await getHospital(id);

  if (!h) {
    return (
      <>
        <BackHeader backHref="/hospital" title="병원" />
        <p className={styles.empty} style={{ padding: 16 }}>
          {error ? "병원 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요." : "찾는 병원이 없어요."}
        </p>
      </>
    );
  }

  const mapUrl = `https://map.naver.com/p/search/${encodeURIComponent(`${h.name} ${h.addr ?? ""}`.trim())}`;
  const site = h.homepage ?? h.nonpayUrl;

  return (
    <>
      <BackHeader backHref="/hospital" bordered={false} />

      <section className={styles.head}>
        <p className={styles.kind}>
          {h.kind} · {h.sidoName} {h.sgguName}
        </p>
        <h1 className={styles.name}>{h.name}</h1>
        {h.addr && <p className={styles.addr}>{h.addr}</p>}

        <div className={styles.actions}>
          {h.tel ? (
            <a href={`tel:${h.tel}`} className="btn-outline">
              <PhoneIcon />
              전화
            </a>
          ) : (
            <span className="btn-outline" aria-disabled="true" style={{ color: "var(--muted)" }}>
              전화번호 없음
            </span>
          )}
          <a href={mapUrl} target="_blank" rel="noopener noreferrer" className="btn-outline">
            <NavigateIcon />
            길찾기
          </a>
          {site && (
            <a href={site} target="_blank" rel="noopener noreferrer" className="btn-outline">
              <ExternalIcon />
              {h.homepage ? "홈페이지" : "비급여 안내"}
            </a>
          )}
        </div>
      </section>

      <div className="band" />

      <NonpayGroups groups={h.groups} itemCount={h.itemCount} detailed={h.detailed} />

      <SiteFooter>
        항목 이름은 병원이 쓰는 표기 그대로입니다. &quot;지역 중간&quot;은 같은 시도({h.sidoName}) 병원들의 중간 가격이며,
        비교할 병원이 3곳 이상일 때만 보여줍니다.
        <br />
        2026년 9월 공개분 · 출처 건강보험심사평가원
      </SiteFooter>
    </>
  );
}
