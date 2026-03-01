package dev.agentchat;

import dev.agentchat.api.ChatAPI;
import dev.agentchat.config.Config;
import dev.agentchat.manager.SessionManager;
import dev.agentchat.util.OpenAIClient;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class AgentChatPlugin extends JavaPlugin {

    private static AgentChatPlugin instance;
    private Config config;
    private SessionManager sessionManager;
    private OpenAIClient client;
    private BukkitTask cleanupTask;
    private boolean initialized = false;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultResources();

        this.config = new Config(this);

        if (!config.isEnabled()) {
            getLogger().info("AgentChatAPI is disabled. Set enabled: true in config.yml to enable.");
            return;
        }

        if (!config.isValid()) {
            getLogger().severe("API key not configured! Please set api.key in config.yml");
            getLogger().severe("Plugin will not function without a valid API key.");
            return;
        }

        initializeApi();
        
        getLogger().info("AgentChatAPI has been enabled!");
    }

    private void initializeApi() {
        try {
            this.client = new OpenAIClient(config.getApiUrl(), config.getApiKey(), config.getProvider());
            this.sessionManager = new SessionManager(this, config, client);
            ChatAPI.initialize(sessionManager);
            startCleanupTask();
            initialized = true;
        } catch (Exception e) {
            getLogger().severe("Failed to initialize AgentChatAPI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        getLogger().info("AgentChatAPI has been disabled!");
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (initialized) {
                    ChatAPI.get().cleanupExpiredSessions();
                }
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L);
    }

    private void saveDefaultResources() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                InputStream inputStream = getResource("config.yml");
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath());
                }
            } catch (IOException e) {
                getLogger().severe("Could not save default config.yml: " + e.getMessage());
            }
        }
    }

    public static AgentChatPlugin getInstance() {
        return instance;
    }

    public Config getPluginConfig() {
        return config;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
