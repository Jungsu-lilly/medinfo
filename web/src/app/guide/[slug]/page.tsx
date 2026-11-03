import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import BackHeader from "@/components/BackHeader";
import SiteFooter from "@/components/SiteFooter";
import { koreanDate } from "@/lib/format";
import { type GuideBlock, GUIDES } from "@/lib/guides";
import styles from "../guide.module.css";

export function generateStaticParams() {
  return GUIDES.map((g) => ({ slug: g.slug }));
}

export async function generateMetadata(props: PageProps<"/guide/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  const g = GUIDES.find((x) => x.slug === slug);
  if (!g) return { title: "가이드" };
  return {
    title: g.title,
    description: g.description,
    openGraph: { title: g.title, description: g.description, type: "article" },
  };
}

export default function GuidePage(props: PageProps<"/guide/[slug]">) {
  return (
    <Suspense fallback={null}>
      <Guide params={props.params} />
    </Suspense>
  );
}

function Block({ b }: { b: GuideBlock }) {
  if ("h" in b) return <h2 className={styles.h2}>{b.h}</h2>;
  if ("p" in b) return <p className={styles.p}>{b.p}</p>;
  if ("ul" in b)
    return (
      <ul className={styles.ul}>
        {b.ul.map((t) => (
          <li key={t}>{t}</li>
        ))}
      </ul>
    );
  return (
    <Link href={b.link.href} className={`btn-outline ${styles.cta}`}>
      {b.link.text} →
    </Link>
  );
}

async function Guide({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const g = GUIDES.find((x) => x.slug === slug);
  if (!g) notFound();
  return (
    <>
      <BackHeader backHref="/guide" />
      <article className={styles.article}>
        <p className="meta">
          가이드 · {g.minutes}분 · {koreanDate(g.updated)} 확인
        </p>
        <h1 className={styles.title}>{g.title}</h1>
        {g.body.map((b, i) => (
          <Block key={i} b={b} />
        ))}

        {g.sources.length > 0 && (
          <section className={styles.sources} aria-label="출처">
            <h2 className={styles.sourcesTitle}>출처</h2>
            <ul>
              {g.sources.map((s) => (
                <li key={s.url}>
                  <a href={s.url} target="_blank" rel="noopener noreferrer">
                    {s.title}
                  </a>
                </li>
              ))}
            </ul>
          </section>
        )}
        <p className={styles.notice}>
          이 글은 일반적인 정보를 정리한 것으로 의학적 판단이나 보험 상담을 대신하지 않습니다. 제도와 가격은 바뀔 수
          있으니 병원·보험사에 최신 내용을 확인하세요.
        </p>
      </article>
      <SiteFooter />
    </>
  );
}
