#!/bin/bash

set -o errexit
set -o pipefail

sed -i -e 's/9000/'"${PORT}"'/g' ./firebase.json

firebase emulators:start --project "${PROJECT_ID}"
