/** 가격 API가 붙기 전 예시 데이터 화면이라는 표시. 실제 데이터로 바꾸면 이 컴포넌트를 지운다 */
export default function SampleBanner() {
  return (
    <p
      role="note"
      style={{
        padding: "8px 16px",
        background: "#fff7e0",
        borderBottom: "1px solid #f0e2b6",
        fontSize: 12,
        color: "#6b4f00",
      }}
    >
      화면 확인용 예시 데이터입니다. 병원 이름과 가격은 실제와 다릅니다.
    </p>
  );
}
