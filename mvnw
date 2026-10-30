#!/bin/sh
# Minimal Maven launcher: uses a system Maven if present, otherwise downloads Apache Maven 3.9.6 into .mvn/
set -e
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_VERSION=3.9.6
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi
MVN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
if [ ! -x "$MVN_HOME/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "Downloading Apache Maven $MAVEN_VERSION ..."
  curl -fsSL "$URL" | tar -xz -C "$BASE_DIR/.mvn"
fi
exec "$MVN_HOME/bin/mvn" "$@"
