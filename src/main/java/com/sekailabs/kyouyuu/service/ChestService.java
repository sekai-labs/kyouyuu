package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.model.ChestLocation;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.storage.ChestRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChestService {

    private final ChestRepository chestRepository;
    private final Logger logger;
    private java.util.function.Consumer<LinkedChest> onChestLinked;
    private java.util.function.Consumer<ChestLocation> onChestUnlinked;

    public ChestService(ChestRepository chestRepository, Logger logger) {
        this.chestRepository = chestRepository;
        this.logger = logger != null ? logger : Logger.getLogger(ChestService.class.getName());
    }

    public void setLinkListeners(java.util.function.Consumer<LinkedChest> onLinked, java.util.function.Consumer<ChestLocation> onUnlinked) {
        this.onChestLinked = onLinked;
        this.onChestUnlinked = onUnlinked;
    }

    public boolean isLinkableContainer(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
    }

    public List<ChestLocation> resolveContainerLocations(Block block) {
        List<ChestLocation> locations = new ArrayList<>();
        if (block == null) return locations;

        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest doubleChest) {
                if (doubleChest.getLeftSide() instanceof Chest left) {
                    Location loc = left.getLocation();
                    locations.add(new ChestLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                }
                if (doubleChest.getRightSide() instanceof Chest right) {
                    Location loc = right.getLocation();
                    locations.add(new ChestLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                }
                if (!locations.isEmpty()) {
                    return locations;
                }
            }
        }

        Location loc = block.getLocation();
        String worldName = block.getWorld() != null ? block.getWorld().getName() : "world";
        locations.add(new ChestLocation(worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        return locations;
    }

    public ChannelService.ServiceResult<List<LinkedChest>> linkChest(String channelId, Block block) {
        if (!isLinkableContainer(block)) {
            return ChannelService.ServiceResult.fail("Target block is not a valid container (chest, trapped chest, or barrel).");
        }

        List<ChestLocation> locations = resolveContainerLocations(block);
        UUID worldUid = block.getWorld() != null ? block.getWorld().getUID() : null;

        for (ChestLocation loc : locations) {
            try {
                Optional<LinkedChest> existing = chestRepository.findByLocation(loc);
                if (existing.isPresent()) {
                    if (existing.get().channelId().equalsIgnoreCase(channelId)) {
                        return ChannelService.ServiceResult.fail("This chest is already linked to channel '" + channelId + "'.");
                    } else {
                        return ChannelService.ServiceResult.fail("This chest is already linked to another channel ('" + existing.get().channelId() + "'). Unlink it first.");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error checking chest link: " + loc, e);
                return ChannelService.ServiceResult.fail("Database error checking chest link.");
            }
        }

        List<LinkedChest> linkedChests = new ArrayList<>();
        for (ChestLocation loc : locations) {
            LinkedChest lc = new LinkedChest(channelId.toLowerCase().trim(), loc.worldName(), worldUid, loc.x(), loc.y(), loc.z());
            try {
                chestRepository.link(lc);
                linkedChests.add(lc);
                if (onChestLinked != null) {
                    try {
                        onChestLinked.accept(lc);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Error in onChestLinked listener", t);
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to link chest at: " + loc, e);
                return ChannelService.ServiceResult.fail("Database error while linking chest: " + e.getMessage());
            }
        }

        String countMsg = linkedChests.size() > 1 ? " (double chest: 2 blocks linked)" : "";
        return ChannelService.ServiceResult.ok(linkedChests, "Successfully linked chest to channel '" + channelId + "'" + countMsg + ".");
    }

    public ChannelService.ServiceResult<Integer> unlinkChest(Block block) {
        List<ChestLocation> locations = resolveContainerLocations(block);
        int unlinkedCount = 0;

        for (ChestLocation loc : locations) {
            try {
                if (chestRepository.unlink(loc.worldName(), loc.x(), loc.y(), loc.z())) {
                    unlinkedCount++;
                }
                    if (onChestUnlinked != null) {
                        try {
                            onChestUnlinked.accept(loc);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Error in onChestUnlinked listener", t);
                        }
                    }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error unlinking chest at: " + loc, e);
            }
        }

        if (unlinkedCount > 0) {
            return ChannelService.ServiceResult.ok(unlinkedCount, "Successfully unlinked chest (" + unlinkedCount + " block" + (unlinkedCount > 1 ? "s" : "") + ").");
        } else {
            return ChannelService.ServiceResult.fail("This chest is not linked to any channel.");
        }
    }

    public boolean unlinkLocation(String worldName, int x, int y, int z) {
        try {
            boolean removed = chestRepository.unlink(worldName, x, y, z);
            if (removed && onChestUnlinked != null) {
                try {
                    onChestUnlinked.accept(new ChestLocation(worldName, x, y, z));
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Error in onChestUnlinked listener", t);
                }
            }
            return removed;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error unlinking location: " + worldName + "[" + x + "," + y + "," + z + "]", e);
            return false;
        }
    }

    public Optional<LinkedChest> getLinkedChest(Block block) {
        if (block == null) return Optional.empty();
        Location loc = block.getLocation();
        String worldName = block.getWorld() != null ? block.getWorld().getName() : "world";
        return getLinkedChest(worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Optional<LinkedChest> getLinkedChest(String worldName, int x, int y, int z) {
        try {
            return chestRepository.findByLocation(worldName, x, y, z);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error retrieving linked chest", e);
            return Optional.empty();
        }
    }

    public List<LinkedChest> getChestsForChannel(String channelId) {
        try {
            return chestRepository.findByChannel(channelId);
        } catch (SQLException e) {
            return List.of();
        }
    }

    public List<LinkedChest> getAllLinkedChests() {
        try {
            return chestRepository.findAll();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error retrieving all linked chests", e);
            return List.of();
        }
    }
}
