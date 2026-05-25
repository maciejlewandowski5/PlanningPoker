# Planning Poker

A real-time planning poker app for agile teams. Create a room, share the code with your team, vote with any number, and reveal results together.

## Features

- Create a room and share the 6-character code with your team
- Join any room by entering its code — no accounts needed
- Vote with any number or a custom value
- Anyone in the room can reveal, hide, or reset votes
- Real-time updates via WebSocket — all participants see changes instantly
- Single-container deployment (frontend served as static files from the backend)

## Running

**Build and run (production):**

```bash
./gradlew run
```

Open `http://localhost:8080`.

**Docker:**

```bash
./gradlew build
docker build -t planning-poker .
docker run -p 8080:8080 -v ./data:/app/data planning-poker
```

SQLite data is persisted in `/app/data` inside the container — mount a volume to keep it across restarts.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/rooms` | Create a new room, returns `{ roomId, code }` |
| `POST` | `/rooms/{code}/join` | Join a room by code, body: `{ displayName }`, returns `{ participantId, roomId, code }` |
| `WS` | `/rooms/{code}/ws?participantId={id}` | WebSocket connection for real-time room state |
| `GET` | `/openapi` | OpenAPI spec |
| `GET` | `/openapi/swagger` | Swagger UI |

**WebSocket messages (client → server):**

```json
{ "type": "vote", "value": "5" }
{ "type": "reveal" }
{ "type": "hide" }
{ "type": "reset" }
```

**WebSocket messages (server → client):**

```json
{
  "type": "state",
  "roomId": "...",
  "code": "ABC123",
  "votesRevealed": false,
  "participants": [
    { "participantId": "...", "displayName": "Alice", "hasVoted": true, "vote": null }
  ]
}
```

Votes are `null` until revealed. After reveal, `vote` contains the submitted value.

## Tech Stack

- **Backend:** Ktor 3.5, Kotlin 2.3, Netty, SQLite + Exposed ORM
- **Frontend:** Compose HTML (Kotlin/JS), compiled to a JS bundle served as static files
- **Build:** Gradle multi-project — `:frontend` webpack output is copied into the backend JAR at build time

## Development

```bash
# Run tests
./gradlew test

# Build only (no run)
./gradlew build

# Compile frontend only
./gradlew :frontend:compileKotlinJs

# HTTP test file (IntelliJ HTTP Client or httpyac)
# See planning-poker.http
```
