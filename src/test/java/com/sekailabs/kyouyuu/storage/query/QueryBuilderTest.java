package com.sekailabs.kyouyuu.storage.query;

import com.sekailabs.kyouyuu.storage.DatabaseType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryBuilderTest {

    @Test
    void testSelectQuery() {
        String sql = QueryBuilder.select()
                .table("users")
                .column("id")
                .column("name")
                .where("id = ?")
                .orderBy("name ASC")
                .limit(10)
                .build()
                .build();
        assertEquals("SELECT id, name FROM users WHERE id = ? ORDER BY name ASC LIMIT 10;", sql);

        String sqlStar = QueryBuilder.select()
                .table("users")
                .build()
                .build();
        assertEquals("SELECT * FROM users;", sqlStar);
    }

    @Test
    void testInsertQuerySimple() {
        String sql = QueryBuilder.insert()
                .table("items")
                .column("name")
                .column("count")
                .build()
                .build(DatabaseType.SQLITE);
        assertEquals("INSERT INTO items (name, count) VALUES (?, ?);", sql);
    }

    @Test
    void testInsertQueryConflictPostgres() {
        String sql = QueryBuilder.insert()
                .table("chests")
                .column("world")
                .column("x")
                .column("channel_id")
                .conflictKey("world")
                .conflictKey("x")
                .updateColumn("channel_id")
                .build()
                .build(DatabaseType.POSTGRESQL);
        assertTrue(sql.contains("ON CONFLICT (world, x) DO UPDATE SET channel_id = EXCLUDED.channel_id"));
    }

    @Test
    void testInsertQueryConflictMysql() {
        String sql = QueryBuilder.insert()
                .table("chests")
                .column("world")
                .column("x")
                .column("channel_id")
                .conflictKey("world")
                .conflictKey("x")
                .updateColumn("channel_id")
                .build()
                .build(DatabaseType.MYSQL);
        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE channel_id = VALUES(channel_id)"));
    }

    @Test
    void testInsertQueryConflictSqlite() {
        String sql = QueryBuilder.insert()
                .table("chests")
                .column("world")
                .column("x")
                .column("channel_id")
                .conflictKey("world")
                .conflictKey("x")
                .updateColumn("channel_id")
                .build()
                .build(DatabaseType.SQLITE);
        assertTrue(sql.startsWith("INSERT OR REPLACE INTO chests"));
    }

    @Test
    void testUpdateQuery() {
        String sql = QueryBuilder.update()
                .table("channels")
                .set("name = ?")
                .set("size = ?")
                .where("id = ?")
                .build()
                .build();
        assertEquals("UPDATE channels SET name = ?, size = ? WHERE id = ?;", sql);
    }

    @Test
    void testDeleteQuery() {
        String sql = QueryBuilder.delete()
                .table("channels")
                .where("id = ?")
                .build()
                .build();
        assertEquals("DELETE FROM channels WHERE id = ?;", sql);

        String sqlAll = QueryBuilder.delete()
                .table("channels")
                .build()
                .build();
        assertEquals("DELETE FROM channels;", sqlAll);
    }
}
