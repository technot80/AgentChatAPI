package dev.agentchat.api;

import dev.agentchat.manager.SessionManager;

import java.util.Collection;

public class ChatAPI {

    private static ChatAPI instance;
    private static SessionManager sessionManager;

    public static void initialize(SessionManager manager) {
        sessionManager = manager;
        instance = new ChatAPI();
    }

    public static ChatAPI get() {
        if (instance == null) {
            throw new IllegalStateException("AgentChatAPI is not initialized. Make sure it's enabled in config.yml");
        }
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null && sessionManager != null;
    }

    public ChatSession createSession(String sessionName, String systemPrompt) {
        if (sessionManager == null) {
            throw new IllegalStateException("AgentChatAPI is not enabled");
        }
        return sessionManager.createSession(sessionName, systemPrompt);
    }

    public ChatSession getSession(String sessionName) {
        if (sessionManager == null) {
            return null;
        }
        return sessionManager.getSession(sessionName);
    }

    public boolean hasSession(String sessionName) {
        if (sessionManager == null) {
            return false;
        }
        return sessionManager.hasSession(sessionName);
    }

    public void endSession(String sessionName) {
        if (sessionManager == null) {
            return;
        }
        sessionManager.endSession(sessionName);
    }

    public Collection<ChatSession> getAllSessions() {
        if (sessionManager == null) {
            return java.util.Collections.emptyList();
        }
        return sessionManager.getAllSessions();
    }

    public int getSessionCount() {
        if (sessionManager == null) {
            return 0;
        }
        return sessionManager.getSessionCount();
    }

    public void cleanupExpiredSessions() {
        if (sessionManager == null) {
            return;
        }
        sessionManager.cleanupExpiredSessions();
    }
}
