package com.wuhumannequin.model;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.wuhumannequin.skin.SkinTexture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A segmented player model rendered with 16 {@link ItemDisplay} entities.
 * Each limb and the torso are split into 3 sub-parts (upper/middle/lower)
 * for pixel-perfect texture mapping — each sub-part is a 4×4×4 cube.
 *
 * <p>All three sub-parts of a limb share the same joint pivot and pose rotation
 * (rigid limbs — the split is purely visual).
 */
public class PlayerModel {

    // ── Geometry ────────────────────────────────────────────────────────────
    //
    // FIXED ItemDisplay renders at half the scale value.
    // Each sub-part: scale 0.5 → visual 0.25 blocks per side (4px cube).
    // Three stacked: 0.25 × 3 = 0.75 blocks = full limb height.
    //
    // Sub-part visual height = 0.25 blocks, half = 0.125

    private static final EnumMap<BodyPart, Vector3f> REST_OFFSETS = new EnumMap<>(BodyPart.class);
    private static final EnumMap<BodyPart, Vector3f> SCALES = new EnumMap<>(BodyPart.class);
    private static final EnumMap<BodyPart, Vector3f> PIVOT_OFFSETS = new EnumMap<>(BodyPart.class);
    private static final EnumMap<BodyPart, Material> DEFAULT_MATERIALS = new EnumMap<>(BodyPart.class);

    // Visual height of one sub-part
    private static final float SUB_H = 0.25f;
    // Half of that
    private static final float SUB_HALF = 0.125f;

