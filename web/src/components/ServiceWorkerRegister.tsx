"use client";

import { useEffect } from "react";

/** 서비스 워커 등록: 오프라인일 때 안내 화면을 보여주는 용도 (public/sw.js) */
export default function ServiceWorkerRegister() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production") return; // 개발 중엔 캐시가 헷갈리게 하므로 끔
    if (!("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/sw.js", { scope: "/" }).catch(() => {
      // 등록 실패해도 사이트 사용에는 지장 없음
    });
  }, []);
  return null;
}
