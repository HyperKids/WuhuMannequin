# AGENTS.md — WuhuMannequin LLM context

Dense, factual reference for AI agents working **inside** WuhuMannequin or **integrating with
it from another Paper plugin**. Optimized for grep + scan, not narrative reading. For human
docs see [`README.md`](README.md).

---

## What this plugin is

Paper 1.21+ plugin that renders 16-piece player models out of `ItemDisplay` entities. Each
limb is split into 3 stacked sub-parts (UPPER / MIDDLE / LOWER) for pixel-perfect texture
mapping. Other Wuhu* plugins depend on it directly to render player-skinned mannequins for
in-world entities.

Java 21, Paper API 1.21.11. Build: `./gradlew build`. Output:
`build/libs/WuhuMannequin-0.1.0.jar`.

## Repo layout

```
src/main/java/com/wuhumannequin/
├── WuhuMannequin.java              # plugin entrypoint, holds singletons
├── command/MannequinCommand.java   # /mannequin debug command (BasicCommand)
├── model/
│   ├── BodyPart.java               # 16-value enum, each with a logicalGroup()
│   ├── PlayerModel.java            # the renderer — owns ItemDisplay entities
│   ├── PlayerModelPose.java        # immutable record: per-part rotations + offsets
│   ├── PlayerModelPoses.java       # 31 static poses + the buildPose helper
│   ├── PlayerModelAnimation.java   # @FunctionalInterface: tick → pose
│   └── PlayerModelAnimations.java  # 14 static animations
└── skin/
    ├── SkinTexture.java            # value+signature record + Model{CLASSIC,SLIM}
    ├── SkinCache.java              # in-memory UUID → textures cache
    ├── SkinApiClient.java          # HTTP client for MannequinAPI
    ├── SkinChangeDetector.java     # join listener + poller
    └── SkinFetchResult.java        # result record (READY / GENERATING / ERROR)

src/main/resources/
├── paper-plugin.yml                # plugin descriptor
└── config.yml                      # default config (interpolation, API key)
```

## Public API surface

Everything under `com.wuhumannequin.model.*` and `com.wuhumannequin.skin.*` is **public** and
intended for external use. There is **no separate API jar**; depend on the plugin jar with
`compileOnly` and declare a `paper-plugin.yml` server dependency.

### Key types

