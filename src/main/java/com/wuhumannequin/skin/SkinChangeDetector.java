package com.wuhumannequin.skin;

import com.wuhumannequin.WuhuMannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * On player join, triggers skin generation via the MannequinAPI if the
 * player's skins aren't already cached. Polls until ready.
 */
public class SkinChangeDetector implements Listener {

    private final WuhuMannequin plugin;
    private final SkinApiClient apiClient;
    private final SkinCache cache;
    private final int pollIntervalTicks;
    private final Set<UUID> polling = new HashSet<>();

    public SkinChangeDetector(WuhuMannequin plugin, SkinApiClient apiClient,
                              SkinCache cache, int pollIntervalSeconds) {
        this.plugin = plugin;
        this.apiClient = apiClient;
        this.cache = cache;
        this.pollIntervalTicks = pollIntervalSeconds * 20;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!apiClient.isConfigured()) return;

        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getLogger().info("Player joined: " + uuid + " (cached=" + cache.has(uuid) + ", polling=" + polling.contains(uuid) + ")");

        // Already cached or already polling
        if (cache.has(uuid) || polling.contains(uuid)) return;

        // Fire initial request (auto-queues generation on the backend if needed)
        plugin.getLogger().info("Requesting skins for " + uuid);
        requestAndPoll(uuid);
    }

    /**
     * Manually trigger skin loading for a player (e.g. from a command).
     */
    public void requestSkins(UUID uuid) {
        if (!apiClient.isConfigured() || polling.contains(uuid)) return;
        cache.remove(uuid);
        requestAndPoll(uuid);
    }

    private void requestAndPoll(UUID uuid) {
        polling.add(uuid);

        apiClient.getSkins(uuid).thenAccept(result -> {
            switch (result.status()) {
                case READY -> {
                    cache.put(uuid, result.textures(), result.model());
                    polling.remove(uuid);
                    plugin.getLogger().info("Loaded skin textures for " + uuid + " (model=" + result.model() + ")");
                }
                case GENERATING -> startPolling(uuid);
                case ERROR -> {
                    plugin.getLogger().warning("Skin fetch failed for " + uuid + ": " + result.errorMessage());
                    polling.remove(uuid);
                }
            }
        });
    }

    private void startPolling(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!polling.contains(uuid)) {
                    cancel();
                    return;
                }

                apiClient.getSkins(uuid).thenAccept(result -> {
                    if (result.status() == SkinFetchResult.Status.READY) {
                        cache.put(uuid, result.textures(), result.model());
                        polling.remove(uuid);
                        plugin.getLogger().info("Loaded skin textures for " + uuid + " (model=" + result.model() + ")");
                        // Cancel from main thread
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                SkinChangeDetector.this.cancelPolling(uuid);
                            }
                        }.runTask(plugin);
                    } else if (result.status() == SkinFetchResult.Status.ERROR) {
                        plugin.getLogger().warning("Skin poll failed for " + uuid + ": " + result.errorMessage());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                SkinChangeDetector.this.cancelPolling(uuid);
                            }
                        }.runTask(plugin);
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, pollIntervalTicks, pollIntervalTicks);
    }

    private void cancelPolling(UUID uuid) {
        polling.remove(uuid);
    }

    public void shutdown() {
        polling.clear();
    }
}
