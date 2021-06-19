# Go Session Server

> A session server for the [Go Mobile App](https://github.com/asattelmaier/go-mobile-app)

The Session Server is responsible for establishing a game session between two clients. It handles the communication
between the two clients and the game logic in the [Go Haskell Socket Server](https://github.com/asattelmaier/go-haskell)
.

## Build

```bash
mvn -B package --file pom.xml
```

## Create Docker Container

You must first build the project.

Create Image:

```bash
docker build -t go-session-server -f docker/Dockerfile .
```

Run Container:

```bash
docker run -p 8080:8080 --name go-session-server --env GAME_CLIENT_SOCKET_HOST=host.docker.internal --env GAME_CLIENT_SOCKET_PORT=8000 go-session-server:latest
```
