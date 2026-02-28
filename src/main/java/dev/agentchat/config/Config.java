package dev.agentchat.config;

import dev.agentchat.AgentChatPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {

    private final AgentChatPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public Config(AgentChatPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");
        reload();
    }

    public void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public String getApiUrl() {
        return yaml.getString("api.url", "https://api.openai.com/v1");
    }

    public String getApiKey() {
        return yaml.getString("api.key", "");
    }

    public String getModel() {
        return yaml.getString("api.model", "gpt-3.5-turbo");
    }

    public int getMaxIdleMinutes() {
        return yaml.getInt("session.max-idle-minutes", 15);
    }

    public int getMaxTokens() {
        return yaml.getInt("session.max-tokens", 10000);
    }

    public double getTemperature() {
        return yaml.getDouble("chat.temperature", 1.0);
    }

    public boolean isValid() {
        String key = getApiKey();
        return key != null && !key.isEmpty() && !key.equals("your-api-key-here");
    }
}
