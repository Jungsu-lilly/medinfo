import type { Metadata } from "next";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import { ExternalIcon, NavigateIcon, PhoneIcon } from "@/components/Icons";
import SiteFooter from "@/components/SiteFooter";
import { getHospital, type HospitalPage as Hospital } from "@/lib/api";
import { koreanDate } from "@/lib/format";
import NonpayGroups from "./NonpayGroups";
import styles from "./hospital.module.css";

type Props = PageProps<"/hospital/[id]">;

export async function generateMetadata(props: Props): Promise<Metadata> {
  const { id } = await props.params;
  const { data } = await getHospital(id);
  if (!data) return { title: "병원" };
  if (data.itemCount === 0) {
    // 가격 없는 병원(대부분 의원급, 약 7만 곳)은 검색엔진에 올리지 않는다: 내용 없는 페이지가 대량으로 노출되면
    // 사이트 전체 품질 평가(애드센스 심사 포함)에 불리
    return { title: data.name, robots: { index: false } };
  }
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

  // 길찾기: 좌표가 있으면 카카오맵 길찾기에 도착지로 바로 (이름+좌표라 정확, 앱이 있으면 앱으로 열림)
  // 좌표가 없으면 네이버 지도 검색. 주소까지 넣으면 "지하1~5층" 같은 건물 안 위치 때문에 검색이 안 됨 → 이름 + 시군구만
  const mapUrl =
    h.lat !== null && h.lng !== null
      ? `https://map.kakao.com/link/to/${encodeURIComponent(h.name)},${h.lat},${h.lng}`
      : `https://map.naver.com/p/search/${encodeURIComponent(`${h.name} ${h.sgguName ?? ""}`.trim())}`;
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

      {h.itemCount > 0 ? (
        <NonpayGroups groups={h.groups} itemCount={h.itemCount} detailed={h.detailed} />
      ) : (
        <NoPrices hospital={h} />
      )}

      <SiteFooter>
        {h.itemCount > 0 && (
          <>
            항목 이름은 병원이 쓰는 표기 그대로입니다. &quot;지역 중간&quot;은 같은 시도({h.sidoName}) 병원들의 중간
            가격이며, 비교할 병원이 3곳 이상일 때만 보여줍니다.
            <br />
          </>
        )}
        출처 건강보험심사평가원 비급여진료비정보{h.collectedOn ? ` (${koreanDate(h.collectedOn)} 수집)` : ""}
      </SiteFooter>
    </>
  );
}

/** 가격이 없을 때 이유를 알려준다. 의원급은 공개 자체가 안 되고, 병원급은 신고를 안 했거나 아직 공개 전 */
function NoPrices({ hospital: h }: { hospital: Hospital }) {
  const clinic = h.noPriceReason === "CLINIC";
  return (
    <section aria-labelledby="np" className={styles.nonpay}>
      <h2 id="np" className="section-title" style={{ marginBottom: 12 }}>
        비급여 가격
      </h2>
      <div className={styles.notice}>
        {clinic ? (
          <>
            <b>의원급 의료기관은 비급여 가격이 공개되지 않아요.</b>
            <br />
            건강보험심사평가원은 병원급(병원·종합병원·상급종합병원 등)의 비급여 가격만 공개하고 있어요. 이 병원의 가격은
            전화로 문의해 주세요.
          </>
        ) : (
          <>
            <b>이 병원이 신고한 비급여 가격이 없어요.</b>
            <br />
            병원이 건강보험심사평가원에 비급여 가격을 신고하지 않았거나, 아직 공개되지 않은 경우예요. 가격은 전화로
            문의해 주세요.
          </>
        )}
      </div>
    </section>
  );
}
