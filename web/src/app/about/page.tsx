import type { Metadata } from "next";
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

        <h2 style={h2}>진단이 아닙니다</h2>
        <p style={p}>
          이 서비스는 의학적 판단을 대신하지 않습니다. 몸에 이상이 있으면 의료진과 상의하고, 위급하면 119에 먼저
          전화하세요.
        </p>

        <h2 id="contact" style={h2}>
          문의
        </h2>
        <p style={p}>[운영자 이메일 주소]</p>
      </article>
    </>
  );
}