```java
// Renderer ─────────────────────────────────────────────────────────────────
class PlayerModel {
    PlayerModel();
    void setHeadProfile(PlayerProfile profile);                  // call before spawn
    void setSkinTextures(Map<SkinTexture.BodyPartKey, SkinTexture> textures);
    void setSkinTextures(Map<SkinTexture.BodyPartKey, SkinTexture> textures,
                         SkinTexture.Model model);               // SLIM uses 3-px arms
    void setFallbackMaterial(BodyPart part, Material material);
    void spawn(World world, Location center, Quaternionf rotation, PlayerModelPose pose);
    void update(Location center, Quaternionf rotation, PlayerModelPose pose);
    void despawn();
    boolean isSpawned();
    static void setInterpolationTicks(int ticks);    // 0..10, default 3 (server-wide)
    static int  getInterpolationTicks();
}

// Pose data (immutable) ────────────────────────────────────────────────────
record PlayerModelPose(
    Map<BodyPart, Quaternionf> rotations,
    Map<BodyPart, Vector3f>    offsetAdjustments
) {
    static final PlayerModelPose IDENTITY;
    Quaternionf getRotation(BodyPart part);          // returns identity if absent
    Vector3f    getOffsetAdjustment(BodyPart part);  // returns zero if absent
}

// Pose builders + 40 named constants ──────────────────────────────────────
final class PlayerModelPoses {
    // Rigid (whole-limb) poses:
    static final PlayerModelPose STANDING, T_POSE, X_POSE, SITTING, ARMS_FORWARD,
        EJECTING, PARACHUTING, WAVING, SALUTING, POINTING, HANDS_UP, CHEER,
        HANDS_ON_HIPS, CROSSED_ARMS, FLEXING, ZOMBIE, DAB, HUG, PRAYER, HIGH_KICK,
        FRONT_KICK, SPLITS, BALLET, ARCHER, RUNNING, WALKING, HEAD_TILT, LOOK_UP,
        LOOK_DOWN, DEFEAT, DISCO;
    // Bendable poses (chain at elbow / knee):
    static final PlayerModelPose BEND_WAVE, BEND_FLEX, BEND_SALUTE, BEND_THINKING,
        BEND_CROSSED_ARMS, BEND_HANDS_ON_HIPS, BEND_SITTING, BEND_KNEEL, BEND_RUNNING;

    static final Map<String, PlayerModelPose> ALL;   // insertion-ordered, lowercase keys

    // Rigid limb builder. For limb groups (LEFT_ARM/RIGHT_ARM/LEFT_LEG/RIGHT_LEG) the
    // rotation goes ONLY to the *_UPPER sub-part; elbow/wrist stay identity → straight
    // rigid limb. For HEAD and TORSO the rotation is applied to every matching sub-part.
    static PlayerModelPose buildPose(
        Map<String, Quaternionf> groupRotations,     // keys: HEAD, TORSO, LEFT_ARM, RIGHT_ARM,
        Map<String, Vector3f>    groupOffsets        //       LEFT_LEG, RIGHT_LEG
    );

    // Bendable limb builder. Each map keyed by limb group; rotations are LOCAL to their
    // joint. Pass Map.of() for joints you don't need.
    static PlayerModelPose buildBendPose(
        Map<String, Quaternionf> shoulders,   // body frame
        Map<String, Quaternionf> elbows,      // upper segment's local frame
        Map<String, Quaternionf> wrists,      // forearm's local frame
        Map<String, Vector3f>    groupOffsets // applied to UPPER only (shifts whole limb)
    );
    static PlayerModelPose buildBendPose(
        Map<String, Quaternionf> shoulders,
        Map<String, Quaternionf> elbows
    );  // wrists + offsets default to empty
}

// Animation interface + 14 named constants ────────────────────────────────
@FunctionalInterface
interface PlayerModelAnimation {
    PlayerModelPose poseAt(long tick);   // tick is 0-based, 20 ticks/sec
}

final class PlayerModelAnimations {
    // Rigid:
    static final PlayerModelAnimation WAVE, JUMPING_JACKS, RUNNING_CYCLE, WALKING_CYCLE,
        CLAPPING, PUNCHING, SWIMMING, CHICKEN, NODDING, SHAKING_HEAD, IDLE_BREATHE,
        DANCING, KICKING, BOWING;
    // Bendable (use buildBendPose under the hood):
    static final PlayerModelAnimation BEND_WAVE, BEND_RUNNING, BEND_FLEX_PUMP, BEND_PUNCHING;
    static final Map<String, PlayerModelAnimation> ALL;
}

// Body parts (per-segment, used inside PlayerModelPose maps) ──────────────
enum BodyPart {
    HEAD,
    TORSO_UPPER, TORSO_MIDDLE, TORSO_LOWER,
    LEFT_ARM_UPPER, LEFT_ARM_MIDDLE, LEFT_ARM_LOWER,
    RIGHT_ARM_UPPER, RIGHT_ARM_MIDDLE, RIGHT_ARM_LOWER,
    LEFT_LEG_UPPER, LEFT_LEG_MIDDLE, LEFT_LEG_LOWER,
    RIGHT_LEG_UPPER, RIGHT_LEG_MIDDLE, RIGHT_LEG_LOWER;
    String logicalGroup();   // e.g. LEFT_ARM_UPPER → "LEFT_ARM"
}
```

### Plugin singletons

`WuhuMannequin.getInstance()` exposes:
- `getSkinCache() → SkinCache` — read/write the in-memory texture cache
- `getSkinApiClient() → SkinApiClient` — async fetcher returning `CompletableFuture<SkinFetchResult>`
- `getSkinChangeDetector() → SkinChangeDetector` — manages preload-on-join + polling

## Coordinate conventions (CRITICAL — wrong sign = wrong limb side)

