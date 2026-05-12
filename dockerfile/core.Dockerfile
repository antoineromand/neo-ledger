# ---------- BUILD ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY dockerfile/settings/core.settings.gradle settings.gradle

COPY common common
COPY neo-ledger-core neo-ledger-core

RUN chmod +x gradlew

RUN ./gradlew :neo-ledger-core:bootJar --no-daemon


# ---------- RUNTIME ----------
FROM gcr.io/distroless/java21-debian13

WORKDIR /app

COPY --from=build /workspace/neo-ledger-core/build/libs/*.jar core.jar

EXPOSE 3001

ENTRYPOINT ["java","-jar","/app/core.jar"]
