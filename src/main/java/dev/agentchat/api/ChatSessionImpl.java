package dev.agentchat.api;

import dev.agentchat.config.Config;
import dev.agentchat.util.OpenAIClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatSessionImpl implements ChatSession {

    private final String name;
    private final String systemPrompt;
    private final Config config;
    private final OpenAIClient client;
    private final List<ChatMessage> context;
    private long lastActivityTime;
    private volatile boolean expired = false;

    public ChatSessionImpl(String name, String systemPrompt, Config config, OpenAIClient client) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.config = config;
        this.client = client;
        this.context = new ArrayList<>();
        this.lastActivityTime = System.currentTimeMillis();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            this.context.add(new ChatMessage("system", systemPrompt));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ChatResponse> sendMessage(String message) {
        if (expired) {
            return CompletableFuture.completedFuture(
                ChatResponse.error("Session has expired")
            );
        }

        if (!ChatAPI.get().allowRequest(name)) {
            return CompletableFuture.completedFuture(
                ChatResponse.error("Rate limit exceeded")
            );
        }

        context.add(new ChatMessage("user", message));
        lastActivityTime = System.currentTimeMillis();

        return client.chat(context, config.getModel(), config.getTemperature())
            .thenApply(response -> {
                if (response.isSuccess() && response.getContent() != null) {
                    context.add(new ChatMessage("assistant", response.getContent()));
                    lastActivityTime = System.currentTimeMillis();

                    if (getApproximateTokenCount() > config.getMaxTokens()) {
                        expire();
                    }
                }
                return response;
            });
    }

    @Override
    public int getApproximateTokenCount() {
        int total = 0;
        for (ChatMessage msg : context) {
            total += msg.getContent().length() / 4;
        }
        return total;
    }

    @Override
    public void clearContext() {
        context.clear();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            context.add(new ChatMessage("system", systemPrompt));
        }
    }

    @Override
    public boolean isExpired() {
        if (expired) {
            return true;
        }

        long idleTime = System.currentTimeMillis() - lastActivityTime;
        long idleMinutes = idleTime / (60 * 1000);
        return idleMinutes >= config.getMaxIdleMinutes();
    }

    @Override
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void expire() {
        this.expired = true;
    }

    public void destroy() {
        clearContext();
        this.expired = true;
    }

    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
