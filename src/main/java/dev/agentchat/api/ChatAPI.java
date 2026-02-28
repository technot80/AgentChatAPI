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
        return instance;
    }

    public ChatSession createSession(String sessionName, String systemPrompt) {
        return sessionManager.createSession(sessionName, systemPrompt);
    }

    public ChatSession getSession(String sessionName) {
        return sessionManager.getSession(sessionName);
    }

    public boolean hasSession(String sessionName) {
        return sessionManager.hasSession(sessionName);
    }

    public void endSession(String sessionName) {
        sessionManager.endSession(sessionName);
    }

    public Collection<ChatSession> getAllSessions() {
        return sessionManager.getAllSessions();
    }

    public int getSessionCount() {
        return sessionManager.getSessionCount();
    }

    public void cleanupExpiredSessions() {
        sessionManager.cleanupExpiredSessions();
    }
}
