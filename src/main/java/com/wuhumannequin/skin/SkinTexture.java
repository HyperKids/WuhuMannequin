package com.wuhumannequin.skin;

/**
 * A signed Minecraft skin texture (value + signature from mineskin.org).
 */
public record SkinTexture(String value, String signature) {

    /**
     * Player skin model variant. Mirrors MannequinAPI's `model` field.
     * - CLASSIC (Steve): 4-pixel-wide arms
     * - SLIM (Alex):     3-pixel-wide arms
     */
    public enum Model {
        CLASSIC, SLIM;

        public static Model fromString(String value) {
            return value != null && value.equalsIgnoreCase("slim") ? SLIM : CLASSIC;
        }
    }

    /**
     * Keys matching the MannequinAPI JSON response fields (camelCase).
     */
    public enum BodyPartKey {
        HEAD("head"),
        TORSO_UPPER("torsoUpper"),
        TORSO_MIDDLE("torsoMiddle"),
        TORSO_LOWER("torsoLower"),
        LEFT_ARM_UPPER("leftArmUpper"),
        LEFT_ARM_MIDDLE("leftArmMiddle"),
        LEFT_ARM_LOWER("leftArmLower"),
        RIGHT_ARM_UPPER("rightArmUpper"),
        RIGHT_ARM_MIDDLE("rightArmMiddle"),
        RIGHT_ARM_LOWER("rightArmLower"),
        LEFT_LEG_UPPER("leftLegUpper"),
        LEFT_LEG_MIDDLE("leftLegMiddle"),
        LEFT_LEG_LOWER("leftLegLower"),
        RIGHT_LEG_UPPER("rightLegUpper"),
        RIGHT_LEG_MIDDLE("rightLegMiddle"),
        RIGHT_LEG_LOWER("rightLegLower");

        private final String jsonKey;

        BodyPartKey(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }
    }
}
