package com.sekailabs.kyouyuu.inventory;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.storage.ChannelRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedInventoryManager {

    private final ChannelRepository channelRepository;
    private final ItemSerializer itemSerializer;
    private final Logger logger;
    private final Server server;

    private final Map<String, Inventory> inventoryCache = new ConcurrentHashMap<>();
    private final Set<String> dirtyChannels = ConcurrentHashMap.newKeySet();
    private final Map<String, ReentrantLock> channelLocks = new ConcurrentHashMap<>();

    public SharedInventoryManager(ChannelRepository channelRepository, ItemSerializer itemSerializer, Logger logger) {
        this(channelRepository, itemSerializer, logger, null);
    }

    public SharedInventoryManager(ChannelRepository channelRepository, ItemSerializer itemSerializer, Logger logger, Server server) {
        this.channelRepository = channelRepository;
        this.itemSerializer = itemSerializer;
        this.logger = logger != null ? logger : Logger.getLogger(SharedInventoryManager.class.getName());
        this.server = server;
    }

    private ReentrantLock getLock(String channelId) {
        return channelLocks.computeIfAbsent(channelId.toLowerCase(), k -> new ReentrantLock());
    }

    public Inventory getOrCreateInventory(Channel channel) {
        String id = channel.id().toLowerCase();
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Inventory existing = inventoryCache.get(id);
            if (existing != null) {
                return existing;
            }

            ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);
            Component title = Component.text(channel.name());
            Inventory inv;
            if (server != null) {
                inv = server.createInventory(holder, channel.size(), title);
            } else {
                inv = Bukkit.createInventory(holder, channel.size(), title);
            }
            holder.setInventory(inv);

            try {
                byte[] data = channelRepository.loadInventoryData(id);
                if (data != null && data.length > 0) {
                    ItemStack[] items = itemSerializer.deserialize(data, channel.size());
                    inv.setContents(items);
                }
            } catch (SQLException | IOException e) {
                logger.log(Level.SEVERE, "Failed to load inventory data for channel: " + id, e);
            }

            inventoryCache.put(id, inv);
            return inv;
        } finally {
            lock.unlock();
        }
    }

    public Inventory getCachedInventory(String channelId) {
        return inventoryCache.get(channelId.toLowerCase());
    }

    public void markDirty(String channelId) {
        dirtyChannels.add(channelId.toLowerCase());
    }

    public boolean isDirty(String channelId) {
        return dirtyChannels.contains(channelId.toLowerCase());
    }

    public boolean flushChannel(String channelId) {
        String id = channelId.toLowerCase();
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Inventory inv = inventoryCache.get(id);
            if (inv == null) {
                dirtyChannels.remove(id);
                return true;
            }

            ItemStack[] contents = inv.getContents();
            byte[] data = itemSerializer.serialize(contents);
            channelRepository.saveInventoryData(id, data);
            dirtyChannels.remove(id);
            return true;
        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Failed to flush inventory data for channel: " + id, e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    public int flushAllDirty() {
        int flushed = 0;
        for (String id : dirtyChannels) {
            if (flushChannel(id)) {
                flushed++;
            }
        }
        return flushed;
    }

    public void resizeOrUpdateChannel(Channel updatedChannel) {
        String id = updatedChannel.id().toLowerCase();
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Inventory oldInv = inventoryCache.get(id);
            if (oldInv == null) {

                return;
            }
            ItemStack[] oldContents = oldInv.getContents();

            ChannelInventoryHolder holder = new ChannelInventoryHolder(updatedChannel);
            Component title = Component.text(updatedChannel.name());
            Inventory newInv;
            if (server != null) {
                newInv = server.createInventory(holder, updatedChannel.size(), title);
            } else {
                newInv = Bukkit.createInventory(holder, updatedChannel.size(), title);
            }
            holder.setInventory(newInv);
            if (oldContents != null) {
                int limit = Math.min(oldContents.length, updatedChannel.size());
                for (int i = 0; i < limit; i++) {
                    newInv.setItem(i, oldContents[i]);
                }
            } else {
                try {
                    byte[] data = channelRepository.loadInventoryData(id);
                    if (data != null && data.length > 0) {
                        ItemStack[] items = itemSerializer.deserialize(data, updatedChannel.size());
                        newInv.setContents(items);
                    }
                } catch (SQLException | IOException e) {
                    logger.log(Level.SEVERE, "Failed to load inventory data for channel resize: " + id, e);
                }
            }

            inventoryCache.put(id, newInv);
            markDirty(id);
        } finally {
            lock.unlock();
        }
    }

    public void unloadChannel(String channelId) {
        String id = channelId.toLowerCase();
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            flushChannel(id);
            inventoryCache.remove(id);
            dirtyChannels.remove(id);
        } finally {
            lock.unlock();
        }
    }

    public Collection<Inventory> getAllLoadedInventories() {
        return inventoryCache.values();
    }
}
