# syntax=docker/dockerfile:1.7
# Spring Boot API 이미지 (멀티 스테이지: 빌드 → 레이어 추출 → 실행)
# 빌드: docker build -t medinfo-api .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# 의존성 먼저: 소스만 바뀌면 이 레이어는 캐시를 그대로 쓴다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar -x test
# 레이어 분리(의존성 / 로더 / 스냅샷 / 우리 코드) → 코드만 바뀌면 이미지 차이도 우리 코드 레이어만
RUN java -Djarmode=tools -jar build/libs/app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
USER app
ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# 8080 = 서비스, 8081 = 관리(헬스체크·지표). 8081은 외부(ALB 리스너)에 열지 않는다
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
