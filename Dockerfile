FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew

# Build frontend webpack bundle
RUN ./gradlew :frontend:jsBrowserProductionWebpack --no-daemon

# Find wherever webpack wrote its output and copy to static resources
RUN JS_FILE=$(find frontend/build -name "planning-poker.js" | head -1) && \
    if [ -z "$JS_FILE" ]; then echo "ERROR: planning-poker.js not found after webpack" && find frontend/build -type f | head -30 && exit 1; fi && \
    JS_DIR=$(dirname "$JS_FILE") && \
    echo "Webpack output found at: $JS_DIR" && \
    mkdir -p src/main/resources/static && \
    cp -r "$JS_DIR"/. src/main/resources/static/ && \
    echo "Copied files:" && ls src/main/resources/static/

# Build fat JAR (webpack task will be UP-TO-DATE)
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/PlanningPoker-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
