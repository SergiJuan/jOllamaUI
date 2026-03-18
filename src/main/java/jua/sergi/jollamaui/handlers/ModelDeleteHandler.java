package jua.sergi.jollamaui.handlers;

import com.sun.net.httpserver.HttpExchange;
import jua.sergi.jollamaui.config.AppConfig;

import java.io.IOException;

/**
 * Handles DELETE /api/models/{name} - Deletes a model from Ollama.
 */
public class ModelDeleteHandler extends BaseHandler {

    private static final String API_PATH = "/api/models/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }

        if (!validateMethod(exchange, "DELETE")) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String modelName = path.substring(API_PATH.length());

        if (modelName.isEmpty()) {
            sendError(exchange, 400, "Model name is required");
            return;
        }

        try {
            AppConfig.getModelManager().delete(modelName);
            sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }
}
