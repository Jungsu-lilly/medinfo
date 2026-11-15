import type { Metadata } from "next";
import Link from "next/link";
import BackHeader from "@/components/BackHeader";
import { SITE_NAME } from "@/lib/site";

export const metadata: Metadata = { title: "서비스 소개" };

const p = { fontSize: 15, lineHeight: "26px", color: "var(--ink-2)", marginTop: 12 } as const;
const h2 = { fontSize: 17, fontWeight: 700, marginTop: 28 } as const;

export default function AboutPage() {
  return (
    <>
      <BackHeader backHref="/" title="서비스 소개" />
      <article style={{ padding: "8px 16px 32px" }}>
        <p style={p}>
          {SITE_NAME}은 병원에 가기 전에 찾아보는 서비스입니다. 같은 검사라도 병원마다 비급여 가격이 다르고, 응급실은
          지금 받을 수 있는지가 중요합니다. 흩어져 있는 공공데이터를 모아 휴대폰에서 바로 비교할 수 있게 만들었습니다.
        </p>

        <h2 style={h2}>데이터 출처</h2>
        <p style={p}>
          비급여 진료비와 병원 정보는 건강보험심사평가원, 응급실 병상 정보는 국립중앙의료원이 공개한 데이터입니다.
          병원이 신고·입력한 값이라 실제 진료비나 병상 상황과 다를 수 있습니다.
        </p>

        <h2 style={h2}>가격을 보여 주는 방식</h2>
        <p style={p}>
          병원 한 곳이 같은 항목에 가격을 여러 개 신고했으면 가장 낮은 가격을 기준으로 비교합니다. 지역 요약의 중간값은
          병원들을 가격순으로 세웠을 때 가운데 병원의 가격이라, 몇몇 아주 비싼 병원에 끌려가지 않습니다. 예방접종은 같은
          질병 백신 제품을 합쳐 병원마다 가장 싼 백신 가격으로 비교합니다.
        </p>
        <p style={p}>
          심평원은 병원급(상급종합병원·종합병원·병원 등) 가격만 공개하므로, 동네 의원 가격은 이 서비스에 없습니다.
        </p>

        <h2 style={h2}>업데이트 주기</h2>
        <p style={p}>
          비급여 진료비는 매월, 병원 기본 정보는 매주, 응급실 병상 정보는 2분마다 새로 받아옵니다. 가격 페이지 아래에
          데이터를 받아온 날짜를 표시합니다.
        </p>

        <h2 style={h2}>운영</h2>
        <p style={p}>
          {SITE_NAME}은 개인이 직접 만들고 운영하는 서비스입니다. 광고 수익으로 서버 비용을 충당하며, 광고는 가격 순서나
          비교 결과에 영향을 주지 않습니다. 잘못된 정보나 개선할 점은{" "}
          <Link href="/contact" style={{ textDecoration: "underline", textUnderlineOffset: 3 }}>
            문의
          </Link>
          로 알려 주세요.
        </p>

        <h2 style={h2}>진단이 아닙니다</h2>
        <p style={p}>
          이 서비스는 의학적 판단을 대신하지 않습니다. 몸에 이상이 있으면 의료진과 상의하고, 위급하면 119에 먼저
          전화하세요.
        </p>
      </article>
    </>
  );
}
