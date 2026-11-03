"use client";

/**
 * 카카오톡 인앱 브라우저에서 위치 권한을 묻기 전 한 번 안내.
 * 카카오톡은 권한 창에 주소를 퓨니코드(xn--639ao13amqh.com) 그대로 보여줘서 낯선 영문 주소로 보인다
 * (크롬·삼성 인터넷은 병원값.com으로 보여줌). 우리가 바꿀 수 없어서 먼저 알려준다.
 * 화면은 LocationNoticeHost(레이아웃에 한 번)가 그리고, useLocate가 권한을 묻기 전에 기다린다.
 */
const KEY = "medinfo.kakaoLocationNotice";

type Open = (answer: (ok: boolean) => void) => void;
let open: Open | null = null;

export function registerLocationNotice(fn: Open | null) {
  open = fn;
}

function isKakaoInApp(): boolean {
  return typeof navigator !== "undefined" && /KAKAOTALK/i.test(navigator.userAgent);
}

function seen(): boolean {
  try {
    return localStorage.getItem(KEY) === "1";
  } catch {
    return false;
  }
}

/** 이미 권한을 줬으면 권한 창이 안 뜨니 안내도 필요 없음 (확인 못 하면 안내) */
async function alreadyGranted(): Promise<boolean> {
  try {
    const p = await navigator.permissions?.query({ name: "geolocation" });
    return p?.state === "granted";
  } catch {
    return false;
  }
}

/** true = 계속(권한 묻기), false = 취소 */
export async function beforeLocationPrompt(): Promise<boolean> {
  if (!open || !isKakaoInApp() || seen() || (await alreadyGranted())) return true;
  const show = open;
  return new Promise((resolve) =>
    show((ok) => {
      if (ok) {
        try {
          localStorage.setItem(KEY, "1");
        } catch {
          /* 저장 못 하면 다음에 또 안내 */
        }
      }
      resolve(ok);
    }),
  );
}
