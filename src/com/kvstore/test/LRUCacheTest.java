package com.kvstore.test;

import com.kvstore.cache.LRUCache;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive test suite for LRU Cache.
 * Run this to verify correctness and thread-safety.
 */
public class LRUCacheTest {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  LRU Cache Test Suite                      ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        testBasicOperations();
        testEvictionPolicy();
        testConcurrency();
        testEdgeCases();
        testPerformance();
        
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Test Results                              ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.printf("║  Passed: %-33d ║%n", testsPassed);
        System.out.printf("║  Failed: %-33d ║%n", testsFailed);
        System.out.println("╚════════════════════════════════════════════╝");
        
        if (testsFailed > 0) {
            System.exit(1);
        }
    }
    
    private static void testBasicOperations() {
        System.out.println("=== Basic Operations ===");
        
        LRUCache<String, String> cache = new LRUCache<>(3);
        
        // Test put and get
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"), "Put/Get single item");
        
        // Test multiple puts
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(3, cache.size(), "Size after 3 puts");
        
        // Test update existing key
        cache.put("key1", "updated1");
        assertEquals("updated1", cache.get("key1"), "Update existing key");
        assertEquals(3, cache.size(), "Size unchanged after update");
        
        // Test non-existent key
        assertNull(cache.get("nonexistent"), "Get non-existent key");
        
        // Test containsKey
        assertTrue(cache.containsKey("key1"), "Contains existing key");
        assertFalse(cache.containsKey("nonexistent"), "Does not contain non-existent key");
        
        // Test remove
        cache.remove("key2");
        assertNull(cache.get("key2"), "Get removed key");
        assertEquals(2, cache.size(), "Size after remove");
        
        // Test clear
        cache.clear();
        assertEquals(0, cache.size(), "Size after clear");
        assertNull(cache.get("key1"), "Get after clear");
        
        System.out.println();
    }
    
    private static void testEvictionPolicy() {
        System.out.println("=== Eviction Policy (LRU) ===");
        
        LRUCache<String, String> cache = new LRUCache<>(3);
        
        // Fill cache
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");
        
        // Access A to make it most recently used
        cache.get("A");
        
        // Add D - should evict B (least recently used)
        cache.put("D", "4");
        
        assertNull(cache.get("B"), "B should be evicted");
        assertEquals("1", cache.get("A"), "A should still exist");
        assertEquals("3", cache.get("C"), "C should still exist");
        assertEquals("4", cache.get("D"), "D should exist");
        
        // Update C to make it most recently used
        cache.put("C", "3-updated");
        
        // Add E - should evict A
        cache.put("E", "5");
        
        assertNull(cache.get("A"), "A should be evicted");
        assertEquals("3-updated", cache.get("C"), "C should still exist");
        
        System.out.println();
    }
    
    private static void testConcurrency() {
        System.out.println("=== Concurrency Test ===");
        
        LRUCache<Integer, String> cache = new LRUCache<>(1000);
        int numThreads = 10;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Concurrent writes
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = threadId * operationsPerThread + i;
                        cache.put(key, "value-" + key);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        assertEquals(0, errors.get(), "No concurrent modification errors");
        assertTrue(cache.size() <= 1000, "Cache size respects capacity under concurrency");
        
        System.out.println("  Concurrent operations: " + (numThreads * operationsPerThread));
        System.out.println("  Final cache size: " + cache.size());
        System.out.println();
    }
    
    private static void testEdgeCases() {
        System.out.println("=== Edge Cases ===");
        
        // Test capacity 1
        LRUCache<String, String> cache1 = new LRUCache<>(1);
        cache1.put("A", "1");
        cache1.put("B", "2");
        assertNull(cache1.get("A"), "Capacity 1: first item evicted");
        assertEquals("2", cache1.get("B"), "Capacity 1: second item exists");
        
        // Test null values
        LRUCache<String, String> cache2 = new LRUCache<>(3);
        cache2.put("key", null);
        assertTrue(cache2.containsKey("key"), "Contains key with null value");
        assertNull(cache2.get("key"), "Get key with null value");
        
        // Test repeated gets
        LRUCache<String, String> cache3 = new LRUCache<>(2);
        cache3.put("A", "1");
        cache3.put("B", "2");
        for (int i = 0; i < 10; i++) {
            cache3.get("A"); // Keep A hot
        }
        cache3.put("C", "3");
        assertEquals("1", cache3.get("A"), "Frequently accessed item not evicted");
        assertNull(cache3.get("B"), "Less accessed item evicted");
        
        System.out.println();
    }
    
    private static void testPerformance() {
        System.out.println("=== Performance Test ===");
        
        int cacheSize = 10000;
        int operations = 100000;
        LRUCache<Integer, String> cache = new LRUCache<>(cacheSize);
        
        // Warm up
        for (int i = 0; i < cacheSize; i++) {
            cache.put(i, "value-" + i);
        }
        
        // Test write performance
        long startWrite = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            cache.put(i % cacheSize, "value-" + i);
        }
        long endWrite = System.nanoTime();
        double writeTimeMs = (endWrite - startWrite) / 1_000_000.0;
        double writeThroughput = operations / (writeTimeMs / 1000.0);
        
        // Test read performance
        long startRead = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            cache.get(i % cacheSize);
        }
        long endRead = System.nanoTime();
        double readTimeMs = (endRead - startRead) / 1_000_000.0;
        double readThroughput = operations / (readTimeMs / 1000.0);
        
        System.out.printf("  Write: %.2f ms (%.0f ops/sec)%n", writeTimeMs, writeThroughput);
        System.out.printf("  Read:  %.2f ms (%.0f ops/sec)%n", readTimeMs, readThroughput);
        
        assertTrue(writeTimeMs < 5000, "Write performance acceptable");
        assertTrue(readTimeMs < 5000, "Read performance acceptable");
        
        System.out.println();
    }
    
    // === Helper assertion methods ===
    
    private static void assertEquals(Object expected, Object actual, String testName) {
        if ((expected == null && actual == null) || 
            (expected != null && expected.equals(actual))) {
            System.out.println("  ✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("  ✗ " + testName);
            System.out.println("    Expected: " + expected);
            System.out.println("    Actual:   " + actual);
            testsFailed++;
        }
    }
    
    private static void assertNull(Object actual, String testName) {
        if (actual == null) {
            System.out.println("  ✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("  ✗ " + testName);
            System.out.println("    Expected: null");
            System.out.println("    Actual:   " + actual);
            testsFailed++;
        }
    }
    
    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            System.out.println("  ✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("  ✗ " + testName);
            testsFailed++;
        }
    }
    
    private static void assertFalse(boolean condition, String testName) {
        assertTrue(!condition, testName);
    }
}