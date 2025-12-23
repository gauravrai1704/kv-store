package com.kvstore.web;

import com.kvstore.cache.LRUCache;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple web dashboard for Key-Value Store.
 * Accessible via browser for non-technical users.
 */
public class WebDashboard {
    
    private final LRUCache<String, String> cache;
    private final int port;
    private HttpServer server;
    private long totalRequests = 0;
    private long startTime;
    
    public WebDashboard(LRUCache<String, String> cache, int port) {
        this.cache = cache;
        this.port = port;
        this.startTime = System.currentTimeMillis();
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Register endpoints
        server.createContext("/", this::handleRoot);
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/api/set", this::handleSet);
        server.createContext("/api/get", this::handleGet);
        server.createContext("/api/delete", this::handleDelete);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Web Dashboard started on http://localhost:" + port);
    }
    
    private void handleRoot(HttpExchange exchange) throws IOException {
        String html = getHtmlDashboard();
        sendResponse(exchange, 200, html, "text/html");
    }
    
    private void handleStats(HttpExchange exchange) throws IOException {
        totalRequests++;
        
        LRUCache.CacheStats stats = cache.getStats();
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        
        String json = String.format(
            "{\"size\":%d,\"capacity\":%d,\"loadFactor\":%.2f,\"uptime\":%d,\"totalRequests\":%d}",
            stats.currentSize, stats.maxCapacity, stats.loadFactor, uptime, totalRequests
        );
        
        sendResponse(exchange, 200, json, "application/json");
    }
    
    private void handleSet(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }
        
        Map<String, String> params = parseFormData(exchange);
        String key = params.get("key");
        String value = params.get("value");
        
