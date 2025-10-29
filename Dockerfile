# syntax=docker/dockerfile:1.7
########## build stage ##########
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle/wrapper ./gradle/wrapper
RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --version

COPY src ./src
# 테스트는 CI에서 돌고 있으니 그대로 -x test 유지
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean bootJar -x test --no-daemon --scan

########## runtime stage ##########
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=10001:0 /workspace/build/libs/*.jar /app/app.jar

USER 10001
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=10 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]