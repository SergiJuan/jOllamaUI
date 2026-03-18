package jua.sergi.jollamaui.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import jua.sergi.jollamaui.config.AppConfig;
import jua.sergi.model.request.ChatRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Handles POST /api/chat - Streams chat responses from Ollama.
 * Request body: { "model": "llama3", "history": [...] }
 * Response: Server-Sent Events (SSE) stream with generated tokens.
 */
public class ChatHandler extends BaseHandler {

    private static final String SSE_DONE = "[DONE]";
    private static final String SSE_THINK_PREFIX = "[THINK]";

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
            JsonArray history = json.getAsJsonArray("history");

            if (model.isEmpty() || history == null || history.isEmpty()) {
                sendError(exchange, 400, "Model and history are required");
                return;
            }

            // Build the chat request with full conversation history
            ChatRequest chatRequest = new ChatRequest(model);
            for (var element : history) {
                JsonObject msg = element.getAsJsonObject();
                chatRequest.addMessage(
                        msg.get("role").getAsString(),
                        msg.get("content").getAsString()
                );
            }

            // Set SSE headers for streaming response
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody();
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                AppConfig.getOllamaClient().chatStreaming(chatRequest, chunk -> {
                    if (chunk.getMessage() == null) {
                        return;
                    }

                    // Handle thinking tokens (model's internal reasoning)
                    String thinking = chunk.getMessage().getThinking();
                    if (thinking != null && !thinking.isEmpty()) {
                        String escaped = thinking.replace("\n", "\\n");
                        writer.write("data: " + SSE_THINK_PREFIX + escaped + "\n\n");
                        writer.flush();
                    }

                    // Handle response tokens
                    String content = chunk.getMessage().getContent();
                    if (content != null && !content.isEmpty()) {
                        String escaped = content.replace("\n", "\\n");
                        writer.write("data: " + escaped + "\n\n");
                        writer.flush();
                    }
                });

                // Signal completion
                writer.write("data: " + SSE_DONE + "\n\n");
                writer.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendError(exchange, 500, e.getMessage());
            } catch (Exception ignored) {
                // Response already sent, ignore
            }
        }
    }
}
