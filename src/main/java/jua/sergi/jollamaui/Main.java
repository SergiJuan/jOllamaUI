package jua.sergi.jollamaui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jua.sergi.OllamaClient;
import jua.sergi.manager.ModelManager;
import jua.sergi.model.entity.ModelInfo;
import jua.sergi.model.request.ChatRequest;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Servidor web embebido para la interfaz de jOllama.
 * Sirve el frontend (index.html desde resources) y expone endpoints REST.
 */
public class Main {

    private static final int PORT = 8080;
    private static final Gson gson = new Gson();

    private static String ollamaHost = "http://localhost:11434";
    private static OllamaClient ollamaClient;
    private static ModelManager modelManager;

    public static void main(String[] args) throws IOException {
        initClients(ollamaHost);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", new StaticHandler());
        server.createContext("/api/models", new ModelsHandler());
        server.createContext("/api/models/", new ModelDeleteHandler());
        server.createContext("/api/pull", new PullHandler());
        server.createContext("/api/chat", new ChatHandler());
        server.createContext("/api/settings", new SettingsHandler());

        server.start();
        System.out.println("Servidor iniciado en http://localhost:" + PORT);
        System.out.println("Presiona Ctrl+C para detener.");
    }

    private static void initClients(String host) {
        ollamaClient  = OllamaClient.builder().host(host).build();
        modelManager  = ModelManager.builder().host(host).build();
    }

    // ─── Handlers ────────────────────────────────────────────────────────────

    /** Sirve index.html desde src/main/resources/index.html */
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!"/".equals(path) && !path.isEmpty() && !"/index.html".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            // Buscar el recurso en el classpath (resources/)
            InputStream is = Main.class.getClassLoader().getResourceAsStream("index.html");
            if (is == null) {
                String msg = "index.html no encontrado en resources/";
                sendResponse(exchange, 500, "text/plain", msg);
                return;
            }

            byte[] bytes = is.readAllBytes();
            is.close();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /** GET /api/models → lista de modelos instalados */
    static class ModelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                List<ModelInfo> models = modelManager.list();
                sendResponse(exchange, 200, "application/json", gson.toJson(models));
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /** POST /api/pull  { "model": "llama3" } → descarga un modelo */
    static class PullHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String model = json.get("model").getAsString().trim();
                if (model.isEmpty()) {
                    sendError(exchange, 400, "Model name required");
                    return;
                }
                var response = modelManager.pull(model);
                if (response.isSuccess()) {
                    sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
                } else {
                    sendError(exchange, 500, response.getStatus());
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /** DELETE /api/models/{name} → elimina un modelo */
    static class ModelDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String modelName = path.substring("/api/models/".length());
            if (modelName.isEmpty()) {
                sendError(exchange, 400, "Model name required");
                return;
            }
            try {
                modelManager.delete(modelName);
                sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    /**
     * POST /api/chat
     * Body: { "model": "llama3", "history": [ {"role":"user","content":"..."}, ... ] }
     * Devuelve: stream SSE con los tokens generados por el modelo.
     *
     * Usa el historial completo para mantener el contexto de la conversación.
     */
    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                String model = json.get("model").getAsString().trim();
                JsonArray history = json.getAsJsonArray("history");

                if (model.isEmpty() || history == null || history.isEmpty()) {
                    sendError(exchange, 400, "model and history required");
                    return;
                }

                // Construir ChatRequest con todo el historial
                ChatRequest chatRequest = new ChatRequest(model);
                for (var element : history) {
                    JsonObject msg = element.getAsJsonObject();
                    chatRequest.addMessage(
                            msg.get("role").getAsString(),
                            msg.get("content").getAsString()
                    );
                }

                // Cabeceras SSE para streaming token a token
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
                exchange.sendResponseHeaders(200, 0);

                try (OutputStream os = exchange.getResponseBody();
                     PrintWriter writer = new PrintWriter(
                             new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                    ollamaClient.chatStreaming(chatRequest, chunk -> {
                        if (chunk.getMessage() == null) return;

                        // Token de pensamiento
                        String thinking = chunk.getMessage().getThinking();
                        if (thinking != null && !thinking.isEmpty()) {
                            String t = thinking.replace("\n", "\\n");
                            writer.write("data: [THINK]" + t + "\n\n");
                            writer.flush();
                        }

                        // Token de respuesta normal
                        String content = chunk.getMessage().getContent();
                        if (content != null && !content.isEmpty()) {
                            String c = content.replace("\n", "\\n");
                            writer.write("data: " + c + "\n\n");
                            writer.flush();
                        }
                    });

                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                }

            } catch (Exception e) {
                e.printStackTrace();
                try { sendError(exchange, 500, e.getMessage()); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * POST /api/settings  { "host": "http://..." }
     * Cambia el host de Ollama en caliente y reinicializa los clientes.
     */
    static class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String host = json.get("host").getAsString().trim();
                if (host.isEmpty()) {
                    sendError(exchange, 400, "Host cannot be empty");
                    return;
                }
                ollamaHost = host;
                initClients(ollamaHost);
                System.out.println("Host actualizado a: " + ollamaHost);
                sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        JsonObject err = new JsonObject();
        err.addProperty("error", message != null ? message : "Unknown error");
        sendResponse(exchange, code, "application/json", gson.toJson(err));
    }

    /** Añade cabeceras CORS para que el navegador no bloquee las peticiones. */
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}