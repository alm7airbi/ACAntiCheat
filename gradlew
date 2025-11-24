#!/usr/bin/env sh
# Minimal wrapper to invoke the available Gradle installation. This allows
# offline builds in constrained environments without downloading a full
# Gradle distribution.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "Gradle is required to build this project. Please install Gradle 8.14+ or add a distribution." >&2
  exit 1
fi
