# Go Session Server

> A session server for the [Go Mobile App](https://github.com/asattelmaier/go-mobile-app)

The Session Server is responsible for establishing a game session between two clients. It handles the communication
between the two clients and the game logic in the [Go Haskell Socket Server](https://github.com/asattelmaier/go-haskell)
.

## Build

To build the project, you can use the following Maven command:

```bash
mvn -B package --file pom.xml
```

## Running Services with Docker Compose

Run Go Session Server:

Before running the service, you need to provide your Google Credentials (`./google-credentials.json`) in the project's
source directory.

```bash
docker-compose -f ./docker/docker-compose.yml up -d start
```

Run Firebase Emulator:

Before running the service, ensure you've provided your Google Credentials (`./google-credentials.json`) in the
project's source directory.

```bash
docker-compose -f ./docker/docker-compose.yml up -d start-firestore-emulator
```

## Docker

First, you'll need to build the project. Once done, follow these steps for Docker image creation and pushing:

Create Go Session Server Image:

```bash
# Docker Hub
docker image build -t asattelmaier/go-session-server:latest -f docker/go-session-server/Dockerfile .
# Google Cloud
docker image build -t europe-west1-docker.pkg.dev/PROJECT_ID/go-services/go-session-server:latest -f docker/Dockerfile .
```

Push Go Session Server Image:

```bash
# Docker Hub
docker push asattelmaier/go-session-server:latest
# Google Cloud
docker push europe-west1-docker.pkg.dev/PROJECT_ID/go-services/go-session-server:latest
```

Create Firestore Emulator Image:

```bash
docker image build -t asattelmaier/firestore-emulator:latest -f docker/firestore-emulator/Dockerfile .
```
