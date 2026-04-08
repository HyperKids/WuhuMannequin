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

    // Slim arm overrides. The arm cube is 3 px wide on X (instead of 4), so:
    //   - X scale becomes 0.375 (= 3/16 * 2, since FIXED ItemDisplay halves)
    //   - The cube center shifts inward by 0.5/16 = 0.03125 to keep the inner
    //     edge flush with the torso edge at ±0.25.
    //   - Y/Z scales and pivot offsets are unchanged: arms are still 12 px tall
    //     and 4 px deep, and the shoulder joint is still at the cube X-center.
    private static final EnumMap<BodyPart, Vector3f> SLIM_ARM_REST_OFFSETS = new EnumMap<>(BodyPart.class);
    private static final EnumMap<BodyPart, Vector3f> SLIM_ARM_SCALES = new EnumMap<>(BodyPart.class);
    private static final float SLIM_ARM_X = 0.34375f; // 0.375 - 0.03125

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

        // The model faces +Z (south) by default — the player_head item in FIXED
        // display puts its front face on +Z. A south-facing body has its
        // anatomical LEFT on +X (east) and anatomical RIGHT on -X (west).

        // Left arm: original center at (+0.375, 0, 0)
        REST_OFFSETS.put(BodyPart.LEFT_ARM_UPPER,  new Vector3f(0.375f, SUB_H, 0));
        REST_OFFSETS.put(BodyPart.LEFT_ARM_MIDDLE, new Vector3f(0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.LEFT_ARM_LOWER,  new Vector3f(0.375f, -SUB_H, 0));

        // Right arm: original center at (-0.375, 0, 0)
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_UPPER,  new Vector3f(-0.375f, SUB_H, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_MIDDLE, new Vector3f(-0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_ARM_LOWER,  new Vector3f(-0.375f, -SUB_H, 0));

        // Left leg: original center at (+0.125, -0.75, 0)
        REST_OFFSETS.put(BodyPart.LEFT_LEG_UPPER,  new Vector3f(0.125f, -0.75f + SUB_H, 0));
        REST_OFFSETS.put(BodyPart.LEFT_LEG_MIDDLE, new Vector3f(0.125f, -0.75f, 0));
        REST_OFFSETS.put(BodyPart.LEFT_LEG_LOWER,  new Vector3f(0.125f, -0.75f - SUB_H, 0));

        // Right leg: original center at (-0.125, -0.75, 0)
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_UPPER,  new Vector3f(-0.125f, -0.75f + SUB_H, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_MIDDLE, new Vector3f(-0.125f, -0.75f, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_LEG_LOWER,  new Vector3f(-0.125f, -0.75f - SUB_H, 0));

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

        // Arms and legs: each sub-part pivots at its OWN top edge — i.e., a joint shared
        // with the segment immediately above it in the hierarchical chain:
        //   UPPER pivots at the shoulder/hip (top of UPPER)
        //   MIDDLE pivots at the elbow/knee  (top of MIDDLE = bottom of UPPER)
        //   LOWER pivots at the wrist/ankle  (top of LOWER  = bottom of MIDDLE)
        // The hierarchical compute in computeAllTransforms() walks this chain and places
        // each segment so its top edge meets its parent's bottom edge in world space —
        // when local rotations are identity the limb behaves as a rigid block.
        for (String group : new String[]{"LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG"}) {
            for (BodyPart p : BodyPart.values()) {
                if (!p.logicalGroup().equals(group)) continue;
                PIVOT_OFFSETS.put(p, new Vector3f(0, SUB_HALF, 0));
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

        // ── Slim arm overrides ──────────────────────────────────────────────
        SLIM_ARM_REST_OFFSETS.put(BodyPart.LEFT_ARM_UPPER,  new Vector3f(SLIM_ARM_X, SUB_H, 0));
        SLIM_ARM_REST_OFFSETS.put(BodyPart.LEFT_ARM_MIDDLE, new Vector3f(SLIM_ARM_X, 0, 0));
        SLIM_ARM_REST_OFFSETS.put(BodyPart.LEFT_ARM_LOWER,  new Vector3f(SLIM_ARM_X, -SUB_H, 0));
        SLIM_ARM_REST_OFFSETS.put(BodyPart.RIGHT_ARM_UPPER,  new Vector3f(-SLIM_ARM_X, SUB_H, 0));
        SLIM_ARM_REST_OFFSETS.put(BodyPart.RIGHT_ARM_MIDDLE, new Vector3f(-SLIM_ARM_X, 0, 0));
        SLIM_ARM_REST_OFFSETS.put(BodyPart.RIGHT_ARM_LOWER,  new Vector3f(-SLIM_ARM_X, -SUB_H, 0));
        for (BodyPart p : new BodyPart[]{
                BodyPart.LEFT_ARM_UPPER, BodyPart.LEFT_ARM_MIDDLE, BodyPart.LEFT_ARM_LOWER,
                BodyPart.RIGHT_ARM_UPPER, BodyPart.RIGHT_ARM_MIDDLE, BodyPart.RIGHT_ARM_LOWER}) {
            SLIM_ARM_SCALES.put(p, new Vector3f(0.375f, 0.5f, 0.5f));
        }
    }

    private static boolean isArmPart(BodyPart p) {
        return p.logicalGroup().equals("LEFT_ARM") || p.logicalGroup().equals("RIGHT_ARM");
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
    private SkinTexture.Model armModel = SkinTexture.Model.CLASSIC;

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
        setSkinTextures(textures, SkinTexture.Model.CLASSIC);
    }

    /**
     * Set skin textures and the player's model variant. Slim (Alex) skins use
     * narrower arm cubes and slightly different rest offsets so the inner edge
     * still meets the torso.
     */
    public void setSkinTextures(Map<SkinTexture.BodyPartKey, SkinTexture> textures, SkinTexture.Model model) {
        skinTextures.clear();
        for (var entry : textures.entrySet()) {
            BodyPart part = bodyPartFromKey(entry.getKey());
            if (part != null) skinTextures.put(part, entry.getValue());
        }
        this.armModel = model == null ? SkinTexture.Model.CLASSIC : model;
    }

    /** Returns the rest offset for a part, accounting for slim-arm geometry. */
    private Vector3f restOffsetFor(BodyPart part) {
        if (armModel == SkinTexture.Model.SLIM && isArmPart(part)) {
            return SLIM_ARM_REST_OFFSETS.get(part);
        }
        return REST_OFFSETS.get(part);
    }

    /** Returns the scale for a part, accounting for slim-arm geometry. */
    private Vector3f scaleFor(BodyPart part) {
        if (armModel == SkinTexture.Model.SLIM && isArmPart(part)) {
            return SLIM_ARM_SCALES.get(part);
        }
        return SCALES.get(part);
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
        Map<BodyPart, PartTransform> transforms = computeAllTransforms(rotation, pose);
        for (BodyPart part : BodyPart.values()) {
            entities.put(part, spawnPart(world, center, transforms.get(part), part));
        }
        spawned = true;
    }

    public void update(Location center, Quaternionf rotation, PlayerModelPose pose) {
        Map<BodyPart, PartTransform> transforms = computeAllTransforms(rotation, pose);
        for (BodyPart part : BodyPart.values()) {
            updatePart(center, transforms.get(part), part);
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

    private ItemDisplay spawnPart(World world, Location center, PartTransform t, BodyPart part) {
        Location spawnLoc = new Location(world,
                center.getX() + t.worldPosition.x,
                center.getY() + t.worldPosition.y,
                center.getZ() + t.worldPosition.z, 0, 0);

        ItemStack item = createItem(part);
        Vector3f scale = new Vector3f(scaleFor(part));

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

    private void updatePart(Location center, PartTransform t, BodyPart part) {
        ItemDisplay entity = entities.get(part);
        if (entity == null || !entity.isValid()) return;

        Location loc = new Location(center.getWorld(),
                center.getX() + t.worldPosition.x,
                center.getY() + t.worldPosition.y,
                center.getZ() + t.worldPosition.z, 0, 0);
        entity.teleport(loc);

        Vector3f scale = new Vector3f(scaleFor(part));
        entity.setInterpolationDelay(0);
        entity.setTransformation(new Transformation(
                t.translation, t.leftRotation, scale, new Quaternionf()));
    }

    /**
     * Compute body-frame transforms for every sub-part in one pass. Head and torso parts
     * use the simple "rotate around own pivot" path. Limbs use a hierarchical chain so
     * that an elbow rotation actually bends the limb (and the forearm visually meets the
     * upper arm at the elbow joint, no air gap).
     *
     * <p>For limbs the pose stores rotations per sub-part with these semantics:
     * <ul>
     *   <li>{@code *_UPPER} = shoulder rotation (in body frame)</li>
     *   <li>{@code *_MIDDLE} = elbow rotation (in the upper arm's local frame)</li>
     *   <li>{@code *_LOWER} = wrist rotation (in the forearm's local frame)</li>
     * </ul>
     * Identity at elbow + wrist gives a rigid limb. {@link PlayerModelPoses#buildPose}
     * preserves rigid-limb behavior by setting only the UPPER key.
     */
    private Map<BodyPart, PartTransform> computeAllTransforms(Quaternionf worldRotation, PlayerModelPose pose) {
        EnumMap<BodyPart, PartTransform> result = new EnumMap<>(BodyPart.class);

        // Head + torso: each rotates around its own pivot, no chain.
        for (BodyPart p : new BodyPart[]{
                BodyPart.HEAD,
                BodyPart.TORSO_UPPER, BodyPart.TORSO_MIDDLE, BodyPart.TORSO_LOWER}) {
            result.put(p, computeSimpleTransform(worldRotation, pose, p));
        }

        // Limbs: walk shoulder → elbow → wrist for each of the four limbs.
        computeLimbChain(worldRotation, pose, result,
                BodyPart.LEFT_ARM_UPPER, BodyPart.LEFT_ARM_MIDDLE, BodyPart.LEFT_ARM_LOWER);
        computeLimbChain(worldRotation, pose, result,
                BodyPart.RIGHT_ARM_UPPER, BodyPart.RIGHT_ARM_MIDDLE, BodyPart.RIGHT_ARM_LOWER);
        computeLimbChain(worldRotation, pose, result,
                BodyPart.LEFT_LEG_UPPER, BodyPart.LEFT_LEG_MIDDLE, BodyPart.LEFT_LEG_LOWER);
        computeLimbChain(worldRotation, pose, result,
                BodyPart.RIGHT_LEG_UPPER, BodyPart.RIGHT_LEG_MIDDLE, BodyPart.RIGHT_LEG_LOWER);

        return result;
    }

    /**
     * Walk one limb's three-segment chain. Each segment's joint position depends on the
     * cumulative rotation of every segment above it, so the chain order matters.
     */
    private void computeLimbChain(Quaternionf worldRotation, PlayerModelPose pose,
                                  Map<BodyPart, PartTransform> out,
                                  BodyPart upper, BodyPart middle, BodyPart lower) {
        // Local joint rotations (default: identity → rigid limb).
        Quaternionf rShoulderLocal = pose.getRotation(upper);
        Quaternionf rElbowLocal = pose.getRotation(middle);
        Quaternionf rWristLocal = pose.getRotation(lower);

        // Cumulative segment orientations in body frame.
        Quaternionf rUpper = new Quaternionf(rShoulderLocal);
        Quaternionf rMiddle = new Quaternionf(rUpper).mul(rElbowLocal);
        Quaternionf rLower = new Quaternionf(rMiddle).mul(rWristLocal);

        // Shoulder joint (top of UPPER) — fixed in body frame.
        Vector3f shoulderJoint = new Vector3f(restOffsetFor(upper)).add(PIVOT_OFFSETS.get(upper));

        // UPPER center = joint + R_upper × (0, -SUB_HALF, 0)
        Vector3f upperCenter = segmentCenter(shoulderJoint, rUpper);
        out.put(upper, finishTransform(worldRotation, rUpper, upperCenter, pose.getOffsetAdjustment(upper)));

        // Elbow joint = bottom of UPPER in body frame.
        Vector3f elbowJoint = new Vector3f(shoulderJoint).add(rotated(rUpper, 0, -SUB_H, 0));

        // MIDDLE center = elbowJoint + R_middle × (0, -SUB_HALF, 0)
        Vector3f middleCenter = segmentCenter(elbowJoint, rMiddle);
        out.put(middle, finishTransform(worldRotation, rMiddle, middleCenter, pose.getOffsetAdjustment(middle)));

        // Wrist joint = bottom of MIDDLE in body frame.
        Vector3f wristJoint = new Vector3f(elbowJoint).add(rotated(rMiddle, 0, -SUB_H, 0));

        // LOWER center = wristJoint + R_lower × (0, -SUB_HALF, 0)
        Vector3f lowerCenter = segmentCenter(wristJoint, rLower);
        out.put(lower, finishTransform(worldRotation, rLower, lowerCenter, pose.getOffsetAdjustment(lower)));
    }

    /** A segment's center sits SUB_HALF below its top joint, in the segment's rotated frame. */
    private static Vector3f segmentCenter(Vector3f topJoint, Quaternionf segmentOrientation) {
        return new Vector3f(topJoint).add(rotated(segmentOrientation, 0, -SUB_HALF, 0));
    }

    /** Allocate a fresh vector and rotate it by {@code q}. */
    private static Vector3f rotated(Quaternionf q, float x, float y, float z) {
        return q.transform(new Vector3f(x, y, z));
    }

    /**
     * Finalize a part transform: add offset adjustments in body frame, apply the world body
     * rotation to get the part's position relative to the model's anchor, and combine the
     * world rotation with the segment's local orientation for the entity rotation.
     */
    private PartTransform finishTransform(Quaternionf worldRotation, Quaternionf localOrientation,
                                          Vector3f bodyFrameCenter, Vector3f offsetAdj) {
        Vector3f totalLocal = new Vector3f(bodyFrameCenter).add(offsetAdj);
        Vector3f worldPos = worldRotation.transform(new Vector3f(totalLocal));
        Quaternionf finalRotation = new Quaternionf(worldRotation).mul(localOrientation);
        return new PartTransform(worldPos, new Vector3f(), finalRotation);
    }

    /** Single-segment transform (head, torso). Each part rotates around its own pivot. */
    private PartTransform computeSimpleTransform(Quaternionf worldRotation, PlayerModelPose pose, BodyPart part) {
        Vector3f restOffset = new Vector3f(restOffsetFor(part));
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
