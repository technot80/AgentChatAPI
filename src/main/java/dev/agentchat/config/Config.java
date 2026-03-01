package dev.agentchat.config;

import dev.agentchat.AgentChatPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {

    private static final int CURRENT_CONFIG_VERSION = 2;

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
        checkAndUpgradeConfig();
    }

    private void checkAndUpgradeConfig() {
        int configVersion = yaml.getInt("config-version", 1);
        
        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("Upgrading config from version " + configVersion + " to " + CURRENT_CONFIG_VERSION);
            upgradeConfig(configVersion);
            configVersion = CURRENT_CONFIG_VERSION;
            yaml.set("config-version", CURRENT_CONFIG_VERSION);
            save();
        }
    }

    private void upgradeConfig(int fromVersion) {
        if (fromVersion < 2) {
            upgradeToV2();
        }
    }

    private void upgradeToV2() {
        plugin.getLogger().info("Applying upgrade to v2 (enabled flag)...");
        
        yaml.set("enabled", yaml.getBoolean("enabled", false));
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return yaml.getBoolean("enabled", false);
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

    public String getProvider() {
        return yaml.getString("api.provider", "");
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
        if (!isEnabled()) {
            return false;
        }
        String key = getApiKey();
        return key != null && !key.isEmpty() && !key.equals("your-api-key-here");
    }

    public int getConfigVersion() {
        return yaml.getInt("config-version", 1);
    }
}
