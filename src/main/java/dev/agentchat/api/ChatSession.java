package dev.agentchat.api;

import java.util.concurrent.CompletableFuture;

public interface ChatSession {

    String getName();

    CompletableFuture<ChatResponse> sendMessage(String message);

    int getApproximateTokenCount();

    void clearContext();

    boolean isExpired();

    long getLastActivityTime();
}
