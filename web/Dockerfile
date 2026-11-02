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
# 빌드 때 정해지는 값 (next.config의 rewrites, robots.txt, 페이지 메타데이터)
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
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    TZ=Asia/Seoul \
    NEXT_TELEMETRY_DISABLED=1
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/public ./public
# .next/cache(서버 캐시)에 쓸 수 있어야 해서 소유자를 app으로
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
USER app
EXPOSE 3000
CMD ["node", "server.js"]
