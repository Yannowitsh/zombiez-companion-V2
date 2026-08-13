package io.github.keoz5.zombiezcompanion.identity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;

/**
 * Version-stable player identity.
 *
 * <p>Rinaorc assigns a different account UUID depending on the client-version route: the native route
 * gets the real Mojang UUID, the proxied one an offline (name-derived) UUID. So {@code getUUID()} is
 * NOT stable across versions, which fragments the friend graph and breaks presence matching for
 * cross-version friends. This resolves the canonical Mojang UUID once per account (backend
 * {@code /identity}, Mojang-API-backed + KV-cached) and the whole mod uses it as its single identity —
 * presence {@code mcuuid}, the friends directory, friend requests and the realtime socket — so a player
 * is the same person on both versions.</p>
 *
 * <p>Until it resolves (and for non-premium accounts, where the Mojang lookup 404s) it falls back to
 * the raw session {@code getUUID()}, which is already correct on the native route.</p>
 */
public final class Identity {
    private static volatile String canonical;
    private static volatile String resolvedName;
    private static final AtomicBoolean resolving = new AtomicBoolean(false);

    private Identity() {
    }

    /**
     * The canonical, version-stable account UUID. Returns the raw session UUID until resolution
     * completes (correct already on the native route), or {@code null} when no player is present.
     * Calling this lazily kicks off resolution, so no external wiring is needed.
     */
    public static String self() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        String raw = mc.player.getUUID().toString();
        String name = mc.player.getName().getString();
        if (name != null && !name.equals(resolvedName)) {
            triggerResolve(name, raw);
        }
        return (canonical != null && name != null && name.equals(resolvedName)) ? canonical : raw;
    }

    /** True once the canonical id for the current account is settled (resolved or a decided fallback). */
    public static boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        String name = mc.player.getName().getString();
        return canonical != null && name != null && name.equals(resolvedName);
    }

    /** Reset on leaving a world so a different account re-resolves on the next join. */
    public static void reset() {
        canonical = null;
        resolvedName = null;
        resolving.set(false);
    }

    private static void triggerResolve(String name, String prev) {
        if (!resolving.compareAndSet(false, true)) {
            return;
        }
        String url = ModInfo.API_BASE + "/identity?name=" + enc(name) + "&prev=" + enc(prev);
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(6L)).GET().build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((r, t) -> {
                try {
                    if (r != null && r.statusCode() == 200) {
                        JsonObject o = JsonParser.parseString((String) r.body()).getAsJsonObject();
                        String uuid = o.has("uuid") && !o.get("uuid").isJsonNull() ? o.get("uuid").getAsString() : null;
                        if (uuid != null && !uuid.isBlank()) {
                            canonical = uuid;
                            resolvedName = name; // settled — stop re-triggering
                            return;
                        }
                    }
                    if (r != null && r.statusCode() == 404) {
                        // Non-premium / unknown name: keep the session UUID for this account, don't retry.
                        canonical = prev;
                        resolvedName = name;
                    }
                    // Otherwise (network / 5xx): leave unresolved so a later call retries.
                } catch (Exception e) {
                    // malformed response — retry on a later call
                } finally {
                    resolving.set(false);
                }
            });
        } catch (Exception e) {
            resolving.set(false);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
