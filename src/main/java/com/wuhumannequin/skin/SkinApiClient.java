package com.wuhumannequin.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.NamedParameterSpec;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * HTTP client for the MannequinAPI backend.
 * Handles Ed25519 request signing and response parsing.
 */
public class SkinApiClient {

    private final String baseUrl;
    private final PrivateKey privateKey;
    private final HttpClient httpClient;
    private final Logger logger;

    public SkinApiClient(String baseUrl, String privateKeyHex, Logger logger) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.privateKey = loadPrivateKey(privateKeyHex);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.logger = logger;
    }

    /**
     * Request skins for a player. Returns the texture data if ready,
     * empty if still generating, or empty on error.
     */
    public CompletableFuture<Optional<Map<SkinTexture.BodyPartKey, SkinTexture>>> getSkins(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String signature = sign(timestamp);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/skins/" + uuid))
                        .header("Authorization", "Ed25519 " + signature + ":" + timestamp)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();

                logger.info("Requesting skins from " + request.uri());
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("API response: " + response.statusCode());

                if (response.statusCode() == 200) {
                    return Optional.of(parseSkinsResponse(response.body()));
                } else if (response.statusCode() == 202) {
                    logger.info("Skins for " + uuid + " are still generating.");
                    return Optional.empty();
                } else {
                    logger.warning("API returned " + response.statusCode() + " for " + uuid + ": " + response.body());
                    return Optional.empty();
                }
            } catch (Exception e) {
                logger.warning("Failed to fetch skins for " + uuid + ": " + e.getMessage());
                return Optional.empty();
            }
        });
    }

    public boolean isConfigured() {
        return privateKey != null;
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Map<SkinTexture.BodyPartKey, SkinTexture> parseSkinsResponse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
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

    private String sign(String message) {
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] signed = sig.sign();
            return bytesToHex(signed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign request", e);
        }
    }

    private static PrivateKey loadPrivateKey(String hexKey) {
        if (hexKey == null || hexKey.isBlank()) return null;
        try {
            byte[] fullKey = hexToBytes(hexKey);
            // tweetnacl secret key = seed (32 bytes) + public key (32 bytes)
            // Java EdDSA needs just the seed
            byte[] seed = new byte[32];
            System.arraycopy(fullKey, 0, seed, 0, 32);

            NamedParameterSpec spec = new NamedParameterSpec("Ed25519");
            EdECPrivateKeySpec privSpec = new EdECPrivateKeySpec(spec, seed);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePrivate(privSpec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Ed25519 private key: " + e.getMessage(), e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
