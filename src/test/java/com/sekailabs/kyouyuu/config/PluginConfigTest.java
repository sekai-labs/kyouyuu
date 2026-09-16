package com.sekailabs.kyouyuu.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {

    @Test
    void testConfigParsing() {
        String yaml = """
                storage:
                  type: MYSQL
                  autosave-interval-seconds: 45
                  mysql:
                    host: "db.internal"
                    port: 3307
                    database: "mc_kyouyuu"
                    username: "dbuser"
                    password: "secretpassword"
                    ssl: true
                linking:
                  session-timeout-seconds: 60
                messages:
                  prefix: "<gold>[Test]</gold> "
                  custom: "<green>Custom message</green>"
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.load(config);

        assertEquals(45, pluginConfig.getAutosaveIntervalSeconds());
        assertEquals(60, pluginConfig.getLinkSessionTimeoutSeconds());
        assertNotNull(pluginConfig.getDatabaseCredentials());
        assertEquals(com.sekailabs.kyouyuu.storage.DatabaseType.MYSQL, pluginConfig.getDatabaseCredentials().type());
        assertEquals("db.internal", pluginConfig.getDatabaseCredentials().host());
        assertEquals(3307, pluginConfig.getDatabaseCredentials().port());
        assertTrue(pluginConfig.getDatabaseCredentials().ssl());

        assertNotNull(pluginConfig.format("<white>Hello</white>"));
        assertNotNull(pluginConfig.rawFormat("<red>Raw</red>"));
        assertNotNull(pluginConfig.getMessage("custom", "fallback"));
        assertNotNull(pluginConfig.getMessage("nonexistent", "fallback"));
    }
}
