package com.kvstore.benchmark;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark tool for measuring server performance.
 * Tests throughput, latency, and concurrent operations.
 */
public class Benchmark {
    
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  KV-Store Performance Benchmark            ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        runSequentialBenchmark();
        System.out.println();
        runConcurrentBenchmark(10);
        System.out.println();
        runConcurrentBenchmark(50);
        System.out.println();
        runLatencyBenchmark();
    }
    
    private static void runSequentialBenchmark() {
        System.out.println("=== Sequential Operations Benchmark ===");
        
        int operations = 10000;
        
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Skip welcome message
            in.readLine();
            
            // Benchmark SET operations
            long startSet = System.nanoTime();
            for (int i = 0; i < operations; i++) {
                out.println("SET key" + i + " value" + i);
                in.readLine(); // Read response
            }
            long endSet = System.nanoTime();
            
            double setTimeMs = (endSet - startSet) / 1_000_000.0;
            double setOpsPerSec = operations / (setTimeMs / 1000.0);
            
            // Benchmark GET operations
            long startGet = System.nanoTime();
            for (int i = 0; i < operations; i++) {
                out.println("GET key" + i);
                in.readLine(); // Read length line
                in.readLine(); // Read value
            }
            long endGet = System.nanoTime();
            
            double getTimeMs = (endGet - startGet) / 1_000_000.0;
            double getOpsPerSec = operations / (getTimeMs / 1000.0);
            
            System.out.printf("  SET: %d operations in %.2f ms (%.0f ops/sec)%n",
                operations, setTimeMs, setOpsPerSec);
            System.out.printf("  GET: %d operations in %.2f ms (%.0f ops/sec)%n",
                operations, getTimeMs, getOpsPerSec);
            
        } catch (IOException e) {
            System.err.println("Benchmark failed: " + e.getMessage());
        }
    }
    
    private static void runConcurrentBenchmark(int numClients) {
        System.out.println("=== Concurrent Operations Benchmark (" + numClients + " clients) ===");
        
        int operationsPerClient = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numClients);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong errors = new AtomicLong(0);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    try (Socket socket = new Socket(HOST, PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        
                        in.readLine(); // Skip welcome
                        
                        for (int j = 0; j < operationsPerClient; j++) {
                            String key = "client" + clientId + "_key" + j;
                            
                            // SET
                            out.println("SET " + key + " value" + j);
                            in.readLine();
                            
                            // GET
                            out.println("GET " + key);
                            in.readLine();
                            in.readLine();
                            
                            totalOperations.addAndGet(2);
                        }
                        
                    } catch (IOException e) {
                        errors.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        
        try {
            endLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.nanoTime();
        executor.shutdown();
        
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        double throughput = totalOperations.get() / (totalTimeMs / 1000.0);
        
        System.out.printf("  Clients: %d%n", numClients);
        System.out.printf("  Total operations: %d%n", totalOperations.get());
        System.out.printf("  Time: %.2f ms%n", totalTimeMs);
        System.out.printf("  Throughput: %.0f ops/sec%n", throughput);
        System.out.printf("  Errors: %d%n", errors.get());
    }
    
    private static void runLatencyBenchmark() {
        System.out.println("=== Latency Distribution Benchmark ===");
        
        int samples = 1000;
        long[] latencies = new long[samples];
        
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            in.readLine(); // Skip welcome
            
            // Warm up
            for (int i = 0; i < 100; i++) {
                out.println("SET warmup" + i + " value");
                in.readLine();
            }
            
            // Measure latencies
            for (int i = 0; i < samples; i++) {
                long start = System.nanoTime();
                out.println("GET warmup50");
                in.readLine();
                in.readLine();
                long end = System.nanoTime();
                
                latencies[i] = end - start;
            }
            
            // Calculate statistics
            java.util.Arrays.sort(latencies);
            
            double avgLatency = java.util.Arrays.stream(latencies).average().orElse(0) / 1_000_000.0;
            double p50 = latencies[samples / 2] / 1_000_000.0;
            double p95 = latencies[(int)(samples * 0.95)] / 1_000_000.0;
            double p99 = latencies[(int)(samples * 0.99)] / 1_000_000.0;
            double max = latencies[samples - 1] / 1_000_000.0;
            
            System.out.printf("  Samples: %d%n", samples);
            System.out.printf("  Average: %.3f ms%n", avgLatency);
            System.out.printf("  P50:     %.3f ms%n", p50);
            System.out.printf("  P95:     %.3f ms%n", p95);
            System.out.printf("  P99:     %.3f ms%n", p99);
            System.out.printf("  Max:     %.3f ms%n", max);
            
        } catch (IOException e) {
            System.err.println("Benchmark failed: " + e.getMessage());
        }
    }
}