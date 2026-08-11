package io.github.keoz5.zombiezcompanion.modules.groups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory snapshot of the active group pings (one per member), fetched from
 * {@code GET /group/pings?uuid=<mcuuid>}. Rendered as shared waypoints for the whole group.
 */
public final class PingCache {
    private static volatile List<Ping> pings = List.of();

    private PingCache() {
    }

    public static List<Ping> pings() {
        return pings;
    }

    public static void clear() {
        pings = List.of();
    }

    public static void update(String json) {
        if (json == null) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            JsonArray arr = obj.getAsJsonArray("pings");
            ArrayList<Ping> list = new ArrayList<Ping>();
            if (arr != null) {
                for (JsonElement el : arr) {
                    try {
                        JsonObject o = el.getAsJsonObject();
                        String uuid = o.has("uuid") ? o.get("uuid").getAsString() : null;
                        if (uuid == null || uuid.isBlank()) continue;
                        String name = o.has("name") ? o.get("name").getAsString() : "?";
                        double x = o.has("x") ? o.get("x").getAsDouble() : 0.0;
                        double y = o.has("y") ? o.get("y").getAsDouble() : 0.0;
                        double z = o.has("z") ? o.get("z").getAsDouble() : 0.0;
                        String dim = o.has("dim") ? o.get("dim").getAsString() : "";
                        list.add(new Ping(uuid, name, x, y, z, dim));
                    }
                    catch (Exception exception) {
                        // skip malformed entry
                    }
                }
            }
            pings = list;
        }
        catch (Exception exception) {
            // keep previous snapshot on a malformed response
        }
    }

    public record Ping(String uuid, String name, double x, double y, double z, String dim) {
    }
}
