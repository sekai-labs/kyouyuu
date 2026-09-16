package com.sekailabs.kyouyuu.inventory;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.storage.ChannelRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SharedInventoryManagerTest {

    private ChannelRepository channelRepository;
    private ItemSerializer itemSerializer;
    private Logger logger;
    private Server server;

    @BeforeEach
    void setUp() {
        channelRepository = Mockito.mock(ChannelRepository.class);
        itemSerializer = Mockito.mock(ItemSerializer.class);
        logger = Logger.getLogger("SharedInventoryManagerTest");
        server = Mockito.mock(Server.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    @Test
    void testChannelInventoryHolderGettersAndSetters() {
        Channel channel = Channel.create("test-holder", "Test Holder", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);

        assertEquals(channel, holder.getChannel());
        assertEquals("test-holder", holder.getChannelId());
        assertNull(holder.getInventory());

        Inventory mockInv = Mockito.mock(Inventory.class);
        holder.setInventory(mockInv);
        assertEquals(mockInv, holder.getInventory());

        assertThrows(NullPointerException.class, () -> new ChannelInventoryHolder(null));
    }

    @Test
    void testGetOrCreateInventoryWithServerInstanceAndSavedData() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("saved-chan", "Saved Chan", 27);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(mockInv);

        byte[] savedData = new byte[]{1, 2, 3};
        ItemStack[] items = new ItemStack[27];
        items[0] = Mockito.mock(ItemStack.class);
        when(channelRepository.loadInventoryData("saved-chan")).thenReturn(savedData);
        when(itemSerializer.deserialize(savedData, 27)).thenReturn(items);

        Inventory created = manager.getOrCreateInventory(channel);
        assertNotNull(created);
        assertEquals(mockInv, created);
        verify(mockInv).setContents(items);

        Inventory cached = manager.getOrCreateInventory(channel);
        assertSame(created, cached);
        verify(server, times(1)).createInventory(any(InventoryHolder.class), eq(27), any(Component.class));
    }

    @Test
    void testGetOrCreateInventoryWithoutServerInstanceFallbackToBukkit() throws Exception {
        Server bukkitServer = Mockito.mock(Server.class);
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, bukkitServer);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(bukkitServer.createInventory(any(InventoryHolder.class), eq(18), any(Component.class))).thenReturn(mockInv);

        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, null);
        Channel channel = Channel.create("bukkit-chan", "Bukkit Chan", 18);

        when(channelRepository.loadInventoryData("bukkit-chan")).thenReturn(null);

        Inventory created = manager.getOrCreateInventory(channel);
        assertSame(mockInv, created);
        verify(mockInv, never()).setContents(any());
        assertEquals(created, manager.getCachedInventory("bukkit-chan"));
    }

    @Test
    void testGetOrCreateInventoryLoadDataThrowsException() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("err-chan", "Error Chan", 27);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(mockInv);

        when(channelRepository.loadInventoryData("err-chan")).thenThrow(new SQLException("DB error"));

        Inventory created = manager.getOrCreateInventory(channel);
        assertNotNull(created);
        verify(mockInv, never()).setContents(any());
    }

    @Test
    void testGetCachedInventoryAndDirtyHandling() {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);

        assertNull(manager.getCachedInventory("nonexistent"));
        assertFalse(manager.isDirty("nonexistent"));

        manager.markDirty("test-chan");
        assertTrue(manager.isDirty("test-chan"));
        assertTrue(manager.isDirty("TEST-CHAN"));
    }

    @Test
    void testFlushChannelNotInCache() {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        manager.markDirty("not-loaded");
        assertTrue(manager.isDirty("not-loaded"));

        boolean result = manager.flushChannel("not-loaded");
        assertTrue(result);
        assertFalse(manager.isDirty("not-loaded"));
    }

    @Test
    void testFlushChannelExistingInCacheSuccess() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("flush-chan", "Flush Chan", 27);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(mockInv);
        ItemStack[] contents = new ItemStack[27];
        when(mockInv.getContents()).thenReturn(contents);

        byte[] serializedData = new byte[]{10, 20};
        when(itemSerializer.serialize(contents)).thenReturn(serializedData);

        manager.getOrCreateInventory(channel);
        manager.markDirty("flush-chan");
        assertTrue(manager.isDirty("flush-chan"));

        boolean success = manager.flushChannel("flush-chan");
        assertTrue(success);
        assertFalse(manager.isDirty("flush-chan"));
        verify(channelRepository).saveInventoryData("flush-chan", serializedData);
    }

    @Test
    void testFlushChannelErrorHandling() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("fail-chan", "Fail Chan", 27);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(mockInv);
        ItemStack[] contents = new ItemStack[27];
        when(mockInv.getContents()).thenReturn(contents);

        when(itemSerializer.serialize(contents)).thenThrow(new IOException("Serialize failure"));

        manager.getOrCreateInventory(channel);
        manager.markDirty("fail-chan");

        boolean success = manager.flushChannel("fail-chan");
        assertFalse(success);
        assertTrue(manager.isDirty("fail-chan"));
    }

    @Test
    void testFlushAllDirty() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);

        Channel c1 = Channel.create("c1", "C1", 9);
        Channel c2 = Channel.create("c2", "C2", 9);

        Inventory inv1 = Mockito.mock(Inventory.class);
        Inventory inv2 = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(9), any(Component.class)))
                .thenReturn(inv1, inv2);

        when(inv1.getContents()).thenReturn(new ItemStack[9]);
        when(inv2.getContents()).thenReturn(new ItemStack[9]);
        when(itemSerializer.serialize(any())).thenReturn(new byte[]{1});

        manager.getOrCreateInventory(c1);
        manager.getOrCreateInventory(c2);

        manager.markDirty("c1");
        manager.markDirty("c2");

        int flushed = manager.flushAllDirty();
        assertEquals(2, flushed);
        assertFalse(manager.isDirty("c1"));
        assertFalse(manager.isDirty("c2"));
    }

    @Test
    void testResizeOrUpdateChannelNullInMemory() {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("not-loaded-resize", "Title", 27);

        assertDoesNotThrow(() -> manager.resizeOrUpdateChannel(channel));
        assertNull(manager.getCachedInventory("not-loaded-resize"));
        assertFalse(manager.isDirty("not-loaded-resize"));
    }

    @Test
    void testResizeOrUpdateChannelActiveInMemoryWithOldContents() {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel initial = Channel.create("resize-chan", "Initial", 27);

        Inventory oldInv = Mockito.mock(Inventory.class);
        Inventory newInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(oldInv);
        when(server.createInventory(any(InventoryHolder.class), eq(54), any(Component.class))).thenReturn(newInv);

        ItemStack item0 = Mockito.mock(ItemStack.class);
        ItemStack[] oldContents = new ItemStack[27];
        oldContents[0] = item0;
        when(oldInv.getContents()).thenReturn(oldContents);

        manager.getOrCreateInventory(initial);

        Channel resized = initial.withSize(54).withName("Resized");
        manager.resizeOrUpdateChannel(resized);

        assertSame(newInv, manager.getCachedInventory("resize-chan"));
        assertTrue(manager.isDirty("resize-chan"));
        verify(newInv).setItem(0, item0);
    }

    @Test
    void testResizeOrUpdateChannelActiveInMemoryNullOldContentsLoadsFromRepo() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel initial = Channel.create("repo-resize", "Initial", 27);

        Inventory oldInv = Mockito.mock(Inventory.class);
        Inventory newInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(oldInv);
        when(server.createInventory(any(InventoryHolder.class), eq(54), any(Component.class))).thenReturn(newInv);

        when(oldInv.getContents()).thenReturn(null);

        byte[] savedData = new byte[]{1, 2, 3};
        ItemStack[] repoItems = new ItemStack[54];
        when(channelRepository.loadInventoryData("repo-resize")).thenReturn(savedData);
        when(itemSerializer.deserialize(savedData, 54)).thenReturn(repoItems);

        manager.getOrCreateInventory(initial);

        Channel resized = initial.withSize(54);
        manager.resizeOrUpdateChannel(resized);

        assertSame(newInv, manager.getCachedInventory("repo-resize"));
        verify(newInv).setContents(repoItems);
    }

    @Test
    void testResizeOrUpdateChannelActiveInMemoryNullOldContentsRepoThrows() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel initial = Channel.create("repo-err-resize", "Initial", 27);

        Inventory oldInv = Mockito.mock(Inventory.class);
        Inventory newInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(oldInv);
        when(server.createInventory(any(InventoryHolder.class), eq(54), any(Component.class))).thenReturn(newInv);

        when(oldInv.getContents()).thenReturn(null);
        when(channelRepository.loadInventoryData("repo-err-resize")).thenThrow(new SQLException("DB error"));

        manager.getOrCreateInventory(initial);

        Channel resized = initial.withSize(54);
        assertDoesNotThrow(() -> manager.resizeOrUpdateChannel(resized));
        assertSame(newInv, manager.getCachedInventory("repo-err-resize"));
    }

    @Test
    void testResizeOrUpdateChannelWithoutServerFallbackToBukkit() throws Exception {
        Server bukkitServer = Mockito.mock(Server.class);
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, bukkitServer);

        Inventory oldInv = Mockito.mock(Inventory.class);
        Inventory newInv = Mockito.mock(Inventory.class);
        when(bukkitServer.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(oldInv);
        when(bukkitServer.createInventory(any(InventoryHolder.class), eq(54), any(Component.class))).thenReturn(newInv);

        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, null);
        Channel initial = Channel.create("bukkit-resize", "Title", 27);

        when(oldInv.getContents()).thenReturn(new ItemStack[27]);

        manager.getOrCreateInventory(initial);

        Channel resized = initial.withSize(54);
        manager.resizeOrUpdateChannel(resized);

        assertSame(newInv, manager.getCachedInventory("bukkit-resize"));
    }

    @Test
    void testUnloadChannelFlushesAndEvicts() throws SQLException, IOException {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);
        Channel channel = Channel.create("unload-chan", "Unload Chan", 27);

        Inventory mockInv = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class))).thenReturn(mockInv);
        ItemStack[] contents = new ItemStack[27];
        when(mockInv.getContents()).thenReturn(contents);
        byte[] serializedData = new byte[]{5, 6};
        when(itemSerializer.serialize(contents)).thenReturn(serializedData);

        manager.getOrCreateInventory(channel);
        manager.markDirty("unload-chan");
        assertTrue(manager.isDirty("unload-chan"));
        assertNotNull(manager.getCachedInventory("unload-chan"));

        manager.unloadChannel("unload-chan");

        verify(channelRepository).saveInventoryData("unload-chan", serializedData);
        assertNull(manager.getCachedInventory("unload-chan"));
        assertFalse(manager.isDirty("unload-chan"));
    }

    @Test
    void testGetAllLoadedInventories() {
        SharedInventoryManager manager = new SharedInventoryManager(channelRepository, itemSerializer, logger, server);

        Channel c1 = Channel.create("inv1", "Inv 1", 9);
        Channel c2 = Channel.create("inv2", "Inv 2", 9);

        Inventory mockInv1 = Mockito.mock(Inventory.class);
        Inventory mockInv2 = Mockito.mock(Inventory.class);
        when(server.createInventory(any(InventoryHolder.class), eq(9), any(Component.class)))
                .thenReturn(mockInv1, mockInv2);

        manager.getOrCreateInventory(c1);
        manager.getOrCreateInventory(c2);

        Collection<Inventory> loaded = manager.getAllLoadedInventories();
        assertEquals(2, loaded.size());
        assertTrue(loaded.contains(mockInv1));
        assertTrue(loaded.contains(mockInv2));
    }
}
