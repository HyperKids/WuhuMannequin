package com.wuhumannequin.skin;

import java.util.Map;

/**
 * Result of a skin fetch from the MannequinAPI. Distinguishes the three
 * outcomes the caller cares about: textures are ready, generation is in
 * progress, or the request failed.
 */
public final class SkinFetchResult {

    public enum Status { READY, GENERATING, ERROR }

    private final Status status;
    private final Map<SkinTexture.BodyPartKey, SkinTexture> textures;
    private final SkinTexture.Model model;
    private final String errorMessage;

    private SkinFetchResult(Status status,
                            Map<SkinTexture.BodyPartKey, SkinTexture> textures,
                            SkinTexture.Model model,
                            String errorMessage) {
        this.status = status;
        this.textures = textures;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    public static SkinFetchResult ready(Map<SkinTexture.BodyPartKey, SkinTexture> textures,
                                        SkinTexture.Model model) {
        return new SkinFetchResult(Status.READY, textures, model, null);
    }

    public static SkinFetchResult generating() {
        return new SkinFetchResult(Status.GENERATING, null, null, null);
    }

    public static SkinFetchResult error(String message) {
        return new SkinFetchResult(Status.ERROR, null, null, message);
    }

    public Status status() { return status; }
    public Map<SkinTexture.BodyPartKey, SkinTexture> textures() { return textures; }
    public SkinTexture.Model model() { return model; }
    public String errorMessage() { return errorMessage; }
}
