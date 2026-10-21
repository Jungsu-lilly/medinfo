import type { Metadata } from "next";

export const metadata: Metadata = { title: "연결 끊김", robots: { index: false } };

/** 서비스 워커가 미리 저장해 두고, 인터넷이 끊겼을 때 대신 보여주는 화면 */
export default function OfflinePage() {
  return (
    <section style={{ padding: "64px 24px", textAlign: "center" }}>
      <h1 style={{ fontSize: 20, fontWeight: 700 }}>인터넷 연결이 끊겼어요</h1>
      <p style={{ marginTop: 12, fontSize: 15, lineHeight: "24px", color: "var(--ink-2)" }}>
        연결되면 다시 시도해 주세요.
        <br />
        위급하면 바로 119에 전화하세요.
      </p>
      <a
        href="tel:119"
        style={{
          display: "inline-flex",
          marginTop: 24,
          height: 48,
          padding: "0 24px",
          alignItems: "center",
          borderRadius: 8,
          background: "var(--red)",
          color: "#fff",
          fontWeight: 700,
        }}
      >
        119 전화
      </a>
    </section>
  );
}
