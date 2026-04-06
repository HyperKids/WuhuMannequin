package com.wuhumannequin.command;

import com.wuhumannequin.WuhuMannequin;
import com.wuhumannequin.model.BodyPart;
import com.wuhumannequin.model.PlayerModel;
import com.wuhumannequin.model.PlayerModelPose;
import com.wuhumannequin.model.PlayerModelPoses;
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
    private final Map<UUID, PlayerModelPose> currentPoses = new HashMap<>();

    private static final Map<String, PlayerModelPose> POSE_MAP = Map.of(
            "standing", PlayerModelPoses.STANDING,
            "sitting", PlayerModelPoses.SITTING,
            "tpose", PlayerModelPoses.T_POSE,
            "arms_forward", PlayerModelPoses.ARMS_FORWARD
    );

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
            case "remove" -> handleRemove(player);
            default -> showHelp(player);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) {
            return filter(List.of("spawn", "spin", "pose", "remove"), args.length > 0 ? args[0] : "");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pose")) {
            return filter(List.of("standing", "sitting", "tpose", "arms_forward"), args[1]);
        }
        return List.of();
    }

    /** Clean up all debug models (call from plugin onDisable). */
    public void cleanupAll() {
        for (BukkitRunnable task : spinTasks.values()) task.cancel();
        spinTasks.clear();
        for (PlayerModel model : debugModels.values()) model.despawn();
        debugModels.clear();
        currentPoses.clear();
    }

    // ── Subcommand handlers ─────────────────────────────────────────────────

    private void handleSpawn(Player player) {
        UUID uuid = player.getUniqueId();
        if (debugModels.containsKey(uuid)) {
            handleRemove(player);
            return;
        }

        PlayerModel model = new PlayerModel();
        model.setHeadProfile(player.getPlayerProfile());
        PlayerModelPose pose = PlayerModelPoses.STANDING;
        model.spawn(player.getWorld(), player.getLocation(),
                yawRotation(player.getLocation().getYaw()), pose);

        debugModels.put(uuid, model);
        currentPoses.put(uuid, pose);
        msg(player, "Debug model spawned. Use /mannequin remove to clean up.");
    }

    private void handleSpin(Player player) {
        UUID uuid = player.getUniqueId();
        handleRemove(player); // clean up any existing

        PlayerModel model = new PlayerModel();
        model.setHeadProfile(player.getPlayerProfile());
        PlayerModelPose pose = PlayerModelPoses.STANDING;
        model.spawn(player.getWorld(), player.getLocation(), new Quaternionf(), pose);

        debugModels.put(uuid, model);
        currentPoses.put(uuid, pose);

        // Animate: rotate 2 degrees per tick on all axes in sequence
        var location = player.getLocation().clone();
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
                    rotation.rotateYXZ(phaseAngle * 0.7f, phaseAngle * 0.5f, phaseAngle * 0.3f);
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
            msg(player, "Usage: /mannequin pose <standing|sitting|tpose|arms_forward>");
            return;
        }

        String poseName = args[1].toLowerCase(Locale.ROOT);
        PlayerModelPose pose = POSE_MAP.get(poseName);
        if (pose == null) {
            msg(player, "Unknown pose. Options: standing, sitting, tpose, arms_forward");
            return;
        }

        currentPoses.put(uuid, pose);

        // If not spinning, apply immediately
        if (!spinTasks.containsKey(uuid)) {
            model.update(player.getLocation(),
                    yawRotation(player.getLocation().getYaw()), pose);
        }

        msg(player, "Pose set to " + poseName + ".");
    }

    private void handleRemove(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = spinTasks.remove(uuid);
        if (task != null) task.cancel();

        PlayerModel model = debugModels.remove(uuid);
        if (model != null) {
            model.despawn();
            msg(player, "Debug model removed.");
        } else {
            msg(player, "No debug model to remove.");
        }
        currentPoses.remove(uuid);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void showHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("─── WuhuMannequin Debug ───", NamedTextColor.GOLD));
        player.sendMessage(helpLine("/mannequin spawn", "Spawn/toggle a static model"));
        player.sendMessage(helpLine("/mannequin spin", "Spawn a spinning model (rotation test)"));
        player.sendMessage(helpLine("/mannequin pose <name>", "Change pose (standing, sitting, tpose, arms_forward)"));
        player.sendMessage(helpLine("/mannequin remove", "Remove debug model"));
        player.sendMessage(Component.empty());
    }

    private static Component helpLine(String cmd, String desc) {
        return Component.text("  " + cmd, NamedTextColor.YELLOW)
                .append(Component.text(" — " + desc, NamedTextColor.GRAY));
    }

    private static void msg(Player player, String text) {
        player.sendMessage(Component.text(text, NamedTextColor.GREEN));
    }

    private static Quaternionf yawRotation(float yawDeg) {
        return new Quaternionf().rotateY((float) Math.toRadians(-yawDeg));
    }

    private static Collection<String> filter(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
