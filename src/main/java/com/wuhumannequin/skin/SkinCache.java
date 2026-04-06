package com.wuhumannequin.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of resolved skin textures per player.
 */
public class SkinCache {

    private final Map<UUID, Map<SkinTexture.BodyPartKey, SkinTexture>> cache = new ConcurrentHashMap<>();

    public void put(UUID uuid, Map<SkinTexture.BodyPartKey, SkinTexture> textures) {
        cache.put(uuid, textures);
    }

    public Map<SkinTexture.BodyPartKey, SkinTexture> get(UUID uuid) {
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
