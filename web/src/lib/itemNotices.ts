/**
 * 특정 항목 가격 페이지 위에 띄우는 안내 (제도가 바뀌어 공개 가격과 실제 금액이 다를 수 있을 때).
 * match = 심평원 항목명(fullName)에 들어 있는 말
 */
export type ItemNotice = { match: string; text: string; href: string; linkText: string };

export const ITEM_NOTICES: ItemNotice[] = [
  {
    match: "도수치료",
    text: "2026년 7월부터 도수치료가 관리급여로 바뀌었어요 (1회 43,850원, 본인부담 95%, 연 15회). 아래는 병원이 신고한 비급여 가격이라 실제로 내는 금액과 다를 수 있어요.",
    href: "/guide/manual-therapy-2026",
    linkText: "바뀐 내용 보기",
  },
];

export function noticeFor(fullName: string): ItemNotice | undefined {
  return ITEM_NOTICES.find((n) => fullName.includes(n.match));
}
