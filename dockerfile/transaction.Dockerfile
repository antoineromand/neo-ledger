# ---------- BUILD ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY neo-ledger-transaction/build.gradle .

COPY common common
COPY neo-ledger-transaction neo-ledger-transaction

RUN chmod +x gradlew

RUN ./gradlew :neo-ledger-transaction:bootJar --no-daemon


# ---------- RUNTIME ----------
FROM gcr.io/distroless/java21-debian13

WORKDIR /app

COPY --from=build /workspace/neo-ledger-transaction/build/libs/*.jar transaction.jar

EXPOSE 3000

ENTRYPOINT ["java","-jar","/app/transaction.jar"]