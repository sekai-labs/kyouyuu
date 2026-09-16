package com.sekailabs.kyouyuu.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class MigrationManager {

    private static final int CURRENT_VERSION = 2;

    private final Connection connection;
    private final DatabaseType databaseType;
    private final Logger logger;

    public MigrationManager(Connection connection, Logger logger) {
        this(connection, DatabaseType.SQLITE, logger);
    }

    public MigrationManager(Connection connection, DatabaseType databaseType, Logger logger) {
        this.connection = connection;
        this.databaseType = databaseType != null ? databaseType : DatabaseType.SQLITE;
        this.logger = logger != null ? logger : Logger.getLogger(MigrationManager.class.getName());
    }

    public void migrate() throws SQLException {
        ensureMigrationTable();
        int installedVersion = getInstalledVersion();

        if (installedVersion < 1) {
            applyMigration1();
        }
        if (installedVersion < 2) {
            applyMigration2();
        }
    }

    private void ensureMigrationTable() throws SQLException {
        String sql;
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS kyouyuu_migrations (
                        version INT PRIMARY KEY,
                        applied_at BIGINT NOT NULL
                    );
                    """;
        } else if (databaseType == DatabaseType.MYSQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS kyouyuu_migrations (
                        version INT PRIMARY KEY,
                        applied_at BIGINT NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;
        } else {
            sql = """
                    CREATE TABLE IF NOT EXISTS kyouyuu_migrations (
                        version INTEGER PRIMARY KEY,
                        applied_at INTEGER NOT NULL
                    );
                    """;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private int getInstalledVersion() throws SQLException {
        String sql = "SELECT MAX(version) FROM kyouyuu_migrations;";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private void recordMigration(int version) throws SQLException {
        String sql = "INSERT INTO kyouyuu_migrations (version, applied_at) VALUES (?, ?);";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, version);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    private void applyMigration1() throws SQLException {
        logger.info("[Kyouyuu] Applying database migration 1 for " + databaseType.name() + ": channels table");
        String sql;
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS channels (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(64) NOT NULL,
                        size INT NOT NULL DEFAULT 54,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        inventory_data BYTEA
                    );
                    """;
        } else if (databaseType == DatabaseType.MYSQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS channels (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(64) NOT NULL,
                        size INT NOT NULL DEFAULT 54,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        inventory_data MEDIUMBLOB
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;
        } else {
            sql = """
                    CREATE TABLE IF NOT EXISTS channels (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(64) NOT NULL,
                        size INTEGER NOT NULL DEFAULT 54,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        inventory_data BLOB
                    );
                    """;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        recordMigration(1);
    }

    private void applyMigration2() throws SQLException {
        logger.info("[Kyouyuu] Applying database migration 2 for " + databaseType.name() + ": linked_chests table");
        String sql;
        String indexSql;
        if (databaseType == DatabaseType.POSTGRESQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS linked_chests (
                        world_name VARCHAR(128) NOT NULL,
                        world_uid VARCHAR(36),
                        x INT NOT NULL,
                        y INT NOT NULL,
                        z INT NOT NULL,
                        channel_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY(world_name, x, y, z),
                        CONSTRAINT fk_channel FOREIGN KEY(channel_id) REFERENCES channels(id) ON DELETE CASCADE
                    );
                    """;
            indexSql = "CREATE INDEX IF NOT EXISTS idx_linked_chests_channel ON linked_chests(channel_id);";
        } else if (databaseType == DatabaseType.MYSQL) {
            sql = """
                    CREATE TABLE IF NOT EXISTS linked_chests (
                        world_name VARCHAR(128) NOT NULL,
                        world_uid VARCHAR(36),
                        x INT NOT NULL,
                        y INT NOT NULL,
                        z INT NOT NULL,
                        channel_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY(world_name, x, y, z),
                        KEY idx_channel (channel_id),
                        CONSTRAINT fk_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;
            indexSql = null;
        } else {
            sql = """
                    CREATE TABLE IF NOT EXISTS linked_chests (
                        world_name VARCHAR(128) NOT NULL,
                        world_uid VARCHAR(36),
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        channel_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY(world_name, x, y, z),
                        FOREIGN KEY(channel_id) REFERENCES channels(id) ON DELETE CASCADE
                    );
                    """;
            indexSql = "CREATE INDEX IF NOT EXISTS idx_linked_chests_channel ON linked_chests(channel_id);";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            if (indexSql != null) {
                stmt.execute(indexSql);
            }
        }
        recordMigration(2);
    }
}
