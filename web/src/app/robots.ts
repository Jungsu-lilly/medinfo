import type { MetadataRoute } from "next";
import { SITE_INDEXING, SITE_URL } from "@/lib/site";

export default function robots(): MetadataRoute.Robots {
  if (!SITE_INDEXING) {
    // 공개 전: 전부 막는다 (배포해 두고 고치는 기간)
    return { rules: { userAgent: "*", disallow: "/" } };
  }
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/search", "/api/"] },
    host: SITE_URL,
  };
}
