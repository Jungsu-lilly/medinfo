import type { MetadataRoute } from "next";
import { getFeatured } from "@/lib/api";
import { GUIDES } from "@/lib/guides";
import { SITE_URL } from "@/lib/site";

/** 예방접종 묶음 페이지 (백엔드 VaccineGroups.java와 같게) */
const VACCINES = [
  "flu",
  "shingles",
  "hpv",
  "pneumococcal",
  "hepatitis-a",
  "hepatitis-b",
  "tdap",
  "td",
  "mmr",
  "varicella",
  "meningococcal",
  "hib",
];

/**
 * 검색엔진에 알려줄 주소 목록 (서치콘솔·네이버 서치어드바이저에 /sitemap.xml 제출).
 * 고정 페이지 + 가이드 + 예방접종 묶음 + 많이 찾는 비급여 항목. 병원·항목 전체는 나중에 늘린다
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const url = (path: string) => `${SITE_URL}${path}`;
  const fixed = ["/", "/hospital", "/er", "/about", "/contact", "/privacy"].map((p) => ({ url: url(p) }));
  const guides = GUIDES.map((g) => ({ url: url(`/guide/${g.slug}`) }));
  const vaccines = VACCINES.map((v) => ({ url: url(`/vaccine/${v}`) }));
  const { data } = await getFeatured(undefined, 20);
  const items = (data ?? []).filter((i) => i.code).map((i) => ({ url: url(`/price/${i.code}`) }));
  return [...fixed, ...guides, ...vaccines, ...items];
}