    static {
        // ── Rest offsets (from torso center) ────────────────────────────────
        // Original limb centers, then offset each third by ±SUB_H from center.
        // Upper = +SUB_H, Middle = 0, Lower = -SUB_H relative to original center.

        // Head (single part)
        REST_OFFSETS.put(BodyPart.HEAD, new Vector3f(0, 0.625f, 0));

        // Torso: original center at (0, 0, 0)
        REST_OFFSETS.put(BodyPart.TORSO_UPPER,  new Vector3f(0, SUB_H, 0));
        REST_OFFSETS.put(BodyPart.TORSO_MIDDLE, new Vector3f(0, 0, 0));
        REST_OFFSETS.put(BodyPart.TORSO_LOWER,  new Vector3f(0, -SUB_H, 0));

        // Left arm: original center at (-0.375, 0, 0)
        REST_OFFSETS.put(BodyPart.LEFT_ARM_UPPER,  new Vector3f(-0.375f, SUB_H, 0));
        REST_OFFSETS.put(BodyPart.LEFT_ARM_MIDDLE, new Vector3f(-0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.LEFT_ARM_LOWER,  new Vector3f(-0.375f, -SUB_H, 0));

        // Right arm: original center at (0.375, 0, 0)
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_UPPER,  new Vector3f(0.375f, SUB_H, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_MIDDLE, new Vector3f(0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_LOWER,  new Vector3f(0.375f, -SUB_H, 0));

        // Left leg: original center at (-0.125, -0.75, 0)
        REST_OFFSETS.put(BodyPart.LEFT_LEG_UPPER,  new Vector3f(-0.125f, -0.75f + SUB_H, 0));
        REST_OFFSETS.put(BodyPart.LEFT_LEG_MIDDLE, new Vector3f(-0.125f, -0.75f, 0));
        REST_OFFSETS.put(BodyPart.LEFT_LEG_LOWER,  new Vector3f(-0.125f, -0.75f - SUB_H, 0));

        // Right leg: original center at (0.125, -0.75, 0)
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_UPPER,  new Vector3f(0.125f, -0.75f + SUB_H, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_MIDDLE, new Vector3f(0.125f, -0.75f, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_LOWER,  new Vector3f(0.125f, -0.75f - SUB_H, 0));

        // ── Scales ─────────────────────────────────────────────────────────
        // Head: 1.0 (renders as 0.5 block cube, matching 8×8×8 pixels)
        // Torso thirds: width 1.0 (8px), height 0.5 (4px), depth 0.5 (4px)
        // Limb thirds: 0.5 × 0.5 × 0.5 (4×4×4 pixels = perfect cube)

        SCALES.put(BodyPart.HEAD, new Vector3f(1.0f, 1.0f, 1.0f));

        for (BodyPart p : new BodyPart[]{BodyPart.TORSO_UPPER, BodyPart.TORSO_MIDDLE, BodyPart.TORSO_LOWER}) {
            SCALES.put(p, new Vector3f(1.0f, 0.5f, 0.5f));
        }

        for (BodyPart p : BodyPart.values()) {
            if (p != BodyPart.HEAD && !p.logicalGroup().equals("TORSO")) {
                SCALES.put(p, new Vector3f(0.5f, 0.5f, 0.5f));
            }
        }

        // ── Pivot offsets (part center → joint) ────────────────────────────
        // All sub-parts of a limb share the same joint (shoulder/hip at top of upper).
        // Pivot offset = distance from this sub-part's center up to the joint.
        //
        // Upper:  joint is SUB_HALF above center
        // Middle: joint is SUB_HALF + SUB_H above center
        // Lower:  joint is SUB_HALF + 2*SUB_H above center

        PIVOT_OFFSETS.put(BodyPart.HEAD, new Vector3f(0, -0.25f, 0));

        // Torso: no pivot
        for (BodyPart p : new BodyPart[]{BodyPart.TORSO_UPPER, BodyPart.TORSO_MIDDLE, BodyPart.TORSO_LOWER}) {
            PIVOT_OFFSETS.put(p, new Vector3f(0, 0, 0));
        }

        // Arms and legs: joint at top of upper sub-part
        for (String group : new String[]{"LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG"}) {
            for (BodyPart p : BodyPart.values()) {
                if (!p.logicalGroup().equals(group)) continue;
                String suffix = p.name().substring(group.length() + 1); // UPPER, MIDDLE, or LOWER
                float pivotY = switch (suffix) {
                    case "UPPER"  -> SUB_HALF;
                    case "MIDDLE" -> SUB_HALF + SUB_H;
                    case "LOWER"  -> SUB_HALF + 2 * SUB_H;
                    default -> 0;
                };
                PIVOT_OFFSETS.put(p, new Vector3f(0, pivotY, 0));
            }
        }

        // ── Fallback materials ─────────────────────────────────────────────

        DEFAULT_MATERIALS.put(BodyPart.HEAD, Material.PLAYER_HEAD);

        for (BodyPart p : BodyPart.values()) {
            if (p == BodyPart.HEAD) continue;
            Material mat = switch (p.logicalGroup()) {
                case "TORSO" -> Material.BROWN_WOOL;
                case "LEFT_ARM", "RIGHT_ARM" -> Material.BIRCH_PLANKS;
                case "LEFT_LEG", "RIGHT_LEG" -> Material.DARK_OAK_PLANKS;
                default -> Material.STONE;
            };
            DEFAULT_MATERIALS.put(p, mat);
        }
    }

    private static int interpolationTicks = 3;

    public static void setInterpolationTicks(int ticks) {
        interpolationTicks = Math.max(0, Math.min(10, ticks));
    }

    public static int getInterpolationTicks() {
        return interpolationTicks;
    }

    // ── Instance state ──────────────────────────────────────────────────────

    private final EnumMap<BodyPart, ItemDisplay> entities = new EnumMap<>(BodyPart.class);
    private final EnumMap<BodyPart, Material> fallbackMaterials = new EnumMap<>(BodyPart.class);
    private final EnumMap<BodyPart, SkinTexture> skinTextures = new EnumMap<>(BodyPart.class);
    private PlayerProfile headProfile;
    private boolean spawned;

    public PlayerModel() {
        fallbackMaterials.putAll(DEFAULT_MATERIALS);
    }

    // ── Configuration (call before spawn) ───────────────────────────────────

    public void setHeadProfile(PlayerProfile profile) {
        this.headProfile = profile;
    }

    public void setFallbackMaterial(BodyPart part, Material material) {
        fallbackMaterials.put(part, material);
    }

    public void setSkinTextures(Map<SkinTexture.BodyPartKey, SkinTexture> textures) {
        skinTextures.clear();
        for (var entry : textures.entrySet()) {
            BodyPart part = bodyPartFromKey(entry.getKey());
            if (part != null) skinTextures.put(part, entry.getValue());
        }
    }

    private static BodyPart bodyPartFromKey(SkinTexture.BodyPartKey key) {
        return switch (key) {
            case HEAD -> BodyPart.HEAD;
            case TORSO_UPPER -> BodyPart.TORSO_UPPER;
            case TORSO_MIDDLE -> BodyPart.TORSO_MIDDLE;
            case TORSO_LOWER -> BodyPart.TORSO_LOWER;
            case LEFT_ARM_UPPER -> BodyPart.LEFT_ARM_UPPER;
            case LEFT_ARM_MIDDLE -> BodyPart.LEFT_ARM_MIDDLE;
            case LEFT_ARM_LOWER -> BodyPart.LEFT_ARM_LOWER;
            case RIGHT_ARM_UPPER -> BodyPart.RIGHT_ARM_UPPER;
            case RIGHT_ARM_MIDDLE -> BodyPart.RIGHT_ARM_MIDDLE;
            case RIGHT_ARM_LOWER -> BodyPart.RIGHT_ARM_LOWER;
            case LEFT_LEG_UPPER -> BodyPart.LEFT_LEG_UPPER;
            case LEFT_LEG_MIDDLE -> BodyPart.LEFT_LEG_MIDDLE;
            case LEFT_LEG_LOWER -> BodyPart.LEFT_LEG_LOWER;
            case RIGHT_LEG_UPPER -> BodyPart.RIGHT_LEG_UPPER;
            case RIGHT_LEG_MIDDLE -> BodyPart.RIGHT_LEG_MIDDLE;
            case RIGHT_LEG_LOWER -> BodyPart.RIGHT_LEG_LOWER;
        };
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    public void spawn(World world, Location center, Quaternionf rotation, PlayerModelPose pose) {
        despawn();
        for (BodyPart part : BodyPart.values()) {
            entities.put(part, spawnPart(world, center, rotation, pose, part));
        }
        spawned = true;
    }

    public void update(Location center, Quaternionf rotation, PlayerModelPose pose) {
        for (BodyPart part : BodyPart.values()) {
            updatePart(center, rotation, pose, part);
        }
    }

    public void despawn() {
        for (ItemDisplay entity : entities.values()) {
            if (entity != null && entity.isValid()) entity.remove();
        }
        entities.clear();
        spawned = false;
    }

    public boolean isSpawned() {
        return spawned;
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private ItemDisplay spawnPart(World world, Location center, Quaternionf rotation,
                                  PlayerModelPose pose, BodyPart part) {
        PartTransform t = computeTransform(rotation, pose, part);
        Location spawnLoc = new Location(world,
                center.getX() + t.worldPosition.x,
                center.getY() + t.worldPosition.y,
                center.getZ() + t.worldPosition.z, 0, 0);

        ItemStack item = createItem(part);
        Vector3f scale = new Vector3f(SCALES.get(part));

        return world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.FIXED);
            display.setInterpolationDuration(interpolationTicks);
            display.setTeleportDuration(interpolationTicks);
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    t.translation, t.leftRotation, scale, new Quaternionf()));
        });
    }

