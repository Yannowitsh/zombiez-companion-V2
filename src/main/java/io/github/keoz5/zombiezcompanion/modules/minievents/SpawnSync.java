package io.github.keoz5.zombiezcompanion.modules.minievents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public final class SpawnSync {
    private static volatile List<Long> marchand = List.of();
    private static volatile List<Long> worldBoss = List.of();

    private SpawnSync() {
    }

    public static List<Long> marchand() {
        return marchand;
    }

    public static List<Long> worldBoss() {
        return worldBoss;
    }

    public static void update(String json) {
        if (json == null) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            List<Long> m = SpawnSync.parse(obj.getAsJsonArray("marchand"));
            List<Long> b = SpawnSync.parse(obj.getAsJsonArray("world_boss"));
            if (m != null) {
                marchand = m;
            }
            if (b != null) {
                worldBoss = b;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void addLocal(boolean boss, long at) {
        List<Long> src;
        List<Long> list = src = boss ? worldBoss : marchand;
        if (!src.isEmpty() && at - src.get(src.size() - 1) < 300000L) {
            return;
        }
        ArrayList<Long> copy = new ArrayList<Long>(src);
        copy.add(at);
        while (copy.size() > 30) {
            copy.remove(0);
        }
        if (boss) {
            worldBoss = copy;
        } else {
            marchand = copy;
        }
    }

    public static void clear() {
        marchand = List.of();
        worldBoss = List.of();
    }

    private static List<Long> parse(JsonArray arr) {
        if (arr == null) {
            return null;
        }
        ArrayList<Long> out = new ArrayList<Long>(arr.size());
        for (JsonElement e : arr) {
            try {
                out.add(e.getAsLong());
            }
            catch (Exception exception) {}
        }
        return out;
    }
}

