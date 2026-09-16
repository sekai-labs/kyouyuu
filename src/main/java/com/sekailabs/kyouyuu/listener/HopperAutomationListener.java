package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.inventory.ChannelInventoryHolder;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import org.bukkit.Location;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;

public class HopperAutomationListener implements Listener {

    private final ChestService chestService;
    private final ChannelService channelService;
    private final SharedInventoryManager inventoryManager;
    private final PluginConfig pluginConfig;

    public HopperAutomationListener(ChestService chestService) {
        this(chestService, null, null, null);
    }

    public HopperAutomationListener(
            ChestService chestService,
            ChannelService channelService,
            SharedInventoryManager inventoryManager,
            PluginConfig pluginConfig
    ) {
        this.chestService = chestService;
        this.channelService = channelService;
        this.inventoryManager = inventoryManager;
        this.pluginConfig = pluginConfig;
    }

    private boolean isHopperEnabled() {
        return pluginConfig == null || pluginConfig.isHopperAutomationEnabled();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperInventorySearch(HopperInventorySearchEvent event) {
        if (!isHopperEnabled() || channelService == null || inventoryManager == null) {
            return;
        }

        org.bukkit.block.Block searchBlock = event.getSearchBlock();
        if (searchBlock == null || !chestService.isLinkableContainer(searchBlock)) {
            return;
        }

        Optional<LinkedChest> linkedOpt = chestService.getLinkedChest(searchBlock);
        if (linkedOpt.isEmpty()) {
            return;
        }

        String channelId = linkedOpt.get().channelId();
        Optional<Channel> channelOpt = channelService.getChannel(channelId);
        if (channelOpt.isEmpty()) {
            return;
        }

        Inventory sharedInv = inventoryManager.getOrCreateInventory(channelOpt.get());
        event.setInventory(sharedInv);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory sourceInv = event.getSource();
        Inventory destInv = event.getDestination();

        boolean hopperAllowed = isHopperEnabled() && channelService != null && inventoryManager != null;

        if (isSharedChannelInventory(sourceInv)) {
            if (!isHopperAllowed(hopperAllowed)) {
                event.setCancelled(true);
                return;
            }
            markDirtyIfShared(sourceInv);
        } else {
            Optional<LinkedChest> linkedSource = findLinkedChest(sourceInv);
            if (linkedSource.isPresent()) {
                if (!hopperAllowed) {
                    event.setCancelled(true);
                    return;
                }
                handleMoveForLinked(event, linkedSource.get(), sourceInv, true);
            }
        }

        if (isSharedChannelInventory(destInv)) {
            if (!isHopperAllowed(hopperAllowed)) {
                event.setCancelled(true);
                return;
            }
            markDirtyIfShared(destInv);
        } else {
            Optional<LinkedChest> linkedDest = findLinkedChest(destInv);
            if (linkedDest.isPresent()) {
                if (!hopperAllowed) {
                    event.setCancelled(true);
                    return;
                }
                handleMoveForLinked(event, linkedDest.get(), destInv, false);
            }
        }
    }

    private boolean isHopperAllowed(boolean hopperAllowed) {
        return hopperAllowed;
    }

    private boolean isSharedChannelInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ChannelInventoryHolder;
    }

    private void markDirtyIfShared(Inventory inv) {
        if (inv != null && inv.getHolder() instanceof ChannelInventoryHolder holder) {
            if (inventoryManager != null) {
                inventoryManager.markDirty(holder.getChannelId());
            }
        }
    }

