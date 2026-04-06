# WuhuMannequin

A Paper 1.21+ plugin that renders segmented player models using display entities. Each body part (head, torso, arms, legs) is an independent `ItemDisplay` entity that can be rotated freely — including roll — unlike Minecraft's `Mannequin`/`ArmorStand` which are always rendered upright.

## Features

- **6-part player model**: head, torso, left/right arms, left/right legs
- **Full 3D rotation**: yaw, pitch, AND roll on the entire model
- **Pivot-correct joints**: limbs rotate around shoulders/hips, not their geometric centers
- **Pose system**: predefined poses (standing, sitting, t-pose, arms-forward) or custom per-limb rotations
- **Smooth motion**: uses `setTeleportDuration()` + `setInterpolationDuration()` for 60fps client-side interpolation
- **Player head**: the head part uses `PLAYER_HEAD` with the actual player's skin

## Building

```bash
./gradlew build
```

Output JAR: `build/libs/WuhuMannequin-0.1.0.jar`

Requires Java 21 and Paper 1.21.11+.

## Installation

Drop the JAR into your server's `plugins/` folder. No configuration needed.

## Debug Commands

All commands require the `wuhumannequin.debug` permission (op by default).

| Command | Description |
|---------|-------------|
| `/mannequin spawn` | Spawn/toggle a static model at your feet |
| `/mannequin spin` | Spawn a model that rotates through all axes (yaw → pitch → roll → combined) |
| `/mannequin pose <name>` | Change the debug model's pose: `standing`, `sitting`, `tpose`, `arms_forward` |
| `/mannequin remove` | Remove your debug model |

## API Usage (for other plugins)

Add WuhuMannequin as a dependency in your `paper-plugin.yml`:

```yaml
dependencies:
  server:
    WuhuMannequin:
      load: BEFORE
      required: true
```

Then use the `PlayerModel` class:

```java
import com.wuhumannequin.model.*;

// Create a model
PlayerModel model = new PlayerModel();
model.setHeadProfile(player.getPlayerProfile());

// Spawn at a location with a rotation and pose
Quaternionf rotation = new Quaternionf().rotateYXZ(yaw, pitch, roll);
model.spawn(world, location, rotation, PlayerModelPoses.SITTING);

// Update every tick
model.update(newLocation, newRotation, PlayerModelPoses.SITTING);

// Clean up
model.despawn();
```

### Available Poses

| Pose | Description |
|------|-------------|
| `PlayerModelPoses.STANDING` | Arms at sides, legs down (default) |
| `PlayerModelPoses.SITTING` | Legs bent 90° forward, arms angled forward |
| `PlayerModelPoses.T_POSE` | Arms extended horizontally |
| `PlayerModelPoses.ARMS_FORWARD` | Arms stretched forward (skydiving/superman) |

### Custom Poses

```java
PlayerModelPose myPose = new PlayerModelPose(
    Map.of(
        BodyPart.LEFT_ARM, new Quaternionf().rotateX((float) Math.toRadians(-45)),
        BodyPart.RIGHT_ARM, new Quaternionf().rotateZ((float) Math.toRadians(30))
    ),
    Map.of() // no offset adjustments
);
```

## Architecture

The model uses colored wool/plank blocks as fallback materials. Future integration with MannequinAPI will provide actual player-skin textures via resource pack custom item models.

| Part | Fallback Material | Scale (blocks) |
|------|------------------|----------------|
| Head | `PLAYER_HEAD` (actual skin) | 0.5 × 0.5 × 0.5 |
| Torso | Brown wool | 0.5 × 0.75 × 0.25 |
| Arms | Birch planks | 0.25 × 0.75 × 0.25 |
| Legs | Dark oak planks | 0.25 × 0.75 × 0.25 |
