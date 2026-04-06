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
 * A segmented player model rendered with 6 {@link ItemDisplay} entities.
 * Each body part can be independently rotated, and the entire model supports
 * full 3D rotation including roll.
 *
 * <p>Parts are positioned relative to the torso center (the model origin).
 * Limb rotations pivot around joints (shoulder/hip/neck), not the limb center.
 */
public class PlayerModel {

    // ── Geometry: rest offsets from torso center ─────────────────────────────

    private static final EnumMap<BodyPart, Vector3f> REST_OFFSETS = new EnumMap<>(BodyPart.class);
    private static final EnumMap<BodyPart, Vector3f> SCALES = new EnumMap<>(BodyPart.class);

    // Pivot offsets: the joint position relative to the part's rest center.
    // For arms/legs this is the top of the limb (shoulder/hip).
    // For head this is the bottom (neck). Torso has no pivot offset.
    private static final EnumMap<BodyPart, Vector3f> PIVOT_OFFSETS = new EnumMap<>(BodyPart.class);

    private static final EnumMap<BodyPart, Material> DEFAULT_MATERIALS = new EnumMap<>(BodyPart.class);

    static {
        // Rest positions computed so all edges are flush (no gaps).
        // FIXED ItemDisplay renders at half the scale value, so visual sizes are:
        //   Torso: 0.5w × 0.75h    Head: 0.5 × 0.5    Arms/Legs: 0.25w × 0.75h
        //
        // Torso visual at origin: top=+0.375  bottom=-0.375  left=-0.25  right=+0.25
        // Head: bottom = torso top → y = 0.375 + 0.25 = 0.625
        // Arms: flush against torso → left x = -0.25 - 0.125 = -0.375
        // Legs: top = torso bottom → y = -0.375 - 0.375 = -0.75, x = ±0.125
        REST_OFFSETS.put(BodyPart.HEAD,      new Vector3f(0, 0.625f, 0));
        REST_OFFSETS.put(BodyPart.TORSO,     new Vector3f(0, 0, 0));
        REST_OFFSETS.put(BodyPart.LEFT_ARM,  new Vector3f(-0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_ARM, new Vector3f(0.375f, 0, 0));
        REST_OFFSETS.put(BodyPart.LEFT_LEG,  new Vector3f(-0.125f, -0.75f, 0));
        REST_OFFSETS.put(BodyPart.RIGHT_LEG, new Vector3f(0.125f, -0.75f, 0));

        // Display scales (width × height × depth in blocks)
        SCALES.put(BodyPart.HEAD,      new Vector3f(1.0f, 1.0f, 1.0f));
        SCALES.put(BodyPart.TORSO,     new Vector3f(1.0f, 1.5f, 0.5f));
        SCALES.put(BodyPart.LEFT_ARM,  new Vector3f(0.5f, 1.5f, 0.5f));
        SCALES.put(BodyPart.RIGHT_ARM, new Vector3f(0.5f, 1.5f, 0.5f));
        SCALES.put(BodyPart.LEFT_LEG,  new Vector3f(0.5f, 1.5f, 0.5f));
        SCALES.put(BodyPart.RIGHT_LEG, new Vector3f(0.5f, 1.5f, 0.5f));

        // Pivot offsets: vector from part center to joint (half the visual height).
        PIVOT_OFFSETS.put(BodyPart.HEAD,      new Vector3f(0, -0.25f, 0));  // neck at bottom of head
        PIVOT_OFFSETS.put(BodyPart.TORSO,     new Vector3f(0, 0, 0));       // no pivot
        PIVOT_OFFSETS.put(BodyPart.LEFT_ARM,  new Vector3f(0, 0.375f, 0));  // shoulder at top
        PIVOT_OFFSETS.put(BodyPart.RIGHT_ARM, new Vector3f(0, 0.375f, 0));
        PIVOT_OFFSETS.put(BodyPart.LEFT_LEG,  new Vector3f(0, 0.375f, 0));  // hip at top
        PIVOT_OFFSETS.put(BodyPart.RIGHT_LEG, new Vector3f(0, 0.375f, 0));

        // Fallback block materials (aviator outfit)
        DEFAULT_MATERIALS.put(BodyPart.HEAD,      Material.PLAYER_HEAD);
        DEFAULT_MATERIALS.put(BodyPart.TORSO,     Material.BROWN_WOOL);
        DEFAULT_MATERIALS.put(BodyPart.LEFT_ARM,  Material.BIRCH_PLANKS);
        DEFAULT_MATERIALS.put(BodyPart.RIGHT_ARM, Material.BIRCH_PLANKS);
        DEFAULT_MATERIALS.put(BodyPart.LEFT_LEG,  Material.DARK_OAK_PLANKS);
        DEFAULT_MATERIALS.put(BodyPart.RIGHT_LEG, Material.DARK_OAK_PLANKS);
    }

    private static final int INTERPOLATION_TICKS = 3;

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

    /** Set the player profile used for the PLAYER_HEAD part (shows the player's face). */
    public void setHeadProfile(PlayerProfile profile) {
        this.headProfile = profile;
    }

    /** Override the fallback material for a specific body part. */
    public void setFallbackMaterial(BodyPart part, Material material) {
        fallbackMaterials.put(part, material);
    }

    /**
     * Set signed skin textures for body parts (from MannequinAPI).
     * Each texture is a PLAYER_HEAD skin where the head region contains
     * that body part's pixels.
     */
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
            case TORSO -> BodyPart.TORSO;
            case LEFT_ARM -> BodyPart.LEFT_ARM;
            case RIGHT_ARM -> BodyPart.RIGHT_ARM;
            case LEFT_LEG -> BodyPart.LEFT_LEG;
            case RIGHT_LEG -> BodyPart.RIGHT_LEG;
        };
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Spawn all 6 display entities at the given location.
     *
     * @param world    the world to spawn in
     * @param center   model origin (torso center) position
     * @param rotation world rotation applied to the entire model (e.g. plane rotation)
     * @param pose     the limb pose to apply
     */
    public void spawn(World world, Location center, Quaternionf rotation, PlayerModelPose pose) {
        despawn();

        for (BodyPart part : BodyPart.values()) {
            ItemDisplay entity = spawnPart(world, center, rotation, pose, part);
            entities.put(part, entity);
        }
        spawned = true;
    }

