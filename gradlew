#!/bin/sh
# Gradle wrapper script - downloads and runs Gradle
set -e
APP_NAME="Gradle"
GRADLE_VERSION="8.0"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

if [ ! -d "$DIST_DIR/gradle-${GRADLE_VERSION}" ]; then
    mkdir -p "$DIST_DIR"
    echo "Downloading Gradle $GRADLE_VERSION..."
    curl -sL "$DIST_URL" -o "$DIST_DIR/gradle.zip"
    unzip -qo "$DIST_DIR/gradle.zip" -d "$DIST_DIR"
    rm "$DIST_DIR/gradle.zip"
fi

exec "$DIST_DIR/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
