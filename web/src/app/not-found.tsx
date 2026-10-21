import Link from "next/link";

export default function NotFound() {
  return (
    <section style={{ padding: "64px 24px", textAlign: "center" }}>
      <h1 style={{ fontSize: 20, fontWeight: 700 }}>페이지를 찾을 수 없어요</h1>
      <p style={{ marginTop: 12, fontSize: 15, color: "var(--ink-2)" }}>주소가 바뀌었거나 없는 항목이에요.</p>
      <Link href="/" className="btn-outline" style={{ marginTop: 24, padding: "0 20px" }}>
        처음으로
      </Link>
    </section>
  );
}
