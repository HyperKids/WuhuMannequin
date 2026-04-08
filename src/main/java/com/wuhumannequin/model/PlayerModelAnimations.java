package com.wuhumannequin.model;

import org.joml.Quaternionf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-defined per-tick animations for the segmented player model.
 *
 * <p>Each animation is a pure function of the tick counter, so they're cheap to add and easy
 * to share between models. Most animations are sinusoidal: smooth, loopable, no keyframe state.
 *
 * <p>The {@link #ALL} map registers every named animation for command lookup; new animations
 * added here automatically appear in {@code /mannequin animation}.
 */
public final class PlayerModelAnimations {
    private PlayerModelAnimations() {}

    private static float rad(double deg) {
        return (float) Math.toRadians(deg);
    }

    /** Right arm raised overhead, hand swinging side to side. */
    public static final PlayerModelAnimation WAVE = tick -> {
        double t = tick * 0.35;
        float swing = (float) (Math.sin(t) * 25); // ±25° about Z
        return PlayerModelPoses.buildPose(
                Map.of(
                        "RIGHT_ARM", new Quaternionf()
                                .rotateZ(rad(-160))
                                .rotateY(rad(swing))
                ),
                Map.of()
        );
    };

    /** Sinusoidal jumping-jack: oscillates between T-pose and X-pose. */
    public static final PlayerModelAnimation JUMPING_JACKS = tick -> {
        double t = tick * 0.30;
        float f = (float) ((Math.sin(t) + 1) / 2); // 0 → 1
        float armAngle = 90 + 45 * f;              // T (90) → X (135)
        float legAngle = 45 * f;                   // 0 → 45
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateZ(rad(armAngle)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(-armAngle)),
                        "LEFT_LEG", new Quaternionf().rotateZ(rad(legAngle)),
                        "RIGHT_LEG", new Quaternionf().rotateZ(rad(-legAngle))
                ),
                Map.of()
        );
    };

    /** Cycling stride with strong arm/leg swings. */
    public static final PlayerModelAnimation RUNNING_CYCLE = tick -> {
        double t = tick * 0.55;
        float swing = (float) (Math.sin(t) * 55);
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateX(rad(-swing)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(swing)),
                        "LEFT_LEG", new Quaternionf().rotateX(rad(swing)),
                        "RIGHT_LEG", new Quaternionf().rotateX(rad(-swing))
                ),
                Map.of()
        );
    };

    /** Gentler stride for walking. */
    public static final PlayerModelAnimation WALKING_CYCLE = tick -> {
        double t = tick * 0.30;
        float swing = (float) (Math.sin(t) * 28);
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateX(rad(-swing)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(swing)),
                        "LEFT_LEG", new Quaternionf().rotateX(rad(swing)),
                        "RIGHT_LEG", new Quaternionf().rotateX(rad(-swing))
                ),
                Map.of()
        );
    };

    /** Hands meet in front, then pull apart, repeating. */
    public static final PlayerModelAnimation CLAPPING = tick -> {
        double t = tick * 0.45;
        float openness = (float) ((Math.sin(t) + 1) / 2); // 0 = clapped, 1 = open
        float zSpread = 5 + 30 * openness;                // 5° → 35° outward
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf()
                                .rotateX(rad(-90))
                                .rotateZ(rad(zSpread)),
                        "RIGHT_ARM", new Quaternionf()
                                .rotateX(rad(-90))
                                .rotateZ(rad(-zSpread))
                ),
                Map.of()
        );
    };

    /** Alternating straight-arm punches. */
    public static final PlayerModelAnimation PUNCHING = tick -> {
        double t = tick * 0.5;
        // Both arms cosine-driven, 180° out of phase
        float left = (float) ((Math.cos(t) + 1) / 2);   // 0 → 1
        float right = 1 - left;
        // 0 = retracted (rotateX -30, rotateZ 25 in), 1 = extended forward (rotateX -90)
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf()
                                .rotateX(rad(-30 - 60 * left))
                                .rotateZ(rad(25 - 25 * left)),
                        "RIGHT_ARM", new Quaternionf()
                                .rotateX(rad(-30 - 60 * right))
                                .rotateZ(rad(-25 + 25 * right))
                ),
                Map.of()
        );
    };

    /** Front-crawl swimming: alternating overhead arm strokes. */
    public static final PlayerModelAnimation SWIMMING = tick -> {
        double t = tick * 0.4;
        // Each arm rotates around X (full circles) but offset 180°.
        float leftAngle = (float) (Math.toDegrees(t) % 360);
        float rightAngle = (float) ((Math.toDegrees(t) + 180) % 360);
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateX(rad(-leftAngle)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(-rightAngle))
                ),
                Map.of()
        );
    };

    /** Chicken flap: arms out to sides, oscillating up and down. */
    public static final PlayerModelAnimation CHICKEN = tick -> {
        double t = tick * 0.45;
        float flap = (float) (Math.sin(t) * 30); // ±30° about T-pose
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateZ(rad(90 + flap)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(-90 - flap))
                ),
                Map.of()
        );
    };

    /** Head nodding "yes". */
    public static final PlayerModelAnimation NODDING = tick -> {
        double t = tick * 0.5;
        float nod = (float) (Math.sin(t) * 20); // ±20° about X
        return PlayerModelPoses.buildPose(
                Map.of(
                        "HEAD", new Quaternionf().rotateX(rad(nod))
                ),
                Map.of()
        );
    };

    /** Head shaking "no". */
    public static final PlayerModelAnimation SHAKING_HEAD = tick -> {
        double t = tick * 0.5;
        float shake = (float) (Math.sin(t) * 25); // ±25° about Y
        return PlayerModelPoses.buildPose(
                Map.of(
                        "HEAD", new Quaternionf().rotateY(rad(shake))
                ),
                Map.of()
        );
    };

    /** Subtle idle breathing: arms gently sway in/out. */
    public static final PlayerModelAnimation IDLE_BREATHE = tick -> {
        double t = tick * 0.12;
        float sway = (float) (3 + Math.sin(t) * 3); // 0° → 6° outward
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateZ(rad(sway)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(-sway))
                ),
                Map.of()
        );
    };

    /** Free-form dance: combined arm wave + hip sway + leg shuffle. */
    public static final PlayerModelAnimation DANCING = tick -> {
        double t = tick * 0.35;
        float armUp = (float) (60 + Math.sin(t) * 40);          // 20° → 100° outward
        float armDown = (float) (60 + Math.sin(t + Math.PI) * 40);
        float legSway = (float) (Math.sin(t * 0.5) * 15);        // ±15°
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_ARM", new Quaternionf().rotateZ(rad(armUp)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(-armDown)),
                        "LEFT_LEG", new Quaternionf().rotateZ(rad(legSway)),
                        "RIGHT_LEG", new Quaternionf().rotateZ(rad(-legSway))
                ),
                Map.of()
        );
    };

    /** Alternating front kicks. */
    public static final PlayerModelAnimation KICKING = tick -> {
        double t = tick * 0.5;
        float left = (float) ((Math.cos(t) + 1) / 2);
        float right = 1 - left;
        return PlayerModelPoses.buildPose(
                Map.of(
                        "LEFT_LEG", new Quaternionf().rotateX(rad(-100 * left)),
                        "RIGHT_LEG", new Quaternionf().rotateX(rad(-100 * right))
                ),
                Map.of()
        );
    };

    // ── BEND_* animations (use the elbow/knee chain) ──────────────────────

    /**
     * Right arm raised in an L; the forearm waves left/right at the elbow without the
     * upper arm moving. This is the canonical "bendable wave" — the rigid {@link #WAVE}
     * has to fake the same effect by rotating the whole arm.
     */
    public static final PlayerModelAnimation BEND_WAVE = tick -> {
        double t = tick * 0.4;
        // Wave the forearm by adding a Y-axis rotation to the elbow joint. The elbow's
        // local frame has Y as the upper-arm axis, so rotateY swings the forearm side
        // to side without changing the upper arm.
        float swing = (float) (Math.sin(t) * 30); // ±30°
        return PlayerModelPoses.buildBendPose(
                Map.of("RIGHT_ARM", new Quaternionf().rotateZ(rad(-90))),
                Map.of("RIGHT_ARM", new Quaternionf()
                        .rotateZ(rad(-90))
                        .rotateY(rad(swing)))
        );
    };

    /**
     * Mid-stride run with bent knees and elbows. Both legs swing around the hip and the
     * forward leg's knee tucks; both arms swing opposite the legs with the forearm bent.
     */
    public static final PlayerModelAnimation BEND_RUNNING = tick -> {
        double t = tick * 0.5;
        float hipSwing = (float) (Math.sin(t) * 50);          // ±50° at hip
        float legPhase = (float) ((Math.sin(t) + 1) / 2);     // 0..1 (front leg phase)
        float frontKneeBend = -60 * legPhase;                 // front leg knee bends ~60°
        float backKneeBend = -60 * (1 - legPhase);            // alternating
        float armSwing = (float) (Math.sin(t) * 35);          // ±35°
        return PlayerModelPoses.buildBendPose(
                Map.of(
                        "LEFT_LEG",  new Quaternionf().rotateX(rad(-hipSwing)),
                        "RIGHT_LEG", new Quaternionf().rotateX(rad(hipSwing)),
                        "LEFT_ARM",  new Quaternionf().rotateX(rad(armSwing)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(-armSwing))
                ),
                Map.of(
                        // hipSwing<0 → left leg back, so left knee bends more on backswing
                        "LEFT_LEG",  new Quaternionf().rotateX(rad(backKneeBend)),
                        "RIGHT_LEG", new Quaternionf().rotateX(rad(frontKneeBend)),
                        "LEFT_ARM",  new Quaternionf().rotateX(rad(-90)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(-90))
                )
        );
    };

    /**
     * Double-bicep flex with the biceps "pumping" — the forearm bend angle oscillates
     * a few degrees in/out so the silhouette flexes rhythmically.
     */
    public static final PlayerModelAnimation BEND_FLEX_PUMP = tick -> {
        double t = tick * 0.35;
        float pump = (float) (Math.sin(t) * 12); // ±12° around the 90° elbow bend
        float elbowL = 90 + pump;
        float elbowR = -(90 + pump);
        return PlayerModelPoses.buildBendPose(
                Map.of(
                        "LEFT_ARM",  new Quaternionf().rotateZ(rad(90)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(-90))
                ),
                Map.of(
                        "LEFT_ARM",  new Quaternionf().rotateZ(rad(elbowL)),
                        "RIGHT_ARM", new Quaternionf().rotateZ(rad(elbowR))
                )
        );
    };

    /**
     * Alternating boxing punches with windup: the elbow bends to retract, then extends
     * sharply forward. Looks like a real punch instead of a stiff arm-swing.
     */
    public static final PlayerModelAnimation BEND_PUNCHING = tick -> {
        double t = tick * 0.45;
        float left = (float) ((Math.cos(t) + 1) / 2);   // 0=retracted, 1=extended
        float right = 1 - left;
        // Upper arm angle: at retract the upper arm hangs slightly back; at extend it
        // rotates forward.
        float upperL = -90 * left;  // 0° → -90° (forward)
        float upperR = -90 * right;
        // Elbow bend: retracted = bent forearm (~-100°), extended = nearly straight (0°).
        float elbowL = -100 * (1 - left);
        float elbowR = -100 * (1 - right);
        return PlayerModelPoses.buildBendPose(
                Map.of(
                        "LEFT_ARM",  new Quaternionf().rotateX(rad(upperL)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(upperR))
                ),
                Map.of(
                        "LEFT_ARM",  new Quaternionf().rotateX(rad(elbowL)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(elbowR))
                )
        );
    };

    /** Bow forward and rise (using head + arm tilt to imply torso bend). */
    public static final PlayerModelAnimation BOWING = tick -> {
        double t = tick * 0.18;
        float bow = (float) ((Math.sin(t - Math.PI / 2) + 1) / 2); // 0 → 1 → 0
        return PlayerModelPoses.buildPose(
                Map.of(
                        "HEAD", new Quaternionf().rotateX(rad(45 * bow)),
                        "LEFT_ARM", new Quaternionf().rotateX(rad(-30 * bow)),
                        "RIGHT_ARM", new Quaternionf().rotateX(rad(-30 * bow))
                ),
                Map.of()
        );
    };

    // ── Registry ────────────────────────────────────────────────────────────

    /** Insertion-ordered map of all named animations (used by commands). */
    public static final Map<String, PlayerModelAnimation> ALL;
    static {
        Map<String, PlayerModelAnimation> m = new LinkedHashMap<>();
        m.put("wave", WAVE);
        m.put("jumping_jacks", JUMPING_JACKS);
        m.put("running", RUNNING_CYCLE);
        m.put("walking", WALKING_CYCLE);
        m.put("clapping", CLAPPING);
        m.put("punching", PUNCHING);
        m.put("swimming", SWIMMING);
        m.put("chicken", CHICKEN);
        m.put("nodding", NODDING);
        m.put("shaking_head", SHAKING_HEAD);
        m.put("idle", IDLE_BREATHE);
        m.put("dancing", DANCING);
        m.put("kicking", KICKING);
        m.put("bowing", BOWING);
        m.put("bend_wave", BEND_WAVE);
        m.put("bend_running", BEND_RUNNING);
        m.put("bend_flex_pump", BEND_FLEX_PUMP);
        m.put("bend_punching", BEND_PUNCHING);
        ALL = Collections.unmodifiableMap(m);
    }
}
