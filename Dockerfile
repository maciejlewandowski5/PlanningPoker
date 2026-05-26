FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew

# Build frontend (compiles Kotlin/JS + runs webpack)
RUN ./gradlew :frontend:jsBrowserProductionWebpack --no-daemon

# Show exactly what the webpack task produced so we can debug path issues
RUN echo "=== frontend/build contents ===" && find frontend/build -type f | sort || echo "(frontend/build is empty or missing)"

# Copy webpack output to static resources — try known paths, then fall back to find
RUN mkdir -p src/main/resources/static && \
    if [ -d frontend/build/dist/js/productionExecutable ]; then \
      cp -r frontend/build/dist/js/productionExecutable/. src/main/resources/static/; \
    elif [ -d frontend/build/productionExecutable ]; then \
      cp -r frontend/build/productionExecutable/. src/main/resources/static/; \
    else \
      JS_FILE=$(find frontend/build -name "planning-poker.js" | head -1) && \
      [ -n "$JS_FILE" ] && cp -r "$(dirname "$JS_FILE")"/. src/main/resources/static/ || \
      (echo "ERROR: cannot find planning-poker.js anywhere in frontend/build" && exit 1); \
    fi && \
    echo "Static resources:" && ls src/main/resources/static/

# Build fat JAR
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/PlanningPoker-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
