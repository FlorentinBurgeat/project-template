#!/bin/bash
# Development watch script for hot reload
# Watches source files and triggers Maven compilation + Spring Boot restart

echo "Starting development mode with hot reload..."
echo "DevTools will restart the application when files change"
echo ""

# Install inotify-tools if not present
if ! command -v inotifywait &> /dev/null; then
    echo "Installing inotify-tools for file watching..."
    apt-get update -qq && apt-get install -y -qq inotify-tools > /dev/null 2>&1
fi

# Start Spring Boot in the background
echo "Starting Spring Boot application..."
mvn spring-boot:run &
SPRING_PID=$!

echo "Spring Boot started with PID $SPRING_PID"
echo "Watching for file changes in /app/src..."
echo ""

# Watch for file changes and trigger recompilation
while true; do
    # Wait for any .kt or .java file to change
    inotifywait -e modify,create,delete -r /app/src --include '\.(kt|java)$' 2>/dev/null

    if [ $? -eq 0 ]; then
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📝 File change detected! Recompiling..."
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        # Compile the changed files
        mvn compile -q

        if [ $? -eq 0 ]; then
            echo "✅ Compilation successful - DevTools will restart the app"
        else
            echo "❌ Compilation failed - check the errors above"
        fi
        echo ""
    fi
done
