#!/bin/bash
# Database clearing script
# Run this before starting the app locally to clear seeded/dev data
# Usage: ./clear-db.sh

set -e

echo "🧹 Clearing database..."
cd "$(dirname "$0")"

echo "📦 Building project..."
mvn clean package -DskipTests -q

# Find the jar file (handles different naming)
JAR_FILE=$(find target -name "*.jar" -type f | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: No jar file found in target directory"
    exit 1
fi

echo "🗑️  Clearing database with $JAR_FILE..."
java -jar "$JAR_FILE" --clear-db

echo "✅ Database cleared"
