/**
 * 가격 페이지 아래 "이 가격, 이렇게 보세요" 설명 (항목 분류별).
 * match = 심평원 항목명(fullName)에 들어 있는 말. 위에서부터 처음 맞는 것 하나만 쓴다.
 * 의학적 판단·효과 주장은 하지 않고, 가격을 읽을 때 알아 둘 것만 쓴다.
 */
export type ItemGuide = {
  match: string[];
  /** 이 항목 가격이 어떻게 정해지는지 (1~2문단) */
  about: string[];
  /** 병원에 물어볼 것 */
  ask: string[];
  /** 관련 가이드 slug (lib/guides.ts) */
  guides: string[];
};

const VACCINE: Omit<ItemGuide, "match" | "guides"> = {
  about: [
    "예방접종 비급여 가격은 백신값과 접종 비용을 합친 1회 가격이에요. 같은 질병 백신도 제품(제조사·종류)에 따라 가격이 달라요.",
    "여러 번 맞아야 하는 백신은 횟수만큼 비용이 늘어요. 어린이·어르신처럼 국가예방접종 대상이면 무료로 맞을 수 있으니, 질병관리청 예방접종도우미에서 대상인지 먼저 확인해 보세요.",
  ],
  ask: ["어떤 백신 제품으로 접종하는지", "전체 접종 횟수와 다음 접종 시기", "예약 없이 바로 접종이 되는지 (재고)"],
};

const ITEM_GUIDES: ItemGuide[] = [
  { ...VACCINE, match: ["예방접종료/대상포진"], guides: ["shingles-vaccine-price", "insurance-and-nonpay"] },
  { ...VACCINE, match: ["예방접종료/인플루엔자"], guides: ["flu-vaccine-2026", "dont-just-pick-cheapest"] },
  { ...VACCINE, match: ["예방접종"], guides: ["dont-just-pick-cheapest", "insurance-and-nonpay"] },
  {
    match: ["MRI", "자기공명"],
    about: [
      "MRI는 촬영 부위와 범위, 조영제 사용 여부, 장비에 따라 가격이 달라요. 병원마다 판독료를 따로 받기도 해요.",
      "같은 MRI라도 의심되는 질환과 의사 소견에 따라 건강보험이 적용될 수 있어요. 여기 가격은 병원이 비급여로 신고한 가격이에요.",
    ],
    ask: ["조영제·판독료가 가격에 포함되는지", "이번 검사에 건강보험이 적용되는지", "촬영 범위 (한쪽·양쪽 등)"],
    guides: ["why-mri-price-differs", "insurance-and-nonpay"],
  },
  {
    match: ["초음파"],
    about: [
      "초음파는 검사 부위와 범위(일반·정밀)에 따라 가격이 달라요. 일부 부위는 조건에 맞으면 건강보험이 적용돼요.",
    ],
    ask: [
      "검사 범위 (일반·정밀)",
      "이번 검사에 건강보험이 적용되는지",
      "같은 날 다른 부위를 함께 보면 가격이 어떻게 되는지",
    ],
    guides: ["dont-just-pick-cheapest", "insurance-and-nonpay"],
  },
  {
    match: ["병실", "입원료"],
    about: [
      "상급병실료는 건강보험이 적용되는 병실보다 적은 인원이 쓰는 병실을 쓸 때 내는 하루 금액이에요. 입원한 날 수만큼 늘어나요.",
      "의학적으로 1인실이 필요하다고 의사가 판단한 경우처럼 보험이 적용되는 예외도 있어요.",
    ],
    ask: [
      "하루 금액에 포함되는 것 (식대 등은 별도인지)",
      "다인실 빈자리가 생기면 옮길 수 있는지",
      "이번 입원이 보험 적용 예외에 해당하는지",
    ],
    guides: ["hospital-room-cost", "insurance-and-nonpay"],
  },
  {
    match: ["도수"],
    about: ["도수치료는 치료사가 손으로 하는 치료로, 1회 시간과 방식에 따라 가격이 달라요."],
    ask: ["1회 치료 시간", "관리급여 기준으로 실제 내는 금액", "올해 남은 횟수"],
    guides: ["manual-therapy-2026", "insurance-and-nonpay"],
  },
  {
    match: ["검체검사"],
    about: [
      "검체검사는 피·소변·콧물 같은 검체로 하는 검사예요. 같은 검사도 방식(현장검사·신속검사 등)에 따라 가격이 달라요.",
    ],
    ask: ["검사 방식과 결과가 나오는 시간", "진찰료가 따로 붙는지", "이번 검사에 건강보험이 적용되는지"],
    guides: ["dont-just-pick-cheapest", "insurance-and-nonpay"],
  },
];

const DEFAULT_GUIDE: ItemGuide = {
  match: [],
  about: [
    "비급여는 건강보험이 적용되지 않아 병원이 가격을 정하는 항목이에요. 그래서 같은 항목도 병원마다 차이가 커요.",
  ],
  ask: ["가격에 포함되는 범위", "이번 진료에 건강보험이 적용되는지", "신고한 가격에서 바뀐 게 있는지"],
  guides: ["dont-just-pick-cheapest", "insurance-and-nonpay"],
};

export function itemGuideFor(fullName: string): ItemGuide {
  return ITEM_GUIDES.find((g) => g.match.some((m) => fullName.includes(m))) ?? DEFAULT_GUIDE;
}
