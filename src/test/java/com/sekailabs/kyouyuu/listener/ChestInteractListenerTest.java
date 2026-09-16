package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import com.sekailabs.kyouyuu.service.LinkSessionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChestInteractListenerTest {

    private ChestService chestService;
    private ChannelService channelService;
    private SharedInventoryManager inventoryManager;
    private AuthorizationService authorizationService;
    private LinkSessionManager sessionManager;
    private ChestInteractListener listener;

    private Player player;
    private UUID playerUuid;
    private Block block;
    private PlayerInteractEvent event;

    @BeforeEach
    void setUp() {
        chestService = mock(ChestService.class);
        channelService = mock(ChannelService.class);
        inventoryManager = mock(SharedInventoryManager.class);
        authorizationService = mock(AuthorizationService.class);
        sessionManager = mock(LinkSessionManager.class);

        listener = new ChestInteractListener(
                chestService,
                channelService,
                inventoryManager,
                authorizationService,
                sessionManager
        );

        player = mock(Player.class);
        playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);

        block = mock(Block.class);
        event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
    }

    @Test
    void testNonRightClickBlockIgnored() {
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
        verify(chestService, never()).getLinkedChest(any(Block.class));
    }

    @Test
    void testPhysicalActionIgnored() {
        when(event.getAction()).thenReturn(Action.PHYSICAL);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
    }

    @Test
    void testNonHandEquipmentSlotIgnored() {
        when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
    }

    @Test
    void testNullEquipmentSlotIgnored() {
        when(event.getHand()).thenReturn(null);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
    }

    @Test
    void testNullClickedBlockIgnored() {
        when(event.getClickedBlock()).thenReturn(null);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
    }

    @Test
    void testNonLinkableContainerIgnored() {
        when(chestService.isLinkableContainer(block)).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(sessionManager, never()).getSession(any());
    }

    @Test
    void testActiveLinkSessionWithoutPermission() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.LINK,
                "vault",
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canLinkChests(player)).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(player).sendMessage(any(Component.class));
        verify(channelService, never()).getChannel(any());
        verify(chestService, never()).linkChest(any(), any());
    }

    @Test
    void testActiveLinkSessionWithPermissionInvalidChannel() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.LINK,
                "nonexistent",
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canLinkChests(player)).thenReturn(true);
        when(channelService.getChannel("nonexistent")).thenReturn(Optional.empty());

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(player).sendMessage(any(Component.class));
        verify(chestService, never()).linkChest(any(), any());
    }

    @Test
    void testActiveLinkSessionWithPermissionValidChannelLinkSuccess() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.LINK,
                "vault",
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canLinkChests(player)).thenReturn(true);

        Channel channel = Channel.create("vault", "Vault", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        LinkedChest linked = new LinkedChest("vault", "world", null, 0, 64, 0);
        when(chestService.linkChest("vault", block))
                .thenReturn(ChannelService.ServiceResult.ok(List.of(linked), "Successfully linked"));

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(chestService).linkChest("vault", block);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testActiveLinkSessionWithPermissionValidChannelLinkFailure() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.LINK,
                "vault",
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canLinkChests(player)).thenReturn(true);

        Channel channel = Channel.create("vault", "Vault", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));

        when(chestService.linkChest("vault", block))
                .thenReturn(ChannelService.ServiceResult.fail("Already linked to another channel"));

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(chestService).linkChest("vault", block);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testActiveUnlinkSessionWithoutPermission() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.UNLINK,
                null,
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canUnlinkChests(player)).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(player).sendMessage(any(Component.class));
        verify(chestService, never()).unlinkChest(any());
    }

    @Test
    void testActiveUnlinkSessionWithPermissionUnlinkSuccess() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.UNLINK,
                null,
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canUnlinkChests(player)).thenReturn(true);
        when(chestService.unlinkChest(block)).thenReturn(ChannelService.ServiceResult.ok(1, "Successfully unlinked"));

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(chestService).unlinkChest(block);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testActiveUnlinkSessionWithPermissionUnlinkFailure() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        LinkSessionManager.Session session = new LinkSessionManager.Session(
                playerUuid,
                LinkSessionManager.SessionType.UNLINK,
                null,
                Instant.now().plusSeconds(30)
        );
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.of(session));
        when(authorizationService.canUnlinkChests(player)).thenReturn(true);
        when(chestService.unlinkChest(block)).thenReturn(ChannelService.ServiceResult.fail("Not linked"));

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(sessionManager).clearSession(playerUuid);
        verify(chestService).unlinkChest(block);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testInteractingWithUnlinkedChest() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.empty());
        when(chestService.getLinkedChest(block)).thenReturn(Optional.empty());

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(anyBoolean());
        verify(channelService, never()).getChannel(any());
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void testInteractingWithLinkedChestChannelDoesNotExistAutoUnlinks() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.empty());

        LinkedChest linked = new LinkedChest("deleted-channel", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest(block)).thenReturn(Optional.of(linked));
        when(channelService.getChannel("deleted-channel")).thenReturn(Optional.empty());

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(player).sendMessage(any(Component.class));
        verify(chestService).unlinkChest(block);
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void testInteractingWithLinkedChestLacksAccessPermission() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.empty());

        LinkedChest linked = new LinkedChest("secret", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest(block)).thenReturn(Optional.of(linked));

        Channel channel = Channel.create("secret", "Secret Channel", 27);
        when(channelService.getChannel("secret")).thenReturn(Optional.of(channel));
        when(authorizationService.canAccess(player, "secret")).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(player).sendMessage(any(Component.class));
        verify(inventoryManager, never()).getOrCreateInventory(any());
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void testInteractingWithLinkedChestWithPermissionOpensInventory() {
        when(chestService.isLinkableContainer(block)).thenReturn(true);
        when(sessionManager.getSession(playerUuid)).thenReturn(Optional.empty());

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestService.getLinkedChest(block)).thenReturn(Optional.of(linked));

        Channel channel = Channel.create("vault", "Vault", 27);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(channel));
        when(authorizationService.canAccess(player, "vault")).thenReturn(true);

        Inventory inventory = mock(Inventory.class);
        when(inventoryManager.getOrCreateInventory(channel)).thenReturn(inventory);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(inventoryManager).getOrCreateInventory(channel);
        verify(player).openInventory(inventory);
    }
}