    private void handleMoveForLinked(InventoryMoveItemEvent event, LinkedChest linkedChest, Inventory containerInv, boolean isSource) {
        Optional<Channel> channelOpt = channelService.getChannel(linkedChest.channelId());
        if (channelOpt.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        Inventory sharedInv = inventoryManager.getOrCreateInventory(channelOpt.get());
        if (isSource) {
            event.setCancelled(true);
            org.bukkit.inventory.ItemStack itemToMove = event.getItem();
            if (itemToMove == null || itemToMove.getAmount() <= 0) {
                return;
            }

            org.bukkit.inventory.ItemStack singleItem = itemToMove.clone();
            singleItem.setAmount(1);

            if (!sharedInv.containsAtLeast(singleItem, 1)) {
                return;
            }

            Inventory destInv = event.getDestination();
            var leftover = destInv.addItem(singleItem.clone());
            if (leftover.isEmpty()) {
                sharedInv.removeItem(singleItem.clone());
                inventoryManager.markDirty(linkedChest.channelId());
            } else {
                int accepted = 1 - leftover.values().stream().mapToInt(org.bukkit.inventory.ItemStack::getAmount).sum();
                if (accepted > 0) {
                    org.bukkit.inventory.ItemStack toRemoveFromDest = singleItem.clone();
                    toRemoveFromDest.setAmount(accepted);
                    destInv.removeItem(toRemoveFromDest);
                }
            }
        } else {
            event.setCancelled(true);
            org.bukkit.inventory.ItemStack itemToMove = event.getItem();
            if (itemToMove == null || itemToMove.getAmount() <= 0) {
                return;
            }

            org.bukkit.inventory.ItemStack singleItem = itemToMove.clone();
            singleItem.setAmount(1);

            var leftover = sharedInv.addItem(singleItem.clone());
            if (leftover.isEmpty()) {
                event.getSource().removeItem(singleItem.clone());
                inventoryManager.markDirty(linkedChest.channelId());
            } else {
                int accepted = 1 - leftover.values().stream().mapToInt(org.bukkit.inventory.ItemStack::getAmount).sum();
                if (accepted > 0) {
                    org.bukkit.inventory.ItemStack toRemoveFromShared = singleItem.clone();
                    toRemoveFromShared.setAmount(accepted);
                    sharedInv.removeItem(toRemoveFromShared);
                }
            }
        }
    }

    private Optional<LinkedChest> findLinkedChest(Inventory inv) {
        if (inv == null) return Optional.empty();

        Location loc = inv.getLocation();
        if (loc != null) {
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            Optional<LinkedChest> linked = chestService.getLinkedChest(worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            if (linked.isPresent()) {
                return linked;
            }
        }

        InventoryHolder holder = inv.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof InventoryHolder leftHolder && leftHolder.getInventory().getLocation() != null) {
                Location leftLoc = leftHolder.getInventory().getLocation();
                String w = leftLoc.getWorld() != null ? leftLoc.getWorld().getName() : "world";
                Optional<LinkedChest> linked = chestService.getLinkedChest(w, leftLoc.getBlockX(), leftLoc.getBlockY(), leftLoc.getBlockZ());
                if (linked.isPresent()) return linked;
            }
            if (doubleChest.getRightSide() instanceof InventoryHolder rightHolder && rightHolder.getInventory().getLocation() != null) {
                Location rightLoc = rightHolder.getInventory().getLocation();
                String w = rightLoc.getWorld() != null ? rightLoc.getWorld().getName() : "world";
                Optional<LinkedChest> linked = chestService.getLinkedChest(w, rightLoc.getBlockX(), rightLoc.getBlockY(), rightLoc.getBlockZ());
                if (linked.isPresent()) return linked;
            }
        }

        if (inv instanceof DoubleChestInventory dci) {
            Inventory left = dci.getLeftSide();
            if (left != null && left.getLocation() != null) {
                Location lLoc = left.getLocation();
                String w = lLoc.getWorld() != null ? lLoc.getWorld().getName() : "world";
                Optional<LinkedChest> linked = chestService.getLinkedChest(w, lLoc.getBlockX(), lLoc.getBlockY(), lLoc.getBlockZ());
                if (linked.isPresent()) return linked;
            }
            Inventory right = dci.getRightSide();
            if (right != null && right.getLocation() != null) {
                Location rLoc = right.getLocation();
                String w = rLoc.getWorld() != null ? rLoc.getWorld().getName() : "world";
                Optional<LinkedChest> linked = chestService.getLinkedChest(w, rLoc.getBlockX(), rLoc.getBlockY(), rLoc.getBlockZ());
                if (linked.isPresent()) return linked;
            }
        }

        return Optional.empty();
    }
}
