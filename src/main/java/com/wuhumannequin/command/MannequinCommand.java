package com.wuhumannequin.command;

import com.wuhumannequin.WuhuMannequin;
import org.bukkit.Location;
import com.wuhumannequin.model.PlayerModel;
import com.wuhumannequin.model.PlayerModelAnimation;
import com.wuhumannequin.model.PlayerModelAnimations;
import com.wuhumannequin.model.PlayerModelPose;
import com.wuhumannequin.model.PlayerModelPoses;
import com.wuhumannequin.skin.SkinCache;
import com.wuhumannequin.skin.SkinTexture;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Quaternionf;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Standalone debug commands for WuhuMannequin.
 *
 * <pre>
 * /mannequin spawn              — Spawn a static model at your feet (toggle)
 * /mannequin spin               — Spawn a model that rotates on all axes
 * /mannequin pose <name>        — Change the debug model's pose
 * /mannequin remove             — Remove your debug model
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage")
public class MannequinCommand implements BasicCommand {

    private final WuhuMannequin plugin;
    private final Map<UUID, PlayerModel> debugModels = new HashMap<>();
    private final Map<UUID, BukkitRunnable> spinTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> animationTasks = new HashMap<>();
    private final Map<UUID, PlayerModelPose> currentPoses = new HashMap<>();
    /** Stable spawn location + yaw per model. Pose/animation updates anchor here so the
     *  model doesn't drift toward the player every time you re-issue a command. */
    private final Map<UUID, Location> modelAnchors = new HashMap<>();
    private final Map<UUID, Float> modelYaws = new HashMap<>();

    private static final Map<String, PlayerModelPose> POSE_MAP = PlayerModelPoses.ALL;
    private static final Map<String, PlayerModelAnimation> ANIMATION_MAP = PlayerModelAnimations.ALL;

