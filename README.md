# WuhuMannequin

A Paper 1.21+ plugin that renders segmented player models using `ItemDisplay` entities. Each
limb and the torso are split into pixel-perfect 4×4×4 thirds, and every sub-part is its own
display entity, so the model can be rotated freely on **all** axes (yaw, pitch, **roll**) —
unlike vanilla armor stands or `Mannequin` entities, which are always rendered upright.

The plugin doubles as a Java API: other Paper plugins (e.g. WuhuFlyover) depend on it directly
to render player-skinned mannequins for in-world entities like vehicles, NPCs, or cutscenes.

---

## Table of contents

1. [Features](#features)
2. [Building & installing](#building--installing)
3. [Configuration](#configuration)
4. [Debug commands](#debug-commands)
5. [Integrating from another plugin](#integrating-from-another-plugin)
   - [Adding the dependency](#adding-the-dependency)
   - [Spawning a model](#spawning-a-model)
   - [Updating per tick](#updating-per-tick)
   - [Despawning](#despawning)
6. [Pose system](#pose-system)
   - [Built-in poses](#built-in-poses)
   - [Custom poses](#custom-poses)
   - [Coordinate system](#coordinate-system)
7. [Animation system](#animation-system)
   - [Built-in animations](#built-in-animations)
   - [Custom animations](#custom-animations)
   - [Driving animations from your plugin](#driving-animations-from-your-plugin)
8. [Skin loading](#skin-loading)
9. [Architecture](#architecture)
10. [Limitations & future work](#limitations--future-work)

---

## Features

- **16-part segmented model** — head + torso(×3) + each arm(×3) + each leg(×3). Each segment
  is an `ItemDisplay` so the texture mapping is pixel-perfect (one 4×4×4 cube per third) and
  every sub-part can be rotated independently.
- **Full 3D rotation** — yaw, pitch, and roll on the whole body or any individual limb.
- **Pivot-correct joints** — limbs rotate around shoulders/hips, not their geometric centers.
- **Pose system** — 40 built-in static poses (`STANDING`, `T_POSE`, `SITTING`, `WAVING`,
  `DAB`, `DISCO`, plus 9 `BEND_*` variants that bend at the elbow/knee) and a
  `PlayerModelPose` builder for custom shapes.
- **Bendable limbs** — limb segments form a hierarchical chain (shoulder → elbow → wrist /
  hip → knee → ankle) so you can actually bend the elbow or knee, not just rotate the
  whole limb. Rigid poses still work; the `BEND_*` constants and `buildBendPose` helper
  opt into bending.
- **Animation system** — 18 built-in tick-driven animations (`WAVE`, `RUNNING_CYCLE`,
  `JUMPING_JACKS`, `DANCING`, `BOWING`, `SWIMMING`, plus `BEND_WAVE`, `BEND_RUNNING`,
  `BEND_FLEX_PUMP`, `BEND_PUNCHING`) and a `PlayerModelAnimation` functional interface
  for custom ones.
- **Smooth motion** — display entities use `setTeleportDuration()` +
  `setInterpolationDuration()` for client-side interpolation; the smoothing window is
  configurable per server.
- **Real player skins** — when paired with [MannequinAPI](https://github.com/iykyk-mannequinapi),
  the model renders the actual 16-part skin texture set for any player UUID. Without an API
  key, falls back to colored wool/plank materials.
- **Slim-arm support** — automatically narrows arm cubes (3 px wide) and shifts rest offsets
  for Alex skins.

## Building & installing

```bash
./gradlew build
```

Output JAR: `build/libs/WuhuMannequin-0.1.0.jar`. Requires **Java 21** and **Paper 1.21.11+**.

Drop the JAR into your server's `plugins/` folder. No configuration is needed for the
fallback (block-material) renderer; the API integration only kicks in if you set an API key
in `config.yml`.

## Configuration

`plugins/WuhuMannequin/config.yml`:

```yaml
# Number of ticks the client interpolates over when the model moves or its pose changes.
# 0 = snap (cohesive but jittery during fast motion), 3 = smooth default, 10 = very floaty.
interpolation-ticks: 3

api:
  url: "http://localhost:3001"   # MannequinAPI server URL
  key: ""                          # leave empty to disable API integration
poll-interval: 5                   # seconds between skin-generation status polls
preload-on-join: true              # fetch skins automatically when players join
```

`/mannequin reload` re-reads this without restarting the server.

## Debug commands

All commands require the `wuhumannequin.debug` permission (op by default).

| Command | What it does |
|---|---|
| `/mannequin spawn` | Spawn (or toggle off) a static debug model 1 block above your feet. |
| `/mannequin spin` | Spawn a model that cycles through yaw → pitch → roll → combined-axis rotation. Useful for verifying joint pivots. |
| `/mannequin pose <name>` | Apply one of the 31 built-in static poses. Tab-complete for the full list. |
| `/mannequin animation <name\|stop>` | Start a built-in animation, or `stop` to freeze the model on its last static pose. Tab-complete for the full list. (`/mannequin anim` is an alias.) |
| `/mannequin remove` | Despawn your debug model and any running animation/spin task. |
| `/mannequin interpolation <0–10>` | Set the smoothing window in ticks. Respawn the model to apply. |
| `/mannequin fetchskin [uuid\|debug]` | Fetch and cache skin textures for yourself, a UUID, or the debug-grid skin. |
| `/mannequin reload` | Re-read `config.yml`. |

The model spawned by `/mannequin spawn` is **anchored** at its spawn location — subsequent
`pose` and `animation` commands re-pose it in place rather than dragging it to wherever you're
currently standing.

---

## Integrating from another plugin

### Adding the dependency

In your `paper-plugin.yml`:

```yaml
dependencies:
  server:
    WuhuMannequin:
      load: BEFORE
      required: true
```

Gradle (compile-only — Paper resolves the runtime dependency from the loaded plugin jar):

```kotlin
dependencies {
    compileOnly(files("libs/WuhuMannequin-0.1.0.jar"))
    // or, if you publish it: compileOnly("com.wuhumannequin:WuhuMannequin:0.1.0")
}
```

There is **no separate API artifact** — the plugin's own classes under
`com.wuhumannequin.model.*` and `com.wuhumannequin.skin.*` are the public API.

### Spawning a model

```java
import com.wuhumannequin.model.*;
import org.bukkit.Location;
import org.joml.Quaternionf;

// 1. Construct
PlayerModel model = new PlayerModel();

// 2. (Optional) attach a player profile so the head shows their face
model.setHeadProfile(player.getPlayerProfile());

// 3. (Optional) attach the rest of the skin via WuhuMannequin's SkinCache
var entry = WuhuMannequin.getInstance().getSkinCache().get(player.getUniqueId());
if (entry != null) model.setSkinTextures(entry.textures(), entry.model());

// 4. Spawn at a location with a body rotation and an initial pose
Location anchor = player.getLocation().add(0, 1, 0);
Quaternionf bodyRotation = new Quaternionf().rotateY(Math.toRadians(-anchor.getYaw()));
model.spawn(anchor.getWorld(), anchor, bodyRotation, PlayerModelPoses.STANDING);
```

After `spawn`, the model is live in the world. It does **not** track the player automatically
— you decide when to move/repose it.

### Updating per tick

The recommended pattern is a `BukkitRunnable` that calls `model.update(...)` once per tick:

```java
new BukkitRunnable() {
    @Override public void run() {
        if (!model.isSpawned()) { cancel(); return; }
        model.update(currentLocation, currentBodyRotation, currentPose);
    }
}.runTaskTimer(plugin, 1L, 1L);
```

`update()` accepts the same arguments as `spawn()`: a `Location`, a body-level
`Quaternionf`, and a `PlayerModelPose`. It teleports each sub-entity to its new transformed
location and updates the per-part rotation. With `interpolation-ticks > 0` the client
smooths between successive updates, so calling once per server tick (20 Hz) yields visually
60 fps motion.

### Despawning

```java
model.despawn();
```

Always despawn before your plugin disables, otherwise the entities will linger as orphaned
display entities until the chunk unloads. The display entities are spawned with
`setPersistent(false)` so they will not survive a server restart.

---

## Pose system

A `PlayerModelPose` is an immutable record:

```java
public record PlayerModelPose(
    Map<BodyPart, Quaternionf> rotations,
    Map<BodyPart, Vector3f>    offsetAdjustments
) {}
```

`rotations` keys are individual sub-parts (`LEFT_ARM_UPPER`, `LEFT_ARM_MIDDLE`, …) but you
rarely set them directly — `PlayerModelPoses.buildPose` lets you specify rotations by
**logical group** (`"LEFT_ARM"`, `"RIGHT_LEG"`, `"HEAD"`, `"TORSO"`) and expands them
automatically to all three sub-parts.

### Built-in poses

`PlayerModelPoses.ALL` is an insertion-ordered map of every named pose for command lookup.
Tab-complete `/mannequin pose <TAB>` for the full list, or read
`PlayerModelPoses.java` for the rotations.

| Category | Poses |
|---|---|
| Core | `STANDING`, `T_POSE`, `X_POSE`, `SITTING`, `ARMS_FORWARD`, `EJECTING`, `PARACHUTING` |
| Greetings & gestures | `WAVING`, `SALUTING`, `POINTING`, `HANDS_UP`, `CHEER`, `HANDS_ON_HIPS`, `CROSSED_ARMS`, `FLEXING`, `ZOMBIE`, `DAB`, `HUG`, `PRAYER` |
| Combat & athletic | `HIGH_KICK`, `FRONT_KICK`, `SPLITS`, `BALLET`, `ARCHER`, `RUNNING`, `WALKING` |
| Head | `HEAD_TILT`, `LOOK_UP`, `LOOK_DOWN`, `DEFEAT`, `DISCO` |
| **Bendable** | `BEND_WAVE`, `BEND_FLEX`, `BEND_SALUTE`, `BEND_THINKING`, `BEND_CROSSED_ARMS`, `BEND_HANDS_ON_HIPS`, `BEND_SITTING`, `BEND_KNEEL`, `BEND_RUNNING` |

### Custom poses

```java
import com.wuhumannequin.model.PlayerModelPose;
import com.wuhumannequin.model.PlayerModelPoses;
import org.joml.Quaternionf;
import java.util.Map;

PlayerModelPose karate = PlayerModelPoses.buildPose(
    Map.of(
        "LEFT_ARM",  new Quaternionf().rotateX((float) Math.toRadians(-90)),
        "RIGHT_ARM", new Quaternionf()
                .rotateX((float) Math.toRadians(-30))
                .rotateZ((float) Math.toRadians(-40)),
        "RIGHT_LEG", new Quaternionf().rotateZ((float) Math.toRadians(-25))
    ),
    Map.of() // offset adjustments — almost always empty
);
```

`buildPose` is `public static`, so any plugin can call it. The values it returns are
immutable and safe to share across models.

### Bendable limbs

Each limb is internally three stacked segments (`UPPER` / `MIDDLE` / `LOWER`). They form
a **hierarchical chain** with three joints:

```
shoulder ──> upper segment ──> elbow ──> middle segment ──> wrist ──> lower segment
   (hip)                      (knee)                       (ankle)
```

A `PlayerModelPose` stores per-sub-part rotations:

| Sub-part key | Joint it controls | Rotation frame |
|---|---|---|
| `*_UPPER` | shoulder / hip | body frame |
| `*_MIDDLE` | elbow / knee | upper segment's local frame (after the shoulder rotation) |
| `*_LOWER` | wrist / ankle | forearm's local frame (after the elbow rotation) |

`PlayerModelPoses.buildPose` (the rigid builder) only sets `*_UPPER`, leaving the elbow
and wrist at identity — that produces a straight, fully-rotated rigid limb. To actually
bend, use **`buildBendPose`**:

```java
import com.wuhumannequin.model.PlayerModelPose;
import com.wuhumannequin.model.PlayerModelPoses;
import org.joml.Quaternionf;
import java.util.Map;

// Right arm raised in an "L": upper arm horizontal out, forearm bent straight up.
PlayerModelPose lShape = PlayerModelPoses.buildBendPose(
    Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))),
    Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90)))
);
```

The two-arg overload bends at the elbow only. The four-arg overload accepts wrist
rotations and per-limb offset adjustments as well:

```java
PlayerModelPoses.buildBendPose(shoulders, elbows, wrists, offsets);
```

The compute pass walks the chain so each segment's top edge lands exactly at its parent's
bottom edge — i.e. straight limbs have **no air gaps**. When a joint bends, the inner edge
of the bend leaves a small wedge gap between two cube segments meeting at a corner — this
is geometrically inevitable for box-segmented limbs and isn't worth working around.

The pre-built `BEND_*` constants in `PlayerModelPoses` show common shapes:

- `BEND_FLEX` — classic double bicep (T-pose arms with forearms bent up)
- `BEND_WAVE` — right arm raised in an L
- `BEND_SALUTE` — angled upper arm + forearm bent toward forehead
- `BEND_THINKING` — hand to chin
- `BEND_CROSSED_ARMS`, `BEND_HANDS_ON_HIPS` — bent-elbow versions of the rigid poses
- `BEND_SITTING` — proper bent-knee sit (thighs forward, calves down)
- `BEND_KNEEL` — kneeling on one knee
- `BEND_RUNNING` — mid-stride run with bent knees and elbows

### Coordinate system

The model faces **+Z (south)** at identity. With the right-hand rule:

| Axis | Positive rotation | Effect on a hanging limb |
|---|---|---|
| `rotateZ(+a)` on a **left** limb | counter-clockwise from above | raises arm/leg outward (east, away from body) |
| `rotateZ(+a)` on a **right** limb | counter-clockwise from above | raises arm/leg **across** the body (also east) |
| `rotateZ(-a)` on a **right** limb | clockwise from above | raises arm/leg outward (west, away from body) |
| `rotateX(-a)` on any limb | swings the limb **forward** (+Z) | leg → kick forward, arm → punch forward |
| `rotateX(+a)` on any limb | swings the limb **backward** (-Z) | |
| `rotateY(±a)` on a hanging limb | rotates around its own axis | usually invisible (limb is symmetric) |
| `rotateY(±a)` on the head | yaw left/right | `+` = look left |
| `rotateZ(±a)` on the head | tilt sideways | `+` = tilt right |
| `rotateX(±a)` on the head | tilt forward/back | `+` = look down |

Limbs can be either **rigid** (the default for `buildPose` and the un-prefixed constants)
or **bent at the elbow/knee/wrist** (see [Bendable limbs](#bendable-limbs) below). Rigid
poses rotate the whole limb at the shoulder; bent poses chain three independent
sub-segment rotations.

---

## Animation system

A `PlayerModelAnimation` is a functional interface:

```java
@FunctionalInterface
public interface PlayerModelAnimation {
    PlayerModelPose poseAt(long tick);
}
```

Implementations are pure functions of the elapsed tick counter — no internal state — which
makes them cheap to share between models and trivial to seed/restart.

### Built-in animations

`PlayerModelAnimations.ALL` is an insertion-ordered map of every named animation. Tab-complete
`/mannequin animation <TAB>` for the list.

| Animation | Description |
|---|---|
| `WAVE` | Right arm raised; hand swings side-to-side via `rotateY` on the arm. |
| `JUMPING_JACKS` | Sinusoidal interpolation between T-pose and X-pose. |
| `RUNNING_CYCLE` | Strong alternating arm/leg swings around X. |
| `WALKING_CYCLE` | Gentler version of `RUNNING_CYCLE`. |
| `CLAPPING` | Both arms forward; spread oscillates in/out. |
| `PUNCHING` | Alternating straight-arm jabs. |
| `SWIMMING` | Front-crawl: arms rotate in full circles, 180° out of phase. |
| `CHICKEN` | Arms held in T-pose; flap up/down. |
| `KICKING` | Alternating front kicks. |
| `NODDING` | Head bob (yes). |
| `SHAKING_HEAD` | Head shake (no). |
| `IDLE_BREATHE` | Subtle arm sway for idle states. |
| `DANCING` | Combined arm waves + leg sway. |
| `BOWING` | Bows down and rises. |
| `BEND_WAVE` | Forearm waves side-to-side at the elbow with the upper arm static. |
| `BEND_RUNNING` | Mid-stride run with bent knees and elbows; alternates legs/arms. |
| `BEND_FLEX_PUMP` | Double-bicep flex with the forearms pumping in/out. |
| `BEND_PUNCHING` | Alternating punches with windup at the elbow before extending. |

### Custom animations

```java
import com.wuhumannequin.model.PlayerModelAnimation;
import com.wuhumannequin.model.PlayerModelPoses;
import org.joml.Quaternionf;
import java.util.Map;

// A salute that lowers and raises every ~3 seconds.
PlayerModelAnimation breathingSalute = tick -> {
    double t = tick * 0.10;
    float angle = (float) (-130 + Math.sin(t) * 10); // ±10° around the salute pose
    return PlayerModelPoses.buildPose(
        Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(angle))),
        Map.of()
    );
};
```

### Driving animations from your plugin

WuhuMannequin only ships a runner for the debug command. For your own model, drive an
animation with your own `BukkitRunnable`:

```java
PlayerModelAnimation animation = PlayerModelAnimations.RUNNING_CYCLE;
PlayerModel model = ...;
Location anchor = ...;
Quaternionf bodyRotation = ...;

new BukkitRunnable() {
    long tick = 0;
    @Override public void run() {
        if (!model.isSpawned()) { cancel(); return; }
        model.update(anchor, bodyRotation, animation.poseAt(tick++));
    }
}.runTaskTimer(plugin, 1L, 1L);
```

`tick` is just an opaque counter — start it at any value, freeze it, scrub backwards,
multiply it for slow-motion, etc. The animation function is stateless.

---

## Skin loading

WuhuMannequin caches skin textures keyed by player UUID. Other plugins can read the cache:

```java
import com.wuhumannequin.WuhuMannequin;
import com.wuhumannequin.skin.SkinCache;

SkinCache cache = WuhuMannequin.getInstance().getSkinCache();
var entry = cache.get(playerUuid);
if (entry != null) {
    model.setSkinTextures(entry.textures(), entry.model());
} else {
    // Trigger an async fetch via the API client
    WuhuMannequin.getInstance().getSkinApiClient()
        .getSkins(playerUuid)
        .thenAccept(result -> {
            if (result.status() == SkinFetchResult.Status.READY) {
                cache.put(playerUuid, result.textures(), result.model());
            }
        });
}
```

If `api.key` is empty in `config.yml`, the cache stays empty and models fall back to
colored wool/plank materials.

---

## Architecture

### Display entity layout

| Part | Entity count | Material (no skin) | Visual size (blocks) |
|---|---|---|---|
| Head | 1 | `PLAYER_HEAD` (real skin) | 0.5 × 0.5 × 0.5 |
| Torso | 3 (upper / middle / lower) | brown wool | 0.5 × 0.25 × 0.25 each |
| Arm (each) | 3 | birch planks | 0.25 × 0.25 × 0.25 each (slim: 0.1875 wide) |
| Leg (each) | 3 | dark oak planks | 0.25 × 0.25 × 0.25 each |
| **Total** | **16 entities per model** | | |

`ItemDisplay` is rendered at half the configured scale, so a "scale 0.5" entity is visually
0.25 blocks per side (4 px). Three stacked thirds form a 0.75-block (12 px) limb.

### Pivot offsets

Each sub-part stores a pivot offset that defines where the joint is relative to its center.
For all three sub-parts of a given limb the pivot points to the **same** shoulder/hip joint —
that's what makes limbs rotate as a single rigid unit. See `PlayerModel.PIVOT_OFFSETS` and
`PlayerModel.computeTransform` for the math.

### Interpolation

`PlayerModel.setInterpolationTicks(int)` controls both `setInterpolationDuration` and
`setTeleportDuration` for newly-spawned models. Updates re-trigger interpolation each tick,
so visible motion lags the logical state by `interpolationTicks` ticks but is much smoother.

### Slim arms

When `setSkinTextures(..., SkinTexture.Model.SLIM)` is called, arm sub-parts switch to a
3-px-wide profile (`SLIM_ARM_REST_OFFSETS`, `SLIM_ARM_SCALES`) so the inner edge still
meets the torso edge cleanly.

---

## Limitations & future work

- **Wedge gaps on bent joints.** Bendable limbs meet at a single edge at each joint, so
  bending creates an unavoidable triangular gap on the inside of the bend (and a small
  overlap on the outside). It's the geometric cost of representing a joint with two cubes
  instead of a sphere. Straight limbs have no gaps.
- **No torso bending.** Torso sub-parts rotate around their own centers if you set rotations
  on them, which doesn't actually bend the torso forward (limbs would float). For "bow"-type
  motion, animate the head + arms instead.
- **Head profile is locked at spawn.** `setHeadProfile` is meant to be called before
  `spawn()`. Changing the head profile mid-life requires a despawn/respawn.
- **No collision.** Display entities are visual-only — the model has no hitbox.
- **One body rotation per update.** The `Quaternionf` you pass to `update()` rotates the
  whole model. Per-limb rotations come from the pose, not from `update()`.

---

For terse, machine-readable integration notes (file paths, type signatures, common pitfalls)
see [`AGENTS.md`](AGENTS.md).
