# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the server
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.ServerTest"

# Build (compile without running)
./gradlew build
```

## Architecture

This is a Ktor 3.5 server running on Netty (JVM 21), bootstrapped via `EngineMain`. Configuration lives in `src/main/resources/application.yaml`, which declares the port (8080) and the list of modules to load at startup.

The server is split into three modules, each an extension function on `Application`:

- **`configureHttp`** ([Http.kt](src/main/kotlin/Http.kt)) — registers the OpenAPI spec and Swagger UI endpoints at `/openapi`.
- **`configureDependencyInjection`** ([DI.kt](src/main/kotlin/DI.kt)) — wires dependencies using Ktor's built-in DI plugin. Services are registered here and injected into routes via `call.resolve<T>()`.
- **`configureRouting`** ([Routing.kt](src/main/kotlin/Routing.kt)) — declares HTTP routes. Currently a single `GET /` returning "Hello, World!".

New modules must be added to the `modules` list in `application.yaml` to be loaded.

### Dependencies

Dependency versions for non-Ktor libraries are in `gradle/libs.versions.toml`. Ktor library versions are managed via the version catalog `io.ktor:ktor-version-catalog:3.5.0` (referenced as `ktorLibs` in `build.gradle.kts`).

### Testing

Tests use `testApplication { }` from `ktor-server-test-host`, which spins up an in-process server with no actual network binding. Call `configure()` inside the block to load the default `application.yaml` modules before making assertions.
