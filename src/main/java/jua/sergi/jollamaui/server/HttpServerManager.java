package jua.sergi.jollamaui.server;

import com.sun.net.httpserver.HttpServer;
import jua.sergi.jollamaui.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Manages the embedded HTTP server lifecycle and route registration.
 */
public class HttpServerManager {

    private final int port;
    private HttpServer server;

    public HttpServerManager(int port) {
        this.port = port;
    }

    /**
     * Starts the HTTP server and registers all API endpoints.
     *
     * @throws IOException if the server cannot be created
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        registerRoutes();

        server.start();
    }

    /**
     * Stops the HTTP server immediately.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Registers all HTTP route handlers.
     */
    private void registerRoutes() {
        // Static files (serves the web UI)
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/models", new ModelsHandler());
        server.createContext("/api/models/", new ModelDeleteHandler());
        server.createContext("/api/pull", new PullHandler());
        server.createContext("/api/chat", new ChatHandler());
        server.createContext("/api/settings", new SettingsHandler());
    }
}
