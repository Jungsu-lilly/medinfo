"use client";

import { useEffect, useState } from "react";
import { registerLocationNotice } from "@/lib/locationNotice";
import styles from "./LocationNoticeHost.module.css";

/** 위치 권한 전 안내 창 (카카오톡 인앱 브라우저에서만, 처음 한 번). 레이아웃에 한 번만 둔다 */
export default function LocationNoticeHost() {
  const [answer, setAnswer] = useState<((ok: boolean) => void) | null>(null);

  useEffect(() => {
    registerLocationNotice((fn) => setAnswer(() => fn));
    return () => registerLocationNotice(null);
  }, []);

  if (!answer) return null;
  const close = (ok: boolean) => {
    answer(ok);
    setAnswer(null);
  };

  return (
    <div className={styles.backdrop} onClick={() => close(false)}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="loc-notice-title"
        className={styles.sheet}
        onClick={(e) => e.stopPropagation()}
      >
        <p id="loc-notice-title" className={styles.title}>
          위치 권한을 물어볼게요
        </p>
        <p className={styles.body}>
          카카오톡에서는 권한 창에 주소가 <b>xn--639ao13amqh.com</b>으로 보일 수 있어요. 병원값.com의 영문 주소라 같은
          사이트예요.
        </p>
        <p className={styles.sub}>위치는 가까운 병원을 찾는 데만 쓰고 서버에 저장하지 않아요.</p>
        <div className={styles.actions}>
          <button type="button" className="btn-outline" onClick={() => close(false)}>
            취소
          </button>
          <button type="button" className={styles.primary} onClick={() => close(true)}>
            계속
          </button>
        </div>
      </div>
    </div>
  );
}
