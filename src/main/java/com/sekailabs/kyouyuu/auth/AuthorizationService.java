package com.sekailabs.kyouyuu.auth;

import org.bukkit.entity.Player;

public interface AuthorizationService {

    String PERM_ADMIN = "kyouyuu.admin";
    String PERM_USE = "kyouyuu.use";
    String PERM_CHANNEL_MANAGE = "kyouyuu.channel.manage";
    String PERM_CHEST_LINK = "kyouyuu.chest.link";
    String PERM_CHEST_UNLINK = "kyouyuu.chest.unlink";

    boolean hasAdmin(Player player);

    boolean canUse(Player player);

    boolean canAccess(Player player, String channelId);

    boolean canDeposit(Player player, String channelId);

    boolean canWithdraw(Player player, String channelId);

    boolean canManageChannels(Player player);

    boolean canLinkChests(Player player);

    boolean canUnlinkChests(Player player);
}
