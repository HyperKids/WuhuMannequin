package com.wuhumannequin.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Pre-defined poses for the segmented player model.
 */
public final class PlayerModelPoses {
    private PlayerModelPoses() {}

    /** Arms at sides, legs straight down. */
    public static final PlayerModelPose STANDING = PlayerModelPose.IDENTITY;

    /** Arms extended horizontally. */
    public static final PlayerModelPose T_POSE = new PlayerModelPose(
            Map.of(
                    BodyPart.LEFT_ARM, new Quaternionf().rotateZ((float) Math.toRadians(90)),
                    BodyPart.RIGHT_ARM, new Quaternionf().rotateZ((float) Math.toRadians(-90))
            ),
            Map.of()
    );

    /** Seated: legs bent 90 degrees forward, arms angled forward on controls. */
    public static final PlayerModelPose SITTING = new PlayerModelPose(
            Map.of(
                    BodyPart.LEFT_LEG, new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    BodyPart.RIGHT_LEG, new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    BodyPart.LEFT_ARM, new Quaternionf().rotateX((float) Math.toRadians(-45)),
                    BodyPart.RIGHT_ARM, new Quaternionf().rotateX((float) Math.toRadians(-45))
            ),
            Map.of(
                    // Legs shift forward + up when bent 90° at the hip
                    // Up by half visual leg height, forward by same
                    BodyPart.LEFT_LEG, new Vector3f(0, 0.375f, 0.375f),
                    BodyPart.RIGHT_LEG, new Vector3f(0, 0.375f, 0.375f)
            )
    );

    /** Arms stretched forward (skydiving / superman). */
    public static final PlayerModelPose ARMS_FORWARD = new PlayerModelPose(
            Map.of(
                    BodyPart.LEFT_ARM, new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    BodyPart.RIGHT_ARM, new Quaternionf().rotateX((float) Math.toRadians(-90))
            ),
            Map.of()
    );
}
