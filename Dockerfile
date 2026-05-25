FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
# Build frontend webpack bundle
RUN ./gradlew :frontend:jsBrowserProductionWebpack --no-daemon
# Copy frontend dist into static resources so processResources always finds it
RUN mkdir -p src/main/resources/static && \
    cp -r frontend/build/dist/js/productionExecutable/. src/main/resources/static/
# Build fat JAR (frontend webpack will be UP-TO-DATE)
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/PlanningPoker-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
