package com.sekailabs.kyouyuu;

import com.sekailabs.kyouyuu.auth.AuthorizationService;
import com.sekailabs.kyouyuu.auth.LuckPermsAuthorizationService;
import com.sekailabs.kyouyuu.auth.PaperAuthorizationService;
import com.sekailabs.kyouyuu.command.KyouyuuCommand;
import com.sekailabs.kyouyuu.config.PluginConfig;
import com.sekailabs.kyouyuu.inventory.ItemSerializer;
import com.sekailabs.kyouyuu.inventory.SharedInventoryManager;
import com.sekailabs.kyouyuu.listener.BlockProtectionListener;
import com.sekailabs.kyouyuu.listener.ChestInteractListener;
import com.sekailabs.kyouyuu.listener.ChestInventoryListener;
import com.sekailabs.kyouyuu.listener.HopperAutomationListener;
import com.sekailabs.kyouyuu.service.ChannelService;
import com.sekailabs.kyouyuu.service.ChestService;
import com.sekailabs.kyouyuu.service.LinkSessionManager;
import com.sekailabs.kyouyuu.storage.ChannelRepository;
import com.sekailabs.kyouyuu.storage.ChestRepository;
import com.sekailabs.kyouyuu.storage.DatabaseManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class KyouyuuPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private ChannelRepository channelRepository;
    private ChestRepository chestRepository;
    private ItemSerializer itemSerializer;
    private SharedInventoryManager inventoryManager;
    private ChannelService channelService;
    private ChestService chestService;
    private AuthorizationService authorizationService;
    private LinkSessionManager linkSessionManager;

    private BukkitTask autosaveTask;
    private BukkitTask sessionPruneTask;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        getLogger().info("Initializing Kyouyuu (共有)...");

        saveDefaultConfig();
        pluginConfig = new PluginConfig();
        pluginConfig.load(getConfig());

        try {
            databaseManager = new DatabaseManager(getDataFolder(), getLogger(), pluginConfig.getDatabaseCredentials());
            databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not initialize database (" + pluginConfig.getDatabaseCredentials().type() + "). Disabling Kyouyuu.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        channelRepository = new ChannelRepository(databaseManager);
        chestRepository = new ChestRepository(databaseManager);

        itemSerializer = new ItemSerializer();
        inventoryManager = new SharedInventoryManager(channelRepository, itemSerializer, getLogger(), getServer());
        channelService = new ChannelService(channelRepository, chestRepository, inventoryManager, itemSerializer);
        chestService = new ChestService(chestRepository, getLogger());
        linkSessionManager = new LinkSessionManager(Duration.ofSeconds(pluginConfig.getLinkSessionTimeoutSeconds()));

        PluginManager pm = getServer().getPluginManager();
        if (pm.isPluginEnabled("LuckPerms")) {
            getLogger().info("LuckPerms detected! Enabling context-aware permissions.");
            authorizationService = new LuckPermsAuthorizationService(getLogger());
        } else {
            getLogger().info("Using native Paper permission provider.");
            authorizationService = new PaperAuthorizationService();
        }

        pm.registerEvents(new ChestInteractListener(chestService, channelService, inventoryManager, authorizationService, linkSessionManager), this);
        pm.registerEvents(new ChestInventoryListener(inventoryManager, authorizationService), this);
        pm.registerEvents(new BlockProtectionListener(chestService, authorizationService), this);
        pm.registerEvents(new HopperAutomationListener(chestService, channelService, inventoryManager, pluginConfig), this);

        KyouyuuCommand kyouyuuCommand = new KyouyuuCommand(
                channelService,
                chestService,
                authorizationService,
                linkSessionManager,
                pluginConfig,
                this::reloadPlugin
        );

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("kyouyuu", List.of("kyo"), kyouyuuCommand);
        });

        long intervalTicks = Math.max(20L, pluginConfig.getAutosaveIntervalSeconds() * 20L);
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                int count = inventoryManager.flushAllDirty();
                if (count > 0) {
                    getLogger().fine("Periodic autosave flushed " + count + " modified channel(s).");
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error during periodic inventory autosave", e);
            }
        }, intervalTicks, intervalTicks);

        sessionPruneTask = Bukkit.getScheduler().runTaskTimer(this, linkSessionManager::pruneExpiredSessions, 100L, 100L);

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("Kyouyuu successfully enabled in " + elapsed + "ms!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Kyouyuu (共有)...");

        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (sessionPruneTask != null) {
            sessionPruneTask.cancel();
        }

        if (inventoryManager != null) {
            for (Inventory inv : inventoryManager.getAllLoadedInventories()) {
                for (HumanEntity viewer : new ArrayList<>(inv.getViewers())) {
                    viewer.closeInventory();
                }
            }

            int flushed = inventoryManager.flushAllDirty();
            getLogger().info("Synchronously saved " + flushed + " dirty inventory channel(s) on shutdown.");
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("Kyouyuu successfully disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.load(getConfig());
        getLogger().info("Configuration reloaded.");
    }

    public ChannelService getChannelService() {
        return channelService;
    }

    public ChestService getChestService() {
        return chestService;
    }

    public SharedInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }
}
