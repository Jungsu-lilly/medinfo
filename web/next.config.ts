import type { NextConfig } from "next";

// Spring Boot API 주소. 로컬은 8080, 운영은 같은 도메인에서 Ingress/ALB가 /api를 Spring으로 보낸다.
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  // 컨테이너 이미지용: node_modules 없이 실행되는 최소 서버 산출물 (.next/standalone)
  output: "standalone",

  // 브라우저는 같은 출처의 /api 로 부르고, Next 서버가 Spring으로 넘긴다 → CORS 설정 불필요
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${BACKEND_URL}/api/:path*` }];
  },

  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
        ],
      },
      {
        // 서비스 워커는 항상 최신본을 받아야 한다
        source: "/sw.js",
        headers: [
          { key: "Content-Type", value: "application/javascript; charset=utf-8" },
          { key: "Cache-Control", value: "no-cache, no-store, must-revalidate" },
        ],
      },
    ];
  },
};

export default nextConfig;
