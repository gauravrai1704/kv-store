package com.kvstore.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Simple CLI client for Key-Value store.
 */
public class KeyValueClient {
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;
    
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public KeyValueClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public KeyValueClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * Connect to server.
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
        // Read welcome message
        String welcome = readResponse();
        System.out.println(welcome);
    }
    
    /**
     * Send command and get response.
     */
    public String sendCommand(String command) throws IOException {
        out.println(command);
        return readResponse();
    }
    
    /**
     * Read response from server.
     */
    private String readResponse() throws IOException {
        String line = in.readLine();
        
        // FIX: Skip empty lines (handle stray newlines from server)
        while (line != null && line.isEmpty()) {
            line = in.readLine();
        }
        
        if (line == null) {
            return null;
        }
        
        char prefix = line.charAt(0);
        
        switch (prefix) {
            case '+': // Simple string
            case '-': // Error
            case ':': // Integer
                return line.substring(1);
                
            case '$': // Bulk string
                // Handle the case where the server might send "$5\r\nHello"
                try {
                    int length = Integer.parseInt(line.substring(1));
                    if (length == -1) {
                        return "(nil)";
                    }
                    char[] buffer = new char[length];
                    int charsRead = 0;
                    while (charsRead < length) {
                        int count = in.read(buffer, charsRead, length - charsRead);
                        if (count == -1) throw new IOException("Unexpected end of stream");
                        charsRead += count;
                    }
                    in.readLine(); // Read trailing \r\n
                    return new String(buffer);
                } catch (NumberFormatException e) {
                    return "Error: Invalid bulk string length";
                }
                
            default:
                // Fallback for non-protocol messages
                return line;
        }
    }
    
    /**
     * Close connection.
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Interactive CLI mode.
     */
    public void startInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  KV-Store Interactive Client               ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  Type 'HELP' for commands                  ║");
        System.out.println("║  Type 'QUIT' or 'EXIT' to disconnect       ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        while (true) {
            System.out.print("kvstore> ");
            String command = scanner.nextLine().trim();
            
            if (command.isEmpty()) {
                continue;
            }
            
            if (command.equalsIgnoreCase("QUIT") || command.equalsIgnoreCase("EXIT")) {
                System.out.println("Disconnecting...");
                break;
            }
            
            try {
                long startTime = System.nanoTime();
                String response = sendCommand(command);
                long endTime = System.nanoTime();
                
                double latencyMs = (endTime - startTime) / 1_000_000.0;
                
                System.out.println(response);
                System.out.printf("(%.3f ms)\n\n", latencyMs);
                
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                break;
            }
        }
        
        scanner.close();
    }
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--host") && i + 1 < args.length) {
                host = args[i + 1];
                i++;
            } else if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        
        KeyValueClient client = new KeyValueClient(host, port);
        
        try {
            System.out.println("Connecting to " + host + ":" + port + "...");
            client.connect();
            System.out.println("Connected!\n");
            
            client.startInteractiveMode();
            
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        } finally {
            client.close();
        }
    }
}