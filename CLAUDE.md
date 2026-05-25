# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the server (serves frontend + backend at :8080)
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.ServerTest"

# Build everything (frontend webpack + backend JAR)
./gradlew build

# Compile frontend only (fast check)
./gradlew :frontend:compileKotlinJs

# Run with CORS enabled (allows frontend dev server at :3000 to reach backend)
$env:DEV_MODE="true"; ./gradlew run   # PowerShell
DEV_MODE=true ./gradlew run           # bash
```

## Architecture

This is a Ktor 3.5 server (Netty, JVM 21) with a Compose HTML (Kotlin/JS) frontend compiled to a static JS bundle and embedded in the backend JAR.

### Backend (`src/main/kotlin/`)

Bootstrapped via `EngineMain`. Modules are declared in `src/main/resources/application.yaml`:

- **`configureDatabase`** (`Database.kt`) — connects to SQLite via Exposed ORM, creates schema on startup. DB path comes from `DATABASE_URL` env var, then `application.yaml`, then defaults to `./data/planningpoker.db`.
- **`configureHttp`** (`Http.kt`) — installs ContentNegotiation (JSON), WebSockets, and CORS (only when `DEV_MODE=true`). Also registers OpenAPI/Swagger at `/openapi`.
- **`configureDependencyInjection`** (`DI.kt`) — no-op; dependencies are wired directly in `configureRouting`.
- **`configureRouting`** (`Routing.kt`) — serves frontend static files from `resources/static`, mounts `roomRoutes` and `webSocketRoutes`.

**Domain files:**
- `Tables.kt` — Exposed table objects: `Rooms`, `Participants`, `Votes`
- `Models.kt` — `@Serializable` DTOs shared between routes and WebSocket
- `RoomRepository.kt` — all DB access, wrapped in `withContext(Dispatchers.IO) { transaction { } }`
- `SessionRegistry.kt` — thread-safe map of roomId → set of WebSocket sessions for broadcasting
- `RoomService.kt` — orchestrates repo + registry; broadcasts updated state after every mutation
- `RoomRoutes.kt` — `POST /rooms`, `POST /rooms/{code}/join`
- `WebSocketRoutes.kt` — `WS /rooms/{code}/ws?participantId=...`; handles vote/reveal/hide/reset messages

### Frontend (`frontend/src/jsMain/kotlin/com/example/poker/`)

Kotlin/JS module compiled by webpack. Output (`planning-poker.js`) is copied into `build/resources/main/static` at build time via the `copyFrontendDist` Gradle task.

- `main.kt` — entry point, mounts `App()` on `<div id="root">`
- `App.kt` — screen router using `remember { mutableStateOf<Screen>(...) }`
- `api/Models.kt` — `@Serializable` DTOs mirroring backend `Models.kt`
- `api/ApiClient.kt` — `createRoom()` and `joinRoom()` via `window.fetch` + `kotlinx.coroutines.await`
- `ws/WebSocketClient.kt` — `roomWebSocketFlow()` wraps browser WebSocket in a `callbackFlow` with exponential-backoff reconnect
- `state/AppState.kt` — `sealed class Screen` (Home, Join, Room)
- `ui/HomeScreen.kt` — create room + join by code
- `ui/JoinScreen.kt` — display name entry
- `ui/RoomScreen.kt` — voting controls, participant cards, reveal/hide/reset actions, stats on reveal
- `ui/components/` — `Styles.kt` (color/spacing constants, CSS helper extensions), `ParticipantCard.kt`, `VoteButton.kt`

### Gradle setup

- Root `build.gradle.kts` — backend (Ktor, JVM). Declares frontend plugins with `apply false` to resolve versions before subprojects apply them. `copyFrontendDist` task copies webpack output before `processResources`.
- `frontend/build.gradle.kts` — Kotlin Multiplatform, JS/IR target, Compose HTML.
- `gradle/libs.versions.toml` — non-Ktor library versions (Kotlin, Compose Multiplatform, Exposed, coroutines, serialization, SQLite).
- `gradle/ktor-version-catalog` — Ktor versions via `io.ktor:ktor-version-catalog:3.5.0` (referenced as `ktorLibs`).

### Dependencies

Key version constraints:
- Kotlin 2.3.21
- Compose Multiplatform 1.8.0
- Ktor 3.5.0
- Exposed 0.61.0 — use `SchemaUtils.create()` (not `createMissing`), `Table.selectAll().where { }` (not the deprecated DSL)

### Testing

Tests use `testApplication { }` from `ktor-server-test-host` (in-process, no network binding). Each test creates a temp SQLite file to avoid cross-test state:

```kotlin
private fun ApplicationTestBuilder.setup() {
    val tempDb = createTempFile("planningpoker_test", ".db").toFile()
    environment { config = MapApplicationConfig("database.url" to "jdbc:sqlite:${tempDb.absolutePath}") }
    application { configureDatabase(); configureHttp(); configureDependencyInjection(); configureRouting() }
}
```

### CSS API notes (Compose HTML 1.8.0)

- `color()` and `backgroundColor()` require `CSSColorValue` — wrap hex strings with `Color("#rrggbb")`
- `background("transparent")` works with a string literal (shorthand property)
- `boxShadow`, `borderBottom`, `textTransform`, `outline` are not available as `StyleScope` extensions — use `property("box-shadow", "...")` etc.
- `border(0.px)` has no overload — use `property("border", "none")`
- `fontWeight` string overload is unreliable — use `property("font-weight", "600")`
- `InputType` is in `org.jetbrains.compose.web.attributes`, not `org.jetbrains.compose.web.dom`
- Number formatting: `"%.1f".format()` is not available in Kotlin/JS — use `value.asDynamic().toFixed(1) as String`
