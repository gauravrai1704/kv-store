package com.kvstore.cache;

/**
 * Custom HashMap implementation using separate chaining for collision resolution.
 * Demonstrates understanding of hash table internals.
 * NOT thread-safe by itself - synchronization handled at LRUCache level.
 */
public class CustomHashMap<K, V> {
    
    private static class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next;
        final int hash;
        
        Entry(K key, V value, int hash, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = next;
        }
    }
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;
    
    private Entry<K, V>[] table;
    private int size;
    private int threshold;
    
    @SuppressWarnings("unchecked")
    public CustomHashMap(int initialCapacity) {
        int capacity = tableSizeFor(initialCapacity);
        this.table = (Entry<K, V>[]) new Entry[capacity];
        this.threshold = (int) (capacity * LOAD_FACTOR);
        this.size = 0;
    }
    
    public CustomHashMap() {
        this(DEFAULT_CAPACITY);
    }
    
    /**
     * Returns next power of 2 for capacity.
     */
    private int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
    }
    
    /**
     * Hash function using bit manipulation.
     */
    private int hash(K key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        // Spread bits to reduce collisions
        return h ^ (h >>> 16);
    }
    
    /**
     * Get index in table for given hash.
     */
    private int indexFor(int hash, int length) {
        return hash & (length - 1);
    }
    
    /**
     * Get value for key.
     */
    public V get(K key) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                return e.value;
            }
        }
        return null;
    }
    
    /**
     * Put key-value pair. Returns old value if key existed.
     */
    public V put(K key, V value) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        // Check if key exists
        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        
        // Add new entry
        addEntry(hash, key, value, index);
        return null;
    }
    
    /**
     * Add new entry at given index.
     */
    private void addEntry(int hash, K key, V value, int index) {
        Entry<K, V> e = table[index];
        table[index] = new Entry<>(key, value, hash, e);
        size++;
        
        if (size >= threshold) {
            resize(table.length * 2);
        }
    }
    
    /**
     * Remove entry for key.
     */
    public V remove(K key) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        Entry<K, V> prev = null;
        Entry<K, V> e = table[index];
        
        while (e != null) {
            Entry<K, V> next = e.next;
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                if (prev == null) {
                    table[index] = next;
                } else {
                    prev.next = next;
                }
                size--;
                return e.value;
            }
            prev = e;
            e = next;
        }
        return null;
    }
    
    /**
     * Check if key exists.
     */
    public boolean containsKey(K key) {
        return get(key) != null || (get(key) == null && hasNullValueForKey(key));
    }
    
    private boolean hasNullValueForKey(K key) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            if (e.hash == hash && (e.key == key || (key != null && key.equals(e.key)))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Resize hash table when load factor exceeded.
     */
    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Entry<K, V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        
        if (oldCapacity >= (1 << 30)) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        
        Entry<K, V>[] newTable = (Entry<K, V>[]) new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int) (newCapacity * LOAD_FACTOR);
    }
    
    /**
     * Transfer entries from old table to new table.
     */
    private void transfer(Entry<K, V>[] newTable) {
        Entry<K, V>[] src = table;
        int newCapacity = newTable.length;
        
        for (int j = 0; j < src.length; j++) {
            Entry<K, V> e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    Entry<K, V> next = e.next;
                    int i = indexFor(e.hash, newCapacity);
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }
    
    /**
     * Clear all entries.
     */
    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        size = 0;
    }
    
    /**
     * Get current size.
     */
    public int size() {
        return size;
    }
    
    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Get current load factor for monitoring.
     */
    public double getLoadFactor() {
        return (double) size / table.length;
    }
    
    /**
     * Get statistics about hash table performance.
     */
    public HashMapStats getStats() {
        int maxChainLength = 0;
        int usedBuckets = 0;
        int totalChainLength = 0;
        
        for (Entry<K, V> e : table) {
            if (e != null) {
                usedBuckets++;
                int chainLength = 0;
                for (Entry<K, V> curr = e; curr != null; curr = curr.next) {
                    chainLength++;
                }
                totalChainLength += chainLength;
                maxChainLength = Math.max(maxChainLength, chainLength);
            }
        }
        
        double avgChainLength = usedBuckets > 0 ? (double) totalChainLength / usedBuckets : 0;
        
        return new HashMapStats(size, table.length, usedBuckets, 
                               maxChainLength, avgChainLength, getLoadFactor());
    }
    
    public static class HashMapStats {
        public final int size;
        public final int capacity;
        public final int usedBuckets;
        public final int maxChainLength;
        public final double avgChainLength;
        public final double loadFactor;
        
        HashMapStats(int size, int capacity, int usedBuckets, 
                    int maxChainLength, double avgChainLength, double loadFactor) {
            this.size = size;
            this.capacity = capacity;
            this.usedBuckets = usedBuckets;
            this.maxChainLength = maxChainLength;
            this.avgChainLength = avgChainLength;
            this.loadFactor = loadFactor;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Size: %d, Capacity: %d, Used Buckets: %d, Load Factor: %.2f, " +
                "Max Chain: %d, Avg Chain: %.2f",
                size, capacity, usedBuckets, loadFactor, maxChainLength, avgChainLength
            );
        }
    }
}