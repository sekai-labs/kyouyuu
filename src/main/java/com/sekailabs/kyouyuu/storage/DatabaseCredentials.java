package com.sekailabs.kyouyuu.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseCredentials {
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean ssl;
    private int poolSize;
    private long connectionTimeoutMs;

    public DatabaseType type() { return type; }
    public String host() { return host; }
    public int port() { return port; }
    public String database() { return database; }
    public String username() { return username; }
    public String password() { return password; }
    public boolean ssl() { return ssl; }
    public int poolSize() { return poolSize; }
    public long connectionTimeoutMs() { return connectionTimeoutMs; }

    public static DatabaseCredentials sqlite() {
        return DatabaseCredentials.builder()
                .type(DatabaseType.SQLITE)
                .poolSize(1)
                .connectionTimeoutMs(5000)
                .build();
    }

    public static DatabaseCredentials mysql(String host, int port, String database, String username, String password, boolean ssl) {
        return DatabaseCredentials.builder()
                .type(DatabaseType.MYSQL)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .ssl(ssl)
                .poolSize(10)
                .connectionTimeoutMs(10000)
                .build();
    }

    public static DatabaseCredentials postgresql(String host, int port, String database, String username, String password, boolean ssl) {
        return DatabaseCredentials.builder()
                .type(DatabaseType.POSTGRESQL)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .ssl(ssl)
                .poolSize(10)
                .connectionTimeoutMs(10000)
                .build();
    }
}
