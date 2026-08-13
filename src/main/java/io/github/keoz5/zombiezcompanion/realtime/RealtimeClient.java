package io.github.keoz5.zombiezcompanion.realtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.modules.groups.GroupsCache;
import io.github.keoz5.zombiezcompanion.modules.groups.GroupsModule;
import io.github.keoz5.zombiezcompanion.modules.groups.PingCache;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.Minecraft;

/**
 * Persistent WebSocket link to the backend Hub (Durable Object). Connects while the player is on ZombieZ,
 * reconnects with backoff, keeps the link warm, and dispatches incoming messages (broadcast toasts now,
 * group pings later). Ticked once per client tick from {@link io.github.keoz5.zombiezcompanion.ZombieZCompanionClient}.
 */
public final class RealtimeClient {
    private static final long RECONNECT_MIN = 3000L;
    private static final long RECONNECT_MAX = 30000L;
    private static final long KEEPALIVE_MS = 30000L;

    private enum State { DISCONNECTED, CONNECTING, CONNECTED }

    private static volatile State state = State.DISCONNECTED;
    private static volatile WebSocket ws;
    private static long lastAttempt;
    private static long backoff = RECONNECT_MIN;
    private static long lastKeepalive;
    private static String sentGroup = "";

    private RealtimeClient() {
    }

    /** Drives connect/disconnect, keepalive, and group sync. Safe to call every tick. */
    public static void tick(Minecraft client) {
        boolean desired = client.player != null && client.level != null && ZombieZDetector.isOnZombieZ();
        long now = System.currentTimeMillis();

        if (!desired) {
            if (state != State.DISCONNECTED) {
                close();
            }
            return;
        }
        if (state == State.DISCONNECTED && now - lastAttempt >= backoff) {
            connect(client, now);
            return;
        }
        if (state == State.CONNECTED && ws != null) {
            if (now - lastKeepalive >= KEEPALIVE_MS) {
                lastKeepalive = now;
                trySend("KA");
            }
            String gid = currentGroup();
            if (!gid.equals(sentGroup)) {
                sentGroup = gid;
                trySend("{\"type\":\"group\",\"group\":\"" + esc(gid) + "\"}");
            }
        }
    }

    private static void connect(Minecraft client, long now) {
        lastAttempt = now;
        state = State.CONNECTING;
        String uuid = client.player.getUUID().toString();
        String name = client.player.getName().getString();
        String gid = currentGroup();
        sentGroup = gid;
        String base = ModInfo.API_BASE.replaceFirst("^http", "ws"); // https:// -> wss://
        String url = base + "/ws?uuid=" + enc(uuid) + "&name=" + enc(name) + "&group=" + enc(gid);
        try {
            HttpClients.SHARED.newWebSocketBuilder()
                .buildAsync(URI.create(url), new Listener())
                .whenComplete((sock, err) -> {
                    if (err != null || sock == null) {
                        state = State.DISCONNECTED;
                        backoff = Math.min(RECONNECT_MAX, backoff * 2);
                    } else {
                        ws = sock;
                        state = State.CONNECTED;
                        backoff = RECONNECT_MIN;
                        lastKeepalive = System.currentTimeMillis();
                        Log.info("Realtime: connecté au hub");
                    }
                });
        } catch (Exception e) {
            state = State.DISCONNECTED;
            backoff = Math.min(RECONNECT_MAX, backoff * 2);
        }
    }

    /** Close the link (on leaving ZombieZ / disconnect). Resets backoff so the next join reconnects promptly. */
    public static void close() {
        WebSocket s = ws;
        ws = null;
        state = State.DISCONNECTED;
        backoff = RECONNECT_MIN;
        sentGroup = "";
        if (s != null) {
            try { s.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); } catch (Exception e) { /* ignore */ }
        }
    }

    private static void trySend(String text) {
        WebSocket s = ws;
        if (s != null) {
            try { s.sendText(text, true); } catch (Exception e) { /* dropped; tick will reconnect */ }
        }
    }

    /** Instantly relay a group ping to connected members (in addition to the persisted POST). */
    public static void sendPing(double x, double y, double z, String dim, String cat) {
        trySend(String.format(java.util.Locale.ROOT,
            "{\"type\":\"ping\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"dim\":\"%s\",\"cat\":\"%s\"}", x, y, z, esc(dim), esc(cat)));
    }

    /** Instantly clear our group ping for connected members. */
    public static void sendPingClear() {
        trySend("{\"type\":\"ping\",\"clear\":true}");
    }

    /** Instantly relay the chief's follow-action (refuge/spawn) to connected members (in addition to the POST). */
    public static void sendAction(String action, String arg) {
        trySend("{\"type\":\"action\",\"action\":\"" + esc(action) + "\",\"arg\":\"" + esc(arg) + "\"}");
    }

    private static String currentGroup() {
        GroupsCache.Group g = GroupsCache.group();
        return g == null ? "" : g.id();
    }

    private static void handle(String json) {
        if (json == null || json.isEmpty() || "KO".equals(json)) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString((String) json).getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "";
            if ("toast".equals(type)) {
                String text = o.has("text") ? o.get("text").getAsString() : "";
                String sev = o.has("severity") ? o.get("severity").getAsString() : "info";
                long ms = o.has("ms") ? o.get("ms").getAsLong() : 5000L;
                BroadcastToasts.push(text, sev, ms);
            } else if ("ping".equals(type)) {
                if (!GroupsCache.inGroup()) {
                    return;
                }
                String from = o.has("from") ? o.get("from").getAsString() : "";
                if (from.isEmpty()) {
                    return;
                }
                if (o.has("clear") && o.get("clear").getAsBoolean()) {
                    PingCache.remove(from);
                } else {
                    String name = o.has("name") ? o.get("name").getAsString() : "?";
                    double x = o.has("x") ? o.get("x").getAsDouble() : 0.0;
                    double y = o.has("y") ? o.get("y").getAsDouble() : 0.0;
                    double z = o.has("z") ? o.get("z").getAsDouble() : 0.0;
                    String dim = o.has("dim") ? o.get("dim").getAsString() : "";
                    String cat = o.has("cat") ? o.get("cat").getAsString() : "";
                    PingCache.put(new PingCache.Ping(from, name, x, y, z, dim, cat));
                }
            } else if ("action".equals(type)) {
                if (!GroupsCache.inGroup()) {
                    return;
                }
                String from = o.has("from") ? o.get("from").getAsString() : "";
                String action = o.has("action") ? o.get("action").getAsString() : "";
                String arg = o.has("arg") ? o.get("arg").getAsString() : "";
                if (from.isEmpty() || action.isEmpty()) {
                    return;
                }
                GroupsModule gm = GroupsModule.get();
                if (gm != null) {
                    gm.onRealtimeAction(from, action, arg);
                }
            }
        } catch (Exception e) {
            // ignore malformed frame
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Reassembles text frames and dispatches complete messages. */
    private static final class Listener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1L);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            this.buf.append(data);
            if (last) {
                String msg = this.buf.toString();
                this.buf.setLength(0);
                RealtimeClient.handle(msg);
            }
            webSocket.request(1L);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (RealtimeClient.ws == webSocket) {
                RealtimeClient.ws = null;
                RealtimeClient.state = State.DISCONNECTED;
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (RealtimeClient.ws == webSocket) {
                RealtimeClient.ws = null;
                RealtimeClient.state = State.DISCONNECTED;
            }
        }
    }
}
