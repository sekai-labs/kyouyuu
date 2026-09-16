package com.sekailabs.kyouyuu.storage;

public enum DatabaseType {
    SQLITE,
    MYSQL,
    POSTGRESQL;

    public static DatabaseType fromString(String str) {
        if (str == null) return SQLITE;
        String s = str.trim().toUpperCase();
        if (s.contains("POSTGRES")) return POSTGRESQL;
        if (s.contains("MYSQL") || s.contains("MARIA")) return MYSQL;
        return SQLITE;
    }
}
