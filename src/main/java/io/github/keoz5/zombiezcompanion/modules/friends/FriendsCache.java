package io.github.keoz5.zombiezcompanion.modules.friends;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory snapshot of the friend graph fetched from {@code GET /friends?uuid=<mcuuid>}.
 * All three lists are keyed on players' real Minecraft account UUIDs.
 */
public final class FriendsCache {
    private static volatile List<Friend> friends = List.of();
    private static volatile List<Request> incoming = List.of();
    private static volatile List<Request> outgoing = List.of();

    private FriendsCache() {
    }

    public static List<Friend> friends() {
        return friends;
    }

    public static List<Request> incoming() {
        return incoming;
    }

    public static List<Request> outgoing() {
        return outgoing;
    }

    public static void clear() {
        friends = List.of();
        incoming = List.of();
        outgoing = List.of();
    }

    public static void update(String json) {
        if (json == null) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            friends = FriendsCache.parseFriends(obj.getAsJsonArray("friends"));
            incoming = FriendsCache.parseRequests(obj.getAsJsonArray("incoming"));
            outgoing = FriendsCache.parseRequests(obj.getAsJsonArray("outgoing"));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static List<Friend> parseFriends(JsonArray arr) {
        ArrayList<Friend> list = new ArrayList<Friend>();
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                String uuid = o.has("uuid") ? o.get("uuid").getAsString() : null;
                if (uuid == null || uuid.isBlank()) continue;
                String name = o.has("name") ? o.get("name").getAsString() : "?";
                list.add(new Friend(uuid, name));
            }
            catch (Exception exception) {
                // skip malformed entry
            }
        }
        return list;
    }

    private static List<Request> parseRequests(JsonArray arr) {
        ArrayList<Request> list = new ArrayList<Request>();
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                String uuid = o.has("uuid") ? o.get("uuid").getAsString() : null;
                if (uuid == null || uuid.isBlank()) continue;
                String name = o.has("name") ? o.get("name").getAsString() : "?";
                long at = o.has("at") ? o.get("at").getAsLong() : 0L;
                list.add(new Request(uuid, name, at));
            }
            catch (Exception exception) {
                // skip malformed entry
            }
        }
        return list;
    }

    public record Friend(String uuid, String name) {
    }

    public record Request(String uuid, String name, long at) {
    }
}
