package com.sekailabs.kyouyuu.storage;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MultiDatabaseMigrationAndRepoTest {

    @Test
    void testPostgresqlSyntaxOnH2() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:pgtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")) {
            MigrationManager manager = new MigrationManager(conn, DatabaseType.POSTGRESQL, Logger.getLogger("Test"));
            assertDoesNotThrow(manager::migrate);

            var checkStmt = conn.createStatement();
            var rs = checkStmt.executeQuery("SELECT version FROM kyouyuu_migrations");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void testMysqlSyntaxOnH2() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:mysqltest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")) {
            MigrationManager manager = new MigrationManager(conn, DatabaseType.MYSQL, Logger.getLogger("Test"));
            assertDoesNotThrow(manager::migrate);

            var checkStmt = conn.createStatement();
            var rs = checkStmt.executeQuery("SELECT version FROM kyouyuu_migrations");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void testDatabaseTypeAndCredentials() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromString("sqlite"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.fromString("mysql"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.fromString("mariadb"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("postgresql"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("postgres"));
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromString(null));

        var sqlite = DatabaseCredentials.sqlite();
        assertEquals(DatabaseType.SQLITE, sqlite.type());

        var mysql = DatabaseCredentials.mysql("127.0.0.1", 3306, "testdb", "user", "pass", false);
        assertEquals(DatabaseType.MYSQL, mysql.type());
        assertEquals("127.0.0.1", mysql.host());
        assertEquals(3306, mysql.port());

        var pg = DatabaseCredentials.postgresql("127.0.0.1", 5432, "testdb", "user", "pass", true);
        assertEquals(DatabaseType.POSTGRESQL, pg.type());
        assertTrue(pg.ssl());
    }
}
