package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChestService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockProtectionListenerTest {

    @Test
    void testBlockBreakWithoutPermissionCancelled() {
        ChestService chestService = Mockito.mock(ChestService.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        BlockProtectionListener listener = new BlockProtectionListener(chestService, authService);

        Block block = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkedChest linked = new LinkedChest("vault", "world", null, 0, 64, 0);
        when(chestService.getLinkedChest(block)).thenReturn(Optional.of(linked));

        Player player = Mockito.mock(Player.class);
        when(authService.canUnlinkChests(player)).thenReturn(false);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        verify(chestService, never()).unlinkChest(any());
    }

    @Test
    void testBlockBreakWithPermissionUnlinks() {
        ChestService chestService = Mockito.mock(ChestService.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        BlockProtectionListener listener = new BlockProtectionListener(chestService, authService);

        Block block = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkedChest linked = new LinkedChest("vault", "world", null, 0, 64, 0);
        when(chestService.getLinkedChest(block)).thenReturn(Optional.of(linked));

        Player player = Mockito.mock(Player.class);
        when(authService.canUnlinkChests(player)).thenReturn(true);

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
        verify(chestService, times(1)).unlinkChest(block);
    }

    @Test
    void testPistonExtendProtection() {
        ChestService chestService = Mockito.mock(ChestService.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        BlockProtectionListener listener = new BlockProtectionListener(chestService, authService);

        Block linkedBlock = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(linkedBlock)).thenReturn(true);
        when(chestService.getLinkedChest(linkedBlock)).thenReturn(Optional.of(new LinkedChest("v", "w", null, 0, 0, 0)));

        BlockPistonExtendEvent event = new BlockPistonExtendEvent(Mockito.mock(Block.class), List.of(linkedBlock), org.bukkit.block.BlockFace.NORTH);
        listener.onBlockPistonExtend(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void testPistonRetractProtection() {
        ChestService chestService = Mockito.mock(ChestService.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        BlockProtectionListener listener = new BlockProtectionListener(chestService, authService);

        Block linkedBlock = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(linkedBlock)).thenReturn(true);
        when(chestService.getLinkedChest(linkedBlock)).thenReturn(Optional.of(new LinkedChest("v", "w", null, 0, 0, 0)));

        BlockPistonRetractEvent event = new BlockPistonRetractEvent(Mockito.mock(Block.class), List.of(linkedBlock), org.bukkit.block.BlockFace.NORTH);
        listener.onBlockPistonRetract(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void testExplosionRemovesLinkedChestFromBlockList() {
        ChestService chestService = Mockito.mock(ChestService.class);
        AuthorizationService authService = Mockito.mock(AuthorizationService.class);
        BlockProtectionListener listener = new BlockProtectionListener(chestService, authService);

        Block normalBlock = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(normalBlock)).thenReturn(false);

        Block linkedBlock = Mockito.mock(Block.class);
        when(chestService.isLinkableContainer(linkedBlock)).thenReturn(true);
        when(chestService.getLinkedChest(linkedBlock)).thenReturn(Optional.of(new LinkedChest("v", "w", null, 0, 0, 0)));

        List<Block> blockList = new ArrayList<>(List.of(normalBlock, linkedBlock));
        BlockExplodeEvent event = Mockito.mock(BlockExplodeEvent.class);
        when(event.blockList()).thenReturn(blockList);
        listener.onBlockExplode(event);

        assertEquals(1, blockList.size());
        assertEquals(normalBlock, blockList.get(0));

        List<Block> entityExplodeList = new ArrayList<>(List.of(normalBlock, linkedBlock));
        EntityExplodeEvent entityEvent = Mockito.mock(EntityExplodeEvent.class);
        when(entityEvent.blockList()).thenReturn(entityExplodeList);
        listener.onEntityExplode(entityEvent);
        assertEquals(1, entityExplodeList.size());
    }
}
