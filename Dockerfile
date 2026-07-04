FROM eclipse-temurin:17

WORKDIR /app

COPY . .

CMD ["./gradlew", "bootRun"]