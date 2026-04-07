package com.wuhumannequin.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of resolved skin textures (and the player's skin model
 * variant) per player.
 */
public class SkinCache {

    /** Cached skin payload: textures + model variant. */
    public record Entry(
            Map<SkinTexture.BodyPartKey, SkinTexture> textures,
            SkinTexture.Model model
    ) {}

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public void put(UUID uuid, Map<SkinTexture.BodyPartKey, SkinTexture> textures, SkinTexture.Model model) {
        cache.put(uuid, new Entry(textures, model == null ? SkinTexture.Model.CLASSIC : model));
    }

    public Entry get(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }
}
