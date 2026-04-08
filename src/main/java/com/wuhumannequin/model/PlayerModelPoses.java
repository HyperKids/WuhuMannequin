package com.wuhumannequin.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-defined poses for the segmented player model.
 * Poses are defined per logical limb — both sub-parts of a limb share the same rotation.
 *
 * <p>Coordinate notes (model faces +Z / south at identity):
 * <ul>
 *   <li>Left arm/leg are on +X (east), right on -X (west).</li>
 *   <li>{@code rotateZ(+a)} on a left limb raises it outward (away from body); negative does the
 *       same for a right limb.</li>
 *   <li>{@code rotateX(-a)} swings a limb forward (+Z); positive swings it backward.</li>
 *   <li>Limbs are rigid — there are no elbow/knee joints, so "bent" poses are approximated
 *       with shoulder/hip rotations only.</li>
 * </ul>
 *
 * <p>The {@link #ALL} map registers every named pose for command lookup; new poses added here
 * automatically appear in {@code /mannequin pose}.
 */
public final class PlayerModelPoses {
    private PlayerModelPoses() {}

    // ── Existing core poses ─────────────────────────────────────────────────

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

    // ── Greetings & gestures ────────────────────────────────────────────────

    /** Right arm raised overhead in a "hello" wave. */
    public static final PlayerModelPose WAVING = buildPose(
            Map.of(
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-160))
            ),
            Map.of()
    );

    /** Right hand to forehead salute (rigid arm pointed up-and-inward). */
    public static final PlayerModelPose SALUTING = buildPose(
            Map.of(
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-130))
            ),
            Map.of()
    );

    /** Right arm extended straight forward, pointing. */
    public static final PlayerModelPose POINTING = buildPose(
            Map.of(
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90))
            ),
            Map.of()
    );

    /** Both arms straight up. */
    public static final PlayerModelPose HANDS_UP = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(180)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-180))
            ),
            Map.of()
    );

    /** Both arms raised in a celebratory V. */
    public static final PlayerModelPose CHEER = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(160)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-160))
            ),
            Map.of()
    );

    /** Hands-on-hips approximation: arms angled inward and slightly forward. */
    public static final PlayerModelPose HANDS_ON_HIPS = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(25))
                            .rotateX((float) Math.toRadians(-20)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(-25))
                            .rotateX((float) Math.toRadians(-20))
            ),
            Map.of()
    );

    /** Arms folded across chest (approximation — rigid arms can't actually overlap). */
    public static final PlayerModelPose CROSSED_ARMS = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-75))
                            .rotateZ((float) Math.toRadians(35)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-75))
                            .rotateZ((float) Math.toRadians(-35))
            ),
            Map.of()
    );

    /** Strongman flex: both arms angled up and out at the sides. */
    public static final PlayerModelPose FLEXING = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(120)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-120))
            ),
            Map.of()
    );

    /** Zombie shamble: arms straight forward, legs slightly bent. */
    public static final PlayerModelPose ZOMBIE = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-15)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-15))
            ),
            Map.of()
    );

    /**
     * Dab: right arm comes across the body just above horizontal, left arm extends
     * diagonally up-and-out, and the head turns and tilts down into the right elbow.
     * (Rigid arms can't actually bend at the elbow, so the bicep + face tuck do the work.)
     */
    public static final PlayerModelPose DAB = buildPose(
            Map.of(
                    // +Z rotation on the right arm carries it ACROSS the body (toward +X).
                    // ~105° = just above horizontal, slight forward tilt.
                    "RIGHT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(105))
                            .rotateX((float) Math.toRadians(-15)),
                    // Left arm raised out and up to ~45° above horizontal.
                    "LEFT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(135))
                            .rotateX((float) Math.toRadians(-10)),
                    // Head turned toward the right arm (rotateY+) and tilted into it
                    // (rotateZ-), with a slight downward tuck (rotateX+).
                    "HEAD", new Quaternionf()
                            .rotateY((float) Math.toRadians(40))
                            .rotateZ((float) Math.toRadians(-25))
                            .rotateX((float) Math.toRadians(15))
            ),
            Map.of()
    );

    /** Open-arm hug: both arms forward and angled inward. */
    public static final PlayerModelPose HUG = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-80))
                            .rotateZ((float) Math.toRadians(25)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-80))
                            .rotateZ((float) Math.toRadians(-25))
            ),
            Map.of()
    );

    /** Hands clasped forward in prayer. */
    public static final PlayerModelPose PRAYER = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-90))
                            .rotateZ((float) Math.toRadians(15)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-90))
                            .rotateZ((float) Math.toRadians(-15))
            ),
            Map.of()
    );

    // ── Combat & athletic ───────────────────────────────────────────────────

    /** High kick: right leg extended high forward. */
    public static final PlayerModelPose HIGH_KICK = buildPose(
            Map.of(
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-110)),
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(60)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-60))
            ),
            Map.of()
    );

    /** Front kick: right leg horizontal forward. */
    public static final PlayerModelPose FRONT_KICK = buildPose(
            Map.of(
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90))
            ),
            Map.of()
    );

    /** Side splits: legs spread 90° outward. */
    public static final PlayerModelPose SPLITS = buildPose(
            Map.of(
                    "LEFT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(85)),
                    "RIGHT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(-85))
            ),
            Map.of()
    );

    /** Ballet arabesque: left leg extended back, arms out to sides. */
    public static final PlayerModelPose BALLET = buildPose(
            Map.of(
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(60)),
                    "LEFT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(70)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-70))
            ),
            Map.of()
    );

    /** Archer drawing a bow: left arm forward, right arm pulled back. */
    public static final PlayerModelPose ARCHER = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-60))
                            .rotateZ((float) Math.toRadians(-40))
            ),
            Map.of()
    );

    /** Mid-stride run pose: left arm/right leg forward, right arm/left leg back. */
    public static final PlayerModelPose RUNNING = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-55)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(55)),
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(45)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-45))
            ),
            Map.of()
    );

    /** Mid-stride walk pose (gentler than run). */
    public static final PlayerModelPose WALKING = buildPose(
            Map.of(
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-25)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(25)),
                    "LEFT_LEG", new Quaternionf().rotateX((float) Math.toRadians(20)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-20))
            ),
            Map.of()
    );

    // ── Head poses ──────────────────────────────────────────────────────────

    /** Head tilted to the right. */
    public static final PlayerModelPose HEAD_TILT = buildPose(
            Map.of(
                    "HEAD", new Quaternionf().rotateZ((float) Math.toRadians(-20))
            ),
            Map.of()
    );

    /** Head tilted back to look up at the sky. */
    public static final PlayerModelPose LOOK_UP = buildPose(
            Map.of(
                    "HEAD", new Quaternionf().rotateX((float) Math.toRadians(-30))
            ),
            Map.of()
    );

    /** Head tilted forward to look down. */
    public static final PlayerModelPose LOOK_DOWN = buildPose(
            Map.of(
                    "HEAD", new Quaternionf().rotateX((float) Math.toRadians(30))
            ),
            Map.of()
    );

    /** Defeated slump: head down, arms hanging slightly forward. */
    public static final PlayerModelPose DEFEAT = buildPose(
            Map.of(
                    "HEAD", new Quaternionf().rotateX((float) Math.toRadians(35)),
                    "LEFT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-15)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-15))
            ),
            Map.of()
    );

    /** "Stayin' Alive" disco point: right arm up-and-across, left arm down-and-out. */
    public static final PlayerModelPose DISCO = buildPose(
            Map.of(
                    "RIGHT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(-145))
                            .rotateX((float) Math.toRadians(-25)),
                    "LEFT_ARM", new Quaternionf()
                            .rotateZ((float) Math.toRadians(35))
                            .rotateX((float) Math.toRadians(20))
            ),
            Map.of()
    );

    // ── BEND_* poses (use the elbow/knee chain) ────────────────────────────
    // Local-rotation reminder for limbs:
    //   shoulder rotation is in body frame
    //   elbow rotation is in the upper segment's local frame (after the shoulder)
    //   wrist rotation is in the forearm's local frame (after the elbow)
    // Identity at elbow + wrist gives a rigid limb. Combined orientation is
    // R_shoulder × R_elbow × R_wrist.

    /** Right arm raised in an L-shape: upper horizontal out, forearm bent straight up. */
    public static final PlayerModelPose BEND_WAVE = buildBendPose(
            Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))),
            Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90)))
    );

    /** Classic double-bicep flex: upper arms horizontal out, forearms bent straight up. */
    public static final PlayerModelPose BEND_FLEX = buildBendPose(
            Map.of(
                    "LEFT_ARM",  new Quaternionf().rotateZ((float) Math.toRadians(90)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))
            ),
            Map.of(
                    "LEFT_ARM",  new Quaternionf().rotateZ((float) Math.toRadians(90)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))
            )
    );

    /**
     * Salute: right upper arm horizontal out + slightly forward, forearm bent up and
     * angled inward toward the forehead.
     */
    public static final PlayerModelPose BEND_SALUTE = buildBendPose(
            Map.of("RIGHT_ARM", new Quaternionf()
                    .rotateZ((float) Math.toRadians(-80))
                    .rotateX((float) Math.toRadians(-25))),
            Map.of("RIGHT_ARM", new Quaternionf()
                    .rotateZ((float) Math.toRadians(-110)))
    );

    /**
     * "Thinking" pose: right arm hanging at the side with the forearm bent forward and
     * up so the hand approaches the chin.
     */
    public static final PlayerModelPose BEND_THINKING = buildBendPose(
            Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-15))),
            Map.of("RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-150)))
    );

    /** Both arms folded across the chest. */
    public static final PlayerModelPose BEND_CROSSED_ARMS = buildBendPose(
            Map.of(
                    "LEFT_ARM",  new Quaternionf()
                            .rotateX((float) Math.toRadians(-80))
                            .rotateZ((float) Math.toRadians(20)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-80))
                            .rotateZ((float) Math.toRadians(-20))
            ),
            Map.of(
                    "LEFT_ARM",  new Quaternionf().rotateZ((float) Math.toRadians(-80)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(80))
            )
    );

    /**
     * Hands on hips: upper arms slightly out, forearms bent forward and angled in toward
     * the body so the hands rest on the hips.
     */
    public static final PlayerModelPose BEND_HANDS_ON_HIPS = buildBendPose(
            Map.of(
                    "LEFT_ARM",  new Quaternionf().rotateZ((float) Math.toRadians(35)),
                    "RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-35))
            ),
            Map.of(
                    "LEFT_ARM",  new Quaternionf()
                            .rotateX((float) Math.toRadians(-90))
                            .rotateY((float) Math.toRadians(-30)),
                    "RIGHT_ARM", new Quaternionf()
                            .rotateX((float) Math.toRadians(-90))
                            .rotateY((float) Math.toRadians(30))
            )
    );

    /**
     * Proper sitting pose: thighs horizontal forward (rotate at hip), calves straight
     * down (bent at knee). Replaces the rigid {@link #SITTING} for a natural look.
     */
    public static final PlayerModelPose BEND_SITTING = buildBendPose(
            Map.of(
                    "LEFT_LEG",  new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90))
            ),
            Map.of(
                    "LEFT_LEG",  new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90))
            )
    );

    /**
     * Kneeling on the right knee: right thigh horizontal forward, right calf straight
     * down. Left leg stays standing.
     */
    public static final PlayerModelPose BEND_KNEEL = buildBendPose(
            Map.of("RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90))),
            Map.of("RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(-90)))
    );

    /**
     * Mid-stride run with bent knees: front leg lifted with the knee tucked, back leg
     * extended. Both forearms bent forward in running form.
     */
    public static final PlayerModelPose BEND_RUNNING = buildBendPose(
            Map.of(
                    "LEFT_LEG",  new Quaternionf().rotateX((float) Math.toRadians(-50)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(30)),
                    "LEFT_ARM",  new Quaternionf().rotateX((float) Math.toRadians(-30)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(40))
            ),
            Map.of(
                    "LEFT_LEG",  new Quaternionf().rotateX((float) Math.toRadians(-60)),
                    "RIGHT_LEG", new Quaternionf().rotateX((float) Math.toRadians(0)),
                    "LEFT_ARM",  new Quaternionf().rotateX((float) Math.toRadians(-90)),
                    "RIGHT_ARM", new Quaternionf().rotateX((float) Math.toRadians(-90))
            )
    );

    // ── Registry ────────────────────────────────────────────────────────────

    /** Insertion-ordered map of all named poses (used by commands). */
    public static final Map<String, PlayerModelPose> ALL;
    static {
        Map<String, PlayerModelPose> m = new LinkedHashMap<>();
        m.put("standing", STANDING);
        m.put("tpose", T_POSE);
        m.put("xpose", X_POSE);
        m.put("sitting", SITTING);
        m.put("arms_forward", ARMS_FORWARD);
        m.put("ejecting", EJECTING);
        m.put("parachuting", PARACHUTING);
        m.put("waving", WAVING);
        m.put("saluting", SALUTING);
        m.put("pointing", POINTING);
        m.put("hands_up", HANDS_UP);
        m.put("cheer", CHEER);
        m.put("hands_on_hips", HANDS_ON_HIPS);
        m.put("crossed_arms", CROSSED_ARMS);
        m.put("flexing", FLEXING);
        m.put("zombie", ZOMBIE);
        m.put("dab", DAB);
        m.put("hug", HUG);
        m.put("prayer", PRAYER);
        m.put("high_kick", HIGH_KICK);
        m.put("front_kick", FRONT_KICK);
        m.put("splits", SPLITS);
        m.put("ballet", BALLET);
        m.put("archer", ARCHER);
        m.put("running", RUNNING);
        m.put("walking", WALKING);
        m.put("head_tilt", HEAD_TILT);
        m.put("look_up", LOOK_UP);
        m.put("look_down", LOOK_DOWN);
        m.put("defeat", DEFEAT);
        m.put("disco", DISCO);
        m.put("bend_wave", BEND_WAVE);
        m.put("bend_flex", BEND_FLEX);
        m.put("bend_salute", BEND_SALUTE);
        m.put("bend_thinking", BEND_THINKING);
        m.put("bend_crossed_arms", BEND_CROSSED_ARMS);
        m.put("bend_hands_on_hips", BEND_HANDS_ON_HIPS);
        m.put("bend_sitting", BEND_SITTING);
        m.put("bend_kneel", BEND_KNEEL);
        m.put("bend_running", BEND_RUNNING);
        ALL = java.util.Collections.unmodifiableMap(m);
    }

    /**
     * Build a rigid-limb pose from logical-group rotations.
     *
     * <p>For limb groups (LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG) the rotation is applied
     * only to the UPPER sub-part (the shoulder/hip joint). The hierarchical compute in
     * {@link PlayerModel} propagates the orientation down the chain — with identity at the
     * elbow and wrist the limb behaves as a single rigid block.
     *
     * <p>For HEAD and TORSO groups the rotation is applied to every matching sub-part
     * directly (those parts use the simple, non-chained transform path).
     *
     * <p>Use {@link #buildBendPose} when you actually want to bend at the elbow/knee.
     *
     * <p>Public so {@link PlayerModelAnimation} implementations can construct per-frame poses
     * with the same vocabulary as the static constants here.
     */
    public static PlayerModelPose buildPose(
            Map<String, Quaternionf> groupRotations,
            Map<String, Vector3f> groupOffsets) {

        EnumMap<BodyPart, Quaternionf> rotations = new EnumMap<>(BodyPart.class);
        EnumMap<BodyPart, Vector3f> offsets = new EnumMap<>(BodyPart.class);

        for (BodyPart part : BodyPart.values()) {
            String group = part.logicalGroup();
            Quaternionf rot = groupRotations.get(group);
            if (rot != null) {
                if (isLimbGroup(group)) {
                    // Rigid limb: only the shoulder gets the rotation; elbow/wrist stay
                    // identity so the chain produces a straight, fully-rotated limb.
                    if (part.name().endsWith("_UPPER")) {
                        rotations.put(part, new Quaternionf(rot));
                    }
                } else {
                    // HEAD / TORSO: every sub-part rotates independently around its own pivot.
                    rotations.put(part, new Quaternionf(rot));
                }
            }

            Vector3f off = groupOffsets.get(group);
            if (off != null) {
                if (isLimbGroup(group)) {
                    if (part.name().endsWith("_UPPER")) {
                        offsets.put(part, new Vector3f(off));
                    }
                } else {
                    offsets.put(part, new Vector3f(off));
                }
            }
        }

        return new PlayerModelPose(rotations, offsets);
    }

    /**
     * Build a pose that bends at the elbow/knee (and optionally the wrist/ankle).
     *
     * <p>Each map is keyed by logical limb group (LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG).
     * Rotations are <em>local</em> to their joint:
     * <ul>
     *   <li>{@code shoulders} — applied to the UPPER segment in body frame</li>
     *   <li>{@code elbows} — applied at the elbow/knee joint, in the upper segment's local frame</li>
     *   <li>{@code wrists} — applied at the wrist/ankle joint, in the forearm's local frame</li>
     * </ul>
     * Each segment's cumulative orientation is the product of its parents'. Identity at any
     * joint leaves that joint straight. Pass {@code Map.of()} for joints you don't need.
     *
     * <p>Example — right arm bent up at the elbow (forearm vertical, upper arm horizontal):
     * <pre>{@code
     * buildBendPose(
     *     Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))),
     *     Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))),
     *     Map.of(),
     *     Map.of()
     * );
     * }</pre>
     */
    public static PlayerModelPose buildBendPose(
            Map<String, Quaternionf> shoulders,
            Map<String, Quaternionf> elbows,
            Map<String, Quaternionf> wrists,
            Map<String, Vector3f> groupOffsets) {

        EnumMap<BodyPart, Quaternionf> rotations = new EnumMap<>(BodyPart.class);
        EnumMap<BodyPart, Vector3f> offsets = new EnumMap<>(BodyPart.class);

        for (BodyPart part : BodyPart.values()) {
            String group = part.logicalGroup();
            if (!isLimbGroup(group)) continue;

            String suffix = part.name().substring(group.length() + 1); // UPPER / MIDDLE / LOWER
            Quaternionf rot = switch (suffix) {
                case "UPPER" -> shoulders.get(group);
                case "MIDDLE" -> elbows.get(group);
                case "LOWER" -> wrists.get(group);
                default -> null;
            };
            if (rot != null) rotations.put(part, new Quaternionf(rot));

            // Offset adjustments only meaningful on UPPER (shifts the whole limb).
            if (suffix.equals("UPPER")) {
                Vector3f off = groupOffsets.get(group);
                if (off != null) offsets.put(part, new Vector3f(off));
            }
        }

        return new PlayerModelPose(rotations, offsets);
    }

    /** Convenience: bend at the elbow only, no wrist or offsets. */
    public static PlayerModelPose buildBendPose(
            Map<String, Quaternionf> shoulders,
            Map<String, Quaternionf> elbows) {
        return buildBendPose(shoulders, elbows, Map.of(), Map.of());
    }

    private static boolean isLimbGroup(String group) {
        return group.equals("LEFT_ARM") || group.equals("RIGHT_ARM")
                || group.equals("LEFT_LEG") || group.equals("RIGHT_LEG");
    }
}
