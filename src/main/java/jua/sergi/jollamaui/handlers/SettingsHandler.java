package jua.sergi.jollamaui.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import jua.sergi.jollamaui.config.AppConfig;

import java.io.IOException;

/**
 * Handles POST /api/settings - Updates the Ollama server configuration.
 * Request body: { "host": "http://localhost:11434" }
 * Reinitializes clients with the new host on success.
 */
public class SettingsHandler extends BaseHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }

        if (!validateMethod(exchange, "POST")) {
            return;
        }

        try {
            String body = readRequestBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String host = json.get("host").getAsString().trim();

            if (host.isEmpty()) {
                sendError(exchange, 400, "Host cannot be empty");
                return;
            }

            // Reinitialize clients with the new host
            AppConfig.initializeClients(host);
            System.out.println("Host updated to: " + host);

            sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }
}
