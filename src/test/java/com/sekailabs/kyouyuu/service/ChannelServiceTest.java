package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.inventory.ItemSerializer;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.storage.ChannelRepository;
import com.sekailabs.kyouyuu.storage.ChestRepository;
import com.sekailabs.kyouyuu.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ChannelServiceTest {

    private DatabaseManager databaseManager;
    private ChannelRepository channelRepository;
    private ChestRepository chestRepository;
    private SharedInventoryManager inventoryManager;
    private ItemSerializer itemSerializer;
    private ChannelService channelService;

    @BeforeEach
    void setUp() throws SQLException {
        databaseManager = new DatabaseManager("jdbc:sqlite:file:channelservicetest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        databaseManager.initialize();
        channelRepository = new ChannelRepository(databaseManager);
        chestRepository = new ChestRepository(databaseManager);
        itemSerializer = new ItemSerializer(new ItemSerializer.ItemCodec() {
            @Override
            public byte[] serialize(org.bukkit.inventory.ItemStack item) {
                return new byte[]{1};
            }

            @Override
            public org.bukkit.inventory.ItemStack deserialize(byte[] bytes) {
                return null;
            }
        });
        inventoryManager = new SharedInventoryManager(channelRepository, itemSerializer, Logger.getLogger("TestLogger"));
        channelService = new ChannelService(channelRepository, chestRepository, inventoryManager, itemSerializer);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void testChannelIdValidation() {
        assertTrue(channelService.isValidChannelId("vault"));
        assertTrue(channelService.isValidChannelId("channel-1_v2"));
        assertFalse(channelService.isValidChannelId("channel with spaces"));
        assertFalse(channelService.isValidChannelId("special!char"));
        assertFalse(channelService.isValidChannelId(""));
        assertFalse(channelService.isValidChannelId("a".repeat(33)));
    }

    @Test
    void testCreateChannel() {
        var result = channelService.createChannel("ores", "Ore Vault", 27);
        assertTrue(result.success());
        assertEquals("ores", result.data().id());
        assertEquals(27, result.data().size());

        var dup = channelService.createChannel("ores", "Duplicate", 54);
        assertFalse(dup.success());
        assertTrue(dup.message().contains("already exists"));

        var badSize = channelService.createChannel("bad", "Bad Size", 15);
        assertFalse(badSize.success());
    }

    @Test
    void testSafeDeletionWithItems() throws SQLException, IOException {
        channelService.createChannel("tools", "Tool Channel", 27);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
        dos.writeByte(1); 
        dos.writeInt(27); 
        dos.writeByte(0x01); 
        dos.writeInt(1); 
        dos.writeByte(0xFF); 
        for (int i = 1; i < 27; i++) {
            dos.writeByte(0x00);
        }
        dos.flush();

        channelRepository.saveInventoryData("tools", baos.toByteArray());

        var delFailed = channelService.deleteChannel("tools", false);
        assertFalse(delFailed.success());
        assertTrue(delFailed.message().contains("--force"));

        var delSuccess = channelService.deleteChannel("tools", true);
        assertTrue(delSuccess.success());
        assertTrue(channelService.getChannel("tools").isEmpty());
    }

    @Test
    void testSafeResizingWithItems() throws SQLException, IOException {
        channelService.createChannel("farming", "Farming Channel", 27);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
        dos.writeByte(1); 
        dos.writeInt(27); 
        for (int i = 0; i < 20; i++) {
            dos.writeByte(0x00);
        }
        dos.writeByte(0x01); 
        dos.writeInt(1);
        dos.writeByte(0xAA);
        for (int i = 21; i < 27; i++) {
            dos.writeByte(0x00);
        }
        dos.flush();

        channelRepository.saveInventoryData("farming", baos.toByteArray());

        var shrinkFail = channelService.resizeChannel("farming", 18, false);
        assertFalse(shrinkFail.success());
        assertTrue(shrinkFail.message().contains("Cannot shrink channel"));

        var shrinkSuccess = channelService.resizeChannel("farming", 18, true);
        assertTrue(shrinkSuccess.success());
        assertEquals(18, shrinkSuccess.data().size());
    }
}
