package dev.agentchat.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.agentchat.api.ChatResponse;
import dev.agentchat.api.ChatSessionImpl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIClient {

    private final String apiUrl;
    private final String apiKey;
    private final okhttp3.OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;

    public OpenAIClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
    }

    public CompletableFuture<ChatResponse> chat(List<ChatSessionImpl.ChatMessage> messages, String model, double temperature) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("temperature", temperature);
                requestBody.addProperty("stream", false);

                JsonArray messagesArray = new JsonArray();
                for (ChatSessionImpl.ChatMessage msg : messages) {
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("role", msg.getRole());
                    messageObj.addProperty("content", msg.getContent());
                    messagesArray.add(messageObj);
                }
                requestBody.add("messages", messagesArray);

                String jsonBody = gson.toJson(requestBody);

                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(apiUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                    .build();

                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        return ChatResponse.error("API request failed: " + response.code() + " - " + errorBody);
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
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
                }

            } catch (IOException e) {
                return ChatResponse.error("Network error: " + e.getMessage());
            } catch (Exception e) {
                return ChatResponse.error("Unexpected error: " + e.getMessage());
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
