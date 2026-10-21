import Link from "next/link";

export default function SiteFooter({ children }: { children?: React.ReactNode }) {
  return (
    <footer className="footer">
      {children ?? (
        <>
          가격은 병원이 건강보험심사평가원에 신고한 비급여 진료비입니다(2026년 9월 공개). 실제 진료비는 검사 범위에
          따라 달라질 수 있습니다.
          <br />
          출처: 건강보험심사평가원, 국립중앙의료원 공공데이터
        </>
      )}
      <div className="footer-links">
        <Link href="/about">서비스 소개</Link>
        <Link href="/privacy">개인정보처리방침</Link>
        <Link href="/about#contact">문의</Link>
      </div>
    </footer>
  );
}
