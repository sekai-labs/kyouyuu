package com.sekailabs.kyouyuu.auth;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    @Test
    void testPaperAuthorizationHierarchy() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player adminPlayer = Mockito.mock(Player.class);
        when(adminPlayer.isOp()).thenReturn(false);
        when(adminPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(true);

        assertTrue(auth.hasAdmin(adminPlayer));
        assertTrue(auth.canUse(adminPlayer));
        assertTrue(auth.canAccess(adminPlayer, "any_channel"));
        assertTrue(auth.canDeposit(adminPlayer, "any_channel"));
        assertTrue(auth.canWithdraw(adminPlayer, "any_channel"));
        assertTrue(auth.canManageChannels(adminPlayer));
        assertTrue(auth.canLinkChests(adminPlayer));
        assertTrue(auth.canUnlinkChests(adminPlayer));

        Player regularPlayer = Mockito.mock(Player.class);
        when(regularPlayer.isOp()).thenReturn(false);
        when(regularPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(regularPlayer.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(regularPlayer.hasPermission("kyouyuu.channel.public.access")).thenReturn(true);
        when(regularPlayer.hasPermission("kyouyuu.channel.public.deposit")).thenReturn(true);
        when(regularPlayer.hasPermission("kyouyuu.channel.public.withdraw")).thenReturn(false);
        when(regularPlayer.hasPermission("kyouyuu.channel.*.withdraw")).thenReturn(false);
        when(regularPlayer.hasPermission(AuthorizationService.PERM_CHANNEL_MANAGE)).thenReturn(false);
        when(regularPlayer.hasPermission(AuthorizationService.PERM_CHEST_LINK)).thenReturn(false);
        when(regularPlayer.hasPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(false);

        assertTrue(auth.canUse(regularPlayer));
        assertTrue(auth.canAccess(regularPlayer, "public"));
        assertTrue(auth.canDeposit(regularPlayer, "public"));
        assertFalse(auth.canWithdraw(regularPlayer, "public"));
        assertFalse(auth.canAccess(regularPlayer, "private_vault"));
        assertFalse(auth.canManageChannels(regularPlayer));
        assertFalse(auth.canLinkChests(regularPlayer));
        assertFalse(auth.canUnlinkChests(regularPlayer));

        Player wildcardPlayer = Mockito.mock(Player.class);
        when(wildcardPlayer.isOp()).thenReturn(false);
        when(wildcardPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(wildcardPlayer.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.custom1.access")).thenReturn(false);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.*.access")).thenReturn(true);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.custom2.deposit")).thenReturn(false);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.*.deposit")).thenReturn(true);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.custom3.withdraw")).thenReturn(false);
        when(wildcardPlayer.hasPermission("kyouyuu.channel.*.withdraw")).thenReturn(true);

        assertTrue(auth.canAccess(wildcardPlayer, "custom1"));
        assertTrue(wildcardPlayer.hasPermission("kyouyuu.channel.*.access"));
        assertTrue(auth.canDeposit(wildcardPlayer, "custom2"));
        assertTrue(auth.canWithdraw(wildcardPlayer, "custom3"));
    }

    @Test
    void testCanManageChannelsLinkUnlinkWithAdminAndNonAdmin() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player adminPlayer = Mockito.mock(Player.class);
        when(adminPlayer.isOp()).thenReturn(false);
        when(adminPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(true);

        assertTrue(auth.canManageChannels(adminPlayer));
        assertTrue(auth.canLinkChests(adminPlayer));
        assertTrue(auth.canUnlinkChests(adminPlayer));

        Player nonAdminPlayer = Mockito.mock(Player.class);
        when(nonAdminPlayer.isOp()).thenReturn(false);
        when(nonAdminPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(nonAdminPlayer.hasPermission(AuthorizationService.PERM_CHANNEL_MANAGE)).thenReturn(true);
        when(nonAdminPlayer.hasPermission(AuthorizationService.PERM_CHEST_LINK)).thenReturn(true);
        when(nonAdminPlayer.hasPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(true);

        assertTrue(auth.canManageChannels(nonAdminPlayer));
        assertTrue(auth.canLinkChests(nonAdminPlayer));
        assertTrue(auth.canUnlinkChests(nonAdminPlayer));

        Player unprivilegedPlayer = Mockito.mock(Player.class);
        when(unprivilegedPlayer.isOp()).thenReturn(false);
        when(unprivilegedPlayer.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(unprivilegedPlayer.hasPermission(AuthorizationService.PERM_CHANNEL_MANAGE)).thenReturn(false);
        when(unprivilegedPlayer.hasPermission(AuthorizationService.PERM_CHEST_LINK)).thenReturn(false);
        when(unprivilegedPlayer.hasPermission(AuthorizationService.PERM_CHEST_UNLINK)).thenReturn(false);

        assertFalse(auth.canManageChannels(unprivilegedPlayer));
        assertFalse(auth.canLinkChests(unprivilegedPlayer));
        assertFalse(auth.canUnlinkChests(unprivilegedPlayer));
    }

    @Test
    void testOpPlayerBypass() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player opPlayer = Mockito.mock(Player.class);
        when(opPlayer.isOp()).thenReturn(true);

        assertTrue(auth.hasAdmin(opPlayer));
        assertTrue(auth.canUse(opPlayer));
        assertTrue(auth.canAccess(opPlayer, "test"));
        assertTrue(auth.canDeposit(opPlayer, "test"));
        assertTrue(auth.canWithdraw(opPlayer, "test"));
        assertTrue(auth.canManageChannels(opPlayer));
        assertTrue(auth.canLinkChests(opPlayer));
        assertTrue(auth.canUnlinkChests(opPlayer));
    }

    @Test
    void testNullPlayerChecks() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        assertFalse(auth.hasAdmin(null));
        assertFalse(auth.canUse(null));
        assertFalse(auth.canAccess(null, "channel"));
        assertFalse(auth.canDeposit(null, "channel"));
        assertFalse(auth.canWithdraw(null, "channel"));
        assertFalse(auth.canManageChannels(null));
        assertFalse(auth.canLinkChests(null));
        assertFalse(auth.canUnlinkChests(null));
    }

    @Test
    void testChannelAccessAndDepositWithoutUsePermission() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player player = Mockito.mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(false);

        assertFalse(auth.canUse(player));
        assertFalse(auth.canAccess(player, "channel"));
        assertFalse(auth.canDeposit(player, "channel"));
        assertFalse(auth.canWithdraw(player, "channel"));
    }

    @Test
    void testWithdrawAllowedByDirectChannelPermission() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player player = Mockito.mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.test.access")).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.test.withdraw")).thenReturn(true);

        assertTrue(auth.canUse(player));
        assertTrue(auth.canAccess(player, "test"));
        assertTrue(auth.canWithdraw(player, "test"));
    }

    @Test
    void testDepositAndWithdrawWithoutAccessPermission() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player player = Mockito.mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.test.access")).thenReturn(false);
        when(player.hasPermission("kyouyuu.channel.*.access")).thenReturn(false);

        assertTrue(auth.canUse(player));
        assertFalse(auth.canAccess(player, "test"));
        assertFalse(auth.canDeposit(player, "test"));
        assertFalse(auth.canWithdraw(player, "test"));
    }

    @Test
    void testDepositDeniedWhenAccessAllowedButDepositDenied() {
        PaperAuthorizationService auth = new PaperAuthorizationService();

        Player player = Mockito.mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_ADMIN)).thenReturn(false);
        when(player.hasPermission(AuthorizationService.PERM_USE)).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.test.access")).thenReturn(true);
        when(player.hasPermission("kyouyuu.channel.test.deposit")).thenReturn(false);
        when(player.hasPermission("kyouyuu.channel.*.deposit")).thenReturn(false);

        assertTrue(auth.canUse(player));
        assertTrue(auth.canAccess(player, "test"));
        assertFalse(auth.canDeposit(player, "test"));
    }
}
