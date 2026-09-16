package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.model.LinkedChest;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkKeepAliveService {

    private final Plugin plugin;
    private final ChestService chestService;
    private final PluginConfig pluginConfig;
    private final Logger logger;
    private final Server server;

    private final Set<String> loadedChunks = new HashSet<>();

    public ChunkKeepAliveService(Plugin plugin, ChestService chestService, PluginConfig pluginConfig, Logger logger) {
        this(plugin, chestService, pluginConfig, logger, null);
    }

    public ChunkKeepAliveService(Plugin plugin, ChestService chestService, PluginConfig pluginConfig, Logger logger, Server server) {
        this.plugin = plugin;
        this.chestService = chestService;
        this.pluginConfig = pluginConfig;
        this.logger = logger != null ? logger : Logger.getLogger(ChunkKeepAliveService.class.getName());
        this.server = server;
    }

    private World getWorld(String worldName) {
        if (server != null) {
            return server.getWorld(worldName);
        }
        return Bukkit.getWorld(worldName);
    }

    public void syncAllTickets() {
        if (pluginConfig != null && !pluginConfig.isChunkLoadingEnabled()) {
            removeAllTickets();
            return;
        }

        List<LinkedChest> chests = chestService.getAllLinkedChests();
        Set<String> desired = new HashSet<>();
        for (LinkedChest chest : chests) {
            int chunkX = chest.x() >> 4;
            int chunkZ = chest.z() >> 4;
            String key = chest.worldName() + ":" + chunkX + ":" + chunkZ;
            desired.add(key);

            if (!loadedChunks.contains(key)) {
                World world = getWorld(chest.worldName());
                if (world != null) {
                    try {
                        world.addPluginChunkTicket(chunkX, chunkZ, plugin);
                        loadedChunks.add(key);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Failed to add chunk ticket for " + key, t);
                    }
                }
            }
        }

        Set<String> toRemove = new HashSet<>(loadedChunks);
        toRemove.removeAll(desired);
        for (String key : toRemove) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                World world = getWorld(parts[0]);
                if (world != null) {
                    try {
                        int cx = Integer.parseInt(parts[1]);
                        int cz = Integer.parseInt(parts[2]);
                        world.removePluginChunkTicket(cx, cz, plugin);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Failed to remove chunk ticket for " + key, t);
                    }
                }
            }
            loadedChunks.remove(key);
        }
    }

    public void addTicketForChest(String worldName, int blockX, int blockZ) {
        if (pluginConfig != null && !pluginConfig.isChunkLoadingEnabled()) {
            return;
        }

        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        String key = worldName + ":" + chunkX + ":" + chunkZ;
        if (loadedChunks.contains(key)) {
            return;
        }

        World world = getWorld(worldName);
        if (world != null) {
            try {
                world.addPluginChunkTicket(chunkX, chunkZ, plugin);
                loadedChunks.add(key);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to add chunk ticket for " + key, t);
            }
        }
    }

    public void removeTicketIfNoOtherChests(String worldName, int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        String key = worldName + ":" + chunkX + ":" + chunkZ;

        List<LinkedChest> chests = chestService.getAllLinkedChests();
        boolean stillNeeded = false;
        for (LinkedChest chest : chests) {
            if (chest.worldName().equalsIgnoreCase(worldName) && (chest.x() >> 4) == chunkX && (chest.z() >> 4) == chunkZ) {
                stillNeeded = true;
                break;
            }
        }

        if (!stillNeeded && loadedChunks.contains(key)) {
            World world = getWorld(worldName);
            if (world != null) {
                try {
                    world.removePluginChunkTicket(chunkX, chunkZ, plugin);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Failed to remove chunk ticket for " + key, t);
                }
            }
            loadedChunks.remove(key);
        }
    }

    public void removeAllTickets() {
        for (String key : new HashSet<>(loadedChunks)) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                World world = getWorld(parts[0]);
                if (world != null) {
                    try {
                        int cx = Integer.parseInt(parts[1]);
                        int cz = Integer.parseInt(parts[2]);
                        world.removePluginChunkTicket(cx, cz, plugin);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Failed to remove chunk ticket for " + key, t);
                    }
                }
            }
        }
        loadedChunks.clear();
        if (server != null) {
            for (World w : server.getWorlds()) {
                try {
                    w.removePluginChunkTickets(plugin);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public Set<String> getLoadedChunks() {
        return Set.copyOf(loadedChunks);
    }
}
