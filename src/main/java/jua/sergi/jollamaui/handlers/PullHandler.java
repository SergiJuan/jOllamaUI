package jua.sergi.jollamaui.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import jua.sergi.jollamaui.config.AppConfig;

import java.io.IOException;

/**
 * Handles POST /api/pull - Downloads a model from the Ollama registry.
 * Request body: { "model": "llama3" }
 */
public class PullHandler extends BaseHandler {

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
            String model = json.get("model").getAsString().trim();

            if (model.isEmpty()) {
                sendError(exchange, 400, "Model name is required");
                return;
            }

            var response = AppConfig.getModelManager().pull(model);
            if (response.isSuccess()) {
                sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
            } else {
                sendError(exchange, 500, response.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }
}
