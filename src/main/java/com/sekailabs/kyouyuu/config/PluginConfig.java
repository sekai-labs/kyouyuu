package com.sekailabs.kyouyuu.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix;
    private int autosaveIntervalSeconds;
    private int linkSessionTimeoutSeconds;
    private com.sekailabs.kyouyuu.storage.DatabaseCredentials databaseCredentials;
    private final Map<String, String> messages = new HashMap<>();

    public void load(FileConfiguration config) {
        this.prefix = config.getString("messages.prefix", "<gradient:#4A90E2:#50E3C2><bold>[Kyouyuu]</bold></gradient> ");
        this.autosaveIntervalSeconds = config.getInt("storage.autosave-interval-seconds", 30);
        this.linkSessionTimeoutSeconds = config.getInt("linking.session-timeout-seconds", 30);

        String dbTypeStr = config.getString("storage.type", "SQLITE");
        com.sekailabs.kyouyuu.storage.DatabaseType dbType = com.sekailabs.kyouyuu.storage.DatabaseType.fromString(dbTypeStr);
        if (dbType == com.sekailabs.kyouyuu.storage.DatabaseType.MYSQL) {
            this.databaseCredentials = com.sekailabs.kyouyuu.storage.DatabaseCredentials.mysql(
                    config.getString("storage.mysql.host", "localhost"),
                    config.getInt("storage.mysql.port", 3306),
                    config.getString("storage.mysql.database", "kyouyuu"),
                    config.getString("storage.mysql.username", "root"),
                    config.getString("storage.mysql.password", ""),
                    config.getBoolean("storage.mysql.ssl", false)
            );
        } else if (dbType == com.sekailabs.kyouyuu.storage.DatabaseType.POSTGRESQL) {
            this.databaseCredentials = com.sekailabs.kyouyuu.storage.DatabaseCredentials.postgresql(
                    config.getString("storage.postgresql.host", "localhost"),
                    config.getInt("storage.postgresql.port", 5432),
                    config.getString("storage.postgresql.database", "kyouyuu"),
                    config.getString("storage.postgresql.username", "postgres"),
                    config.getString("storage.postgresql.password", ""),
                    config.getBoolean("storage.postgresql.ssl", false)
            );
        } else {
            this.databaseCredentials = com.sekailabs.kyouyuu.storage.DatabaseCredentials.sqlite();
        }
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                if (!key.equals("prefix")) {
                    messages.put(key, config.getString("messages." + key));
                }
            }
        }
    }

    public Component format(String rawMessage, TagResolver... tagResolvers) {
        String full = (prefix != null ? prefix : "") + rawMessage;
        return miniMessage.deserialize(full, tagResolvers);
    }

    public Component rawFormat(String rawMessage, TagResolver... tagResolvers) {
        return miniMessage.deserialize(rawMessage, tagResolvers);
    }

    public Component getMessage(String key, String fallback, TagResolver... tagResolvers) {
        String msg = messages.getOrDefault(key, fallback);
        return format(msg, tagResolvers);
    }

    public int getAutosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    public int getLinkSessionTimeoutSeconds() {
        return linkSessionTimeoutSeconds;
    }

    public com.sekailabs.kyouyuu.storage.DatabaseCredentials getDatabaseCredentials() {
        return databaseCredentials != null ? databaseCredentials : com.sekailabs.kyouyuu.storage.DatabaseCredentials.sqlite();
    }
}
