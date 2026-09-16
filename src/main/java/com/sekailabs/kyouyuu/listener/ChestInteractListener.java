package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import com.sekailabs.kyouyuu.service.LinkSessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Optional;

public class ChestInteractListener implements Listener {

    private final ChestService chestService;
    private final ChannelService channelService;
    private final SharedInventoryManager inventoryManager;
    private final AuthorizationService authorizationService;
    private final LinkSessionManager sessionManager;

    public ChestInteractListener(
            ChestService chestService,
            ChannelService channelService,
            SharedInventoryManager inventoryManager,
            AuthorizationService authorizationService,
            LinkSessionManager sessionManager
    ) {
        this.chestService = chestService;
        this.channelService = channelService;
        this.inventoryManager = inventoryManager;
        this.authorizationService = authorizationService;
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !chestService.isLinkableContainer(block)) {
            return;
        }

        Player player = event.getPlayer();

        Optional<LinkSessionManager.Session> sessionOpt = sessionManager.getSession(player.getUniqueId());
        if (sessionOpt.isPresent()) {
            event.setCancelled(true);
            LinkSessionManager.Session session = sessionOpt.get();
            sessionManager.clearSession(player.getUniqueId());

            if (session.type() == LinkSessionManager.SessionType.LINK) {
                handleLinkSessionClick(player, session.targetChannelId(), block);
            } else if (session.type() == LinkSessionManager.SessionType.UNLINK) {
                handleUnlinkSessionClick(player, block);
            }
            return;
        }

        Optional<LinkedChest> linkedOpt = chestService.getLinkedChest(block);
        if (linkedOpt.isEmpty()) {
            return; 
        }

        event.setCancelled(true);

        LinkedChest linkedChest = linkedOpt.get();
        String channelId = linkedChest.channelId();

        Optional<Channel> channelOpt = channelService.getChannel(channelId);
        if (channelOpt.isEmpty()) {
            player.sendMessage(Component.text("The channel linked to this chest ('" + channelId + "') no longer exists. Unlinking...", NamedTextColor.RED));
            chestService.unlinkChest(block);
            return;
        }

        if (!authorizationService.canAccess(player, channelId)) {
            player.sendMessage(Component.text("You do not have permission to access channel '" + channelId + "'.", NamedTextColor.RED));
            return;
        }

        Channel channel = channelOpt.get();
        Inventory inv = inventoryManager.getOrCreateInventory(channel);
        player.openInventory(inv);
    }

    private void handleLinkSessionClick(Player player, String channelId, Block block) {
        if (!authorizationService.canLinkChests(player)) {
            player.sendMessage(Component.text("You do not have permission to link chests.", NamedTextColor.RED));
            return;
        }

        Optional<Channel> channelOpt = channelService.getChannel(channelId);
        if (channelOpt.isEmpty()) {
            player.sendMessage(Component.text("Channel '" + channelId + "' does not exist.", NamedTextColor.RED));
            return;
        }

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest(channelId, block);
        if (result.success()) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
        }
    }

    private void handleUnlinkSessionClick(Player player, Block block) {
        if (!authorizationService.canUnlinkChests(player)) {
            player.sendMessage(Component.text("You do not have permission to unlink chests.", NamedTextColor.RED));
            return;
        }

        ChannelService.ServiceResult<Integer> result = chestService.unlinkChest(block);
        if (result.success()) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
        }
    }
}
