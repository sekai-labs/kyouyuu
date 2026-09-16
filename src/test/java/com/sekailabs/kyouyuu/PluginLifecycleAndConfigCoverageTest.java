package com.sekailabs.kyouyuu;

import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.storage.DatabaseCredentials;
import com.sekailabs.kyouyuu.storage.DatabaseManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PluginLifecycleAndConfigCoverageTest {

    @Test
    void testConfigPostgresAndFallback() {
        String yaml = """
                storage:
                  type: POSTGRESQL
                  postgresql:
                    host: "pg.internal"
                    port: 5433
                    database: "kyouyuu_pg"
                    username: "pguser"
                    password: "pgpassword"
                    ssl: false
                messages:
                  prefix: "[Kyo] "
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.load(config);

        DatabaseCredentials creds = pluginConfig.getDatabaseCredentials();
        assertEquals(com.sekailabs.kyouyuu.storage.DatabaseType.POSTGRESQL, creds.type());
        assertEquals("pg.internal", creds.host());
        assertEquals(5433, creds.port());
        assertEquals("kyouyuu_pg", creds.database());
        assertEquals("pguser", creds.username());
        assertEquals("pgpassword", creds.password());
        assertFalse(creds.ssl());
    }

    @Test
    void testDatabaseManagerWithPostgresCredentials() {
        DatabaseCredentials creds = DatabaseCredentials.postgresql("localhost", 5432, "testdb", "postgres", "pass", false);
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:", Logger.getLogger("Test"));
        assertEquals(com.sekailabs.kyouyuu.storage.DatabaseType.SQLITE, db.getDatabaseType());
        String pgUrl = db.buildJdbcUrl(creds);
        assertTrue(pgUrl.startsWith("jdbc:postgresql://localhost:5432/testdb"));

        DatabaseCredentials mysqlCreds = DatabaseCredentials.mysql("10.0.0.1", 3306, "mydb", "root", "root", true);
        String mysqlUrl = db.buildJdbcUrl(mysqlCreds);
        assertTrue(mysqlUrl.startsWith("jdbc:mysql://10.0.0.1:3306/mydb"));
    }
}
