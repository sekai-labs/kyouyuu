package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.inventory.ItemSerializer;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.storage.ChannelRepository;
import com.sekailabs.kyouyuu.storage.ChestRepository;
import org.bukkit.inventory.Inventory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class ChannelService {

    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

    private final ChannelRepository channelRepository;
    private final ChestRepository chestRepository;
    private final SharedInventoryManager inventoryManager;
    private final ItemSerializer itemSerializer;

    public ChannelService(
            ChannelRepository channelRepository,
            ChestRepository chestRepository,
            SharedInventoryManager inventoryManager,
            ItemSerializer itemSerializer
    ) {
        this.channelRepository = channelRepository;
        this.chestRepository = chestRepository;
        this.inventoryManager = inventoryManager;
        this.itemSerializer = itemSerializer;
    }

    public record ServiceResult<T>(boolean success, String message, T data) {
        public static <T> ServiceResult<T> ok(T data, String message) {
            return new ServiceResult<>(true, message, data);
        }

        public static <T> ServiceResult<T> ok(T data) {
            return new ServiceResult<>(true, null, data);
        }

        public static <T> ServiceResult<T> fail(String message) {
            return new ServiceResult<>(false, message, null);
        }
    }

    public boolean isValidChannelId(String id) {
        return id != null && VALID_ID_PATTERN.matcher(id).matches();
    }

    public ServiceResult<Channel> createChannel(String rawId, String displayName, int size) {
        if (rawId == null || rawId.isBlank()) {
            return ServiceResult.fail("Channel ID cannot be empty.");
        }
        String id = rawId.toLowerCase().trim();
        if (!isValidChannelId(id)) {
            return ServiceResult.fail("Channel ID must be 1-32 characters and contain only alphanumeric characters, underscores, or hyphens.");
        }
        if (size <= 0 || size > 54 || size % 9 != 0) {
            return ServiceResult.fail("Channel size must be a multiple of 9 between 9 and 54 slots.");
        }
        String name = (displayName == null || displayName.isBlank()) ? rawId : displayName.trim();

        try {
            if (channelRepository.findById(id).isPresent()) {
                return ServiceResult.fail("A channel with ID '" + id + "' already exists.");
            }

            Channel channel = Channel.create(id, name, size);
            channelRepository.insert(channel);
            return ServiceResult.ok(channel, "Channel '" + id + "' created successfully.");
        } catch (SQLException e) {
            return ServiceResult.fail("Database error while creating channel: " + e.getMessage());
        }
    }

    public ServiceResult<Channel> resizeChannel(String rawId, int newSize, boolean force) {
        if (rawId == null) return ServiceResult.fail("Channel ID cannot be null.");
        String id = rawId.toLowerCase().trim();
        if (newSize <= 0 || newSize > 54 || newSize % 9 != 0) {
            return ServiceResult.fail("Channel size must be a multiple of 9 between 9 and 54 slots.");
        }

        try {
            Optional<Channel> opt = channelRepository.findById(id);
            if (opt.isEmpty()) {
                return ServiceResult.fail("Channel '" + id + "' not found.");
            }

            Channel channel = opt.get();
            if (channel.size() == newSize) {
                return ServiceResult.ok(channel, "Channel is already of size " + newSize + ".");
            }

            if (newSize < channel.size() && !force) {
                Inventory cached = inventoryManager.getCachedInventory(id);
                if (cached != null) {
                    for (int i = newSize; i < cached.getSize(); i++) {
                        if (!ItemSerializer.isSlotEmpty(cached.getItem(i))) {
                            return ServiceResult.fail("Cannot shrink channel: items exist in slots " + (newSize + 1) + " to " + channel.size() + ". Empty them or use --force.");
                        }
                    }
                } else {
                    byte[] data = channelRepository.loadInventoryData(id);
                    if (data != null && itemSerializer.hasItemsBeyondSlot(data, newSize)) {
                        return ServiceResult.fail("Cannot shrink channel: items exist beyond new size. Empty them or use --force.");
                    }
                }
            }

            Channel updated = channel.withSize(newSize);
            channelRepository.update(updated);
            inventoryManager.resizeOrUpdateChannel(updated);
            return ServiceResult.ok(updated, "Channel '" + id + "' resized to " + newSize + " slots.");
        } catch (SQLException | IOException e) {
            return ServiceResult.fail("Error resizing channel: " + e.getMessage());
        }
    }

    public ServiceResult<Boolean> deleteChannel(String rawId, boolean force) {
        if (rawId == null) return ServiceResult.fail("Channel ID cannot be null.");
        String id = rawId.toLowerCase().trim();

        try {
            Optional<Channel> opt = channelRepository.findById(id);
            if (opt.isEmpty()) {
                return ServiceResult.fail("Channel '" + id + "' not found.");
            }

            if (!force) {

                Inventory cached = inventoryManager.getCachedInventory(id);
                boolean hasItems = false;
                if (cached != null) {
                    hasItems = ItemSerializer.hasAnyItems(cached.getContents());
                } else {
                    byte[] data = channelRepository.loadInventoryData(id);
                    if (data != null && itemSerializer.hasItemsBeyondSlot(data, 0)) {
                        hasItems = true;
                    }
                }

                if (hasItems) {
                    return ServiceResult.fail("Channel contains items. Use --force to delete it and discard all contents.");
                }
            }

            inventoryManager.unloadChannel(id);
            chestRepository.deleteByChannel(id);
            channelRepository.delete(id);
            return ServiceResult.ok(true, "Channel '" + id + "' deleted successfully.");
        } catch (SQLException | IOException e) {
            return ServiceResult.fail("Error deleting channel: " + e.getMessage());
        }
    }

    public Optional<Channel> getChannel(String rawId) {
        if (rawId == null) return Optional.empty();
        try {
            return channelRepository.findById(rawId.toLowerCase().trim());
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<Channel> listChannels() {
        try {
            return channelRepository.findAll();
        } catch (SQLException e) {
            return List.of();
        }
    }
}
