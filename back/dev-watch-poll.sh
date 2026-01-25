#!/bin/bash
# Development watch script using polling (works on Windows)
# Polls source files every 2 seconds and triggers Maven compilation on changes

echo "Starting development mode with hot reload (polling mode)..."
echo "Checking for file changes every 2 seconds..."
echo ""

# Start Spring Boot in the background
echo "Starting Spring Boot application..."
mvn spring-boot:run &
SPRING_PID=$!

echo "Spring Boot started with PID $SPRING_PID"
echo "Watching for file changes in /app/src..."
echo ""

# Store initial checksums
CHECKSUM_FILE="/tmp/checksums.txt"
find /app/src -name "*.kt" -o -name "*.java" | sort | xargs md5sum > "$CHECKSUM_FILE" 2>/dev/null

# Poll for changes
while true; do
    sleep 2

    # Calculate current checksums
    CURRENT_CHECKSUM=$(find /app/src -name "*.kt" -o -name "*.java" | sort | xargs md5sum 2>/dev/null)

    # Compare with previous checksums
    if [ "$CURRENT_CHECKSUM" != "$(cat $CHECKSUM_FILE 2>/dev/null)" ]; then
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📝 File change detected! Recompiling..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        # Compile the changed files
        mvn compile -q

        if [ $? -eq 0 ]; then
            echo "✅ Compilation successful - DevTools will restart the app"
            # Update checksums
            echo "$CURRENT_CHECKSUM" > "$CHECKSUM_FILE"
        else
            echo "❌ Compilation failed - check the errors above"
        fi
        echo ""
    fi
done
