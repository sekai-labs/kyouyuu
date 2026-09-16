package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChestService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HopperAutomationListenerTest {

    @Test
    void testCancelHopperTransferFromLinkedChest() {
        ChestService chestService = Mockito.mock(ChestService.class);
        HopperAutomationListener listener = new HopperAutomationListener(chestService);

        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");

        Location srcLoc = new Location(world, 10, 64, 20);
        Inventory srcInv = Mockito.mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Inventory destInv = Mockito.mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));
        when(chestService.getLinkedChest("world", 10, 63, 20)).thenReturn(Optional.empty());

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, Mockito.mock(org.bukkit.inventory.ItemStack.class), destInv, false);
        listener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void testCancelHopperTransferIntoLinkedChest() {
        ChestService chestService = Mockito.mock(ChestService.class);
        HopperAutomationListener listener = new HopperAutomationListener(chestService);

        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");

        Location srcLoc = new Location(world, 10, 65, 20);
        Inventory srcInv = Mockito.mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Location destLoc = new Location(world, 10, 64, 20);
        Inventory destInv = Mockito.mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(destLoc);

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 65, 20)).thenReturn(Optional.empty());
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, Mockito.mock(org.bukkit.inventory.ItemStack.class), destInv, false);
        listener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
    }
}
