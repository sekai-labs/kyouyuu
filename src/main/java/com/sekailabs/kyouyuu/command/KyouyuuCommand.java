package com.sekailabs.kyouyuu.command;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import com.sekailabs.kyouyuu.service.LinkSessionManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NullMarked
public class KyouyuuCommand implements BasicCommand {

    private final ChannelService channelService;
    private final ChestService chestService;
    private final AuthorizationService authService;
    private final LinkSessionManager sessionManager;
    private final PluginConfig pluginConfig;
    private final Runnable reloadAction;

    public KyouyuuCommand(
            ChannelService channelService,
            ChestService chestService,
            AuthorizationService authService,
            LinkSessionManager sessionManager,
            PluginConfig pluginConfig,
            Runnable reloadAction
    ) {
        this.channelService = channelService;
        this.chestService = chestService;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.pluginConfig = pluginConfig;
        this.reloadAction = reloadAction;
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        CommandSender sender = sourceStack.getSender();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "channel" -> handleChannel(sender, Arrays.copyOfRange(args, 1, args.length));
            case "link" -> handleLink(sender, Arrays.copyOfRange(args, 1, args.length));
            case "unlink" -> handleUnlink(sender);
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(pluginConfig.format("<red>Unknown subcommand. Use <yellow>/kyo help<red> for commands.</red>"));
            }
        }
    }

    private void handleChannel(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(pluginConfig.format("<yellow>Usage: /kyo channel <create|delete|list|info|resize></yellow>"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "create" -> {
                if (sender instanceof Player p && !authService.canManageChannels(p)) {
                    sender.sendMessage(pluginConfig.format("<red>You do not have permission to manage channels.</red>"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(pluginConfig.format("<yellow>Usage: /kyo channel create <name> [size]</yellow>"));
                    return;
                }
                String name = args[1];
                int size = 54;
                if (args.length >= 3) {
                    try {
                        size = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(pluginConfig.format("<red>Size must be a multiple of 9 between 9 and 54.</red>"));
                        return;
                    }
                }
                var res = channelService.createChannel(name, name, size);
                if (res.success()) {
                    sender.sendMessage(pluginConfig.format("<green>" + res.message() + "</green>"));
                } else {
                    sender.sendMessage(pluginConfig.format("<red>" + res.message() + "</red>"));
                }
            }
            case "delete" -> {
                if (sender instanceof Player p && !authService.canManageChannels(p)) {
                    sender.sendMessage(pluginConfig.format("<red>You do not have permission to manage channels.</red>"));
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(pluginConfig.format("<yellow>Usage: /kyo channel delete <name> [--force]</yellow>"));
                    return;
                }
                String name = args[1];
                boolean force = args.length >= 3 && args[2].equalsIgnoreCase("--force");
                var res = channelService.deleteChannel(name, force);
                if (res.success()) {
                    sender.sendMessage(pluginConfig.format("<green>" + res.message() + "</green>"));
                } else {
                    sender.sendMessage(pluginConfig.format("<red>" + res.message() + "</red>"));
                }
            }
            case "resize" -> {
                if (sender instanceof Player p && !authService.canManageChannels(p)) {
                    sender.sendMessage(pluginConfig.format("<red>You do not have permission to manage channels.</red>"));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(pluginConfig.format("<yellow>Usage: /kyo channel resize <name> <new_size> [--force]</yellow>"));
                    return;
                }
                String name = args[1];
                int newSize;
                try {
                    newSize = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(pluginConfig.format("<red>Size must be a multiple of 9 between 9 and 54.</red>"));
                    return;
                }
                boolean force = args.length >= 4 && args[3].equalsIgnoreCase("--force");
                var res = channelService.resizeChannel(name, newSize, force);
                if (res.success()) {
                    sender.sendMessage(pluginConfig.format("<green>" + res.message() + "</green>"));
                } else {
                    sender.sendMessage(pluginConfig.format("<red>" + res.message() + "</red>"));
                }
            }
            case "list" -> {
                List<Channel> channels = channelService.listChannels();
                if (channels.isEmpty()) {
                    sender.sendMessage(pluginConfig.format("<gray>No channels currently exist.</gray>"));
                    return;
                }
                sender.sendMessage(pluginConfig.rawFormat("<gold>--- <yellow>Kyouyuu Channels</yellow> (" + channels.size() + ") ---</gold>"));
                for (Channel ch : channels) {
                    List<LinkedChest> chests = chestService.getChestsForChannel(ch.id());
                    sender.sendMessage(pluginConfig.rawFormat("<aqua>• <bold>" + ch.id() + "</bold></aqua> <gray>(" + ch.name() + ")</gray> - <yellow>" + ch.size() + " slots</yellow>, <green>" + chests.size() + " chests linked</green>"));
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(pluginConfig.format("<yellow>Usage: /kyo channel info <name></yellow>"));
                    return;
                }
                String id = args[1];
                Optional<Channel> opt = channelService.getChannel(id);
                if (opt.isEmpty()) {
                    sender.sendMessage(pluginConfig.format("<red>Channel '" + id + "' not found.</red>"));
                    return;
                }
                Channel ch = opt.get();
                List<LinkedChest> chests = chestService.getChestsForChannel(ch.id());
                sender.sendMessage(pluginConfig.rawFormat("<gold>--- <yellow>Channel: " + ch.id() + "</yellow> ---</gold>"));
                sender.sendMessage(pluginConfig.rawFormat("<gray>Display Name:</gray> <white>" + ch.name() + "</white>"));
                sender.sendMessage(pluginConfig.rawFormat("<gray>Capacity:</gray> <yellow>" + ch.size() + " slots</yellow>"));
                sender.sendMessage(pluginConfig.rawFormat("<gray>Linked Chests:</gray> <green>" + chests.size() + " block(s)</green>"));
            }
            default -> sender.sendMessage(pluginConfig.format("<red>Unknown channel action. Use create, delete, resize, list, or info.</red>"));
        }
    }

    private void handleLink(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players in-game.", NamedTextColor.RED));
            return;
        }

        if (!authService.canLinkChests(player)) {
            player.sendMessage(pluginConfig.format("<red>You do not have permission to link chests.</red>"));
            return;
        }
        String channelId;
        if (args.length < 1 || args[0].trim().isEmpty()) {
            if (channelService.getChannel("global").isPresent()) {
                channelId = "global";
            } else {
                player.sendMessage(pluginConfig.format("<yellow>Usage: /kyo link <channel></yellow>"));
                return;
            }
        } else {
            channelId = args[0].toLowerCase().trim();
            Optional<Channel> chOpt = channelService.getChannel(channelId);
            if (chOpt.isEmpty()) {
                player.sendMessage(pluginConfig.format("<red>Channel '" + channelId + "' does not exist. Create it first with /kyo channel create " + channelId + ".</red>"));
                return;
            }
        }

        sessionManager.startLinkSession(player.getUniqueId(), channelId);
        player.sendMessage(pluginConfig.format("<green>Linking mode activated for channel <yellow>" + channelId + "</yellow>! <white>Right-click any chest/barrel to link it (30s timeout).</white></green>"));
    }

    private void handleUnlink(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players in-game.", NamedTextColor.RED));
            return;
        }

        if (!authService.canUnlinkChests(player)) {
            player.sendMessage(pluginConfig.format("<red>You do not have permission to unlink chests.</red>"));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target != null && chestService.isLinkableContainer(target) && chestService.getLinkedChest(target).isPresent()) {
            var res = chestService.unlinkChest(target);
            if (res.success()) {
                player.sendMessage(pluginConfig.format("<green>" + res.message() + "</green>"));
            } else {
                player.sendMessage(pluginConfig.format("<red>" + res.message() + "</red>"));
            }
            return;
        }

        sessionManager.startUnlinkSession(player.getUniqueId());
        player.sendMessage(pluginConfig.format("<green>Unlinking mode activated! <white>Right-click any linked chest/barrel to unlink it (30s timeout).</white></green>"));
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players in-game.", NamedTextColor.RED));
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || !chestService.isLinkableContainer(target)) {
            player.sendMessage(pluginConfig.format("<red>You are not looking at a valid container within 5 blocks.</red>"));
            return;
        }

        Optional<LinkedChest> linkedOpt = chestService.getLinkedChest(target);
        if (linkedOpt.isEmpty()) {
            player.sendMessage(pluginConfig.format("<gray>Target chest is not linked to any Kyouyuu channel.</gray>"));
            return;
        }

        LinkedChest lc = linkedOpt.get();
        player.sendMessage(pluginConfig.rawFormat("<gold>--- <yellow>Chest Link Info</yellow> ---</gold>"));
        player.sendMessage(pluginConfig.rawFormat("<gray>Channel:</gray> <aqua><bold>" + lc.channelId() + "</bold></aqua>"));
        player.sendMessage(pluginConfig.rawFormat("<gray>Location:</gray> <white>" + lc.toLocation() + "</white>"));
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player p && !authService.hasAdmin(p)) {
            sender.sendMessage(pluginConfig.format("<red>You do not have permission to reload Kyouyuu.</red>"));
            return;
        }

        if (reloadAction != null) {
            reloadAction.run();
        }
        sender.sendMessage(pluginConfig.format("<green>Configuration reloaded successfully.</green>"));
    }

    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player player) {
            try {
                net.kyori.adventure.inventory.Book advBook = net.kyori.adventure.inventory.Book.book(
                        Component.text("Kyouyuu Guide", NamedTextColor.AQUA),
                        Component.text("Kyouyuu", NamedTextColor.DARK_AQUA),
                        Component.text("=== Kyouyuu Guide ===\n\n", NamedTextColor.AQUA)
                                .append(Component.text("Kyouyuu links physical chests across the server to shared inventory channels.\n\n", NamedTextColor.BLACK))
                                .append(Component.text("Quick Steps:\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("1. Create channel:\n/kyo channel create <name> [size]\n\n", NamedTextColor.BLACK))
                                .append(Component.text("2. Link chest:\n/kyo link <channel>\nRight-click any chest.", NamedTextColor.BLACK)),
                        Component.text("=== Commands ===\n\n", NamedTextColor.AQUA)
                                .append(Component.text("/kyo link <ch>\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("Link clicked chest\n\n", NamedTextColor.BLACK))
                                .append(Component.text("/kyo unlink\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("Unlink chest\n\n", NamedTextColor.BLACK))
                                .append(Component.text("/kyo info\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("Inspect chest\n\n", NamedTextColor.BLACK))
                                .append(Component.text("/kyo reload\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("Reload plugin config", NamedTextColor.BLACK)),
                        Component.text("=== Channels ===\n\n", NamedTextColor.AQUA)
                                .append(Component.text("/kyo channel create <name> [size]\n\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("/kyo channel delete <name> [--force]\n\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("/kyo channel resize <name> <size>\n\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("/kyo channel list\n\n", NamedTextColor.DARK_BLUE))
                                .append(Component.text("/kyo channel info <name>", NamedTextColor.DARK_BLUE))
                );
                player.openBook(advBook);
                try {
                    org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK);
                    if (book.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta meta) {
                        meta.title(Component.text("Kyouyuu Guide", NamedTextColor.AQUA));
                        meta.author(Component.text("Kyouyuu", NamedTextColor.DARK_AQUA));
                        meta.pages(advBook.pages());
                        book.setItemMeta(meta);
                    }
                    var leftover = player.getInventory().addItem(book);
                    if (!leftover.isEmpty() && player.getWorld() != null) {
                        player.getWorld().dropItemNaturally(player.getLocation(), book);
                    }
                } catch (Throwable ignored) {
                }
                player.sendMessage(pluginConfig.format("<green>You received the Kyouyuu guide book.</green>"));
                return;
            } catch (Throwable ignored) {
            }
        }

        sender.sendMessage(pluginConfig.rawFormat("<aqua><bold>=== Kyouyuu Commands ===</bold></aqua>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo link <channel></yellow> <gray>- Enter link mode for a channel</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo unlink</yellow> <gray>- Unlink targeted or clicked chest</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo info</yellow> <gray>- Inspect link status of targeted chest</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo channel create <name> [size]</yellow> <gray>- Create channel</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo channel delete <name> [--force]</yellow> <gray>- Delete channel</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo channel resize <name> <size> [--force]</yellow> <gray>- Resize channel</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo channel list</yellow> <gray>- List all channels</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo channel info <name></yellow> <gray>- Inspect channel details</gray>"));
        sender.sendMessage(pluginConfig.rawFormat("<yellow>/kyo reload</yellow> <gray>- Reload configuration</gray>"));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack sourceStack, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 0) {
            return List.of("channel", "link", "unlink", "info", "reload", "help");
        }
        if (args.length == 1) {
            List<String> subs = List.of("channel", "link", "unlink", "info", "reload", "help");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
            return completions;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("channel")) {
            if (args.length == 2) {
                List<String> actions = List.of("create", "delete", "resize", "list", "info");
                for (String a : actions) {
                    if (a.startsWith(args[1].toLowerCase())) completions.add(a);
                }
                return completions;
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("resize") || args[1].equalsIgnoreCase("info"))) {
                for (Channel ch : channelService.listChannels()) {
                    if (ch.id().startsWith(args[2].toLowerCase())) completions.add(ch.id());
                }
                return completions;
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("create")) {
                if (args[2].isEmpty()) {
                    return List.of("<name>");
                }
                List<String> sizes = List.of("9", "18", "27", "36", "45", "54");
                return sizes;
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
                List<String> sizes = List.of("9", "18", "27", "36", "45", "54");
                List<String> matches = new ArrayList<>();
                for (String s : sizes) {
                    if (s.startsWith(args[3].toLowerCase())) matches.add(s);
                }
                return matches;
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("resize")) {
                List<String> sizes = List.of("9", "18", "27", "36", "45", "54");
                List<String> matches = new ArrayList<>();
                for (String s : sizes) {
                    if (s.startsWith(args[3].toLowerCase())) matches.add(s);
                }
                return matches;
            }
        } else if (sub.equals("link") && args.length == 2) {
            for (Channel ch : channelService.listChannels()) {
                if (ch.id().startsWith(args[1].toLowerCase())) completions.add(ch.id());
            }
            return completions;
        }

        return completions;
    }
}
