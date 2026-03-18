package jua.sergi.jollamaui.handlers;

import com.sun.net.httpserver.HttpExchange;
import jua.sergi.jollamaui.config.AppConfig;
import jua.sergi.model.entity.ModelInfo;

import java.io.IOException;
import java.util.List;

/**
 * Handles GET /api/models - Returns the list of installed Ollama models.
 */
public class ModelsHandler extends BaseHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }

        if (!validateMethod(exchange, "GET")) {
            return;
        }

        try {
            List<ModelInfo> models = AppConfig.getModelManager().list();
            sendJsonResponse(exchange, 200, AppConfig.GSON.toJson(models));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, e.getMessage());
        }
    }
}
