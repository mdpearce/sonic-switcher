#!/bin/bash

# CI/CD Validation Script
# Run this locally to simulate the GitHub Actions PR checks

set -e  # Exit on error

echo "🚀 Running CI/CD validation checks..."
echo ""

echo "📋 Step 1: Running ktlint..."
./gradlew ktlintCheck
echo "✅ ktlint passed"
echo ""

echo "🧪 Step 2: Running unit tests..."
./gradlew test --continue
echo "✅ Unit tests passed"
echo ""

echo "✨ All checks passed! Your PR is ready."
echo ""
