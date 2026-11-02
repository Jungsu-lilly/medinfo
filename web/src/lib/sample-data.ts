/**
 * ⚠ 임시 예시 데이터 — 비급여 대량 적재(Spring Batch)와 가격 API가 붙기 전까지 화면 확인용.
 * 병원 이름·가격은 실제 값이 아니다. 가격 API가 생기면 이 파일을 지우고 lib/api.ts로 바꾼다.
 */

export type PopularItem = { slug: string; name: string; count: number; low: number; high: number };

export type PriceRow = {
  hospitalId: number;
  name: string;
  type: "상급종합" | "종합병원" | "병원";
  distanceM: number;
  price: number;
};

export type PriceItem = {
  slug: string;
  name: string;
  path: string[]; // 분류 경로 (중분류 › 소분류 › 상세)
  note: string;
  region: string;
  rows: PriceRow[];
};

export type HospitalItem = { name: string; std: string; price: number; regionMedian: number };
export type HospitalGroup = { name: string; count: number; items?: HospitalItem[] };
export type HospitalDetail = {
  id: number;
  name: string;
  type: string;
  distanceM: number;
  addr: string;
  tel: string;
  homepage: string;
  itemCount: number;
  groups: HospitalGroup[];
};

export const KEYWORDS = [
  { label: "MRI", slug: "mri-knee" },
  { label: "대상포진 예방접종", slug: "shingles-vaccine" },
  { label: "1인실", slug: "single-room" },
  { label: "초음파", slug: "thyroid-ultrasound" },
  { label: "독감 예방접종", slug: "flu-vaccine" },
];

export const POPULAR: PopularItem[] = [
  { slug: "shingles-vaccine", name: "대상포진 예방접종", count: 41, low: 160000, high: 250000 },
  { slug: "mri-knee", name: "무릎 MRI", count: 58, low: 320000, high: 890000 },
  { slug: "single-room", name: "상급병실료 1인실", count: 72, low: 180000, high: 450000 },
  { slug: "thyroid-ultrasound", name: "갑상선 초음파", count: 64, low: 50000, high: 190000 },
  { slug: "mri-lumbar", name: "허리 MRI", count: 57, low: 350000, high: 950000 },
  { slug: "flu-rapid-test", name: "인플루엔자 신속항원검사", count: 69, low: 20000, high: 65000 },
];

export const GUIDES = [
  { slug: "why-mri-price-differs", title: "MRI 가격이 병원마다 3배까지 차이 나는 이유", minutes: 5 },
  { slug: "dont-just-pick-cheapest", title: "비급여 가격표, 최저가만 보면 안 되는 경우", minutes: 4 },
  { slug: "insurance-and-nonpay", title: "실손보험으로 돌려받을 수 있는 비급여, 없는 비급여", minutes: 6 },
];

const MRI_KNEE: PriceItem = {
  slug: "mri-knee",
  name: "무릎 MRI",
  path: ["MRI 진단료", "근골격계", "무릎"],
  note: "병원이 신고한 가격이에요. 한쪽·양쪽, 조영제 사용 여부에 따라 달라질 수 있어요.",
  region: "마포구 반경 5km",
  rows: [
    { hospitalId: 1, name: "한빛병원", type: "병원", distanceM: 800, price: 320000 },
    { hospitalId: 2, name: "마포중앙병원", type: "종합병원", distanceM: 1200, price: 350000 },
    { hospitalId: 3, name: "새솔병원", type: "병원", distanceM: 2400, price: 410000 },
    { hospitalId: 4, name: "연희대학교병원", type: "상급종합", distanceM: 3100, price: 480000 },
    { hospitalId: 5, name: "서강종합병원", type: "종합병원", distanceM: 2900, price: 520000 },
    { hospitalId: 6, name: "합정성모병원", type: "종합병원", distanceM: 1900, price: 590000 },
    { hospitalId: 7, name: "신촌세브병원", type: "상급종합", distanceM: 3600, price: 890000 },
    { hospitalId: 8, name: "망원병원", type: "병원", distanceM: 1500, price: 455000 },
    { hospitalId: 9, name: "성산종합병원", type: "종합병원", distanceM: 2100, price: 498000 },
    { hospitalId: 10, name: "공덕병원", type: "병원", distanceM: 3300, price: 440000 },
    { hospitalId: 11, name: "아현병원", type: "병원", distanceM: 2700, price: 470000 },
    { hospitalId: 12, name: "상암중앙병원", type: "종합병원", distanceM: 4100, price: 560000 },
    { hospitalId: 13, name: "서울강변병원", type: "병원", distanceM: 4600, price: 505000 },
    { hospitalId: 14, name: "홍대우리병원", type: "병원", distanceM: 900, price: 485000 },
  ],
};

export const PRICE_ITEMS: Record<string, PriceItem> = { [MRI_KNEE.slug]: MRI_KNEE };

/** 아직 예시가 없는 항목은 이름만 바꿔 같은 표를 보여준다 */
export function getPriceItem(slug: string): PriceItem {
  const known = PRICE_ITEMS[slug];
  if (known) return known;
  const pop = POPULAR.find((p) => p.slug === slug) ?? KEYWORDS.map((k) => ({ slug: k.slug, name: k.label })).find((k) => k.slug === slug);
  return { ...MRI_KNEE, slug, name: pop?.name ?? slug, path: [pop?.name ?? slug] };
}

export const HOSPITAL: HospitalDetail = {
  id: 2,
  name: "마포중앙병원",
  type: "종합병원",
  distanceM: 1200,
  addr: "서울 마포구 [도로명 주소]",
  tel: "0000000000",
  homepage: "https://example.com",
  itemCount: 412,
  groups: [
    {
      name: "MRI 진단료",
      count: 24,
      items: [
        { name: "무릎 MRI (Knee, 한쪽)", std: "근골격계 › 무릎", price: 350000, regionMedian: 480000 },
        { name: "허리 MRI (L-spine)", std: "척추 › 요추", price: 420000, regionMedian: 480000 },
        { name: "뇌 MRI (Brain, 조영제 포함)", std: "뇌혈관 › 뇌", price: 610000, regionMedian: 580000 },
        { name: "어깨 MRI (Shoulder, 한쪽)", std: "근골격계 › 어깨", price: 450000, regionMedian: 455000 },
      ],
    },
    { name: "초음파 검사료", count: 31 },
    { name: "상급병실료", count: 4 },
    { name: "예방접종", count: 9 },
    { name: "검체검사료", count: 108 },
    { name: "제증명 수수료", count: 22 },
  ],
};
