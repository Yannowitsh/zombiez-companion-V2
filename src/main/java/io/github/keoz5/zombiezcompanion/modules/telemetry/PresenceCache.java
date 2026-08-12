package io.github.keoz5.zombiezcompanion.modules.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public final class PresenceCache {
    private static volatile List<Presence> presences = List.of();

    private PresenceCache() {
    }

    public static List<Presence> presences() {
        return presences;
    }

    public static void clear() {
        presences = List.of();
    }

    public static void update(String json, String selfUuid) {
        if (json == null) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            JsonArray arr = obj.getAsJsonArray("presences");
            if (arr == null) {
                return;
            }
            ArrayList<Presence> list = new ArrayList<Presence>(arr.size());
            for (JsonElement el : arr) {
                JsonObject p = el.getAsJsonObject();
                String uuid = p.has("uuid") ? p.get("uuid").getAsString() : null;
                if (uuid == null || uuid.equals(selfUuid)) continue;
                String name = p.has("name") ? p.get("name").getAsString() : "?";
                double x = p.has("x") ? p.get("x").getAsDouble() : 0.0;
                double y = p.has("y") ? p.get("y").getAsDouble() : 0.0;
                double z = p.has("z") ? p.get("z").getAsDouble() : 0.0;
                String dim = p.has("dim") ? p.get("dim").getAsString() : "";
                String mcuuid = p.has("mcuuid") ? p.get("mcuuid").getAsString() : "";
                long lu = p.has("last_update") ? p.get("last_update").getAsLong() : 0L;
                list.add(new Presence(uuid, name, x, y, z, dim, mcuuid, lu));
            }
            presences = list;
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public record Presence(String uuid, String name, double x, double y, double z, String dim, String mcuuid, long lastUpdate) {
    }
}

