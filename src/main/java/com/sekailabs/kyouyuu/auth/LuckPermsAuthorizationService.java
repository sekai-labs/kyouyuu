package com.sekailabs.kyouyuu.auth;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LuckPermsAuthorizationService implements AuthorizationService {

    private final PaperAuthorizationService fallback;
    private final Logger logger;
    private LuckPerms luckPerms;

    public LuckPermsAuthorizationService(Logger logger) {
        this.fallback = new PaperAuthorizationService();
        this.logger = logger != null ? logger : Logger.getLogger(LuckPermsAuthorizationService.class.getName());
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException | NoClassDefFoundError e) {
            this.luckPerms = null;
        }
    }

    public LuckPermsAuthorizationService(LuckPerms luckPerms, Logger logger) {
        this.fallback = new PaperAuthorizationService();
        this.logger = logger != null ? logger : Logger.getLogger(LuckPermsAuthorizationService.class.getName());
        this.luckPerms = luckPerms;
    }

    private boolean checkPermission(Player player, String permission) {
        if (player == null) return false;
        if (player.isOp()) return true;

        if (luckPerms == null) {
            return player.hasPermission(permission);
        }

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);
                return user.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "LuckPerms check failed, falling back to Bukkit", e);
        }

        return player.hasPermission(permission);
    }

    @Override
    public boolean hasAdmin(Player player) {
        return checkPermission(player, PERM_ADMIN);
    }

    @Override
    public boolean canUse(Player player) {
        return hasAdmin(player) || checkPermission(player, PERM_USE);
    }

    @Override
    public boolean canAccess(Player player, String channelId) {
        if (hasAdmin(player)) return true;
        if (!canUse(player)) return false;
        String normalized = channelId.toLowerCase().trim();
        return checkPermission(player, "kyouyuu.channel." + normalized + ".access") ||
                checkPermission(player, "kyouyuu.channel.*.access");
    }

    @Override
    public boolean canDeposit(Player player, String channelId) {
        if (hasAdmin(player)) return true;
        if (!canAccess(player, channelId)) return false;
        String normalized = channelId.toLowerCase().trim();
        return checkPermission(player, "kyouyuu.channel." + normalized + ".deposit") ||
                checkPermission(player, "kyouyuu.channel.*.deposit");
    }

    @Override
    public boolean canWithdraw(Player player, String channelId) {
        if (hasAdmin(player)) return true;
        if (!canAccess(player, channelId)) return false;
        String normalized = channelId.toLowerCase().trim();
        return checkPermission(player, "kyouyuu.channel." + normalized + ".withdraw") ||
                checkPermission(player, "kyouyuu.channel.*.withdraw");
    }

    @Override
    public boolean canManageChannels(Player player) {
        return hasAdmin(player) || checkPermission(player, PERM_CHANNEL_MANAGE);
    }

    @Override
    public boolean canLinkChests(Player player) {
        return hasAdmin(player) || checkPermission(player, PERM_CHEST_LINK);
    }

    @Override
    public boolean canUnlinkChests(Player player) {
        return hasAdmin(player) || checkPermission(player, PERM_CHEST_UNLINK);
    }
}
