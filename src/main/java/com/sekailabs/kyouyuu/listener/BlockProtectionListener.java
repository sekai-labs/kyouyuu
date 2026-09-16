package com.sekailabs.kyouyuu.listener;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class BlockProtectionListener implements Listener {

    private final ChestService chestService;
    private final AuthorizationService authorizationService;

    public BlockProtectionListener(ChestService chestService, AuthorizationService authorizationService) {
        this.chestService = chestService;
        this.authorizationService = authorizationService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!chestService.isLinkableContainer(block)) {
            return;
        }

        Optional<LinkedChest> linkedOpt = chestService.getLinkedChest(block);
        if (linkedOpt.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (!authorizationService.canUnlinkChests(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You do not have permission to break a linked chest. Unlink it first with /kyo unlink.", NamedTextColor.RED));
            return;
        }

        chestService.unlinkChest(block);
        player.sendMessage(Component.text("Linked chest unlinked due to block break.", NamedTextColor.YELLOW));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (chestService.isLinkableContainer(block) && chestService.getLinkedChest(block).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (chestService.isLinkableContainer(block) && chestService.getLinkedChest(block).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlockList(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionBlockList(event.blockList());
    }

    private void handleExplosionBlockList(List<Block> blocks) {
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (chestService.isLinkableContainer(block)) {
                Optional<LinkedChest> linkedOpt = chestService.getLinkedChest(block);
                if (linkedOpt.isPresent()) {

                    iterator.remove();
                }
            }
        }
    }
}
