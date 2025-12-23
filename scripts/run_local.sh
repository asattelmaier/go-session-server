#!/bin/bash

# Load environment variables
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$SCRIPT_DIR/env.sh"

echo "Starting Docker dependencies..."
(cd "$SCRIPT_DIR/../docker" && docker compose up -d --build start-firestore-emulator gnugo)

echo "Starting Go Session Server locally..."
(cd "$SCRIPT_DIR/.." && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev)
