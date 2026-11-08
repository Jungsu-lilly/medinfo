# syntax=docker/dockerfile:1.7
# Next.js 화면 이미지 (standalone 산출물만 담아 작게)
# 빌드: docker build -t medinfo-web --build-arg SITE_URL=https://도메인 --build-arg SITE_INDEXING=false .

FROM node:26-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN --mount=type=cache,target=/root/.npm npm ci

FROM node:26-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# 빌드 때 쓰는 값 (next.config의 rewrites, 미리 만드는 페이지의 메타데이터, robots.txt)
ARG BACKEND_URL=http://api:8080
ARG SITE_URL=http://localhost:3000
ARG SITE_INDEXING=false
ENV BACKEND_URL=$BACKEND_URL \
    SITE_URL=$SITE_URL \
    SITE_INDEXING=$SITE_INDEXING \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:26-alpine
WORKDIR /app
# 실행 중에도 필요: 요청 때 그리는 페이지(가격·예방접종)의 메타데이터, 10분마다 다시 만드는 sitemap.xml
# (빌드 단계에만 두면 실행 중엔 비어서 noindex·localhost가 들어감 → 2026-11 검색 노출 막혔던 원인)
ARG SITE_URL=http://localhost:3000
ARG SITE_INDEXING=false
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    TZ=Asia/Seoul \
    SITE_URL=$SITE_URL \
    SITE_INDEXING=$SITE_INDEXING \
    NEXT_TELEMETRY_DISABLED=1
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/public ./public
# .next/cache(서버 캐시)에 쓸 수 있어야 해서 소유자를 app으로
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
USER app
EXPOSE 3000
CMD ["node", "server.js"]
