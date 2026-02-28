package dev.agentchat.manager;

import dev.agentchat.AgentChatPlugin;
import dev.agentchat.api.ChatSession;
import dev.agentchat.api.ChatSessionImpl;
import dev.agentchat.config.Config;
import dev.agentchat.util.OpenAIClient;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final AgentChatPlugin plugin;
    private final Config config;
    private final OpenAIClient client;
    private final Map<String, ChatSessionImpl> sessions;
    private volatile boolean cleanupRunning = false;

    public SessionManager(AgentChatPlugin plugin, Config config, OpenAIClient client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        this.sessions = new ConcurrentHashMap<>();
    }

    public ChatSession createSession(String sessionName, String systemPrompt) {
        if (sessions.containsKey(sessionName)) {
            plugin.getLogger().warning("Session already exists: " + sessionName + ". Ending existing session first.");
            endSession(sessionName);
        }

        ChatSessionImpl session = new ChatSessionImpl(sessionName, systemPrompt, config, client);
        sessions.put(sessionName, session);
        plugin.getLogger().info("Created new chat session: " + sessionName);
        return session;
    }

    public ChatSession getSession(String sessionName) {
        return sessions.get(sessionName);
    }

    public boolean hasSession(String sessionName) {
        return sessions.containsKey(sessionName);
    }

    public void endSession(String sessionName) {
        ChatSessionImpl session = sessions.remove(sessionName);
        if (session != null) {
            session.destroy();
            plugin.getLogger().info("Ended chat session: " + sessionName);
        }
    }

    public Collection<ChatSession> getAllSessions() {
        return new java.util.ArrayList<>(sessions.values());
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public void cleanupExpiredSessions() {
        if (cleanupRunning) {
            return;
        }
        cleanupRunning = true;

        try {
            for (Map.Entry<String, ChatSessionImpl> entry : sessions.entrySet()) {
                ChatSessionImpl session = entry.getValue();
                if (session.isExpired()) {
                    sessions.remove(entry.getKey());
                    session.destroy();
                    plugin.getLogger().info("Cleaned up expired session: " + entry.getKey());
                }
            }
        } finally {
            cleanupRunning = false;
        }
    }

    public void shutdown() {
        for (ChatSessionImpl session : sessions.values()) {
            session.destroy();
        }
        sessions.clear();
        client.shutdown();
        plugin.getLogger().info("SessionManager shutdown complete");
    }
}
