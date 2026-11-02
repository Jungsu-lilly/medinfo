/**
 * 광고 자리. 애드센스 승인 전에는 자리만 잡아둔다(레이아웃이 나중에 밀리지 않게 높이 고정).
 * 승인 후 여기에 애드센스 반응형 광고 코드를 넣는다. 응급실 화면에는 쓰지 않는다.
 */
export default function AdSlot() {
  return (
    <aside
      aria-label="광고"
      style={{ margin: "12px 0", padding: "8px 10px", border: "1px solid #e4e5e1", borderRadius: 6 }}
    >
      <span style={{ fontSize: 11, color: "var(--muted)" }}>광고</span>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          height: 84,
          fontSize: 13,
          color: "var(--muted)",
        }}
      >
        광고 영역
      </div>
    </aside>
  );
}
