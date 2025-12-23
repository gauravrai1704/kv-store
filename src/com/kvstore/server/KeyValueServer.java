package com.kvstore.server;

import com.kvstore.cache.LRUCache;
import com.kvstore.web.WebDashboard;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-threaded TCP server for Key-Value store.
 * Handles concurrent connections using thread pool.
 */
public class KeyValueServer {
    
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_CACHE_SIZE = 10000;
    private static final int THREAD_POOL_SIZE = 50;
    private static final int MAX_QUEUE_SIZE = 1000;
    
    private final int port;
    private final LRUCache<String, String> cache;
    private WebDashboard dashboard;
    private final CommandHandler commandHandler;
    private final ExecutorService threadPool;
    private final AtomicBoolean running;
    private final AtomicLong requestCount;
    private final AtomicLong connectionCount;
    private ServerSocket serverSocket;
    
    public KeyValueServer(int port, int cacheSize) {
        this.port = port;
        this.cache = new LRUCache<>(cacheSize);
        this.commandHandler = new CommandHandler(cache);
        this.threadPool = new ThreadPoolExecutor(
            THREAD_POOL_SIZE / 2,
            THREAD_POOL_SIZE,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.running = new AtomicBoolean(false);
        this.requestCount = new AtomicLong(0);
        this.connectionCount = new AtomicLong(0);
    }
    
    public KeyValueServer() {
        this(DEFAULT_PORT, DEFAULT_CACHE_SIZE);
    }
    
    /**
     * Start the server.
     */
    public void start() throws IOException {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Server already running");
        }
        
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  Key-Value Store Server Started            ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  Port: " + port + "                              ║");
        System.out.println("║  Cache Size: " + cache.size() + "/" + DEFAULT_CACHE_SIZE + "                      ║");
        System.out.println("║  Thread Pool: " + THREAD_POOL_SIZE + " threads                 ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
        
        // Accept connections
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                connectionCount.incrementAndGet();
                
                // Handle client in thread pool
                threadPool.execute(new ClientHandler(clientSocket));
                
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gracefully shutdown server.
     */
    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }
        
        System.out.println("\nShutting down server...");
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Server stopped.");
        printStats();
    }
    
    /**
     * Print server statistics.
     */
    private void printStats() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Server Statistics                         ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  Total Connections: " + connectionCount.get() + "                   ║");
        System.out.println("║  Total Requests: " + requestCount.get() + "                      ║");
        System.out.println("║  Cache: " + cache.getStats() + "           ║");
        System.out.println("╚════════════════════════════════════════════╝");
    }
    
    /**
     * Client connection handler.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        
        ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            System.out.println("[" + Thread.currentThread().getName() + "] Client connected: " + clientAddress);
            
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                // Send welcome message
                out.println("+Welcome to KV-Store. Type HELP for commands.\r\n");
                
                String line;
                while ((line = in.readLine()) != null) {
                    requestCount.incrementAndGet();
                    
                    // Handle command
                    long startTime = System.nanoTime();
                    String response = commandHandler.handleCommand(line);
                    long endTime = System.nanoTime();
                    
                    out.print(response);
                    out.flush();
                    
                    // Log command execution
                    double latencyMs = (endTime - startTime) / 1_000_000.0;
                    System.out.printf("[%s] Command: %s | Latency: %.3f ms%n",
                        Thread.currentThread().getName(),
                        line.length() > 50 ? line.substring(0, 50) + "..." : line,
                        latencyMs
                    );
                    
                    // Exit command
                    if (line.trim().equalsIgnoreCase("QUIT") || 
                        line.trim().equalsIgnoreCase("EXIT")) {
                        break;
                    }
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client " + clientAddress + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
                System.out.println("[" + Thread.currentThread().getName() + "] Client disconnected: " + clientAddress);
            }
        }
    }
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        int cacheSize = DEFAULT_CACHE_SIZE;
        int webPort = 8080;
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("--cache-size") && i + 1 < args.length) {
                cacheSize = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("--web-port") && i + 1 < args.length) {
                webPort = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        
        final KeyValueServer server = new KeyValueServer(port, cacheSize);
        // Start web dashboard
        final WebDashboard dashboard = new WebDashboard(server.cache, webPort);
        
        // Shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            dashboard.stop();
        }));
        
        try {
            // Start dashboard in separate thread
            new Thread(() -> {
                try {
                    dashboard.start();
                } catch (IOException e) {
                    System.err.println("Failed to start dashboard: " + e.getMessage());
                }
            }).start();
            
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}