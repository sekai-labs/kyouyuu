package com.sekailabs.kyouyuu.auth;

import org.bukkit.entity.Player;

public class PaperAuthorizationService implements AuthorizationService {

    @Override
    public boolean hasAdmin(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission(PERM_ADMIN);
    }

    @Override
    public boolean canUse(Player player) {
        if (player == null) return false;
        return hasAdmin(player) || player.hasPermission(PERM_USE);
    }

    @Override
    public boolean canAccess(Player player, String channelId) {
        if (player == null) return false;
        if (hasAdmin(player)) return true;
        if (!canUse(player)) return false;
        String normalized = channelId.toLowerCase().trim();
        return player.hasPermission("kyouyuu.channel." + normalized + ".access") ||
                player.hasPermission("kyouyuu.channel.*.access");
    }

    @Override
    public boolean canDeposit(Player player, String channelId) {
        if (player == null) return false;
        if (hasAdmin(player)) return true;
        if (!canAccess(player, channelId)) return false;
        String normalized = channelId.toLowerCase().trim();
        return player.hasPermission("kyouyuu.channel." + normalized + ".deposit") ||
                player.hasPermission("kyouyuu.channel.*.deposit");
    }

    @Override
    public boolean canWithdraw(Player player, String channelId) {
        if (player == null) return false;
        if (hasAdmin(player)) return true;
        if (!canAccess(player, channelId)) return false;
        String normalized = channelId.toLowerCase().trim();
        return player.hasPermission("kyouyuu.channel." + normalized + ".withdraw") ||
                player.hasPermission("kyouyuu.channel.*.withdraw");
    }

    @Override
    public boolean canManageChannels(Player player) {
        if (player == null) return false;
        return hasAdmin(player) || player.hasPermission(PERM_CHANNEL_MANAGE);
    }

    @Override
    public boolean canLinkChests(Player player) {
        if (player == null) return false;
        return hasAdmin(player) || player.hasPermission(PERM_CHEST_LINK);
    }

    @Override
    public boolean canUnlinkChests(Player player) {
        if (player == null) return false;
        return hasAdmin(player) || player.hasPermission(PERM_CHEST_UNLINK);
    }
}
