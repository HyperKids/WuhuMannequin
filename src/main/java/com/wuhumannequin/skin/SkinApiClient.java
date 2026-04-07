package com.wuhumannequin.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP client for the MannequinAPI backend.
 * Authenticates with a Bearer API key.
 */
public class SkinApiClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Logger logger;
    private volatile boolean disabled;
    private volatile String disabledReason;

    public SkinApiClient(String baseUrl, String apiKey, Logger logger) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.logger = logger;
        this.disabled = false;
        this.disabledReason = null;
    }

    /**
     * Hit the side-effect-free /api/skins/auth-check endpoint and confirm the
     * configured API key is valid. On 401 the client is permanently disabled
     * (until /mannequin reload) so subsequent calls fail fast with a clear
     * message instead of silently passing through to the backend.
     *
     * Runs asynchronously; the returned future completes with true if the key
     * was accepted, false otherwise.
     */
    public CompletableFuture<Boolean> validateKey() {
        return CompletableFuture.supplyAsync(() -> {
            if (apiKey == null || apiKey.isBlank()) {
                disabled = true;
                disabledReason = "no API key configured";
                return false;
            }
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/skins/auth-check"))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    logger.info("API key validated against " + baseUrl);
                    return true;
                }
                if (response.statusCode() == 401) {
                    logger.log(Level.SEVERE,
                            "API key rejected by MannequinAPI (401). Disabling API calls. " +
                            "Fix api.key in config.yml and run /mannequin reload.");
                    disabled = true;
                    disabledReason = "API key rejected by server (401)";
                    return false;
                }
                logger.warning("Unexpected status " + response.statusCode() + " from auth-check; " +
                        "leaving API enabled but skins fetches may fail.");
                return false;
            } catch (Exception e) {
                logger.warning("Could not reach MannequinAPI for auth check: " + e.getMessage() +
                        ". Leaving API enabled — fetches will retry on demand.");
                return false;
            }
        });
    }

    public String disabledReason() {
        return disabledReason;
    }

    /**
     * Request skins for a player. The result distinguishes ready, still
     * generating, and error outcomes so callers can surface the right
     * message to the player.
     */
    public CompletableFuture<SkinFetchResult> getSkins(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (disabled) {
                return SkinFetchResult.error("API calls disabled: " +
                        (disabledReason != null ? disabledReason : "unknown reason") +
                        ". Fix config.yml and /mannequin reload.");
            }

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/skins/" + uuid))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();

                logger.info("Requesting skins from " + request.uri());
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("API response: " + response.statusCode());

                if (response.statusCode() == 200) {
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                    SkinTexture.Model model = SkinTexture.Model.fromString(
                            root.has("model") && !root.get("model").isJsonNull()
                                    ? root.get("model").getAsString()
                                    : null);
                    return SkinFetchResult.ready(parseSkinsResponse(root), model);
                } else if (response.statusCode() == 202) {
                    logger.info("Skins for " + uuid + " are still generating.");
                    return SkinFetchResult.generating();
                } else if (response.statusCode() == 401) {
                    logger.log(Level.SEVERE,
                            "API key is invalid or revoked — disabling API calls. " +
                            "Fix config and run /mannequin reload to resume.");
                    disabled = true;
                    disabledReason = "API key rejected by server (401)";
                    return SkinFetchResult.error("API key is invalid or revoked. Fix config and /mannequin reload.");
                } else {
                    logger.warning("API returned " + response.statusCode() + " for " + uuid + ": " + response.body());
                    return SkinFetchResult.error("API returned HTTP " + response.statusCode() + ".");
                }
            } catch (Exception e) {
                logger.warning("Failed to fetch skins for " + uuid + ": " + e.getMessage());
                return SkinFetchResult.error("Failed to reach MannequinAPI: " + e.getMessage());
            }
        });
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !disabled;
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Map<SkinTexture.BodyPartKey, SkinTexture> parseSkinsResponse(JsonObject root) {
        JsonObject textures = root.getAsJsonObject("textures");

        Map<SkinTexture.BodyPartKey, SkinTexture> result = new EnumMap<>(SkinTexture.BodyPartKey.class);
        for (SkinTexture.BodyPartKey key : SkinTexture.BodyPartKey.values()) {
            JsonElement element = textures.get(key.jsonKey());
            if (element != null && !element.isJsonNull()) {
                JsonObject tex = element.getAsJsonObject();
                result.put(key, new SkinTexture(
                        tex.get("value").getAsString(),
                        tex.get("signature").getAsString()
                ));
            }
        }
        return result;
    }
}