    /**
     * Update all parts to a new position, rotation, and pose. Call every tick.
     */
    public void update(Location center, Quaternionf rotation, PlayerModelPose pose) {
        for (BodyPart part : BodyPart.values()) {
            updatePart(center, rotation, pose, part);
        }
    }

    /** Remove all entities from the world. */
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
        // Zero yaw/pitch so the entity has no inherent rotation —
        // the Transformation's leftRotation is the sole source of visual rotation.
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
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
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

    /**
     * Compute the world position and transformation for a body part.
     *
     * <p>For parts with no pose rotation (or torso), the entity is simply
     * teleported to (center + worldRot * restOffset) and the Transformation
     * applies worldRot as its rotation.
     *
     * <p>For posed limbs (e.g. arm rotated at shoulder), we:
     * <ol>
     *   <li>Find the joint position in local space (restOffset + pivotOffset)</li>
     *   <li>Rotate the vector from joint to part-center by the pose rotation</li>
     *   <li>Compute the final local position = joint + rotated-offset + poseAdjustment</li>
     *   <li>Rotate that into world space and teleport there</li>
     *   <li>Combined rotation = worldRot * poseRot goes in leftRotation</li>
     * </ol>
     */
    private PartTransform computeTransform(Quaternionf worldRotation, PlayerModelPose pose, BodyPart part) {
        Vector3f restOffset = new Vector3f(REST_OFFSETS.get(part));
        Vector3f pivotOffset = new Vector3f(PIVOT_OFFSETS.get(part));
        Vector3f poseOffsetAdj = pose.getOffsetAdjustment(part);
        Quaternionf poseRotation = pose.getRotation(part);

        // Vector from joint to part center (e.g. shoulder to arm center = downward)
        Vector3f jointToCenter = new Vector3f(pivotOffset).negate();

        // Rotate that vector by the pose (e.g. arm swings forward)
        poseRotation.transform(jointToCenter);

        // Joint position in local space
        Vector3f jointLocal = new Vector3f(restOffset).add(pivotOffset);

        // Final part center in local space = joint + rotated offset + pose adjustment
        Vector3f partLocal = new Vector3f(jointLocal).add(jointToCenter).add(poseOffsetAdj);

        // Transform to world space
        Vector3f partWorld = worldRotation.transform(new Vector3f(partLocal));

        // Combined rotation for the display entity
        Quaternionf combinedRotation = new Quaternionf(worldRotation).mul(poseRotation);

        // No translation needed — entity is teleported to the correct position
        return new PartTransform(partWorld, new Vector3f(), combinedRotation);
    }

    private ItemStack createItem(BodyPart part) {
        // If we have a signed skin texture for this body part, use it as a PLAYER_HEAD
        SkinTexture skinTex = skinTextures.get(part);
        if (skinTex != null) {
            return createTexturedHead(skinTex);
        }

        // Head part: use the player's actual profile if available
        Material material = fallbackMaterials.getOrDefault(part, DEFAULT_MATERIALS.get(part));
        if (part == BodyPart.HEAD && material == Material.PLAYER_HEAD && headProfile != null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setPlayerProfile(headProfile);
            skull.setItemMeta(meta);
            return skull;
        }

        // Fallback: colored block
        return new ItemStack(material);
    }

    private ItemStack createTexturedHead(SkinTexture texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        // Create a profile with the signed texture property
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
        profile.setProperty(new ProfileProperty("textures", texture.value(), texture.signature()));
        meta.setPlayerProfile(profile);

        skull.setItemMeta(meta);
        return skull;
    }

    // ── Transform result ────────────────────────────────────────────────────

    private record PartTransform(
            /** Offset from model center to entity position, in world space. */
            Vector3f worldPosition,
            /** Transformation translation (shifts visual from entity pos to correct render pos). */
            Vector3f translation,
            /** Combined world + pose rotation for the Transformation leftRotation. */
            Quaternionf leftRotation
    ) {}
}
