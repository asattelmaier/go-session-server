#!/bin/bash

# Load environment variables
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$SCRIPT_DIR/env.sh"

echo "Running tests..."
# Pass any arguments to mvnw (e.g. -Dtest=...)
(cd "$SCRIPT_DIR/.." && ./mvnw test "$@")
