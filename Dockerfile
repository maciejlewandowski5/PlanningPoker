FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/PlanningPoker-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
