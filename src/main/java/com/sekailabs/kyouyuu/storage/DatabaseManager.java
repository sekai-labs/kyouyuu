package com.sekailabs.kyouyuu.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {

    private final DatabaseType databaseType;
    private final String customJdbcUrl;
    private final DatabaseCredentials credentials;
    private final Logger logger;
    private HikariDataSource dataSource;
    private Connection sqliteConnection;

    public DatabaseManager(File dataFolder, Logger logger) {
        this(dataFolder, logger, DatabaseCredentials.sqlite());
    }

    public DatabaseManager(File dataFolder, Logger logger, DatabaseCredentials credentials) {
        this.logger = logger != null ? logger : Logger.getLogger(DatabaseManager.class.getName());
        this.credentials = credentials;
        this.databaseType = credentials.type();

        if (databaseType == DatabaseType.SQLITE) {
            try {
                if (!dataFolder.exists()) {
                    Files.createDirectories(dataFolder.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not create database directory: " + dataFolder, e);
            }
            File dbFile = new File(dataFolder, "kyouyuu.db");
            this.customJdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        } else {
            this.customJdbcUrl = null;
        }
    }

    public DatabaseManager(String customJdbcUrl, Logger logger) {
        this.customJdbcUrl = customJdbcUrl;
        this.logger = logger != null ? logger : Logger.getLogger(DatabaseManager.class.getName());
        this.databaseType = DatabaseType.SQLITE;
        this.credentials = DatabaseCredentials.sqlite();
    }

    public synchronized void initialize() throws SQLException {
        if (databaseType == DatabaseType.SQLITE) {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                sqliteConnection = DriverManager.getConnection(customJdbcUrl);
                configureSqlitePragmas(sqliteConnection);
                runMigrations(sqliteConnection);
            }
        } else {
            if (dataSource == null || dataSource.isClosed()) {
                HikariConfig config = new HikariConfig();
                String jdbcUrl = buildJdbcUrl(credentials);
                config.setJdbcUrl(jdbcUrl);
                if (credentials.username() != null) config.setUsername(credentials.username());
                if (credentials.password() != null) config.setPassword(credentials.password());
                config.setMaximumPoolSize(Math.max(2, credentials.poolSize()));
                config.setConnectionTimeout(credentials.connectionTimeoutMs());
                config.setPoolName("Kyouyuu-" + databaseType.name());

                if (databaseType == DatabaseType.MYSQL) {
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("useServerPrepStmts", "true");
                } else if (databaseType == DatabaseType.POSTGRESQL) {
                    config.setDriverClassName("org.postgresql.Driver");
                }

                dataSource = createDataSource(config);
                try (Connection conn = dataSource.getConnection()) {
                    runMigrations(conn);
                }
            }
        }
    }

    protected HikariDataSource createDataSource(HikariConfig config) {
        return new HikariDataSource(config);
    }

    public void setDataSourceForTesting(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String buildJdbcUrl(DatabaseCredentials creds) {
        if (creds.type() == DatabaseType.MYSQL) {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8",
                    creds.host(),
                    creds.port() > 0 ? creds.port() : 3306,
                    creds.database(),
                    creds.ssl());
        } else if (creds.type() == DatabaseType.POSTGRESQL) {
            return String.format("jdbc:postgresql://%s:%d/%s?ssl=%s",
                    creds.host(),
                    creds.port() > 0 ? creds.port() : 5432,
                    creds.database(),
                    creds.ssl());
        }
        return customJdbcUrl;
    }

    private void configureSqlitePragmas(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
    }

    public void runMigrations(Connection conn) throws SQLException {
        MigrationManager migrationManager = new MigrationManager(conn, databaseType, logger);
        migrationManager.migrate();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (databaseType == DatabaseType.SQLITE) {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                sqliteConnection = DriverManager.getConnection(customJdbcUrl);
                configureSqlitePragmas(sqliteConnection);
            }
            return sqliteConnection;
        } else {
            if (dataSource == null || dataSource.isClosed()) {
                initialize();
            }
            return dataSource.getConnection();
        }
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public synchronized void close() {
        if (sqliteConnection != null) {
            try {
                if (!sqliteConnection.isClosed()) {
                    sqliteConnection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing sqlite connection", e);
            } finally {
                sqliteConnection = null;
            }
        }
        if (dataSource != null) {
            try {
                if (!dataSource.isClosed()) {
                    dataSource.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing data source", e);
            } finally {
                dataSource = null;
            }
        }
    }
}
