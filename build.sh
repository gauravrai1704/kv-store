#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project directories
SRC_DIR="src"
BIN_DIR="bin"

# Print colored message
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Print section header
print_header() {
    echo ""
    print_message "$BLUE" "════════════════════════════════════════════"
    print_message "$BLUE" " $1"
    print_message "$BLUE" "════════════════════════════════════════════"
    echo ""
}

# Clean build directory
clean() {
    print_header "Cleaning build directory"
    rm -rf "$BIN_DIR"
    print_message "$GREEN" "✓ Build directory cleaned"
}

# Compile all source files
compile() {
    print_header "Compiling source files"
    
    # Create bin directory if it doesn't exist
    mkdir -p "$BIN_DIR"
    
    # Find all Java files
    java_files=$(find "$SRC_DIR" -name "*.java")
    
    if [ -z "$java_files" ]; then
        print_message "$RED" "✗ No Java files found in $SRC_DIR"
        exit 1
    fi
    
    # Compile
    javac -d "$BIN_DIR" $java_files
    
    if [ $? -eq 0 ]; then
        print_message "$GREEN" "✓ Compilation successful"
        
        # Count compiled classes
        class_count=$(find "$BIN_DIR" -name "*.class" | wc -l)
        print_message "$GREEN" "  Generated $class_count class files"
    else
        print_message "$RED" "✗ Compilation failed"
        exit 1
    fi
}

# Run tests
test() {
    print_header "Running tests"
    java -cp "$BIN_DIR" com.kvstore.test.LRUCacheTest
    
    if [ $? -eq 0 ]; then
        print_message "$GREEN" "✓ All tests passed"
    else
        print_message "$RED" "✗ Tests failed"
        exit 1
    fi
}

# Run benchmarks
benchmark() {
    print_header "Running benchmarks"
    java -cp "$BIN_DIR" com.kvstore.benchmark.Benchmark
}

# Start server
server() {
    print_header "Starting server"
    
    local port=${1:-6379}
    local cache_size=${2:-10000}
    
    print_message "$YELLOW" "Server configuration:"
    echo "  Port: $port"
    echo "  Cache size: $cache_size"
    echo ""
    print_message "$YELLOW" "Press Ctrl+C to stop the server"
    echo ""
    
    java -cp "$BIN_DIR" com.kvstore.server.KeyValueServer --port "$port" --cache-size "$cache_size"
}

# Start client
client() {
    print_header "Starting client"
    
    local host=${1:-localhost}
    local port=${2:-6379}
    
    java -cp "$BIN_DIR" com.kvstore.client.KeyValueClient --host "$host" --port "$port"
}

# Run full build and test
full() {
    clean
    compile
    test
    
    print_header "Build Summary"
    print_message "$GREEN" "✓ Clean successful"
    print_message "$GREEN" "✓ Compilation successful"
    print_message "$GREEN" "✓ Tests passed"
    echo ""
    print_message "$BLUE" "Ready to run!"
    echo ""
    echo "Start server: ./build.sh server"
    echo "Start client: ./build.sh client"
    echo "Run benchmark: ./build.sh benchmark"
}

# Show usage
usage() {
    echo "Usage: ./build.sh [command] [options]"
    echo ""
    echo "Commands:"
    echo "  clean              - Remove compiled files"
    echo "  compile            - Compile source files"
    echo "  test               - Run unit tests"
    echo "  benchmark          - Run performance benchmarks"
    echo "  server [port] [cache-size] - Start server (default: port=6379, cache-size=10000)"
    echo "  client [host] [port] - Start client (default: host=localhost, port=6379)"
    echo "  full               - Clean, compile, and test"
    echo "  help               - Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./build.sh full"
    echo "  ./build.sh server 6379 5000"
    echo "  ./build.sh client localhost 6379"
    echo "  ./build.sh benchmark"
}

# Main script logic
case "${1:-help}" in
    clean)
        clean
        ;;
    compile)
        compile
        ;;
    test)
        if [ ! -d "$BIN_DIR" ]; then
            compile
        fi
        test
        ;;
    benchmark)
        if [ ! -d "$BIN_DIR" ]; then
            compile
        fi
        benchmark
        ;;
    server)
        if [ ! -d "$BIN_DIR" ]; then
            compile
        fi
        server "${2}" "${3}"
        ;;
    client)
        if [ ! -d "$BIN_DIR" ]; then
            compile
        fi
        client "${2}" "${3}"
        ;;
    full)
        full
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        print_message "$RED" "Unknown command: $1"
        echo ""
        usage
        exit 1
        ;;
esac