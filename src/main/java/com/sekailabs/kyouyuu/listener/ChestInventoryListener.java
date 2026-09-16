package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.inventory.ChannelInventoryHolder;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.Set;

public class ChestInventoryListener implements Listener {

    private final SharedInventoryManager inventoryManager;
    private final AuthorizationService authorizationService;

    public ChestInventoryListener(SharedInventoryManager inventoryManager, AuthorizationService authorizationService) {
        this.inventoryManager = inventoryManager;
        this.authorizationService = authorizationService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();

        if (!(topInventory.getHolder() instanceof ChannelInventoryHolder holder)) {
            return;
        }

        String channelId = holder.getChannelId();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean isTopInventory = rawSlot >= 0 && rawSlot < topInventory.getSize();
        InventoryAction action = event.getAction();
        ClickType clickType = event.getClick();

        boolean attemptingDeposit = false;
        boolean attemptingWithdraw = false;

        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            attemptingWithdraw = true;
        } else if (isTopInventory) {

            switch (action) {
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY -> attemptingWithdraw = true;
                case PLACE_ALL, PLACE_SOME, PLACE_ONE -> attemptingDeposit = true;
                case SWAP_WITH_CURSOR -> {
                    attemptingDeposit = true;
                    attemptingWithdraw = true;
                }
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                    attemptingDeposit = true;
                    attemptingWithdraw = true;
                }
                default -> {}
            }
        } else {

            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {

                attemptingDeposit = true;
            }
        }

        if (attemptingDeposit && !authorizationService.canDeposit(player, channelId)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You do not have permission to deposit items into channel '" + channelId + "'.", NamedTextColor.RED));
            return;
        }

        if (attemptingWithdraw && !authorizationService.canWithdraw(player, channelId)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You do not have permission to withdraw items from channel '" + channelId + "'.", NamedTextColor.RED));
            return;
        }

        inventoryManager.markDirty(channelId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();

        if (!(topInventory.getHolder() instanceof ChannelInventoryHolder holder)) {
            return;
        }

        String channelId = holder.getChannelId();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Set<Integer> rawSlots = event.getRawSlots();
        boolean affectedTop = false;
        for (int slot : rawSlots) {
            if (slot < topInventory.getSize()) {
                affectedTop = true;
                break;
            }
        }

        if (affectedTop) {
            if (!authorizationService.canDeposit(player, channelId)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You do not have permission to deposit items into channel '" + channelId + "'.", NamedTextColor.RED));
                return;
            }
            inventoryManager.markDirty(channelId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof ChannelInventoryHolder holder) {
            String channelId = holder.getChannelId();

            if (inventory.getViewers().size() <= 1 && inventoryManager.isDirty(channelId)) {
                inventoryManager.flushChannel(channelId);
            }
            if (event.getPlayer() instanceof Player player) {
                org.bukkit.block.Block block = ChestInteractListener.getAndRemoveOpenBlock(player.getUniqueId());
                if (block != null) {
                    org.bukkit.block.BlockState state = block.getState();
                    if (state instanceof org.bukkit.block.Lidded lidded) {
                        try {
                            lidded.close();
                        } catch (Throwable ignored) {
                        }
                    }
                    org.bukkit.Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                    if (block.getType() == org.bukkit.Material.BARREL) {
                        block.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_BARREL_CLOSE, 0.5f, 1.0f);
                    } else {
                        block.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                    }
                }
            }
        }
    }
}