- **Model facing:** identity rotation faces +Z (south).
- **Anatomical sides:** LEFT limbs at +X (east), RIGHT limbs at -X (west).
- `rotateZ(+a)` on **LEFT** limb → raises **outward** (away from body, eastward).
- `rotateZ(+a)` on **RIGHT** limb → raises **across** the body (also eastward, into the
  body's left side). To raise the right limb outward use `rotateZ(-a)`.
- `rotateX(-a)` on any limb → swings **forward** (+Z). `+a` swings backward.
- Identity arm/leg points **down** (-Y). To point straight up: `rotateZ(180°)` for left
  arm, `rotateX(-180°)` for either.
- Head: `rotateY(+a)` = look left, `rotateZ(+a)` = tilt right (left ear up),
  `rotateX(+a)` = look down.

When in doubt, read existing constants in `PlayerModelPoses.java` — `T_POSE`, `X_POSE`,
`SITTING`, and `ARMS_FORWARD` cover the cardinal cases.

## Lifecycle invariants

1. **Always despawn before disable.** `setPersistent(false)` covers restarts but not
   `/reload`. Hold a reference to every model you spawn.
2. **`setHeadProfile` and `setSkinTextures` must precede `spawn`.** Changing them after
   spawn does not re-render existing entities; you must `despawn()` and re-`spawn()`.
3. **Anchor your model — don't follow the player by accident.** `update()` accepts a fresh
   `Location` each tick, but if you keep passing `player.getLocation()` your model snaps to
   the player. The debug command stores a per-model anchor (`MannequinCommand.modelAnchors`)
   to avoid this; mirror that pattern in your own driver.
4. **`update()` is the only way to apply a new pose.** There is no `setPose()` — pose
   changes ride along with the per-tick transform update.
5. **`PlayerModelPose` is immutable + share-safe.** `getRotation()` returns a defensive copy.
6. **Animations are stateless.** Pass the tick counter explicitly; do not assume the
   animation remembers anything between calls.
7. **Body rotation vs pose rotation.** The `Quaternionf rotation` arg to `update()` rotates
   the whole model in world space (yaw/pitch/roll of the body). The pose contributes
   per-limb rotations on top.

## Common pitfalls (observed bugs from past sessions)

- **Anchor drift:** using `player.getLocation()` for `update()` while the model was spawned
  at `player.getLocation().add(0,1,0)` re-anchors the model 1 block lower every call. Use a
  stored anchor (`MannequinCommand.java:46-50` shows the map pattern).
- **Limb-side sign error:** flipping the sign of a `rotateZ` on a right limb sends it
  outward instead of across the body. The `DAB` pose's history (right arm initially used
  `rotateZ(-120)`, looked like an X-pose) is the canonical example.
- **Rigid vs bendable confusion:** the un-prefixed pose constants are *rigid*
  approximations (`SALUTE`, `DAB`, `CROSSED_ARMS`, `FLEXING`) — the limb is one block
  rotated at the shoulder. Their `BEND_*` counterparts (`BEND_SALUTE`, `BEND_FLEX`,
  `BEND_CROSSED_ARMS`) actually bend at the elbow. Don't compose a rigid pose with extra
  elbow rotations expecting accumulation; build a bendable pose with `buildBendPose`
  instead.
