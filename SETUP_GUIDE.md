# Setup & Quick Start Guide

Get your Key-Value Store up and running in 5 minutes!

## 📋 Prerequisites

- **Java 17 or higher** installed
- Basic terminal/command line knowledge
- Text editor or IDE (VS Code, IntelliJ IDEA, Eclipse)

### Check Java Installation

```bash
java -version
javac -version
```

You should see something like:
```
java version "17.0.x" or higher
javac 17.0.x
```

If not installed, download from: https://adoptium.net/

## 🚀 Quick Start (Linux/Mac)

### Step 1: Make build script executable
```bash
chmod +x build.sh
```

### Step 2: Build and test everything
```bash
./build.sh full
```

This will:
- Clean previous builds
- Compile all source files
- Run unit tests
- Display results

### Step 3: Start the server (Terminal 1)
```bash
./build.sh server
```

You should see:
```
╔════════════════════════════════════════════╗
║  Key-Value Store Server Started            ║
╠════════════════════════════════════════════╣
║  Port: 6379                                ║
║  Cache Size: 0/10000                       ║
║  Thread Pool: 50 threads                   ║
╚════════════════════════════════════════════╝
```

### Step 4: Connect with client (Terminal 2)
```bash
./build.sh client
```

### Step 5: Try some commands
```
kvstore> SET hello world
OK

kvstore> GET hello
world

kvstore> SET counter 1
OK

kvstore> GET counter
1

kvstore> STATS
Size: 2/10000 (0.02% full), HashMap Load: 0.00

kvstore> HELP
Available Commands:
SET key value - Store key-value pair
GET key - Retrieve value for key
...
```

## 🪟 Quick Start (Windows)

### Step 1: Create directory structure
```cmd
mkdir src\com\kvstore\cache
mkdir src\com\kvstore\server
mkdir src\com\kvstore\client
mkdir src\com\kvstore\test
mkdir src\com\kvstore\benchmark
mkdir bin
```

### Step 2: Compile
```cmd
javac -d bin src\com\kvstore\cache\*.java src\com\kvstore\server\*.java src\com\kvstore\client\*.java src\com\kvstore\test\*.java src\com\kvstore\benchmark\*.java
```

### Step 3: Run tests
```cmd
java -cp bin com.kvstore.test.LRUCacheTest
```

### Step 4: Start server
```cmd
java -cp bin com.kvstore.server.KeyValueServer
```

### Step 5: Start client (new terminal)
```cmd
java -cp bin com.kvstore.client.KeyValueClient
```

## 🎯 Common Tasks

### Run with custom settings
```bash
# Custom port and cache size
./build.sh server 8080 5000

# Connect to custom server
./build.sh client localhost 8080
```

### Run benchmarks
```bash
./build.sh benchmark
```

### Clean and rebuild
```bash
./build.sh clean
./build.sh compile
```

## 🧪 Verify Installation

Run this test sequence to verify everything works:

```bash
# Terminal 1: Start server
./build.sh server

# Terminal 2: Test with client
./build.sh client

# In client, run these commands:
kvstore> SET test1 value1
kvstore> GET test1
kvstore> SET test2 value2
kvstore> SIZE
kvstore> STATS
kvstore> DELETE test1
kvstore> GET test1
kvstore> CLEAR
kvstore> SIZE
```

Expected output:
```
kvstore> SET test1 value1
OK

kvstore> GET test1
value1

kvstore> SET test2 value2
OK

kvstore> SIZE
2

kvstore> STATS
Size: 2/10000 (0.02% full), HashMap Load: 0.00

kvstore> DELETE test1
1

kvstore> GET test1
(nil)

kvstore> CLEAR
OK

kvstore> SIZE
0
```

## 🐛 Troubleshooting

### Issue: "java: command not found"
**Solution**: Install Java JDK 17+
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Mac with Homebrew
brew install openjdk@17

# Or download from: https://adoptium.net/
```

### Issue: "Address already in use"
**Solution**: Port 6379 is taken
```bash
# Use different port
./build.sh server 6380

# Connect to it
./build.sh client localhost 6380
```

### Issue: "Class not found"
**Solution**: Recompile
```bash
./build.sh clean
./build.sh compile
```

### Issue: Tests fail on Windows
**Solution**: Check line endings
```bash
# Convert to Windows line endings
dos2unix build.sh
```

### Issue: Permission denied on build.sh
**Solution**: Make it executable
```bash
chmod +x build.sh
```

## 📊 Performance Testing

### Quick performance check
```bash
# Terminal 1: Start server
./build.sh server

# Terminal 2: Run benchmark
./build.sh benchmark
```

Expected results (will vary by hardware):
```
=== Sequential Operations Benchmark ===
  SET: 10000 operations in 234.56 ms (42,631 ops/sec)
  GET: 10000 operations in 189.23 ms (52,852 ops/sec)

=== Concurrent Operations Benchmark (10 clients) ===
  Clients: 10
  Total operations: 20000
  Time: 456.78 ms
  Throughput: 43,785 ops/sec
```

## 🎓 Next Steps

### 1. Understand the code
- Start with `LRUCache.java` - see how LRU works
- Review `CustomHashMap.java` - understand hash table internals
- Study `KeyValueServer.java` - learn multi-threading patterns

### 2. Experiment
- Modify cache size and measure performance impact
- Add new commands (e.g., INCREMENT, DECREMENT)
- Implement new eviction policies (LFU, FIFO)

### 3. Prepare for interviews
- Be ready to explain the LRU algorithm
- Practice walking through HashMap implementation
- Discuss concurrency control strategy

### 4. Extend the project
- Add persistence (write-ahead log)
- Implement pub/sub
- Add data expiration (TTL)
- Support for different data types

## 📝 IDE Setup

### IntelliJ IDEA
1. File → Open → Select project folder
2. Right-click `src` → Mark Directory as → Sources Root
3. Run → Edit Configurations → Add Application
   - Main class: `com.kvstore.server.KeyValueServer`
   - Working directory: project root

### VS Code
1. Install "Extension Pack for Java"
2. Open project folder
3. Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Server",
            "request": "launch",
            "mainClass": "com.kvstore.server.KeyValueServer",
            "projectName": "kv-store"
        },
        {
            "type": "java",
            "name": "Client",
            "request": "launch",
            "mainClass": "com.kvstore.client.KeyValueClient",
            "projectName": "kv-store"
        }
    ]
}
```

### Eclipse
1. File → Import → Existing Projects into Workspace
2. Select project folder
3. Right-click project → Build Path → Configure Build Path
4. Add `src` as source folder

## 🎯 Testing Checklist

Before showing to recruiters, verify:

- [x] All tests pass (`./build.sh test`)
- [x] Server starts without errors
- [x] Client can connect and execute commands
- [x] Benchmark runs successfully
- [x] Code is well-commented
- [x] README is complete
- [x] Can explain every component

## 💡 Tips for Success

1. **Run tests frequently** - Catch issues early
2. **Use benchmarks** - Prove performance claims
3. **Keep code clean** - Readable > clever
4. **Document thoroughly** - Explain your decisions
5. **Practice explaining** - You'll be asked "why"

## 🆘 Need Help?

Common resources:
- Java Concurrency: "Java Concurrency in Practice" book
- Data Structures: "Introduction to Algorithms" (CLRS)
- System Design: "Designing Data-Intensive Applications"

---

**You're all set!** 🚀 Start with `./build.sh full` and explore from there.