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

echo "📊 Step 3: Generating coverage reports..."
./gradlew koverXmlReport koverHtmlReport
echo "✅ Coverage reports generated"
echo ""

echo "📈 Coverage Summary:"
./gradlew koverLog | grep -A 20 "Coverage summary"
echo ""

echo "✨ All checks passed! Your PR is ready."
echo ""
echo "View coverage report:"
echo "  HTML: build/reports/kover/html/index.html"
echo "  XML:  build/reports/kover/report.xml"
