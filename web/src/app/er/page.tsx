import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { PhoneIcon } from "@/components/Icons";
import { DEFAULT_POINT, fetchErFromServer } from "@/lib/er";
import ErLive from "./ErLive";
import styles from "./er.module.css";

export const metadata: Metadata = {
  title: "응급실 실시간 병상",
  description: "근처 응급실의 남은 병상을 2분마다 갱신해 보여줍니다. 생명이 위급하면 먼저 119에 전화하세요.",
};

export default function ErPage() {
  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>응급실</h1>
        <span className="meta">2분마다 갱신</span>
      </header>

      {/* 119 안내는 데이터와 상관없이 항상 먼저 보이게 정적 부분에 둔다 */}
      <div className={styles.callout}>
        <span className={styles.calloutText}>
          <b>생명이 위급하면</b> 병상을 찾기 전에
          <br />
          먼저 119에 전화하세요.
        </span>
        <a href="tel:119" className={styles.call119}>
          <PhoneIcon strokeWidth={2.2} />
          119
        </a>
      </div>

      <Suspense fallback={<p className={styles.loading}>응급실 정보를 불러오는 중…</p>}>
        <ErInitial />
      </Suspense>

      <footer className="footer">
        병상 수는 각 병원이 직접 입력한 값이라 실제와 다를 수 있습니다. 출발 전 응급실에 전화로 확인하세요.
        <br />
        출처: 국립중앙의료원 응급의료정보
      </footer>
    </>
  );
}

/** 첫 화면은 서버에서 바로 채워 보내고(빈 화면 없이), 이후 갱신은 브라우저가 맡는다 */
async function ErInitial() {
  await connection(); // 요청 시점마다 새로 읽음 (빌드 때 미리 만들지 않음)
  const initial = await fetchErFromServer(DEFAULT_POINT.lat, DEFAULT_POINT.lng);
  return <ErLive initial={initial} />;
}
