package com.kvstore.server;

import java.io.*;
import com.kvstore.cache.LRUCache;
import java.util.Arrays;

/**
 * Handles parsing and execution of Redis-like commands.
 * Supports: SET, GET, DELETE, EXISTS, CLEAR, STATS, PING
 */
public class CommandHandler {
    
    private final LRUCache<String, String> cache;
    
    public CommandHandler(LRUCache<String, String> cache) {
        this.cache = cache;
    }
    
    /**
     * Parse and execute command.
     */
    public String handleCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return "-ERR empty command\r\n";
        }
        
        String[] parts = parseCommand(commandLine.trim());
        if (parts.length == 0) {
            return "-ERR empty command\r\n";
        }
        
        String command = parts[0].toUpperCase();
        
        try {
            switch (command) {
                case "SET":
                    return handleSet(parts);
                case "GET":
                    return handleGet(parts);
                case "DELETE":
                case "DEL":
                    return handleDelete(parts);
                case "EXISTS":
                    return handleExists(parts);
                case "CLEAR":
                    return handleClear(parts);
                case "STATS":
                    return handleStats(parts);
                case "PING":
                    return handlePing(parts);
                case "SIZE":
                    return handleSize(parts);
                case "SAVE":
                    return handleSave(parts);
                case "HELP":
                    return handleHelp();
                default:
                    return "-ERR unknown command '" + command + "'\r\n";
            }
        } catch (Exception e) {
            return "-ERR " + e.getMessage() + "\r\n";
        }
    }
    
    /**
     * Parse command line into tokens, handling quoted strings.
     */
    private String[] parseCommand(String line) {
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        int len = line.length();
        int tokenCount = 0;
        
        // First pass: count tokens
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false;
                tokenCount++;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokenCount++;
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokenCount++;
        }
        
        // Second pass: extract tokens
        String[] tokens = new String[tokenCount];
        current.setLength(0);
        inQuotes = false;
        int tokenIndex = 0;
        
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false;
                tokens[tokenIndex++] = current.toString();
                current.setLength(0);
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens[tokenIndex++] = current.toString();
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens[tokenIndex] = current.toString();
        }
        
        return tokens;
    }
    
    private String handleSet(String[] parts) {
        if (parts.length < 3) {
            return "-ERR SET requires key and value\r\n";
        }
        
        String key = parts[1];
        // Join remaining parts as value (in case value has spaces)
        String value = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        
        cache.put(key, value);
        return "+OK\r\n";
    }
    
    private String handleGet(String[] parts) {
        if (parts.length < 2) {
            return "-ERR GET requires key\r\n";
        }
        
        String key = parts[1];
        String value = cache.get(key);
        
        if (value == null) {
            return "$-1\r\n"; // Redis null bulk string
        }
        
        // Redis bulk string format: $<length>\r\n<data>\r\n
        return "$" + value.length() + "\r\n" + value + "\r\n";
    }
    
    private String handleDelete(String[] parts) {
        if (parts.length < 2) {
            return "-ERR DELETE requires key\r\n";
        }
        
        String key = parts[1];
        String removed = cache.remove(key);
        
        // Return 1 if deleted, 0 if not found
        return ":" + (removed != null ? 1 : 0) + "\r\n";
    }
    
    private String handleExists(String[] parts) {
        if (parts.length < 2) {
            return "-ERR EXISTS requires key\r\n";
        }
        
        String key = parts[1];
        boolean exists = cache.containsKey(key);
        
        return ":" + (exists ? 1 : 0) + "\r\n";
    }
    
    private String handleClear(String[] parts) {
        cache.clear();
        return "+OK\r\n";
    }
    
    private String handleStats(String[] parts) {
        LRUCache.CacheStats stats = cache.getStats();
        
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(stats.toString()).append("\r\n");
        
        return sb.toString();
    }
    
    private String handlePing(String[] parts) {
        if (parts.length > 1) {
            String message = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
            return "$" + message.length() + "\r\n" + message + "\r\n";
        }
        return "+PONG\r\n";
    }
    
    private String handleSize(String[] parts) {
        int size = cache.size();
        return ":" + size + "\r\n";
    }
    
    private String handleHelp() {
        StringBuilder help = new StringBuilder();
        help.append("Available Commands:\n");
        help.append("SET <key> <value>   - Store a key-value pair\n");
        help.append("GET <key>           - Retrieve a value by key\n");
        help.append("DELETE <key>        - Remove a key\n");
        help.append("EXISTS <key>        - Check if key exists\n");
        help.append("SIZE                - Get number of entries\n");
        help.append("STATS               - Get cache statistics\n");
        help.append("CLEAR               - Remove all entries\n");
        help.append("PING [message]      - Test connection\n");
        help.append("SAVE                - Save data to disk\n"); // Added this
        help.append("HELP                - Show this help");

        String helpText = help.toString();
        
        // Return as Bulk String: $<length>\r\n<content>\r\n
        return "$" + helpText.length() + "\r\n" + helpText + "\r\n";
    }

    private String handleSave(String[] parts) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("kvstore.dat"))) {
            // This attempts to serialize the entire LRUCache object
            oos.writeObject(cache);
            return "+OK Data saved\r\n";
        } catch (IOException e) {
            return "-ERR Failed to save: " + e.getMessage() + "\r\n";
        }
    }
}