    private void updatePart(Location center, Quaternionf rotation,
                            PlayerModelPose pose, BodyPart part) {
        ItemDisplay entity = entities.get(part);
        if (entity == null || !entity.isValid()) return;

        PartTransform t = computeTransform(rotation, pose, part);
        Location loc = new Location(center.getWorld(),
                center.getX() + t.worldPosition.x,
                center.getY() + t.worldPosition.y,
                center.getZ() + t.worldPosition.z, 0, 0);
        entity.teleport(loc);

        Vector3f scale = new Vector3f(SCALES.get(part));
        entity.setInterpolationDelay(0);
        entity.setTransformation(new Transformation(
                t.translation, t.leftRotation, scale, new Quaternionf()));
    }

    private PartTransform computeTransform(Quaternionf worldRotation, PlayerModelPose pose, BodyPart part) {
        Vector3f restOffset = new Vector3f(REST_OFFSETS.get(part));
        Vector3f pivotOffset = new Vector3f(PIVOT_OFFSETS.get(part));
        Vector3f poseOffsetAdj = pose.getOffsetAdjustment(part);
        Quaternionf poseRotation = pose.getRotation(part);

        Vector3f jointToCenter = new Vector3f(pivotOffset).negate();
        poseRotation.transform(jointToCenter);

        Vector3f jointLocal = new Vector3f(restOffset).add(pivotOffset);
        Vector3f partLocal = new Vector3f(jointLocal).add(jointToCenter).add(poseOffsetAdj);
        Vector3f partWorld = worldRotation.transform(new Vector3f(partLocal));

        Quaternionf combinedRotation = new Quaternionf(worldRotation).mul(poseRotation);

        return new PartTransform(partWorld, new Vector3f(), combinedRotation);
    }

    private ItemStack createItem(BodyPart part) {
        SkinTexture skinTex = skinTextures.get(part);
        if (skinTex != null) {
            return createTexturedHead(skinTex);
        }

        Material material = fallbackMaterials.getOrDefault(part, DEFAULT_MATERIALS.get(part));
        if (part == BodyPart.HEAD && material == Material.PLAYER_HEAD && headProfile != null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setPlayerProfile(headProfile);
            skull.setItemMeta(meta);
            return skull;
        }

        return new ItemStack(material);
    }

    private ItemStack createTexturedHead(SkinTexture texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
        profile.setProperty(new ProfileProperty("textures", texture.value(), texture.signature()));
        meta.setPlayerProfile(profile);
        skull.setItemMeta(meta);
        return skull;
    }

    private record PartTransform(
            Vector3f worldPosition,
            Vector3f translation,
            Quaternionf leftRotation
    ) {}
}