        if (key == null || value == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing key or value\"}", "application/json");
            return;
        }
        
        cache.put(key, value);
        totalRequests++;
        
        sendResponse(exchange, 200, "{\"status\":\"ok\"}", "application/json");
    }
    
    private void handleGet(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String key = parseQueryParam(query, "key");
        
        if (key == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing key parameter\"}", "application/json");
            return;
        }
        
        String value = cache.get(key);
        totalRequests++;
        
        String json = value != null 
            ? String.format("{\"key\":\"%s\",\"value\":\"%s\"}", key, value)
            : "{\"error\":\"Key not found\"}";
        
        sendResponse(exchange, value != null ? 200 : 404, json, "application/json");
    }
    
    private void handleDelete(HttpExchange exchange) throws IOException {
        if (!"DELETE".equals(exchange.getRequestMethod()) && !"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            return;
        }
        
        String query = exchange.getRequestURI().getQuery();
        String key = parseQueryParam(query, "key");
        
        if (key == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing key parameter\"}", "application/json");
            return;
        }
        
        String removed = cache.remove(key);
        totalRequests++;
        
        String json = removed != null 
            ? "{\"status\":\"deleted\"}"
            : "{\"error\":\"Key not found\"}";
        
        sendResponse(exchange, removed != null ? 200 : 404, json, "application/json");
    }
    
    private String getHtmlDashboard() {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>KV-Store Dashboard</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container { 
            max-width: 1200px; 
            margin: 0 auto; 
        }
        h1 { 
            color: white; 
            text-align: center; 
            margin-bottom: 30px;
            font-size: 2.5em;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }
        .card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            margin-bottom: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }
        .stat {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        .stat-value {
            font-size: 2em;
            font-weight: bold;
            margin-bottom: 5px;
        }
        .stat-label {
            font-size: 0.9em;
            opacity: 0.9;
        }
        input, button {
            padding: 12px;
            border-radius: 6px;
            border: 1px solid #ddd;
            font-size: 1em;
            width: 100%;
            margin-bottom: 10px;
        }
        button {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            cursor: pointer;
            font-weight: bold;
            transition: transform 0.2s;
        }
        button:hover { transform: translateY(-2px); }
        button:active { transform: translateY(0); }
        .result {
            margin-top: 15px;
            padding: 15px;
            border-radius: 6px;
            background: #f0f0f0;
            min-height: 50px;
            font-family: monospace;
        }
        .grid { 
            display: grid; 
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); 
            gap: 20px; 
        }
        .success { background: #d4edda; color: #155724; }
        .error { background: #f8d7da; color: #721c24; }
        .footer {
            text-align: center;
            color: white;
            margin-top: 30px;
            font-size: 0.9em;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 Key-Value Store Dashboard</h1>
        
        <div class="card">
            <div class="stats" id="stats">
                <div class="stat">
                    <div class="stat-value" id="size">-</div>
                    <div class="stat-label">Entries</div>
                </div>
                <div class="stat">
                    <div class="stat-value" id="capacity">-</div>
                    <div class="stat-label">Capacity</div>
                </div>
                <div class="stat">
                    <div class="stat-value" id="uptime">-</div>
                    <div class="stat-label">Uptime (sec)</div>
                </div>
                <div class="stat">
                    <div class="stat-value" id="requests">-</div>
                    <div class="stat-label">Total Requests</div>
                </div>
            </div>
        </div>

        <div class="grid">
            <div class="card">
                <h2>Set Value</h2>
                <input type="text" id="setKey" placeholder="Key">
                <input type="text" id="setValue" placeholder="Value">
                <button onclick="setKV()">SET</button>
                <div class="result" id="setResult"></div>
            </div>

            <div class="card">
                <h2>Get Value</h2>
                <input type="text" id="getKey" placeholder="Key">
                <button onclick="getKV()">GET</button>
                <div class="result" id="getResult"></div>
            </div>

            <div class="card">
                <h2>Delete Value</h2>
                <input type="text" id="delKey" placeholder="Key">
                <button onclick="deleteKV()">DELETE</button>
                <div class="result" id="delResult"></div>
            </div>
        </div>

        <div class="footer">
            Built with pure Java | No frameworks | Production-ready
        </div>
    </div>

    <script>
        function updateStats() {
            fetch('/api/stats')
                .then(r => r.json())
                .then(data => {
                    document.getElementById('size').textContent = data.size;
                    document.getElementById('capacity').textContent = data.capacity;
                    document.getElementById('uptime').textContent = data.uptime;
                    document.getElementById('requests').textContent = data.totalRequests;
                });
        }

        function setKV() {
            const key = document.getElementById('setKey').value;
            const value = document.getElementById('setValue').value;
            const result = document.getElementById('setResult');
            
            if (!key || !value) {
                result.className = 'result error';
                result.textContent = 'Please provide both key and value';
                return;
            }
            
            fetch('/api/set', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `key=${encodeURIComponent(key)}&value=${encodeURIComponent(value)}`
            })
            .then(r => r.json())
            .then(data => {
                result.className = 'result success';
                result.textContent = `✓ Set ${key} = ${value}`;
                updateStats();
            })
            .catch(err => {
                result.className = 'result error';
                result.textContent = '✗ Error: ' + err.message;
            });
        }

        function getKV() {
            const key = document.getElementById('getKey').value;
            const result = document.getElementById('getResult');
            
            if (!key) {
                result.className = 'result error';
                result.textContent = 'Please provide a key';
                return;
            }
            
            fetch(`/api/get?key=${encodeURIComponent(key)}`)
                .then(r => r.json())
                .then(data => {
                    if (data.value) {
                        result.className = 'result success';
                        result.textContent = `✓ ${key} = ${data.value}`;
                    } else {
                        result.className = 'result error';
                        result.textContent = '✗ Key not found';
                    }
                    updateStats();
                })
                .catch(err => {
                    result.className = 'result error';
                    result.textContent = '✗ Error: ' + err.message;
                });
        }

        function deleteKV() {
            const key = document.getElementById('delKey').value;
            const result = document.getElementById('delResult');
            
            if (!key) {
                result.className = 'result error';
                result.textContent = 'Please provide a key';
                return;
            }
            
            fetch(`/api/delete?key=${encodeURIComponent(key)}`, {method: 'POST'})
                .then(r => r.json())
                .then(data => {
                    if (data.status === 'deleted') {
                        result.className = 'result success';
                        result.textContent = `✓ Deleted ${key}`;
                    } else {
                        result.className = 'result error';
                        result.textContent = '✗ Key not found';
                    }
                    updateStats();
                })
                .catch(err => {
                    result.className = 'result error';
                    result.textContent = '✗ Error: ' + err.message;
                });
        }

        // Update stats every 2 seconds
        updateStats();
        setInterval(updateStats, 2000);
    </script>
</body>
</html>
        """;
    }
    
    private void sendResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    private Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String formData = br.readLine();
        
        Map<String, String> result = new HashMap<>();
        if (formData != null) {
            for (String pair : formData.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    result.put(java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                              java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }
    
    private String parseQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
