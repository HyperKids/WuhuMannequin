package com.wuhumannequin.model;

/**
 * A time-varying pose. Implementations return the pose to apply at a given animation tick
 * (0-based, 20 ticks/sec). Animations are stateless — the tick counter is supplied by the
 * caller, so the same animation can drive multiple models independently.
 */
@FunctionalInterface
public interface PlayerModelAnimation {
    /**
     * @param tick ticks elapsed since the animation started (0 on the first frame)
     * @return the pose to apply this frame
     */
    PlayerModelPose poseAt(long tick);
}
