import type { Metadata } from "next";
import Link from "next/link";
import BackHeader from "@/components/BackHeader";
import SiteFooter from "@/components/SiteFooter";
import { GUIDES } from "@/lib/guides";
import styles from "./guide.module.css";

export const metadata: Metadata = {
  title: "가이드",
  description: "비급여 진료비, 실손보험, 예방접종, 응급실 이용까지 병원 가기 전에 알아두면 좋은 내용을 정리했어요.",
};

export default function GuideIndexPage() {
  return (
    <>
      <BackHeader backHref="/" title="알고 가면 덜 내는 법" />
      <ul className={styles.list}>
        {GUIDES.map((g) => (
          <li key={g.slug}>
            <Link href={`/guide/${g.slug}`} className={styles.item}>
              <span className={styles.itemTitle}>{g.title}</span>
              <span className={styles.itemDesc}>{g.description}</span>
              <span className="meta">가이드 · {g.minutes}분</span>
            </Link>
          </li>
        ))}
      </ul>
      <SiteFooter />
    </>
  );
}
