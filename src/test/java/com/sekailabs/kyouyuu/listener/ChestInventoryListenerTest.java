package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.inventory.ChannelInventoryHolder;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChestInventoryListenerTest {

    @Test
    void testWithdrawDenied() {
        SharedInventoryManager invManager = Mockito.mock(SharedInventoryManager.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        ChestInventoryListener listener = new ChestInventoryListener(invManager, authService);

        Channel channel = Channel.create("vault", "Vault", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);
        Inventory topInv = Mockito.mock(Inventory.class);
        when(topInv.getHolder()).thenReturn(holder);
        when(topInv.getSize()).thenReturn(27);

        InventoryView view = Mockito.mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(topInv);

        Player player = Mockito.mock(Player.class);
        when(authService.canWithdraw(player, "vault")).thenReturn(false);
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(0);
        when(event.getAction()).thenReturn(InventoryAction.PICKUP_ALL);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getWhoClicked()).thenReturn(player);

        listener.onInventoryClick(event);
        verify(event).setCancelled(true);
        verify(invManager, never()).markDirty(any());
    }

    @Test
    void testDepositDenied() {
        SharedInventoryManager invManager = Mockito.mock(SharedInventoryManager.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        ChestInventoryListener listener = new ChestInventoryListener(invManager, authService);

        Channel channel = Channel.create("vault", "Vault", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);
        Inventory topInv = Mockito.mock(Inventory.class);
        when(topInv.getHolder()).thenReturn(holder);
        when(topInv.getSize()).thenReturn(27);

        InventoryView view = Mockito.mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(topInv);

        Player player = Mockito.mock(Player.class);
        when(authService.canDeposit(player, "vault")).thenReturn(false);

        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(30);
        when(event.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(event.getClick()).thenReturn(ClickType.SHIFT_LEFT);
        when(event.getWhoClicked()).thenReturn(player);

        listener.onInventoryClick(event);
        verify(event).setCancelled(true);
        verify(invManager, never()).markDirty(any());
    }

    @Test
    void testDepositAndWithdrawAllowed() {
        SharedInventoryManager invManager = Mockito.mock(SharedInventoryManager.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        ChestInventoryListener listener = new ChestInventoryListener(invManager, authService);

        Channel channel = Channel.create("vault", "Vault", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);
        Inventory topInv = Mockito.mock(Inventory.class);
        when(topInv.getHolder()).thenReturn(holder);
        when(topInv.getSize()).thenReturn(27);

        InventoryView view = Mockito.mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(topInv);

        Player player = Mockito.mock(Player.class);
        when(authService.canWithdraw(player, "vault")).thenReturn(true);
        when(authService.canDeposit(player, "vault")).thenReturn(true);
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(0);
        when(event.getAction()).thenReturn(InventoryAction.PICKUP_ALL);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getWhoClicked()).thenReturn(player);

        listener.onInventoryClick(event);
        verify(event, never()).setCancelled(true);
        verify(invManager, times(1)).markDirty("vault");
    }

    @Test
    void testDragDeniedWhenNoDepositPermission() {
        SharedInventoryManager invManager = Mockito.mock(SharedInventoryManager.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        ChestInventoryListener listener = new ChestInventoryListener(invManager, authService);

        Channel channel = Channel.create("vault", "Vault", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);
        Inventory topInv = Mockito.mock(Inventory.class);
        when(topInv.getHolder()).thenReturn(holder);
        when(topInv.getSize()).thenReturn(27);

        InventoryView view = Mockito.mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(topInv);

        Player player = Mockito.mock(Player.class);
        when(authService.canDeposit(player, "vault")).thenReturn(false);
        InventoryDragEvent event = Mockito.mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlots()).thenReturn(Set.of(0, 1));
        when(event.getWhoClicked()).thenReturn(player);

        listener.onInventoryDrag(event);
        verify(event).setCancelled(true);
        verify(invManager, never()).markDirty(any());
    }
}
