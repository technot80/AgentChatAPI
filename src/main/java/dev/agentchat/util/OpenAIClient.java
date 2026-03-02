package dev.agentchat.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.agentchat.api.ChatResponse;
import dev.agentchat.api.ChatSessionImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIClient {

    private final String apiUrl;
    private final String apiKey;
    private final String provider;
    private final Gson gson;
    private final ExecutorService executor;

    public OpenAIClient(String apiUrl, String apiKey, String provider) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.provider = provider;
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
    }

    public CompletableFuture<ChatResponse> chat(List<ChatSessionImpl.ChatMessage> messages, String model, double temperature) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl + "/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("temperature", temperature);
                requestBody.addProperty("stream", false);

                if (provider != null && !provider.isEmpty()) {
                    JsonObject providerObj = new JsonObject();
                    JsonArray orderArray = new JsonArray();
                    orderArray.add(provider);
                    providerObj.add("order", orderArray);
                    providerObj.addProperty("allow_fallbacks", false);
                    requestBody.add("provider", providerObj);
                }

                JsonArray messagesArray = new JsonArray();
                for (ChatSessionImpl.ChatMessage msg : messages) {
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("role", msg.getRole());
                    messageObj.addProperty("content", msg.getContent());
                    messagesArray.add(messageObj);
                }
                requestBody.add("messages", messagesArray);

                String jsonBody = gson.toJson(requestBody);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    String errorBody = "";
                    try (InputStream errorStream = connection.getErrorStream()) {
                        if (errorStream != null) {
                            try (Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                                errorBody = scanner.hasNext() ? scanner.next() : "Unknown error";
                            }
                        }
                    }
                    if (errorBody.length() > 500) {
                        errorBody = errorBody.substring(0, 500) + "...";
                    }
                    return ChatResponse.error("API request failed: " + responseCode + " - " + errorBody);
                }

                String responseBody;
                try (InputStream inputStream = connection.getInputStream();
                     Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    responseBody = scanner.hasNext() ? scanner.next() : "";
                }

                if (responseBody.length() > 10000) {
                    return ChatResponse.error("API response too large");
                }

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                    JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message = choice.getAsJsonObject("message");
                        String content = message.get("content").getAsString();

                        int tokens = estimateTokens(messages, content);

                        return ChatResponse.success(content, tokens);
                    }
                }

                return ChatResponse.error("No response content from API");

            } catch (IOException e) {
                return ChatResponse.error("Network error: " + e.getMessage());
            } catch (Exception e) {
                return ChatResponse.error("Unexpected error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, executor);
    }

    private int estimateTokens(List<ChatSessionImpl.ChatMessage> messages, String response) {
        int total = 0;
        for (ChatSessionImpl.ChatMessage msg : messages) {
            total += msg.getContent().length() / 4;
        }
        total += response.length() / 4;
        return total;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
