package com.sekailabs.kyouyuu.storage;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.ChestLocation;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerFullTest {

    @Test
    void testCustomSqliteJdbcUrlInitializationAndClose() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:customdbtest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        assertEquals(DatabaseType.SQLITE, manager.getDatabaseType());

        manager.initialize();
        Connection conn = manager.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        Connection conn2 = manager.getConnection();
        assertSame(conn, conn2);

        manager.close();
        assertTrue(conn.isClosed());

        manager.close();
    }

    @Test
    void testSqliteDataFolderConstructorAndDirectoryCreation(@TempDir Path tempDir) throws SQLException {
        File subFolder = tempDir.resolve("nested").resolve("dbfolder").toFile();
        assertFalse(subFolder.exists());

        DatabaseManager manager = new DatabaseManager(subFolder, Logger.getLogger("TestLogger"));
        assertTrue(subFolder.exists());
        assertEquals(DatabaseType.SQLITE, manager.getDatabaseType());

        manager.initialize();
        Connection conn = manager.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        File expectedDbFile = new File(subFolder, "kyouyuu.db");
        assertTrue(expectedDbFile.exists());

        manager.close();
        assertTrue(conn.isClosed());
    }

    @Test
    void testSqliteDataFolderWithNullLogger(@TempDir Path tempDir) throws SQLException {
        File folder = tempDir.resolve("nulllogger").toFile();
        DatabaseManager manager = new DatabaseManager(folder, null);
        assertEquals(DatabaseType.SQLITE, manager.getDatabaseType());

        manager.initialize();
        Connection conn = manager.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        manager.close();
    }

    @Test
    void testCustomJdbcUrlWithNullLogger() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:nullurltest?mode=memory&cache=shared", null);
        assertEquals(DatabaseType.SQLITE, manager.getDatabaseType());

        manager.initialize();
        Connection conn = manager.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        manager.close();
    }

    @Test
    void testSqliteConstructorDirectoryCreationFailure() {
        File invalidFileAsFolder = new File("/proc/invalid_non_existent_folder_name/kyouyuu_test");
        assertThrows(RuntimeException.class, () -> new DatabaseManager(invalidFileAsFolder, Logger.getLogger("TestLogger")));
    }

    @Test
    void testBuildJdbcUrlViaReflection() throws Exception {
        DatabaseManager sqliteManager = new DatabaseManager("jdbc:sqlite::memory:", Logger.getLogger("TestLogger"));
        Method buildJdbcUrlMethod = DatabaseManager.class.getDeclaredMethod("buildJdbcUrl", DatabaseCredentials.class);
        buildJdbcUrlMethod.setAccessible(true);

        DatabaseCredentials mysqlCreds = DatabaseCredentials.mysql("mysql.internal", 3306, "kyouyuu_db", "user1", "secret", true);
        String mysqlUrl = (String) buildJdbcUrlMethod.invoke(sqliteManager, mysqlCreds);
        assertEquals("jdbc:mysql://mysql.internal:3306/kyouyuu_db?useSSL=true&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8", mysqlUrl);

        DatabaseCredentials mysqlDefaultPort = new DatabaseCredentials(DatabaseType.MYSQL, "dbhost", 0, "testdb", "user", "pass", false, 10, 5000);
        String mysqlDefaultPortUrl = (String) buildJdbcUrlMethod.invoke(sqliteManager, mysqlDefaultPort);
        assertEquals("jdbc:mysql://dbhost:3306/testdb?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8", mysqlDefaultPortUrl);

        DatabaseCredentials pgCreds = DatabaseCredentials.postgresql("pg.internal", 5432, "kyouyuu_pg", "pguser", "pgpass", false);
        String pgUrl = (String) buildJdbcUrlMethod.invoke(sqliteManager, pgCreds);
        assertEquals("jdbc:postgresql://pg.internal:5432/kyouyuu_pg?ssl=false", pgUrl);

        DatabaseCredentials pgDefaultPort = new DatabaseCredentials(DatabaseType.POSTGRESQL, "pghost", 0, "testpg", "user", "pass", true, 10, 5000);
        String pgDefaultPortUrl = (String) buildJdbcUrlMethod.invoke(sqliteManager, pgDefaultPort);
        assertEquals("jdbc:postgresql://pghost:5432/testpg?ssl=true", pgDefaultPortUrl);

        DatabaseCredentials sqliteCreds = DatabaseCredentials.sqlite();
        String sqliteUrl = (String) buildJdbcUrlMethod.invoke(sqliteManager, sqliteCreds);
        assertEquals("jdbc:sqlite::memory:", sqliteUrl);

        sqliteManager.close();
    }

    @Test
    void testPostgresqlAndMysqlDataSourceWithMockOrH2() throws Exception {
        File dummyFolder = new File("build/tmp/testdummy");
        DatabaseCredentials creds = DatabaseCredentials.mysql("127.0.0.1", 3306, "testdb", "user", "pass", false);
        DatabaseManager manager = new DatabaseManager(dummyFolder, Logger.getLogger("TestLogger"), creds);
        assertEquals(DatabaseType.MYSQL, manager.getDatabaseType());

        Field customJdbcUrlField = DatabaseManager.class.getDeclaredField("customJdbcUrl");
        customJdbcUrlField.setAccessible(true);
        assertNull(customJdbcUrlField.get(manager));

        Field credentialsField = DatabaseManager.class.getDeclaredField("credentials");
        credentialsField.setAccessible(true);
        DatabaseCredentials actualCreds = (DatabaseCredentials) credentialsField.get(manager);
        assertEquals("testdb", actualCreds.database());
        assertEquals("user", actualCreds.username());
        assertEquals("pass", actualCreds.password());

        DatabaseCredentials pgCreds = DatabaseCredentials.postgresql("127.0.0.1", 5432, "pgdb", "pguser", "pgpass", true);
        DatabaseManager pgManager = new DatabaseManager(dummyFolder, Logger.getLogger("TestLogger"), pgCreds);
        assertEquals(DatabaseType.POSTGRESQL, pgManager.getDatabaseType());

        manager.close();
        pgManager.close();
    }

    @Test
    void testGetConnectionReopensSqliteWhenClosed() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:reopentest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        manager.initialize();
        Connection firstConn = manager.getConnection();
        assertNotNull(firstConn);
        assertFalse(firstConn.isClosed());

        firstConn.close();
        assertTrue(firstConn.isClosed());

        Connection secondConn = manager.getConnection();
        assertNotNull(secondConn);
        assertFalse(secondConn.isClosed());
        assertNotSame(firstConn, secondConn);

        manager.close();
    }

    @Test
    void testCloseWithAlreadyClosedConnectionAndDataSource() throws Exception {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:alreadyclosed?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        manager.initialize();
        Connection conn = manager.getConnection();
        conn.close();

        assertDoesNotThrow(manager::close);
        assertDoesNotThrow(manager::close);

        Field dsField = DatabaseManager.class.getDeclaredField("dataSource");
        dsField.setAccessible(true);
        HikariDataSource mockDs = org.mockito.Mockito.mock(HikariDataSource.class);
        org.mockito.Mockito.when(mockDs.isClosed()).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("Simulated exception")).when(mockDs).close();

        dsField.set(manager, mockDs);
        assertDoesNotThrow(manager::close);
    }

    @Test
    void testChannelRepositoryEdgeCases() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:chanrepotest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        manager.initialize();

        ChannelRepository channelRepo = new ChannelRepository(manager);

        List<Channel> initialChannels = channelRepo.findAll();
        assertNotNull(initialChannels);
        assertTrue(initialChannels.isEmpty());

        Optional<Channel> nonExistent = channelRepo.findById("does-not-exist");
        assertFalse(nonExistent.isPresent());

        assertNull(channelRepo.loadInventoryData("does-not-exist"));

        assertFalse(channelRepo.delete("does-not-exist"));

        Channel channel = Channel.create("empty-inv-channel", "Empty Inventory Channel", 27);
        channelRepo.insert(channel);

        Optional<Channel> found = channelRepo.findById("empty-inv-channel");
        assertTrue(found.isPresent());
        assertEquals("empty-inv-channel", found.get().id());

        assertNull(channelRepo.loadInventoryData("empty-inv-channel"));

        List<Channel> channelsAfterInsert = channelRepo.findAll();
        assertEquals(1, channelsAfterInsert.size());
        assertEquals("empty-inv-channel", channelsAfterInsert.get(0).id());

        channelRepo.saveInventoryData("empty-inv-channel", new byte[0]);
        byte[] emptyBytes = channelRepo.loadInventoryData("empty-inv-channel");
        assertNotNull(emptyBytes);
        assertEquals(0, emptyBytes.length);

        assertTrue(channelRepo.delete("empty-inv-channel"));
        assertTrue(channelRepo.findAll().isEmpty());

        manager.close();
    }

    @Test
    void testChestRepositoryEdgeCases() throws SQLException {
        DatabaseManager manager = new DatabaseManager("jdbc:sqlite:file:chestrepotest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        manager.initialize();

        ChestRepository chestRepo = new ChestRepository(manager);
        ChannelRepository channelRepo = new ChannelRepository(manager);

        List<LinkedChest> initialChests = chestRepo.findAll();
        assertNotNull(initialChests);
        assertTrue(initialChests.isEmpty());

        List<LinkedChest> byChannelEmpty = chestRepo.findByChannel("nonexistent-chan");
        assertNotNull(byChannelEmpty);
        assertTrue(byChannelEmpty.isEmpty());

        Optional<LinkedChest> notFoundLoc = chestRepo.findByLocation("world", 999, 999, 999);
        assertFalse(notFoundLoc.isPresent());

        Optional<LinkedChest> notFoundLocRecord = chestRepo.findByLocation(new ChestLocation("world", 999, 999, 999));
        assertFalse(notFoundLocRecord.isPresent());

        assertFalse(chestRepo.unlink("world", 999, 999, 999));

        int deletedCount = chestRepo.deleteByChannel("nonexistent-chan");
        assertEquals(0, deletedCount);

        Channel channel = Channel.create("test-chan", "Test Channel", 18);
        channelRepo.insert(channel);

        LinkedChest chestWithNullUid = new LinkedChest("test-chan", "world_null_uid", null, 10, 20, 30);
        chestRepo.link(chestWithNullUid);

        Optional<LinkedChest> foundWithNullUid = chestRepo.findByLocation("world_null_uid", 10, 20, 30);
        assertTrue(foundWithNullUid.isPresent());
        assertNull(foundWithNullUid.get().worldUid());
        assertEquals("test-chan", foundWithNullUid.get().channelId());
        assertEquals(new ChestLocation("world_null_uid", 10, 20, 30), foundWithNullUid.get().toLocation());

        UUID uid = UUID.randomUUID();
        LinkedChest chestWithUid = new LinkedChest("test-chan", "world_uid", uid, 40, 50, 60);
        chestRepo.link(chestWithUid);

        List<LinkedChest> allChests = chestRepo.findAll();
        assertEquals(2, allChests.size());

        List<LinkedChest> chanChests = chestRepo.findByChannel("test-chan");
        assertEquals(2, chanChests.size());

        int deletedChests = chestRepo.deleteByChannel("test-chan");
        assertEquals(2, deletedChests);
        assertTrue(chestRepo.findByChannel("test-chan").isEmpty());
        assertTrue(chestRepo.findAll().isEmpty());

        manager.close();
    }
}
