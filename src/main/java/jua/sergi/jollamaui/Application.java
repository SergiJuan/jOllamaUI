package jua.sergi.jollamaui;

import jua.sergi.jollamaui.config.AppConfig;
import jua.sergi.jollamaui.server.HttpServerManager;

import java.io.IOException;

/**
 * Main entry point for the jOllamaUI application.
 * Initializes and starts the embedded HTTP server.
 */
public class Application {

    public static void main(String[] args) {
        try {
            HttpServerManager server = new HttpServerManager(AppConfig.DEFAULT_PORT);
            server.start();

            System.out.println("Server started at http://localhost:" + AppConfig.DEFAULT_PORT);
            System.out.println("Press Ctrl+C to stop.");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
