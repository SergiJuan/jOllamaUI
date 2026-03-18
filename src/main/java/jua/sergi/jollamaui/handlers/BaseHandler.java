package jua.sergi.jollamaui.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jua.sergi.jollamaui.config.AppConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Base handler providing common functionality for all HTTP handlers.
 * Includes CORS support, request/response utilities, and error handling.
 */
public abstract class BaseHandler implements HttpHandler {

    /**
     * Reads the request body as a UTF-8 string.
     *
     * @param exchange the HTTP exchange
     * @return the request body as a string
     * @throws IOException if reading fails
     */
    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Sends a JSON response with the specified status code.
     *
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param body the response body as a string
     * @throws IOException if writing fails
     */
    protected void sendJsonResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        sendResponse(exchange, statusCode, "application/json", body);
    }

    /**
     * Sends a response with the specified content type and status code.
     *
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param contentType the content type
     * @param body the response body
     * @throws IOException if writing fails
     */
    protected void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends an error response as JSON.
     *
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param message the error message
     * @throws IOException if writing fails
     */
    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message != null ? message : "Unknown error");
        sendJsonResponse(exchange, statusCode, AppConfig.GSON.toJson(error));
    }

    /**
     * Adds CORS headers to allow cross-origin requests from browsers.
     *
     * @param exchange the HTTP exchange
     */
    protected void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Handles OPTIONS requests for CORS preflight.
     *
     * @param exchange the HTTP exchange
     * @return true if the request was handled (OPTIONS), false otherwise
     * @throws IOException if sending the response fails
     */
    protected boolean handleOptions(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /**
     * Validates that the request method matches the expected one.
     *
     * @param exchange the HTTP exchange
     * @param expectedMethod the expected HTTP method
     * @return true if the method matches, false otherwise
     * @throws IOException if sending the error response fails
     */
    protected boolean validateMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        addCorsHeaders(exchange);
        if (!expectedMethod.equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return false;
        }
        return true;
    }
}
