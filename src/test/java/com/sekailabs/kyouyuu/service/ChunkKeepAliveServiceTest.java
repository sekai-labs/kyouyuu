package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.model.LinkedChest;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChunkKeepAliveServiceTest {

    private Plugin plugin;
    private ChestService chestService;
    private PluginConfig config;
    private Server server;
    private World world;
    private ChunkKeepAliveService service;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        chestService = mock(ChestService.class);
        server = mock(Server.class);
        world = mock(World.class);

        config = new PluginConfig();
        config.load(new YamlConfiguration());

        when(server.getWorld("world")).thenReturn(world);
        when(server.getWorlds()).thenReturn(List.of(world));

        service = new ChunkKeepAliveService(plugin, chestService, config, Logger.getLogger("Test"), server);
    }

    @Test
    void testSyncAllTicketsAddsTickets() {
        LinkedChest c1 = new LinkedChest("vault", "world", null, 16, 64, 32); // chunk (1, 2)
        LinkedChest c2 = new LinkedChest("vault", "world", null, 20, 64, 35); // same chunk (1, 2)
        LinkedChest c3 = new LinkedChest("vault", "world", null, 160, 64, 320); // chunk (10, 20)

        when(chestService.getAllLinkedChests()).thenReturn(List.of(c1, c2, c3));

        service.syncAllTickets();

        verify(world, times(1)).addPluginChunkTicket(1, 2, plugin);
        verify(world, times(1)).addPluginChunkTicket(10, 20, plugin);
        assertEquals(2, service.getLoadedChunks().size());
        assertTrue(service.getLoadedChunks().contains("world:1:2"));
        assertTrue(service.getLoadedChunks().contains("world:10:20"));
    }

    @Test
    void testSyncRemovesUnneededTickets() {
        LinkedChest c1 = new LinkedChest("vault", "world", null, 16, 64, 32);
        when(chestService.getAllLinkedChests()).thenReturn(List.of(c1));
        service.syncAllTickets();

        assertEquals(1, service.getLoadedChunks().size());

        when(chestService.getAllLinkedChests()).thenReturn(List.of());
        service.syncAllTickets();

        verify(world, times(1)).removePluginChunkTicket(1, 2, plugin);
        assertTrue(service.getLoadedChunks().isEmpty());
    }

    @Test
    void testAddAndRemoveTicketLifecycle() {
        when(chestService.getAllLinkedChests()).thenReturn(List.of());

        service.addTicketForChest("world", 32, 48); // chunk (2, 3)
        verify(world, times(1)).addPluginChunkTicket(2, 3, plugin);
        assertTrue(service.getLoadedChunks().contains("world:2:3"));

        service.removeTicketIfNoOtherChests("world", 32, 48);
        verify(world, times(1)).removePluginChunkTicket(2, 3, plugin);
        assertFalse(service.getLoadedChunks().contains("world:2:3"));
    }

    @Test
    void testDisabledConfigDoesNotAddTickets() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader("chunk-loading:\n  enabled: false\n"));
        PluginConfig disabledConfig = new PluginConfig();
        disabledConfig.load(yaml);

        ChunkKeepAliveService disabledService = new ChunkKeepAliveService(plugin, chestService, disabledConfig, Logger.getLogger("Test"), server);
        LinkedChest c1 = new LinkedChest("vault", "world", null, 16, 64, 32);
        when(chestService.getAllLinkedChests()).thenReturn(List.of(c1));

        disabledService.syncAllTickets();
        verify(world, never()).addPluginChunkTicket(anyInt(), anyInt(), any());
        assertTrue(disabledService.getLoadedChunks().isEmpty());
    }

    @Test
    void testRemoveAllTickets() {
        LinkedChest c1 = new LinkedChest("vault", "world", null, 16, 64, 32);
        when(chestService.getAllLinkedChests()).thenReturn(List.of(c1));
        service.syncAllTickets();

        service.removeAllTickets();
        verify(world).removePluginChunkTickets(plugin);
        assertTrue(service.getLoadedChunks().isEmpty());
    }
}
