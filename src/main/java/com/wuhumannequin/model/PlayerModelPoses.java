package com.wuhumannequin.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Pre-defined poses for the segmented player model.
 * Poses are defined per logical limb — both sub-parts of a limb share the same rotation.
 */
public final class PlayerModelPoses {
    private PlayerModelPoses() {}

    /** Arms at sides, legs straight down. */
    public static final PlayerModelPose STANDING = PlayerModelPose.IDENTITY;

    /** Arms extended horizontally. */
    public static final PlayerModelPose T_POSE = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(90)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))
            ),
            Map.of()
    );

    /** Star jump: arms angled up-and-out 45° above horizontal, legs spread 45° outward. */
    public static final PlayerModelPose X_POSE = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(135)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-135)),
                    "LEFT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(45)),
                    "RIGHT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(-45))
            ),
            Map.of()
    );

    /** Seated: legs bent 90 degrees forward, arms angled forward on controls. */
    public static final PlayerModelPose SITTING = buildPose(
            Map.of(
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-45)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-45))
            ),
            Map.of()
    );

    /** Arms stretched forward (skydiving / superman). */
    public static final PlayerModelPose ARMS_FORWARD = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90))
            ),
            Map.of()
    );

    /** Ejecting from plane: arms raised up, legs dangling loosely. */
    public static final PlayerModelPose EJECTING = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-170)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-170)),
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(15)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(10))
            ),
            Map.of()
    );

    /** Holding parachute lines: arms up and slightly inward, legs hanging naturally. */
    public static final PlayerModelPose PARACHUTING = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-160)).rotateZ((float) Math.toRadians(-10)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-160)).rotateZ((float) Math.toRadians(10)),
                    "LEFT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(8)),
                    "RIGHT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(-8))
            ),
            Map.of()
    );

    /**
     * Build a pose from logical-group-level rotations and offsets.
     * Automatically expands to both sub-parts of each limb.
     */
    private static PlayerModelPose buildPose(
            Map<String, Quaternionf> groupRotations,
            Map<String, Vector3f> groupOffsets) {

        EnumMap<BodyPart, Quaternionf> rotations = new EnumMap<>(BodyPart.class);
        EnumMap<BodyPart, Vector3f> offsets = new EnumMap<>(BodyPart.class);

        for (BodyPart part : BodyPart.values()) {
            Quaternionf rot = groupRotations.get(part.logicalGroup());
            if (rot != null) rotations.put(part, new Quaternionf(rot));

            Vector3f off = groupOffsets.get(part.logicalGroup());
            if (off != null) offsets.put(part, new Vector3f(off));
        }

        return new PlayerModelPose(rotations, offsets);
    }
}
