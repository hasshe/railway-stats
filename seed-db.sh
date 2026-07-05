#!/bin/bash
# Database seeding script
# Run this before starting the app locally to populate with test data
# Usage: ./seed-db.sh

set -e

echo "🔄 Building and seeding database..."
cd "$(dirname "$0")"

# Build the project
echo "📦 Building project..."
mvn clean package -DskipTests -q

# Find the jar file (handles different naming)
JAR_FILE=$(find target -name "*.jar" -type f | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: No jar file found in target directory"
    exit 1
fi

echo "🌱 Seeding database with $JAR_FILE..."
java -jar "$JAR_FILE" --seed-db

echo "✅ Database seeding complete!"
