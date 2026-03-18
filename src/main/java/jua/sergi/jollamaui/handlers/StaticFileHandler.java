package jua.sergi.jollamaui.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves static files (HTML, CSS, JS) from the classpath resources.
 * This handler serves the web UI frontend assets.
 */
public class StaticFileHandler extends BaseHandler {

    private static final String INDEX_HTML = "index.html";
    private static final String RESOURCE_BASE = "";

    // MIME type mappings for common file extensions
    private static final java.util.Map<String, String> MIME_TYPES = new java.util.HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default to index.html for root path
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }

        // Remove leading slash for resource lookup
        String resourcePath = path.substring(1);

        // Security: prevent directory traversal attacks
        if (resourcePath.contains("..") || resourcePath.startsWith("/")) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        // Try to load the requested resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Resource not found - serve index.html for SPA routing
                // or return 404 for actual missing assets
                if (path.endsWith(".html") || path.equals("/index.html")) {
                    serveFallback(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
                return;
            }

            // Determine content type
            String contentType = getContentType(resourcePath);

            // Read and serve the file
            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /**
     * Serves the fallback index.html for SPA routing.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if serving fails
     */
    private void serveFallback(HttpExchange exchange) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(INDEX_HTML)) {
            if (is == null) {
                String msg = "index.html not found in resources";
                sendResponse(exchange, 500, "text/plain", msg);
                return;
            }

            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /**
     * Determines the MIME content type based on file extension.
     *
     * @param filename the filename to check
     * @return the MIME type string
     */
    private String getContentType(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            String ext = filename.substring(lastDot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