    public MannequinCommand(WuhuMannequin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        var sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("wuhumannequin.debug")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> handleSpawn(player);
            case "spin" -> handleSpin(player);
            case "pose" -> handlePose(player, args);
            case "animation", "anim" -> handleAnimation(player, args);
            case "remove" -> handleRemove(player);
            case "reload" -> handleReload(player);
            case "fetchskin" -> handleFetchSkin(player, args);
            case "interpolation" -> handleInterpolation(player, args);
            default -> showHelp(player);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            return filter(List.of("spawn", "spin", "pose", "animation", "remove", "reload", "fetchskin", "interpolation"), args.length > 0 ? args[0] : "");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pose")) {
            return filter(POSE_MAP.keySet(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))) {
            List<String> options = new java.util.ArrayList<>(ANIMATION_MAP.keySet());
            options.add("stop");
            return filter(options, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("fetchskin")) {
            return filter(List.of("debug"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("interpolation")) {
            return filter(List.of("0", "1", "2", "3", "5"), args[1]);
        }
        return List.of();
    }

    /** Clean up all debug models (call from plugin onDisable). */
    public void cleanupAll() {
        for (BukkitRunnable task : spinTasks.values()) task.cancel();
        spinTasks.clear();
        for (BukkitRunnable task : animationTasks.values()) task.cancel();
        animationTasks.clear();
        for (PlayerModel model : debugModels.values()) model.despawn();
        debugModels.clear();
        currentPoses.clear();
        modelAnchors.clear();
        modelYaws.clear();
    }

    // ── Subcommand handlers ─────────────────────────────────────────────────

    private void handleSpawn(Player player) {
        UUID uuid = player.getUniqueId();
        if (debugModels.containsKey(uuid)) {
            handleRemove(player);
            return;
        }

        PlayerModel model = createModelForPlayer(player);
        PlayerModelPose pose = PlayerModelPoses.STANDING;
        Location spawnLoc = player.getLocation().add(0, 1, 0);
        model.spawn(player.getWorld(), spawnLoc,
                yawRotation(spawnLoc.getYaw()), pose);

        debugModels.put(uuid, model);
        currentPoses.put(uuid, pose);
        modelAnchors.put(uuid, spawnLoc.clone());
        modelYaws.put(uuid, spawnLoc.getYaw());
        msg(player, "Debug model spawned. Use /mannequin remove to clean up.");
    }

    private void handleSpin(Player player) {
        UUID uuid = player.getUniqueId();
        handleRemove(player); // clean up any existing

        PlayerModel model = createModelForPlayer(player);
        PlayerModelPose pose = PlayerModelPoses.STANDING;
        Location spawnLoc = player.getLocation().add(0, 1, 0);
        model.spawn(player.getWorld(), spawnLoc, new Quaternionf(), pose);

        debugModels.put(uuid, model);
        currentPoses.put(uuid, pose);
        modelAnchors.put(uuid, spawnLoc.clone());
        modelYaws.put(uuid, 0f);

        // Animate: rotate 2 degrees per tick on all axes in sequence
        var location = spawnLoc.clone();
        BukkitRunnable task = new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                if (!model.isSpawned()) {
                    cancel();
                    return;
                }

                // Angle resets each phase so transitions are smooth
                float phaseAngle = (float) Math.toRadians((tick % 90) * 4.0);
                Quaternionf rotation = new Quaternionf();

                // Cycle through axes: yaw → pitch → roll → combined
                long phase = (tick / 90) % 4;
                if (phase == 0) {
                    rotation.rotateY(phaseAngle);
                } else if (phase == 1) {
                    rotation.rotateX(phaseAngle);
                } else if (phase == 2) {
                    rotation.rotateZ(phaseAngle);
                } else {
                    // Integer multipliers so all axes complete whole turns and end at identity
                    rotation.rotateYXZ(phaseAngle * 1f, phaseAngle * 2f, phaseAngle * 3f);
                }

                PlayerModelPose currentPose = currentPoses.getOrDefault(uuid, PlayerModelPoses.STANDING);
                model.update(location, rotation, currentPose);
                tick++;
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
        spinTasks.put(uuid, task);

        msg(player, "Spin test started. Cycles through yaw → pitch → roll → combined.");
    }

    private void handlePose(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        PlayerModel model = debugModels.get(uuid);
        if (model == null) {
            msg(player, "No debug model active. Use /mannequin spawn or /mannequin spin first.");
            return;
        }

        if (args.length < 2) {
            msg(player, "Usage: /mannequin pose <name>. Options: " + String.join(", ", POSE_MAP.keySet()));
            return;
        }

        String poseName = args[1].toLowerCase(Locale.ROOT);
        PlayerModelPose pose = POSE_MAP.get(poseName);
        if (pose == null) {
            msg(player, "Unknown pose '" + poseName + "'. Options: " + String.join(", ", POSE_MAP.keySet()));
            return;
        }

        // A static pose overrides any running animation.
        BukkitRunnable existingAnim = animationTasks.remove(uuid);
        if (existingAnim != null) existingAnim.cancel();

        currentPoses.put(uuid, pose);

        // If not spinning, apply immediately at the model's anchored location/yaw — never
        // teleport the model to wherever the player happens to be standing right now.
        if (!spinTasks.containsKey(uuid)) {
            Location anchor = modelAnchors.getOrDefault(uuid, player.getLocation());
            float yaw = modelYaws.getOrDefault(uuid, anchor.getYaw());
            model.update(anchor, yawRotation(yaw), pose);
        }

        msg(player, "Pose set to " + poseName + ".");
    }

    private void handleAnimation(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        PlayerModel model = debugModels.get(uuid);
        if (model == null) {
            msg(player, "No debug model active. Use /mannequin spawn first.");
            return;
        }

        if (args.length < 2) {
            msg(player, "Usage: /mannequin animation <name|stop>. Options: " + String.join(", ", ANIMATION_MAP.keySet()));
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);

        // Always stop any currently running animation or spin first — both write the model
        // every tick and would otherwise fight for control.
        BukkitRunnable existing = animationTasks.remove(uuid);
        if (existing != null) existing.cancel();
        BukkitRunnable spin = spinTasks.remove(uuid);
        if (spin != null) spin.cancel();

        if (name.equals("stop") || name.equals("none")) {
            // Restore the last static pose (or standing) so the model doesn't freeze mid-frame.
            PlayerModelPose restorePose = currentPoses.getOrDefault(uuid, PlayerModelPoses.STANDING);
            if (!spinTasks.containsKey(uuid)) {
                Location anchor = modelAnchors.getOrDefault(uuid, player.getLocation());
                float yaw = modelYaws.getOrDefault(uuid, anchor.getYaw());
                model.update(anchor, yawRotation(yaw), restorePose);
            }
            msg(player, "Animation stopped.");
            return;
        }

        PlayerModelAnimation animation = ANIMATION_MAP.get(name);
        if (animation == null) {
            msg(player, "Unknown animation '" + name + "'. Options: " + String.join(", ", ANIMATION_MAP.keySet()));
            return;
        }

        // Animations replace the static pose while running. Use the model's stable spawn
        // anchor so each new animation doesn't snap the model to the player's current spot.
        Location anchor = modelAnchors.getOrDefault(uuid, player.getLocation().add(0, 1, 0));
        float yaw = modelYaws.getOrDefault(uuid, anchor.getYaw());
        Quaternionf bodyRotation = yawRotation(yaw);

        BukkitRunnable task = new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                if (!model.isSpawned()) {
                    cancel();
                    animationTasks.remove(uuid);
                    return;
                }
                PlayerModelPose pose = animation.poseAt(tick);
                model.update(anchor, bodyRotation, pose);
                tick++;
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
        animationTasks.put(uuid, task);

        msg(player, "Animation '" + name + "' started. Use /mannequin animation stop to end it.");
    }

    private void handleFetchSkin(Player player, String[] args) {
        var apiClient = plugin.getSkinApiClient();
        if (apiClient == null || !apiClient.isConfigured()) {
            String reason = (apiClient != null && apiClient.disabledReason() != null)
                    ? apiClient.disabledReason()
                    : "no API key set";
            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "MannequinAPI unavailable: " + reason + ". Fix api.key in config.yml and /mannequin reload.",
                    NamedTextColor.RED));
            return;
        }

        // Determine which UUID to fetch: self, "debug", or a specific UUID
        final UUID targetUuid;
        final String label;
        if (args.length >= 2) {
            String arg = args[1].toLowerCase(Locale.ROOT);
            if (arg.equals("debug")) {
                targetUuid = new UUID(0, 0);
                label = "debug skin";
            } else {
                UUID parsed = parseUuid(arg);
                if (parsed == null) {
                    msg(player, "Invalid UUID. Usage: /mannequin fetchskin [uuid|debug]");
                    return;
                }
                targetUuid = parsed;
                label = targetUuid.toString();
            }
        } else {
            targetUuid = player.getUniqueId();
            label = "your skin";
        }

        // Store textures under the PLAYER's UUID so they apply to their model
        UUID storageUuid = player.getUniqueId();

        // No optimistic "Requesting..." chat — wait until the API actually
        // answers before saying anything, so we never tell the user their
        // skins were queued when in reality the request failed.
        apiClient.getSkins(targetUuid).thenAccept(result -> {
            switch (result.status()) {
                case READY -> {
                    plugin.getSkinCache().put(storageUuid, result.textures(), result.model());
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "Skin textures loaded for " + label + " (" + result.model() + "). Use /mannequin spawn to see them.",
                            NamedTextColor.GREEN));
                }
                case GENERATING -> {
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "Server is generating " + label + " (this can take ~50s). Polling started — re-run /mannequin fetchskin once it finishes.",
                            NamedTextColor.YELLOW));
                    var detector = plugin.getSkinChangeDetector();
                    if (detector != null) {
                        detector.requestSkins(targetUuid);
                    }
                }
                case ERROR -> player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Failed to fetch " + label + ": " + result.errorMessage(),
                        NamedTextColor.RED));
            }
        });
    }

    private void handleInterpolation(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, "Current: " + PlayerModel.getInterpolationTicks() + " ticks. Usage: /mannequin interpolation <0-10>");
            return;
        }
        try {
            int ticks = Integer.parseInt(args[1]);
            PlayerModel.setInterpolationTicks(ticks);
            msg(player, "Interpolation set to " + PlayerModel.getInterpolationTicks() + " ticks. Respawn model to apply.");
        } catch (NumberFormatException e) {
            msg(player, "Invalid number. Usage: /mannequin interpolation <0-10>");
        }
    }

    private void handleReload(Player player) {
        plugin.reloadConfig();
        plugin.reinitialize();
        msg(player, "Config reloaded.");
    }

    private void handleRemove(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = spinTasks.remove(uuid);
        if (task != null) task.cancel();
        BukkitRunnable animTask = animationTasks.remove(uuid);
        if (animTask != null) animTask.cancel();

        PlayerModel model = debugModels.remove(uuid);
        if (model != null) {
            model.despawn();
            msg(player, "Debug model removed.");
        } else {
            msg(player, "No debug model to remove.");
        }
        currentPoses.remove(uuid);
        modelAnchors.remove(uuid);
        modelYaws.remove(uuid);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void showHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("─── WuhuMannequin Debug ───", NamedTextColor.GOLD));
        player.sendMessage(helpLine("/mannequin spawn", "Spawn/toggle a static model"));
        player.sendMessage(helpLine("/mannequin spin", "Spawn a spinning model (rotation test)"));
        player.sendMessage(helpLine("/mannequin pose <name>", "Change static pose (" + POSE_MAP.size() + " options, tab-complete)"));
        player.sendMessage(helpLine("/mannequin animation <name|stop>", "Play animation (" + ANIMATION_MAP.size() + " options, tab-complete)"));
        player.sendMessage(helpLine("/mannequin remove", "Remove debug model"));
        player.sendMessage(helpLine("/mannequin interpolation <0-10>", "Set smoothing ticks (0=cohesive, 3=smooth)"));
        player.sendMessage(helpLine("/mannequin fetchskin [uuid|debug]", "Fetch skins (self, UUID, or debug grid)"));
        player.sendMessage(helpLine("/mannequin reload", "Reload config"));
        player.sendMessage(Component.empty());
    }

    private static Component helpLine(String cmd, String desc) {
        return Component.text("  " + cmd, NamedTextColor.YELLOW)
                .append(Component.text(" — " + desc, NamedTextColor.GRAY));
    }

    private static void msg(Player player, String text) {
        player.sendMessage(Component.text(text, NamedTextColor.GREEN));
    }

    private PlayerModel createModelForPlayer(Player player) {
        PlayerModel model = new PlayerModel();
        model.setHeadProfile(player.getPlayerProfile());

        SkinCache cache = plugin.getSkinCache();
        if (cache != null) {
            var entry = cache.get(player.getUniqueId());
            if (entry != null) {
                model.setSkinTextures(entry.textures(), entry.model());
            }
        }
        return model;
    }

    private static UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            try {
                String dashed = input.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(dashed);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    private static Quaternionf yawRotation(float yawDeg) {
        return new Quaternionf().rotateY((float) Math.toRadians(-yawDeg));
    }

    private static Collection<String> filter(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
