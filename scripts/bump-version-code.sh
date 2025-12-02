#!/bin/bash

# Script to bump versionCode in app/build.gradle.kts
# Usage: ./scripts/bump-version-code.sh

set -e

BUILD_FILE="app/build.gradle.kts"

# Extract current versionCode
CURRENT_VERSION=$(grep -E '^\s*versionCode\s*=\s*[0-9]+' "$BUILD_FILE" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/')

if [ -z "$CURRENT_VERSION" ]; then
    echo "❌ Error: Could not find versionCode in $BUILD_FILE"
    exit 1
fi

# Calculate new versionCode
NEW_VERSION=$((CURRENT_VERSION + 1))

echo "📦 Bumping versionCode: $CURRENT_VERSION → $NEW_VERSION"

# Update the file
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' -E "s/(versionCode\s*=\s*)$CURRENT_VERSION/\1$NEW_VERSION/" "$BUILD_FILE"
else
    # Linux
    sed -i -E "s/(versionCode\s*=\s*)$CURRENT_VERSION/\1$NEW_VERSION/" "$BUILD_FILE"
fi

echo "✅ Updated $BUILD_FILE"
echo "   versionCode: $NEW_VERSION"

# Verify the change
NEW_CHECK=$(grep -E '^\s*versionCode\s*=\s*[0-9]+' "$BUILD_FILE" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/')

if [ "$NEW_CHECK" != "$NEW_VERSION" ]; then
    echo "❌ Error: Version bump verification failed"
    exit 1
fi

echo "✅ Version bump successful!"
