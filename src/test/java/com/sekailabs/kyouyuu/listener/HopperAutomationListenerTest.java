package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.inventory.ChannelInventoryHolder;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HopperAutomationListenerTest {

    private ChestService chestService;
    private ChannelService channelService;
    private SharedInventoryManager inventoryManager;
    private PluginConfig pluginConfig;
    private HopperAutomationListener listener;
    private World world;

    @BeforeEach
    void setUp() {
        chestService = mock(ChestService.class);
        channelService = mock(ChannelService.class);
        inventoryManager = mock(SharedInventoryManager.class);
        pluginConfig = new PluginConfig();
        pluginConfig.load(new YamlConfiguration());

        listener = new HopperAutomationListener(chestService, channelService, inventoryManager, pluginConfig);

        world = mock(World.class);
        when(world.getName()).thenReturn("world");
    }

    @Test
    void testHopperDisabledCancelsTransfer() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader("hopper:\n  enabled: false\n"));
        PluginConfig disabledConfig = new PluginConfig();
        disabledConfig.load(config);

        HopperAutomationListener disabledListener = new HopperAutomationListener(chestService, channelService, inventoryManager, disabledConfig);

        Location srcLoc = new Location(world, 10, 64, 20);
        Inventory srcInv = mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Inventory destInv = mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, mock(ItemStack.class), destInv, false);
        disabledListener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void testLegacyConstructorCancelsTransfer() {
        HopperAutomationListener legacyListener = new HopperAutomationListener(chestService);

        Location srcLoc = new Location(world, 10, 64, 20);
        Inventory srcInv = mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Inventory destInv = mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, mock(ItemStack.class), destInv, false);
        legacyListener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void testHopperSearchEventBindsSharedInventory() {
        Block chestBlock = mock(Block.class);
        when(chestService.isLinkableContainer(chestBlock)).thenReturn(true);

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest(chestBlock)).thenReturn(Optional.of(linked));

        Channel channel = Channel.create("vault", "Vault Channel", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        Inventory sharedInv = mock(Inventory.class);
        when(inventoryManager.getOrCreateInventory(channel)).thenReturn(sharedInv);

        Block hopperBlock = mock(Block.class);
        HopperInventorySearchEvent event = new HopperInventorySearchEvent(
                mock(Inventory.class),
                HopperInventorySearchEvent.ContainerType.SOURCE,
                hopperBlock,
                chestBlock
        );

        listener.onHopperInventorySearch(event);

        assertEquals(sharedInv, event.getInventory());
    }

    @Test
    void testHopperExtractFromLinkedChestSuccess() {
        Location srcLoc = new Location(world, 10, 64, 20);
        Inventory srcInv = mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Inventory destInv = mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));
        when(chestService.getLinkedChest("world", 10, 63, 20)).thenReturn(Optional.empty());

        Channel channel = Channel.create("vault", "Vault Channel", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        Inventory sharedInv = mock(Inventory.class);
        when(inventoryManager.getOrCreateInventory(channel)).thenReturn(sharedInv);

        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);

        when(sharedInv.containsAtLeast(any(ItemStack.class), eq(1))).thenReturn(true);
        when(destInv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, item, destInv, false);
        listener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
        verify(sharedInv).removeItem(any(ItemStack.class));
        verify(inventoryManager).markDirty("vault");
    }

    @Test
    void testHopperInsertIntoLinkedChestSuccess() {
        Location srcLoc = new Location(world, 10, 65, 20);
        Inventory srcInv = mock(Inventory.class);
        when(srcInv.getLocation()).thenReturn(srcLoc);

        Location destLoc = new Location(world, 10, 64, 20);
        Inventory destInv = mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(destLoc);

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 65, 20)).thenReturn(Optional.empty());
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        Channel channel = Channel.create("vault", "Vault Channel", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        Inventory sharedInv = mock(Inventory.class);
        when(inventoryManager.getOrCreateInventory(channel)).thenReturn(sharedInv);

        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);

        when(sharedInv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, item, destInv, false);
        listener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
        verify(srcInv).removeItem(any(ItemStack.class));
        verify(inventoryManager).markDirty("vault");
    }

    @Test
    void testDirectSharedInventoryMoveItemMarksDirty() {
        Channel channel = Channel.create("vault", "Vault Channel", 27);
        ChannelInventoryHolder holder = new ChannelInventoryHolder(channel);

        Inventory sharedSource = mock(Inventory.class);
        when(sharedSource.getHolder()).thenReturn(holder);

        Inventory normalDest = mock(Inventory.class);
        when(normalDest.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(sharedSource, mock(ItemStack.class), normalDest, false);
        listener.onInventoryMoveItem(event);

        assertFalse(event.isCancelled());
        verify(inventoryManager).markDirty("vault");
    }

    @Test
    void testDoubleChestExtractionFromLeftSide() {
        Inventory srcInv = mock(DoubleChestInventory.class);
        when(srcInv.getLocation()).thenReturn(null);

        Inventory leftSide = mock(Inventory.class);
        Location leftLoc = new Location(world, 10, 64, 20);
        when(leftSide.getLocation()).thenReturn(leftLoc);
        when(((DoubleChestInventory) srcInv).getLeftSide()).thenReturn(leftSide);

        Inventory destInv = mock(Inventory.class);
        when(destInv.getLocation()).thenReturn(new Location(world, 10, 63, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        Channel channel = Channel.create("vault", "Vault Channel", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        Inventory sharedInv = mock(Inventory.class);
        when(inventoryManager.getOrCreateInventory(channel)).thenReturn(sharedInv);

        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);

        when(sharedInv.containsAtLeast(any(ItemStack.class), eq(1))).thenReturn(true);
        when(destInv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(srcInv, item, destInv, false);
        listener.onInventoryMoveItem(event);

        assertTrue(event.isCancelled());
        verify(sharedInv).removeItem(any(ItemStack.class));
        verify(inventoryManager).markDirty("vault");
    }
}
