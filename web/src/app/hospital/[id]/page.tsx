import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { ExternalIcon, NavigateIcon, PhoneIcon } from "@/components/Icons";
import SampleBanner from "@/components/SampleBanner";
import SiteFooter from "@/components/SiteFooter";
import { distance } from "@/lib/format";
import { HOSPITAL } from "@/lib/sample-data";
import NonpayGroups from "./NonpayGroups";
import styles from "./hospital.module.css";

export function generateStaticParams() {
  return [{ id: String(HOSPITAL.id) }];
}

export async function generateMetadata(props: PageProps<"/hospital/[id]">): Promise<Metadata> {
  await props.params;
  return {
    title: `${HOSPITAL.name} 비급여 가격`,
    description: `${HOSPITAL.name}의 비급여 진료비 ${HOSPITAL.itemCount}개 항목과 지역 중간 가격 비교.`,
  };
}

export default function HospitalPage(props: PageProps<"/hospital/[id]">) {
  return (
    <Suspense fallback={<div style={{ height: 52 }} />}>
      <HospitalContent params={props.params} />
    </Suspense>
  );
}

async function HospitalContent({ params }: { params: Promise<{ id: string }> }) {
  await params; // 가격 API가 붙으면 id로 조회. 지금은 예시 병원 1곳
  const h = HOSPITAL;
  const mapUrl = `https://map.naver.com/p/search/${encodeURIComponent(h.name)}`;

  return (
    <>
      <BackHeader backHref="/hospital" bordered={false} />
      <SampleBanner />

      <section className={styles.head}>
        <p className={styles.kind}>
          {h.type} · {distance(h.distanceM)}
        </p>
        <h1 className={styles.name}>{h.name}</h1>
        <p className={styles.addr}>{h.addr}</p>

        <div className={styles.actions}>
          <a href={`tel:${h.tel}`} className="btn-outline">
            <PhoneIcon />
            전화
          </a>
          <a href={mapUrl} target="_blank" rel="noopener noreferrer" className="btn-outline">
            <NavigateIcon />
            길찾기
          </a>
          <a href={h.homepage} target="_blank" rel="noopener noreferrer" className="btn-outline">
            <ExternalIcon />
            홈페이지
          </a>
        </div>
      </section>

      <div className="band" />

      <NonpayGroups groups={h.groups} itemCount={h.itemCount} />

      <SiteFooter>
        항목 이름은 병원이 쓰는 표기 그대로입니다. &quot;지역 중간&quot;은 반경 5km 병원들의 중간 가격입니다.
        <br />
        2026년 9월 3일 적용 · 출처 건강보험심사평가원
      </SiteFooter>
    </>
  );
}
