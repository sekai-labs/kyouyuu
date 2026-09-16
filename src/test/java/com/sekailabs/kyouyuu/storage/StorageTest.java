package com.sekailabs.kyouyuu.storage;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {

    private DatabaseManager databaseManager;
    private ChannelRepository channelRepository;
    private ChestRepository chestRepository;

    @BeforeEach
    void setUp() throws SQLException {

        databaseManager = new DatabaseManager("jdbc:sqlite:file:kyouyuutest?mode=memory&cache=shared", Logger.getLogger("TestLogger"));
        databaseManager.initialize();
        channelRepository = new ChannelRepository(databaseManager);
        chestRepository = new ChestRepository(databaseManager);
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    void testChannelLifecycle() throws SQLException {
        Channel channel = Channel.create("storage-1", "Storage Channel 1", 54);
        channelRepository.insert(channel);

        Optional<Channel> found = channelRepository.findById("storage-1");
        assertTrue(found.isPresent());
        assertEquals("storage-1", found.get().id());
        assertEquals("Storage Channel 1", found.get().name());
        assertEquals(54, found.get().size());

        Channel updated = found.get().withName("Renamed Storage").withSize(27);
        channelRepository.update(updated);

        Optional<Channel> afterUpdate = channelRepository.findById("storage-1");
        assertTrue(afterUpdate.isPresent());
        assertEquals("Renamed Storage", afterUpdate.get().name());
        assertEquals(27, afterUpdate.get().size());

        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        channelRepository.saveInventoryData("storage-1", payload);

        byte[] loadedPayload = channelRepository.loadInventoryData("storage-1");
        assertNotNull(loadedPayload);
        assertArrayEquals(payload, loadedPayload);

        assertTrue(channelRepository.delete("storage-1"));
        assertFalse(channelRepository.findById("storage-1").isPresent());
    }

    @Test
    void testChestLinkAndCascade() throws SQLException {
        Channel channel = Channel.create("vault", "Vault Channel", 27);
        channelRepository.insert(channel);

        UUID worldUid = UUID.randomUUID();
        LinkedChest chest1 = new LinkedChest("vault", "world", worldUid, 100, 64, 200);
        LinkedChest chest2 = new LinkedChest("vault", "world", worldUid, 101, 64, 200);

        chestRepository.link(chest1);
        chestRepository.link(chest2);

        Optional<LinkedChest> found = chestRepository.findByLocation("world", 100, 64, 200);
        assertTrue(found.isPresent());
        assertEquals("vault", found.get().channelId());
        assertEquals(worldUid, found.get().worldUid());

        List<LinkedChest> channelChests = chestRepository.findByChannel("vault");
        assertEquals(2, channelChests.size());

        assertTrue(chestRepository.unlink("world", 100, 64, 200));
        assertFalse(chestRepository.findByLocation("world", 100, 64, 200).isPresent());
        assertEquals(1, chestRepository.findByChannel("vault").size());

        channelRepository.delete("vault");
        assertEquals(0, chestRepository.findByChannel("vault").size());
    }
}
