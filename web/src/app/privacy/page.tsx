import type { Metadata } from "next";
import Link from "next/link";
import BackHeader from "@/components/BackHeader";

export const metadata: Metadata = { title: "개인정보처리방침" };

/**
 * 회원가입이 없고, 위치는 가까운 병원·응급실을 찾을 때만 쓰고 저장하지 않는다는 전제.
 * 수집하는 것이 바뀌면 이 방침과 시행일을 같이 고친다. (2026-11-04 Google 애널리틱스 추가)
 */
const p = { fontSize: 14, lineHeight: "24px", color: "var(--ink-2)", marginTop: 10 } as const;
const h2 = { fontSize: 16, fontWeight: 700, marginTop: 24 } as const;

export default function PrivacyPage() {
  return (
    <>
      <BackHeader backHref="/" title="개인정보처리방침" />
      <article style={{ padding: "8px 16px 32px" }}>
        <p className="meta">시행일: 2026년 11월 4일</p>

        <h2 style={h2}>수집하는 정보</h2>
        <p style={p}>회원가입이 없으며 이름, 연락처 같은 개인정보를 받지 않습니다.</p>
        <p style={p}>
          &quot;내 위치&quot;를 누르면 기기 위치(위도·경도)를 가까운 병원·응급실을 찾는 데만 사용하며 서버에 저장하지
          않습니다. 다시 묻지 않도록 내 위치(30분)와 직접 고른 지역은 이용자의 브라우저에만 기억해 둡니다.
        </p>

        <h2 style={h2}>쿠키와 광고</h2>
        <p style={p}>
          이 사이트는 Google 애드센스 광고를 게재할 수 있습니다. Google을 포함한 제3자 광고 사업자는 쿠키를 사용해
          이용자의 이 사이트 및 다른 사이트 방문 기록을 바탕으로 광고를 제공할 수 있습니다. 이용자는 Google 광고
          설정에서 맞춤 광고를 끌 수 있습니다.
        </p>

        <h2 style={h2}>방문 통계</h2>
        <p style={p}>
          서비스 개선을 위해 Google 애널리틱스로 방문 통계(본 페이지, 머문 시간, 기기·브라우저 종류, 대략적인 지역)를
          수집합니다. Google 애널리틱스는 쿠키를 사용하며, 이름·연락처처럼 개인을 알아볼 수 있는 정보는 보내지 않습니다.
          브라우저 설정에서 쿠키를 차단하거나{" "}
          <a
            href="https://tools.google.com/dlpage/gaoptout"
            target="_blank"
            rel="noopener noreferrer"
            style={{ textDecoration: "underline" }}
          >
            Google 애널리틱스 차단 브라우저 부가기능
          </a>
          으로 수집을 거부할 수 있습니다.
        </p>

        <h2 style={h2}>접속 기록</h2>
        <p style={p}>서비스 안정성 확인을 위해 접속 IP, 요청 시각, 브라우저 정보가 서버 로그에 30일 동안 남습니다.</p>

        <h2 style={h2}>문의</h2>
        <p style={p}>
          개인정보 관련 문의는{" "}
          <Link href="/contact" style={{ textDecoration: "underline" }}>
            문의
          </Link>{" "}
          페이지로 보내 주세요.
        </p>
      </article>
    </>
  );
}
