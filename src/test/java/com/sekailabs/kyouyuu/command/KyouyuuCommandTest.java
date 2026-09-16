package com.sekailabs.kyouyuu.command;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import com.sekailabs.kyouyuu.service.LinkSessionManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KyouyuuCommandTest {

    private ChannelService channelService;
    private ChestService chestService;
    private AuthorizationService authService;
    private LinkSessionManager sessionManager;
    private PluginConfig pluginConfig;
    private AtomicBoolean reloaded;
    private Runnable reloadAction;
    private KyouyuuCommand command;

    private CommandSourceStack sourceStack;
    private CommandSender consoleSender;
    private Player player;

    @BeforeEach
    void setUp() {
        channelService = mock(ChannelService.class);
        chestService = mock(ChestService.class);
        authService = mock(AuthorizationService.class);
        sessionManager = mock(LinkSessionManager.class);

        pluginConfig = new PluginConfig();
        pluginConfig.load(YamlConfiguration.loadConfiguration(new StringReader("messages:\n  prefix: ''\n")));

        reloaded = new AtomicBoolean(false);
        reloadAction = () -> reloaded.set(true);

        command = new KyouyuuCommand(
                channelService,
                chestService,
                authService,
                sessionManager,
                pluginConfig,
                reloadAction
        );

        sourceStack = mock(CommandSourceStack.class);
        consoleSender = mock(CommandSender.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void testExecuteZeroArgsSendsHelp() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[0]);
        verify(consoleSender, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteHelpSubcommandSendsHelp() {
        when(sourceStack.getSender()).thenReturn(player);
        command.execute(sourceStack, new String[]{"help"});
        verify(player, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteHelpCaseInsensitive() {
        when(sourceStack.getSender()).thenReturn(player);
        command.execute(sourceStack, new String[]{"HeLP"});
        verify(player, atLeastOnce()).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteHelpPlayerOpensBook() {
        when(sourceStack.getSender()).thenReturn(player);
        org.bukkit.inventory.PlayerInventory inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());

        command.execute(sourceStack, new String[]{"help"});
        verify(player).openBook(any(net.kyori.adventure.inventory.Book.class));
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testSuggestChannelCreateNameHint() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "create", ""});
        assertEquals(List.of("<name>"), result);
    }

    @Test
    void testSuggestChannelCreateSlotSizesArg4() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "create", "test", ""});
        assertEquals(List.of("9", "18", "27", "36", "45", "54"), result);

        Collection<String> resultFiltered = command.suggest(sourceStack, new String[]{"channel", "create", "test", "5"});
        assertEquals(List.of("54"), resultFiltered);
    }

    @Test
    void testExecuteUnknownSubcommand() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"foobar"});
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelNoArgsUsage() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel"});
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelUnknownAction() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "invalid"});
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelCreateNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"channel", "create", "test"});

        verify(player).sendMessage(any(Component.class));
        verify(channelService, never()).createChannel(any(), any(), anyInt());
    }

    @Test
    void testExecuteChannelCreateMissingArg() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "create"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).createChannel(any(), any(), anyInt());
    }

    @Test
    void testExecuteChannelCreateInvalidSizeNumberFormat() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "create", "test", "abc"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).createChannel(any(), any(), anyInt());
    }

    @Test
    void testExecuteChannelCreateDefaultSizeSuccess() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.createChannel("test", "test", 54))
                .thenReturn(ChannelService.ServiceResult.ok(null, "Channel created"));

        command.execute(sourceStack, new String[]{"channel", "create", "test"});

        verify(channelService).createChannel("test", "test", 54);
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelCreateCustomSizeFailure() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(true);
        when(channelService.createChannel("test", "test", 27))
                .thenReturn(ChannelService.ServiceResult.fail("Channel exists"));

        command.execute(sourceStack, new String[]{"channel", "create", "test", "27"});

        verify(channelService).createChannel("test", "test", 27);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelDeleteNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"channel", "delete", "test"});

        verify(player).sendMessage(any(Component.class));
        verify(channelService, never()).deleteChannel(any(), anyBoolean());
    }

    @Test
    void testExecuteChannelDeleteMissingArg() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "delete"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).deleteChannel(any(), anyBoolean());
    }

    @Test
    void testExecuteChannelDeleteWithoutForceSuccess() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.deleteChannel("test", false))
                .thenReturn(ChannelService.ServiceResult.ok(true, "Deleted successfully"));

        command.execute(sourceStack, new String[]{"channel", "delete", "test"});

        verify(channelService).deleteChannel("test", false);
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelDeleteArg3NotForce() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.deleteChannel("test", false))
                .thenReturn(ChannelService.ServiceResult.ok(true, "Deleted"));

        command.execute(sourceStack, new String[]{"channel", "delete", "test", "--somethingElse"});

        verify(channelService).deleteChannel("test", false);
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelDeleteWithForceFailure() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(true);
        when(channelService.deleteChannel("test", true))
                .thenReturn(ChannelService.ServiceResult.fail("Channel not found"));

        command.execute(sourceStack, new String[]{"channel", "delete", "test", "--force"});

        verify(channelService).deleteChannel("test", true);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelResizeNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"channel", "resize", "test", "27"});

        verify(player).sendMessage(any(Component.class));
        verify(channelService, never()).resizeChannel(any(), anyInt(), anyBoolean());
    }

    @Test
    void testExecuteChannelResizeMissingArg() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "resize", "test"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).resizeChannel(any(), anyInt(), anyBoolean());
    }

    @Test
    void testExecuteChannelResizeInvalidNumberFormat() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "resize", "test", "invalid"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).resizeChannel(any(), anyInt(), anyBoolean());
    }

    @Test
    void testExecuteChannelResizeWithoutForceSuccess() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.resizeChannel("test", 27, false))
                .thenReturn(ChannelService.ServiceResult.ok(null, "Resized"));

        command.execute(sourceStack, new String[]{"channel", "resize", "test", "27"});

        verify(channelService).resizeChannel("test", 27, false);
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelResizeArg4NotForce() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.resizeChannel("test", 27, false))
                .thenReturn(ChannelService.ServiceResult.ok(null, "Resized"));

        command.execute(sourceStack, new String[]{"channel", "resize", "test", "27", "--other"});

        verify(channelService).resizeChannel("test", 27, false);
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelResizeWithForceFailure() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canManageChannels(player)).thenReturn(true);
        when(channelService.resizeChannel("test", 18, true))
                .thenReturn(ChannelService.ServiceResult.fail("Cannot shrink channel"));

        command.execute(sourceStack, new String[]{"channel", "resize", "test", "18", "--force"});

        verify(channelService).resizeChannel("test", 18, true);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelListEmpty() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.listChannels()).thenReturn(List.of());

        command.execute(sourceStack, new String[]{"channel", "list"});

        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelListNonEmpty() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        Channel c1 = Channel.create("ch1", "Channel One", 27);
        Channel c2 = Channel.create("ch2", "Channel Two", 54);
        when(channelService.listChannels()).thenReturn(List.of(c1, c2));

        LinkedChest chest1 = new LinkedChest("ch1", "world", UUID.randomUUID(), 0, 64, 0);
        when(chestService.getChestsForChannel("ch1")).thenReturn(List.of(chest1));
        when(chestService.getChestsForChannel("ch2")).thenReturn(List.of());

        command.execute(sourceStack, new String[]{"channel", "list"});

        verify(consoleSender, atLeast(3)).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelInfoMissingArg() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"channel", "info"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(channelService, never()).getChannel(any());
    }

    @Test
    void testExecuteChannelInfoNotFound() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        when(channelService.getChannel("nonexistent")).thenReturn(Optional.empty());

        command.execute(sourceStack, new String[]{"channel", "info", "nonexistent"});

        verify(channelService).getChannel("nonexistent");
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteChannelInfoFound() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        Channel ch = Channel.create("vault", "Main Vault", 54);
        when(channelService.getChannel("vault")).thenReturn(Optional.of(ch));
        when(chestService.getChestsForChannel("vault")).thenReturn(List.of(
                new LinkedChest("vault", "world", UUID.randomUUID(), 10, 64, 10)
        ));

        command.execute(sourceStack, new String[]{"channel", "info", "vault"});

        verify(channelService).getChannel("vault");
        verify(chestService).getChestsForChannel("vault");
        verify(consoleSender, atLeast(4)).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteLinkConsoleRejection() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"link", "mychannel"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(sessionManager, never()).startLinkSession(any(), any());
    }

    @Test
    void testExecuteLinkNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canLinkChests(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"link", "mychannel"});

        verify(player).sendMessage(any(Component.class));
        verify(sessionManager, never()).startLinkSession(any(), any());
    }

    @Test
    void testExecuteLinkNoArgsDefaultsToGlobalWhenPresent() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canLinkChests(player)).thenReturn(true);
        Channel globalChannel = Channel.create("global", "Global", 54);
        when(channelService.getChannel("global")).thenReturn(Optional.of(globalChannel));

        command.execute(sourceStack, new String[]{"link"});

        verify(sessionManager).startLinkSession(player.getUniqueId(), "global");
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteLinkNoArgsFailsWhenGlobalChannelDeleted() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canLinkChests(player)).thenReturn(true);
        when(channelService.getChannel("global")).thenReturn(Optional.empty());

        command.execute(sourceStack, new String[]{"link"});

        verify(sessionManager, never()).startLinkSession(any(), any());
        verify(player).sendMessage(any(Component.class));
    }


    @Test
    void testExecuteLinkNonExistentChannel() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canLinkChests(player)).thenReturn(true);
        when(channelService.getChannel("missing")).thenReturn(Optional.empty());

        command.execute(sourceStack, new String[]{"link", "missing"});

        verify(player).sendMessage(any(Component.class));
        verify(sessionManager, never()).startLinkSession(any(), any());
    }

    @Test
    void testExecuteLinkSuccess() {
        when(sourceStack.getSender()).thenReturn(player);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(authService.canLinkChests(player)).thenReturn(true);
        Channel ch = Channel.create("shared", "Shared", 27);
        when(channelService.getChannel("shared")).thenReturn(Optional.of(ch));

        command.execute(sourceStack, new String[]{"link", "shared"});

        verify(sessionManager).startLinkSession(uuid, "shared");
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteUnlinkConsoleRejection() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"unlink"});
        verify(consoleSender).sendMessage(any(Component.class));
        verify(chestService, never()).unlinkChest(any());
        verify(sessionManager, never()).startUnlinkSession(any());
    }

    @Test
    void testExecuteUnlinkNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canUnlinkChests(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"unlink"});

        verify(player).sendMessage(any(Component.class));
        verify(chestService, never()).unlinkChest(any());
        verify(sessionManager, never()).startUnlinkSession(any());
    }

    @Test
    void testExecuteUnlinkDirectLookingAtLinkedChestSuccess() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canUnlinkChests(player)).thenReturn(true);

        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(true);
        when(chestService.getLinkedChest(targetBlock))
                .thenReturn(Optional.of(new LinkedChest("ch", "world", UUID.randomUUID(), 0, 64, 0)));
        when(chestService.unlinkChest(targetBlock))
                .thenReturn(ChannelService.ServiceResult.ok(1, "Unlinked successfully"));

        command.execute(sourceStack, new String[]{"unlink"});

        verify(chestService).unlinkChest(targetBlock);
        verify(player).sendMessage(any(Component.class));
        verify(sessionManager, never()).startUnlinkSession(any());
    }

    @Test
    void testExecuteUnlinkDirectLookingAtLinkedChestFailure() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.canUnlinkChests(player)).thenReturn(true);

        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(true);
        when(chestService.getLinkedChest(targetBlock))
                .thenReturn(Optional.of(new LinkedChest("ch", "world", UUID.randomUUID(), 0, 64, 0)));
        when(chestService.unlinkChest(targetBlock))
                .thenReturn(ChannelService.ServiceResult.fail("Database error"));

        command.execute(sourceStack, new String[]{"unlink"});

        verify(chestService).unlinkChest(targetBlock);
        verify(player).sendMessage(any(Component.class));
        verify(sessionManager, never()).startUnlinkSession(any());
    }

    @Test
    void testExecuteUnlinkTargetNullStartsSession() {
        when(sourceStack.getSender()).thenReturn(player);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(authService.canUnlinkChests(player)).thenReturn(true);
        when(player.getTargetBlockExact(5)).thenReturn(null);

        command.execute(sourceStack, new String[]{"unlink"});

        verify(sessionManager).startUnlinkSession(uuid);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteUnlinkTargetNotLinkableStartsSession() {
        when(sourceStack.getSender()).thenReturn(player);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(authService.canUnlinkChests(player)).thenReturn(true);

        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(false);

        command.execute(sourceStack, new String[]{"unlink"});

        verify(sessionManager).startUnlinkSession(uuid);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteUnlinkTargetNotLinkedStartsSession() {
        when(sourceStack.getSender()).thenReturn(player);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(authService.canUnlinkChests(player)).thenReturn(true);

        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(true);
        when(chestService.getLinkedChest(targetBlock)).thenReturn(Optional.empty());

        command.execute(sourceStack, new String[]{"unlink"});

        verify(sessionManager).startUnlinkSession(uuid);
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteInfoConsoleRejection() {
        when(sourceStack.getSender()).thenReturn(consoleSender);
        command.execute(sourceStack, new String[]{"info"});
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteInfoTargetNull() {
        when(sourceStack.getSender()).thenReturn(player);
        when(player.getTargetBlockExact(5)).thenReturn(null);

        command.execute(sourceStack, new String[]{"info"});

        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteInfoTargetNotLinkable() {
        when(sourceStack.getSender()).thenReturn(player);
        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(false);

        command.execute(sourceStack, new String[]{"info"});

        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteInfoTargetNotLinked() {
        when(sourceStack.getSender()).thenReturn(player);
        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(true);
        when(chestService.getLinkedChest(targetBlock)).thenReturn(Optional.empty());

        command.execute(sourceStack, new String[]{"info"});

        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteInfoTargetLinked() {
        when(sourceStack.getSender()).thenReturn(player);
        Block targetBlock = mock(Block.class);
        when(player.getTargetBlockExact(5)).thenReturn(targetBlock);
        when(chestService.isLinkableContainer(targetBlock)).thenReturn(true);
        LinkedChest linked = new LinkedChest("main", "world", UUID.randomUUID(), 1, 2, 3);
        when(chestService.getLinkedChest(targetBlock)).thenReturn(Optional.of(linked));

        command.execute(sourceStack, new String[]{"info"});

        verify(player, times(3)).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteReloadPlayerNoPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.hasAdmin(player)).thenReturn(false);

        command.execute(sourceStack, new String[]{"reload"});

        verify(player).sendMessage(any(Component.class));
        assertFalse(reloaded.get());
    }

    @Test
    void testExecuteReloadPlayerWithPermission() {
        when(sourceStack.getSender()).thenReturn(player);
        when(authService.hasAdmin(player)).thenReturn(true);

        command.execute(sourceStack, new String[]{"reload"});

        assertTrue(reloaded.get());
        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteReloadConsoleWithAction() {
        when(sourceStack.getSender()).thenReturn(consoleSender);

        command.execute(sourceStack, new String[]{"reload"});

        assertTrue(reloaded.get());
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testExecuteReloadWithNullAction() {
        KyouyuuCommand cmdWithoutAction = new KyouyuuCommand(
                channelService,
                chestService,
                authService,
                sessionManager,
                pluginConfig,
                null
        );
        when(sourceStack.getSender()).thenReturn(consoleSender);

        assertDoesNotThrow(() -> cmdWithoutAction.execute(sourceStack, new String[]{"reload"}));
        verify(consoleSender).sendMessage(any(Component.class));
    }

    @Test
    void testSuggestZeroArgs() {
        Collection<String> result = command.suggest(sourceStack, new String[0]);
        assertTrue(result.containsAll(List.of("channel", "link", "unlink", "info", "reload", "help")));
        assertEquals(6, result.size());
    }

    @Test
    void testSuggestOneArgPrefixMatching() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"c"});
        assertEquals(List.of("channel"), result);

        Collection<String> resultLin = command.suggest(sourceStack, new String[]{"lin"});
        assertEquals(List.of("link"), resultLin);
    }

    @Test
    void testSuggestOneArgNonMatching() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"xyz"});
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestChannelSubcommands() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", ""});
        assertTrue(result.containsAll(List.of("create", "delete", "resize", "list", "info")));

        Collection<String> resultFiltered = command.suggest(sourceStack, new String[]{"channel", "de"});
        assertEquals(List.of("delete"), resultFiltered);
    }

    @Test
    void testSuggestChannelDeleteChannelNames() {
        Channel c1 = Channel.create("alpha", "Alpha", 18);
        Channel c2 = Channel.create("beta", "Beta", 27);
        when(channelService.listChannels()).thenReturn(List.of(c1, c2));

        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "delete", "al"});
        assertEquals(List.of("alpha"), result);
    }

    @Test
    void testSuggestChannelResizeChannelNames() {
        Channel c1 = Channel.create("gamma", "Gamma", 36);
        when(channelService.listChannels()).thenReturn(List.of(c1));

        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "resize", ""});
        assertEquals(List.of("gamma"), result);
    }

    @Test
    void testSuggestChannelInfoChannelNames() {
        Channel c1 = Channel.create("delta", "Delta", 45);
        when(channelService.listChannels()).thenReturn(List.of(c1));

        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "info", "del"});
        assertEquals(List.of("delta"), result);
    }

    @Test
    void testSuggestChannelCreateSlotSizes() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "create", "test"});
        assertEquals(List.of("9", "18", "27", "36", "45", "54"), result);
    }

    @Test
    void testSuggestChannelResizeSlotSizes() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "resize", "test", ""});
        assertEquals(List.of("9", "18", "27", "36", "45", "54"), result);
    }

    @Test
    void testSuggestChannelLength4NotResize() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "delete", "test", "extra"});
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestChannelLength3NotCreateOrMatchingActions() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"channel", "list", "extra"});
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestLinkChannelNames() {
        Channel c1 = Channel.create("shared1", "Shared One", 9);
        Channel c2 = Channel.create("other", "Other", 9);
        when(channelService.listChannels()).thenReturn(List.of(c1, c2));

        Collection<String> result = command.suggest(sourceStack, new String[]{"link", "sha"});
        assertEquals(List.of("shared1"), result);
    }

    @Test
    void testSuggestLinkLength3() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"link", "test", "extra"});
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestDefaultUnmatched() {
        Collection<String> result = command.suggest(sourceStack, new String[]{"info", "something", "extra"});
        assertTrue(result.isEmpty());

        Collection<String> result2 = command.suggest(sourceStack, new String[]{"channel", "list", "extra"});
        assertTrue(result2.isEmpty());
    }
}
