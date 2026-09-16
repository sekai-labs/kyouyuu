package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChestService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

public class HopperAutomationListener implements Listener {

    private final ChestService chestService;

    public HopperAutomationListener(ChestService chestService) {
        this.chestService = chestService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Inventory sourceInv = event.getSource();
        Inventory destInv = event.getDestination();

        Location sourceLoc = sourceInv.getLocation();
        if (sourceLoc != null) {
            String worldName = sourceLoc.getWorld() != null ? sourceLoc.getWorld().getName() : "world";
            Optional<LinkedChest> linkedSource = chestService.getLinkedChest(worldName, sourceLoc.getBlockX(), sourceLoc.getBlockY(), sourceLoc.getBlockZ());
            if (linkedSource.isPresent()) {
                event.setCancelled(true);
                return;
            }
        }

        Location destLoc = destInv.getLocation();
        if (destLoc != null) {
            String worldName = destLoc.getWorld() != null ? destLoc.getWorld().getName() : "world";
            Optional<LinkedChest> linkedDest = chestService.getLinkedChest(worldName, destLoc.getBlockX(), destLoc.getBlockY(), destLoc.getBlockZ());
            if (linkedDest.isPresent()) {
                event.setCancelled(true);
            }
        }
    }
}
