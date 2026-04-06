package com.wuhumannequin.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Immutable snapshot of per-limb rotations and offset adjustments that define a pose.
 * Limbs not present in the maps use identity rotation and zero offset adjustment.
 */
public record PlayerModelPose(
        Map<BodyPart, Quaternionf> rotations,
        Map<BodyPart, Vector3f> offsetAdjustments
) {
    public static final PlayerModelPose IDENTITY = new PlayerModelPose(Map.of(), Map.of());

    public Quaternionf getRotation(BodyPart part) {
        Quaternionf r = rotations.get(part);
        return r != null ? new Quaternionf(r) : new Quaternionf();
    }

    public Vector3f getOffsetAdjustment(BodyPart part) {
        Vector3f v = offsetAdjustments.get(part);
        return v != null ? new Vector3f(v) : new Vector3f();
    }
}
