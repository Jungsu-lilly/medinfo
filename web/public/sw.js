// 서비스 워커: 인터넷이 끊겼을 때 안내 화면만 보여준다.
// 가격·병상 데이터는 캐시하지 않는다 (오래된 병상 정보를 최신처럼 보여주면 위험).
const CACHE = "offline-v1";
const OFFLINE_URL = "/offline";

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.add(new Request(OFFLINE_URL, { cache: "reload" }))));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  // 페이지 이동만 처리: 네트워크 먼저, 실패하면 오프라인 화면
  if (event.request.mode !== "navigate") return;
  event.respondWith(
    fetch(event.request).catch(() => caches.open(CACHE).then((cache) => cache.match(OFFLINE_URL))),
  );
});
