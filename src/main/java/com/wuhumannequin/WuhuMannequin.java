package com.wuhumannequin;

import com.wuhumannequin.command.MannequinCommand;
import com.wuhumannequin.skin.SkinApiClient;
import com.wuhumannequin.skin.SkinCache;
import com.wuhumannequin.skin.SkinChangeDetector;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class WuhuMannequin extends JavaPlugin {

    private static WuhuMannequin instance;
    private MannequinCommand mannequinCommand;
    private SkinApiClient skinApiClient;
    private SkinCache skinCache;
    private SkinChangeDetector skinChangeDetector;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Skin API integration
        String apiUrl = getConfig().getString("api.url", "http://localhost:3001");
        String privateKey = getConfig().getString("api.private-key", "");
        int pollInterval = getConfig().getInt("poll-interval", 5);
        boolean preloadOnJoin = getConfig().getBoolean("preload-on-join", true);

        skinCache = new SkinCache();
        skinApiClient = new SkinApiClient(apiUrl, privateKey, getLogger());

        if (skinApiClient.isConfigured()) {
            getLogger().info("MannequinAPI configured at " + apiUrl);

            if (preloadOnJoin) {
                skinChangeDetector = new SkinChangeDetector(this, skinApiClient, skinCache, pollInterval);
                getServer().getPluginManager().registerEvents(skinChangeDetector, this);
                getLogger().info("Skin preloading on player join enabled.");
            }
        } else {
            getLogger().info("No API private key configured — using colored block fallback.");
        }

        // Debug commands
        mannequinCommand = new MannequinCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("mannequin", "WuhuMannequin debug commands", mannequinCommand);
        });

        getLogger().info("WuhuMannequin enabled.");
    }

    @Override
    public void onDisable() {
        if (mannequinCommand != null) mannequinCommand.cleanupAll();
        if (skinChangeDetector != null) skinChangeDetector.shutdown();
        getLogger().info("WuhuMannequin disabled.");
    }

    public static WuhuMannequin getInstance() {
        return instance;
    }

    public SkinApiClient getSkinApiClient() {
        return skinApiClient;
    }

    public SkinCache getSkinCache() {
        return skinCache;
    }

    public SkinChangeDetector getSkinChangeDetector() {
        return skinChangeDetector;
    }

    /**
     * Reinitialize the API client and skin system from the current config.
     * Called by the reload command.
     */
    public void reinitialize() {
        if (skinChangeDetector != null) {
            skinChangeDetector.shutdown();
            org.bukkit.event.HandlerList.unregisterAll(skinChangeDetector);
            skinChangeDetector = null;
        }

        String apiUrl = getConfig().getString("api.url", "http://localhost:3001");
        String privateKey = getConfig().getString("api.private-key", "");
        int pollInterval = getConfig().getInt("poll-interval", 5);
        boolean preloadOnJoin = getConfig().getBoolean("preload-on-join", true);

        skinCache = new SkinCache();
        skinApiClient = new SkinApiClient(apiUrl, privateKey, getLogger());

        if (skinApiClient.isConfigured()) {
            getLogger().info("MannequinAPI configured at " + apiUrl);
            if (preloadOnJoin) {
                skinChangeDetector = new SkinChangeDetector(this, skinApiClient, skinCache, pollInterval);
                getServer().getPluginManager().registerEvents(skinChangeDetector, this);
                getLogger().info("Skin preloading on player join enabled.");
            }
        } else {
            getLogger().info("No API private key configured — using colored block fallback.");
        }
    }
}
