package jua.sergi.jollamaui.config;

import com.google.gson.Gson;
import jua.sergi.OllamaClient;
import jua.sergi.manager.ModelManager;

/**
 * Application configuration and shared state.
 * Manages Ollama client instances and global settings.
 */
public class AppConfig {

    public static final int DEFAULT_PORT = 8080;
    public static final Gson GSON = new Gson();

    private static String ollamaHost = "http://localhost:11434";
    private static OllamaClient ollamaClient;
    private static ModelManager modelManager;

    static {
        initializeClients(ollamaHost);
    }

    /**
     * Initializes or reinitializes Ollama clients with the specified host.
     *
     * @param host the Ollama server URL
     */
    public static synchronized void initializeClients(String host) {
        ollamaHost = host;
        ollamaClient = OllamaClient.builder().host(host).build();
        modelManager = ModelManager.builder().host(host).build();
    }

    public static String getOllamaHost() {
        return ollamaHost;
    }

    public static OllamaClient getOllamaClient() {
        return ollamaClient;
    }

    public static ModelManager getModelManager() {
        return modelManager;
    }
}
