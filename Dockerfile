FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew

# Build frontend webpack bundle
RUN ./gradlew :frontend:jsBrowserProductionWebpack --no-daemon

# Copy JS bundle + index.html into static resources
# Kotlin/JS 2.x puts the webpack bundle and HTML resources in separate directories
RUN mkdir -p src/main/resources/static && \
    cp -r frontend/build/kotlin-webpack/js/productionExecutable/. src/main/resources/static/ && \
    cp -r frontend/build/processedResources/js/main/. src/main/resources/static/ && \
    echo "Static resources:" && ls src/main/resources/static/

# Build fat JAR (webpack task will be UP-TO-DATE)
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/PlanningPoker-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
