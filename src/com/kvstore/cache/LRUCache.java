package com.kvstore.cache;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe LRU Cache implementation using doubly-linked list and custom hash map.
 * O(1) time complexity for both get and put operations.
 */
public class LRUCache<K, V> implements Serializable{
    
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final int capacity;
    private int size;
    private final Node<K, V> head; // Most recently used
    private final Node<K, V> tail; // Least recently used
    private final CustomHashMap<K, Node<K, V>> cache;
    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.size = 0;
        this.cache = new CustomHashMap<>(capacity);
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        
        // Initialize sentinel nodes
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Get value for key. Returns null if not found.
     * Moves accessed node to front (most recently used).
     */
    public V get(K key) {
        readLock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null) {
                return null;
            }
            
            // Upgrade to write lock to move node
            readLock.unlock();
            writeLock.lock();
            try {
                // Re-check in case of race condition
                node = cache.get(key);
                if (node != null) {
                    moveToHead(node);
                    return node.value;
                }
                return null;
            } finally {
                readLock.lock();
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Put key-value pair. Evicts LRU item if at capacity.
     */
    public void put(K key, V value) {
        writeLock.lock();
        try {
            Node<K, V> node = cache.get(key);
            
            if (node != null) {
                // Update existing node
                node.value = value;
                moveToHead(node);
            } else {
                // Create new node
                Node<K, V> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                size++;
                
                // Evict LRU if over capacity
                if (size > capacity) {
                    Node<K, V> removed = removeTail();
                    cache.remove(removed.key);
                    size--;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Remove key from cache.
     */
    public V remove(K key) {
        writeLock.lock();
        try {
            Node<K, V> node = cache.remove(key);
            if (node != null) {
                removeNode(node);
                size--;
                return node.value;
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Check if key exists.
     */
    public boolean containsKey(K key) {
        readLock.lock();
        try {
            return cache.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Get current size.
     */
    public int size() {
        readLock.lock();
        try {
            return size;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Clear all entries.
     */
    public void clear() {
        writeLock.lock();
        try {
            cache.clear();
            head.next = tail;
            tail.prev = head;
            size = 0;
        } finally {
            writeLock.unlock();
        }
    }
    
    // --- Private helper methods for doubly-linked list operations ---
    
    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }
    
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        removeNode(node);
        return node;
    }
    
    /**
     * Get statistics for monitoring.
     */
    public CacheStats getStats() {
        readLock.lock();
        try {
            return new CacheStats(size, capacity, cache.getLoadFactor());
        } finally {
            readLock.unlock();
        }
    }
    
    public static class CacheStats {
        public final int currentSize;
        public final int maxCapacity;
        public final double loadFactor;
        
        CacheStats(int currentSize, int maxCapacity, double loadFactor) {
            this.currentSize = currentSize;
            this.maxCapacity = maxCapacity;
            this.loadFactor = loadFactor;
        }
        
        @Override
        public String toString() {
            return String.format("Size: %d/%d (%.2f%% full), HashMap Load: %.2f",
                currentSize, maxCapacity, (currentSize * 100.0 / maxCapacity), loadFactor);
        }
    }
}
