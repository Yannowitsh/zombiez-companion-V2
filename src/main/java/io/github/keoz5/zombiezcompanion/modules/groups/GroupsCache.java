package io.github.keoz5.zombiezcompanion.modules.groups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory snapshot of the local player's group, fetched from {@code GET /group?uuid=<mcuuid>}.
 * A player is in at most one group at a time. All uuids are real Minecraft account UUIDs.
 */
public final class GroupsCache {
    private static volatile Group group = null;
    private static volatile List<Invite> invites = List.of();

    private GroupsCache() {
    }

    public static Group group() {
        return group;
    }

    public static List<Invite> invites() {
        return invites;
    }

    public static boolean inGroup() {
        return group != null;
    }

    /** Account uuids of the current group's members (empty when not in a group). */
    public static Set<String> memberUuids() {
        Group g = group;
        if (g == null) {
            return Set.of();
        }
        HashSet<String> s = new HashSet<String>();
        for (Member m : g.members()) {
            s.add(m.uuid());
        }
        return s;
    }

    public static void clear() {
        group = null;
        invites = List.of();
    }

    public static void update(String json) {
        if (json == null) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
            group = GroupsCache.parseGroup(obj.has("group") ? obj.get("group") : null);
            invites = GroupsCache.parseInvites(obj.getAsJsonArray("invites"));
        }
        catch (Exception exception) {
            // keep the previous snapshot on a malformed response
        }
    }

    private static Group parseGroup(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            JsonObject o = el.getAsJsonObject();
            String id = o.has("id") ? o.get("id").getAsString() : null;
            String chief = o.has("chief") ? o.get("chief").getAsString() : "";
            if (id == null || id.isBlank()) {
                return null;
            }
            ArrayList<Member> members = new ArrayList<Member>();
            JsonArray arr = o.getAsJsonArray("members");
            if (arr != null) {
                for (JsonElement me : arr) {
                    JsonObject mo = me.getAsJsonObject();
                    String uuid = mo.has("uuid") ? mo.get("uuid").getAsString() : null;
                    if (uuid == null || uuid.isBlank()) continue;
                    String name = mo.has("name") ? mo.get("name").getAsString() : "?";
                    members.add(new Member(uuid, name));
                }
            }
            return new Group(id, chief, members);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static List<Invite> parseInvites(JsonArray arr) {
        ArrayList<Invite> list = new ArrayList<Invite>();
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                String gid = o.has("gid") ? o.get("gid").getAsString() : null;
                if (gid == null || gid.isBlank()) continue;
                String from = o.has("from") ? o.get("from").getAsString() : "";
                String fromName = o.has("fromName") ? o.get("fromName").getAsString() : "?";
                long at = o.has("at") ? o.get("at").getAsLong() : 0L;
                list.add(new Invite(gid, from, fromName, at));
            }
            catch (Exception exception) {
                // skip malformed entry
            }
        }
        return list;
    }

    public record Member(String uuid, String name) {
    }

    public record Group(String id, String chief, List<Member> members) {
    }

    public record Invite(String gid, String from, String fromName, long at) {
    }
}
