import type { Metadata } from "next";
import { Suspense } from "react";
import { notFound } from "next/navigation";
import BackHeader from "@/components/BackHeader";
import SiteFooter from "@/components/SiteFooter";
import { GUIDES } from "@/lib/sample-data";

// 가이드 글은 애드센스 승인에 필요한 "사람이 쓴 콘텐츠" 자리. 본문은 직접 작성 예정
export function generateStaticParams() {
  return GUIDES.map((g) => ({ slug: g.slug }));
}

export async function generateMetadata(props: PageProps<"/guide/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  const g = GUIDES.find((x) => x.slug === slug);
  // 본문 작성 전에는 검색엔진에 올리지 않는다 (빈 페이지는 품질 평가에 불리)
  return { title: g?.title ?? "가이드", robots: { index: false } };
}

export default function GuidePage(props: PageProps<"/guide/[slug]">) {
  return (
    <Suspense fallback={null}>
      <Guide params={props.params} />
    </Suspense>
  );
}

async function Guide({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const g = GUIDES.find((x) => x.slug === slug);
  if (!g) notFound();
  return (
    <>
      <BackHeader backHref="/" />
      <article style={{ padding: "8px 16px 24px" }}>
        <p className="meta" style={{ marginBottom: 6 }}>
          가이드 · {g.minutes}분
        </p>
        <h1 style={{ fontSize: 22, fontWeight: 700, lineHeight: "30px", letterSpacing: -0.5 }}>{g.title}</h1>
        <p style={{ marginTop: 20, fontSize: 15, lineHeight: "26px", color: "var(--ink-2)" }}>
          글을 준비하고 있어요.
        </p>
      </article>
      <SiteFooter />
    </>
  );
}
