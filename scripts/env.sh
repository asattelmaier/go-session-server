#!/bin/bash

# Configuration for local execution

export FIRESTORE_EMULATOR_ENABLED=true
export FIRESTORE_EMULATOR_HOST_PORT="localhost:9000"
export FIRESTORE_EMULATOR_PROJECT_ID="local-project"
export SECURITY_JWT_SECRET_KEY="8Gr0MjVACbywAYACtN6o0wl4FKIG4s3F2iOGwMA1BQLKXh5ScLIuun0PgZnZ94vm"
export SECURITY_JWT_ACCESS_TOKEN_EXPIRATION=86400000
export SECURITY_JWT_REFRESH_TOKEN_EXPIRATION=604800000
export SECURITY_GUEST_PASSWORD="guest-password"
export CORS_ALLOWED_ORIGINS="*"

# For Local socket server (if running via Docker):
export GAME_CLIENT_SOCKET_PROTOCOL="ws"
export GAME_CLIENT_SOCKET_HOST="localhost"
export GAME_CLIENT_SOCKET_PORT="8000"

# --- Java Configuration ---

# 1. Try to load local overrides (gitignored)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ENV_LOCAL="$SCRIPT_DIR/.env.local"

if [ -f "$ENV_LOCAL" ]; then
    echo "Loading local environment from $ENV_LOCAL"
    source "$ENV_LOCAL"
fi

# 2. Check if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    # 3. Fallback to system java or warn
    echo "[WARN] JAVA_HOME not set and default not found."
    echo "Please create '$ENV_LOCAL' and set JAVA_HOME there."
    echo "Example: export JAVA_HOME=/path/to/java-21"
fi

echo "Using JAVA_HOME=$JAVA_HOME"
