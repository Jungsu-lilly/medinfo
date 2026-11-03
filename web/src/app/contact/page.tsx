import type { Metadata } from "next";
import BackHeader from "@/components/BackHeader";

export const metadata: Metadata = { title: "문의" };

const p = { fontSize: 15, lineHeight: "26px", color: "var(--ink-2)", marginTop: 12 } as const;

/** 운영자 연락처는 이 페이지에만 둔다 (푸터 "문의", 개인정보처리방침에서 여기로 연결) */
export default function ContactPage() {
  return (
    <>
      <BackHeader backHref="/" title="문의" />
      <article style={{ padding: "8px 16px 32px" }}>
        <p style={p}>잘못된 가격·병원 정보, 오류 제보, 개인정보 관련 문의는 메일로 보내 주세요.</p>
        <p style={p}>
          <a href="mailto:gkwjdtn1235@gmail.com" style={{ textDecoration: "underline" }}>
            gkwjdtn1235@gmail.com
          </a>
        </p>
        <p className="meta" style={{ marginTop: 16 }}>
          가격과 병원 정보는 공공데이터를 그대로 보여주므로, 병원별 진료·예약 문의는 해당 병원에 직접 해 주세요.
        </p>
      </article>
    </>
  );
}
