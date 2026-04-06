package com.wuhumannequin.model;

/**
 * Each display entity in the segmented player model.
 * Limbs and torso are split into upper/middle/lower thirds for pixel-perfect
 * texture mapping (4×4×4 cubes from 4×12×4 limbs).
 */
public enum BodyPart {
    HEAD("HEAD"),

    TORSO_UPPER("TORSO"),
    TORSO_MIDDLE("TORSO"),
    TORSO_LOWER("TORSO"),

    LEFT_ARM_UPPER("LEFT_ARM"),
    LEFT_ARM_MIDDLE("LEFT_ARM"),
    LEFT_ARM_LOWER("LEFT_ARM"),

    RIGHT_ARM_UPPER("RIGHT_ARM"),
    RIGHT_ARM_MIDDLE("RIGHT_ARM"),
    RIGHT_ARM_LOWER("RIGHT_ARM"),

    LEFT_LEG_UPPER("LEFT_LEG"),
    LEFT_LEG_MIDDLE("LEFT_LEG"),
    LEFT_LEG_LOWER("LEFT_LEG"),

    RIGHT_LEG_UPPER("RIGHT_LEG"),
    RIGHT_LEG_MIDDLE("RIGHT_LEG"),
    RIGHT_LEG_LOWER("RIGHT_LEG");

    private final String logicalGroup;

    BodyPart(String logicalGroup) {
        this.logicalGroup = logicalGroup;
    }

    /**
     * The logical limb this sub-part belongs to.
     * All three sub-parts of an arm share the same group, so poses apply uniformly.
     */
    public String logicalGroup() {
        return logicalGroup;
    }
}
