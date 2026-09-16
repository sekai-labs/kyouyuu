package com.sekailabs.kyouyuu.auth;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LuckPermsAuthorizationServiceTest {

    @Mock
    private LuckPerms luckPerms;

    @Mock
    private UserManager userManager;

    @Mock
    private ContextManager contextManager;

    @Mock
    private User user;

    @Mock
    private CachedDataManager cachedDataManager;

    @Mock
    private CachedPermissionData permissionData;

    @Mock
    private QueryOptions queryOptions;

    @Mock
    private Player player;

    private Logger logger;
    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("TestLogger");
    }

    @Test
    void testConstructorWithNullLuckPermsProviderFallback() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(logger);

        when(player.isOp()).thenReturn(false);
        when(player.hasPermission("kyouyuu.use")).thenReturn(true);
        when(player.hasPermission("kyouyuu.admin")).thenReturn(false);

        assertTrue(service.canUse(player));
        assertFalse(service.hasAdmin(player));

        LuckPermsAuthorizationService serviceWithNullLogger = new LuckPermsAuthorizationService(null);
        assertTrue(serviceWithNullLogger.canUse(player));
    }

    @Test
    void testConstructorWithMockedLuckPermsAndNullLogger() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, null);
        assertNotNull(service);

        when(player.isOp()).thenReturn(true);
        assertTrue(service.canUse(player));
    }

    @Test
    void testOpPlayerBypass() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(true);

        assertTrue(service.hasAdmin(player));
        assertTrue(service.canUse(player));
        assertTrue(service.canAccess(player, "channel1"));
        assertTrue(service.canDeposit(player, "channel1"));
        assertTrue(service.canWithdraw(player, "channel1"));
        assertTrue(service.canManageChannels(player));
        assertTrue(service.canLinkChests(player));
        assertTrue(service.canUnlinkChests(player));

        verifyNoInteractions(luckPerms);
    }

    @Test
    void testNullPlayerReturnsFalse() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        assertFalse(service.hasAdmin(null));
        assertFalse(service.canUse(null));
        assertFalse(service.canAccess(null, "channel1"));
        assertFalse(service.canDeposit(null, "channel1"));
        assertFalse(service.canWithdraw(null, "channel1"));
        assertFalse(service.canManageChannels(null));
        assertFalse(service.canLinkChests(null));
        assertFalse(service.canUnlinkChests(null));
    }

    @Test
    void testUserFoundInUserManagerWithQueryOptionsAndPermissionData() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_USE)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.alpha.access")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.alpha.deposit")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.alpha.withdraw")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission("kyouyuu.channel.*.withdraw")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_CHANNEL_MANAGE)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission(AuthorizationService.PERM_CHEST_LINK)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(Tristate.FALSE);

        assertFalse(service.hasAdmin(player));
        assertTrue(service.canUse(player));
        assertTrue(service.canAccess(player, "alpha"));
        assertTrue(service.canDeposit(player, "alpha"));
        assertFalse(service.canWithdraw(player, "alpha"));
        assertTrue(service.canManageChannels(player));
        assertTrue(service.canLinkChests(player));
        assertFalse(service.canUnlinkChests(player));
    }

    @Test
    void testWildcardPermissionInLuckPerms() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_USE)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.beta.access")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission("kyouyuu.channel.*.access")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.beta.deposit")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission("kyouyuu.channel.*.deposit")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.beta.withdraw")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission("kyouyuu.channel.*.withdraw")).thenReturn(Tristate.TRUE);

        assertTrue(service.canAccess(player, "beta"));
        assertTrue(service.canDeposit(player, "beta"));
        assertTrue(service.canWithdraw(player, "beta"));
    }

    @Test
    void testUserNullInUserManagerFallingBackToPlayerHasPermission() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(null);

        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.vault.access")).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.vault.deposit")).thenReturn(false);
        when(player.hasPermission("kyouyuu.channel.*.deposit")).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_CHANNEL_MANAGE)).thenReturn(true);

        assertFalse(service.hasAdmin(player));
        assertTrue(service.canUse(player));
        assertTrue(service.canAccess(player, "vault"));
        assertFalse(service.canDeposit(player, "vault"));
        assertTrue(service.canManageChannels(player));
    }

    @Test
    void testExceptionInLuckPermsCheckFallingBackToPlayerHasPermission() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(luckPerms.getUserManager()).thenThrow(new RuntimeException("LuckPerms error"));
        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(player.hasPermission(AuthorizationService.PERM_CHEST_LINK)).thenReturn(true);
        when(player.hasPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(false);

        assertFalse(service.hasAdmin(player));
        assertTrue(service.canUse(player));
        assertTrue(service.canLinkChests(player));
        assertFalse(service.canUnlinkChests(player));
    }

    @Test
    void testAdminBypassesAccessDepositWithdraw() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.TRUE);

        assertTrue(service.hasAdmin(player));
        assertTrue(service.canUse(player));
        assertTrue(service.canAccess(player, "admin_channel"));
        assertTrue(service.canDeposit(player, "admin_channel"));
        assertTrue(service.canWithdraw(player, "admin_channel"));
        assertTrue(service.canManageChannels(player));
        assertTrue(service.canLinkChests(player));
        assertTrue(service.canUnlinkChests(player));
    }

    @Test
    void testCanAccessDeniedWhenCannotUse() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_USE)).thenReturn(Tristate.FALSE);

        assertFalse(service.canUse(player));
        assertFalse(service.canAccess(player, "channel_x"));
        assertFalse(service.canDeposit(player, "channel_x"));
        assertFalse(service.canWithdraw(player, "channel_x"));
    }

    @Test
    void testCanDepositAndWithdrawDeniedWhenCannotAccess() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_USE)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.restricted.access")).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission("kyouyuu.channel.*.access")).thenReturn(Tristate.FALSE);

        assertTrue(service.canUse(player));
        assertFalse(service.canAccess(player, "restricted"));
        assertFalse(service.canDeposit(player, "restricted"));
        assertFalse(service.canWithdraw(player, "restricted"));
    }

    @Test
    void testChannelNameNormalization() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_USE)).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.my_channel.access")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.my_channel.deposit")).thenReturn(Tristate.TRUE);
        when(permissionData.checkPermission("kyouyuu.channel.my_channel.withdraw")).thenReturn(Tristate.TRUE);

        assertTrue(service.canAccess(player, "  MY_CHANNEL  "));
        assertTrue(service.canDeposit(player, "  MY_CHANNEL  "));
        assertTrue(service.canWithdraw(player, "  MY_CHANNEL  "));
    }

    @Test
    void testCanUnlinkChestsNonAdminPermitted() {
        LuckPermsAuthorizationService service = new LuckPermsAuthorizationService(luckPerms, logger);

        when(player.isOp()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.getUser(playerUuid)).thenReturn(user);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getQueryOptions(player)).thenReturn(queryOptions);
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(permissionData);

        when(permissionData.checkPermission(AuthorizationService.PERM_ADMIN)).thenReturn(Tristate.FALSE);
        when(permissionData.checkPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(Tristate.TRUE);

        assertTrue(service.canUnlinkChests(player));
    }
}