- **Rotation frame at the elbow/wrist:** the elbow rotation is in the upper segment's
  *local* frame (after the shoulder rotation has been applied), not the body frame. Same
  for the wrist relative to the forearm. Combined orientation = R_shoulder × R_elbow ×
  R_wrist (JOML's `mul` order). If you set elbow = R_shoulder you get a 2× shoulder
  rotation, not "shoulder + bend".
- **Two animation drivers writing to the same model:** if you start an animation on top of
  a spin task, both runnables will write to the model every tick and fight. The debug
  command cancels one before starting the other; do the same in your driver.
- **Despawning during iteration:** `model.update()` loops over the entities map. If
  another thread or hook removes the model mid-loop, you can NPE. All operations are
  expected to run on the main server thread.

## Integration recipes

### Minimal: spawn a static T-posed mannequin

```java
PlayerModel m = new PlayerModel();
m.setHeadProfile(player.getPlayerProfile());
Location anchor = player.getLocation().add(0, 1, 0);
Quaternionf yaw = new Quaternionf().rotateY((float) Math.toRadians(-anchor.getYaw()));
m.spawn(anchor.getWorld(), anchor, yaw, PlayerModelPoses.T_POSE);
// later: m.despawn();
```

### Per-tick driver with custom animation

```java
PlayerModelAnimation anim = tick -> PlayerModelPoses.buildPose(
    Map.of("RIGHT_ARM",
           new Quaternionf().rotateZ((float) Math.toRadians(-160 + Math.sin(tick * 0.3) * 20))),
    Map.of()
);

new BukkitRunnable() {
    long tick = 0;
    @Override public void run() {
        if (!model.isSpawned()) { cancel(); return; }
        model.update(anchor, bodyRotation, anim.poseAt(tick++));
    }
}.runTaskTimer(plugin, 1L, 1L);
```

### Skin loading (async)

```java
SkinCache cache = WuhuMannequin.getInstance().getSkinCache();
var entry = cache.get(uuid);
if (entry != null) {
    model.setSkinTextures(entry.textures(), entry.model());
} else {
    WuhuMannequin.getInstance().getSkinApiClient().getSkins(uuid).thenAccept(result -> {
        if (result.status() == SkinFetchResult.Status.READY) {
            cache.put(uuid, result.textures(), result.model());
            // Schedule a respawn on the main thread if you want the new skin to apply
        }
    });
}
```

### Driving a built-in animation

```java
PlayerModelAnimation anim = PlayerModelAnimations.ALL.get("running");   // or .RUNNING_CYCLE
// ... use it the same way as the custom animation above
```

### Bendable limb pose

```java
import com.wuhumannequin.model.PlayerModelPoses;
import org.joml.Quaternionf;
import java.util.Map;

// Right arm raised in an L: upper horizontal out, forearm vertical.
var pose = PlayerModelPoses.buildBendPose(
    Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90))),  // shoulder
    Map.of("RIGHT_ARM", new Quaternionf().rotateZ((float) Math.toRadians(-90)))   // elbow
);
```

The compute walks shoulder → elbow → wrist for each limb. With identity at elbow + wrist
the chain produces a straight rigid limb — that's how `buildPose` (the rigid builder)
works. Wedge gaps appear on the inside of bent joints; this is geometric and unavoidable
for cube-segmented limbs.

## Configuration knobs

`config.yml` keys:

| Key | Type | Default | Effect |
|---|---|---|---|
| `interpolation-ticks` | int 0..10 | 3 | Smoothing window for `setInterpolationDuration` + `setTeleportDuration`. Server-wide. New models pick this up at spawn. |
| `api.url` | string | `http://localhost:3001` | MannequinAPI server URL |
| `api.key` | string | `""` | API key — empty disables skin loading |
| `poll-interval` | int seconds | 5 | Skin-generation status poll interval |
| `preload-on-join` | bool | true | Fetch skins when players join |

`/mannequin reload` re-reads everything via `WuhuMannequin.reinitialize()`.

## Permissions

| Permission | Default | Grants |
|---|---|---|
| `wuhumannequin.debug` | op | All `/mannequin` subcommands |

## Build, test, dev workflow

- **Build:** `./gradlew build` (auto-memory: always rebuild after edits)
- **Run a dev server:** `./gradlew runServer` (uses `xyz.jpenilla.run-paper`)
- **No tests** — there is no `src/test/`. Validation is via the in-game debug commands
  (`/mannequin spawn`, `/mannequin spin`, `/mannequin pose`, `/mannequin animation`).
- **Lints:** `-Xlint:all` is on, treat warnings seriously.

## Quick map: where to put new code

| You want to add... | File |
|---|---|
| A new static pose | `PlayerModelPoses.java` — declare a constant, add it to `ALL` |
| A new animation | `PlayerModelAnimations.java` — declare a constant, add it to `ALL` |
| New geometry / pivots | `PlayerModel.java` — `REST_OFFSETS`, `PIVOT_OFFSETS`, `SCALES` |
| A new debug subcommand | `MannequinCommand.java` — extend `execute()` switch + `suggest()` |
| A new skin source | `skin/SkinApiClient.java` (or new class implementing similar shape) |
| Plugin-level wiring | `WuhuMannequin.java` — `onEnable()` / `reinitialize()` |
