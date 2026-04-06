package com.wuhumannequin.skin;

/**
 * A signed Minecraft skin texture (value + signature from mineskin.org).
 */
public record SkinTexture(String value, String signature) {

    /**
     * Keys matching the MannequinAPI JSON response fields.
     */
    public enum BodyPartKey {
        HEAD("head"),
        TORSO("torso"),
        LEFT_ARM("leftArm"),
        RIGHT_ARM("rightArm"),
        LEFT_LEG("leftLeg"),
        RIGHT_LEG("rightLeg");

        private final String jsonKey;

        BodyPartKey(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }
    }
}
