# Go Session Server

> A session server for the [Go Mobile App](https://github.com/asattelmaier/go-mobile-app)

The Session Server is responsible for establishing a game session between two clients. It handles the communication
between the two clients and the game logic in the [Go Haskell Socket Server](https://github.com/asattelmaier/go-haskell)
.

## Build

```bash
mvn -B package --file pom.xml
```

## Docker

You must first build the project.

### Build

```bash
# Docker Hub
docker image build -t asattelmaier/go-session-server:latest -f docker/Dockerfile .
# Google Cloud
docker image build -t europe-west1-docker.pkg.dev/PROJECT_ID/go-services/go-session-server:latest -f docker/Dockerfile .
```

### Run

```bash
docker run -p 8080:8080 --name go-session-server --env GAME_CLIENT_SOCKET_HOST=host.docker.internal --env GAME_CLIENT_SOCKET_PORT=8000 asattelmaier/go-session-server:latest
```

### Push

```bash
# Docker Hub
docker push asattelmaier/website:latest
# Google Cloud
docker push europe-west1-docker.pkg.dev/PROJECT_ID/go-services/go-session-server:latest
```