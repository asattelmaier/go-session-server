# Go Session Server

> Backend service for the Go Game Project, handling sessions, authentication, and bot orchestration.

The **Session Server** is a Spring Boot application responsible for:
*   **Session Management:** Creation, matching, and lifecycle of game sessions.
*   **Authentication:** JWT-based user authentication.
*   **Game Orchestration:** Manages turn order, session state, and persistence.
*   **Game Rules & AI:** Delegates move validation and bot logic to the **GnuGo Sidecar**.

## Prerequisites
*   **Java 21** (Eclipse Temurin recommended)
*   **Docker** (for GnuGo and Firestore Emulator)

## Architecture
The server follows a **Sidecar Pattern** for AI integration:
*   **Main Container:** Spring Boot App (on port `8080`).
*   **Sidecar:** GnuGo Engine (on `localhost:8001`, TCP).

## Build & Test

This project uses **Maven** and the **Spock Framework** for testing.

```bash
# Build the application
./mvnw clean package

# Run all tests (Unit + Integration)
./mvnw test
```

## Running Locally (Docker Compose)

The easiest way to run the full stack (Session Server + GnuGo + Firestore Emulator) is via Docker Compose.

1.  Ensure you have `google-credentials.json` (if needed for production mode) or use the emulator profile.
2.  Start the stack:

```bash
docker compose -f ./docker/docker-compose.yml up --build
```

This will spin up:
-   `go-session-server` (Port 8080)
-   `gnugo` (Internal Port 8001, accessible by server)
-   `firestore-emulator` (Database)


