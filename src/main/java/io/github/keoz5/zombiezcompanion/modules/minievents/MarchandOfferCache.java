package io.github.keoz5.zombiezcompanion.modules.minievents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropClassifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The current shared "what's for sale" of the traveling merchant: the featured items one mod user
 * captured by opening the shop, fetched by everyone else and rendered as icons next to the merchant
 * timer. The SUPER Marchand's stock is global for a given spawn, so a single latest-wins entry is
 * enough. Reconstructs a render {@link ItemStack} from each item's base registry id.
 */
public final class MarchandOfferCache {
    private static volatile List<OfferItem> items = List.of();
    private static volatile long at;
    private static volatile long expiresAt;
    private static volatile int zone;
    // Local, per-player "sold out" tracking, by normalized item name: the buyer gets a personal
    // "[Marchand] Achat reussi : <name>" chat line, so we cross the matching icon. Stays local (buying
    // is personal) and persists after closing/leaving, so you can tell from afar what you still need.
    private static final Set<String> sold = ConcurrentHashMap.newKeySet();

    private MarchandOfferCache() {
    }

    public static List<OfferItem> items() {
        return items;
    }

    public static long at() {
        return at;
    }

    /** Epoch ms when the merchant leaves; the icons disappear at this point. */
    public static long expiresAt() {
        return expiresAt;
    }

    public static int zone() {
        return zone;
    }

    /**
     * Fold an observed offer into the cache. While the same merchant is alive the item set only ever
     * GROWS (union by id): a scout who bought some items and reopens sees a depleted grid, but that must
     * never shrink the shared "what's for sale" for everyone else (buying is personal). A fresh merchant
     * (previous offer expired) replaces everything and resets the local sold tracking.
     */
    public static void ingest(long at_, long expiresAt_, int zone_, List<OfferItem> its) {
        long now = System.currentTimeMillis();
        if (expiresAt > now && !items.isEmpty()) {
            ArrayList<OfferItem> merged = new ArrayList<OfferItem>(items);
            HashSet<String> have = new HashSet<String>();
            for (OfferItem it : items) {
                have.add(it.id());
            }
            for (OfferItem it : its) {
                if (have.add(it.id())) {
                    merged.add(it);
                }
            }
            items = List.copyOf(merged);
            if (expiresAt_ > expiresAt) {
                expiresAt = expiresAt_;
            }
        } else {
            items = List.copyOf(its);
            at = at_;
            expiresAt = expiresAt_;
            zone = zone_;
            sold.clear();
        }
    }

    public static void clear() {
        items = List.of();
        at = 0L;
        expiresAt = 0L;
        zone = 0;
        sold.clear();
    }

    /** Mark an item bought, from the "[Marchand] Achat reussi : &lt;name&gt;" chat line (normalized match). */
    public static void markSold(String name) {
        String n = DropClassifier.normalizeName(name);
        if (!n.isEmpty()) {
            sold.add(n);
        }
    }

    /** True once we've bought this offer item (matched by normalized name). Local to this player. */
    public static boolean isSold(OfferItem it) {
        return it != null && sold.contains(DropClassifier.normalizeName(it.name()));
    }

    /** Parse the backend {@code /marchand/offer} response into the cache. */
    public static void update(String json) {
        if (json == null) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString((String) json).getAsJsonObject();
            JsonArray arr = o.has("items") ? o.getAsJsonArray("items") : null;
            List<OfferItem> list = new ArrayList<OfferItem>();
            if (arr != null) {
                for (JsonElement el : arr) {
                    JsonObject it = el.getAsJsonObject();
                    String id = it.has("id") ? it.get("id").getAsString() : null;
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    String name = it.has("name") ? it.get("name").getAsString() : "";
                    String rarity = it.has("rarity") ? it.get("rarity").getAsString() : "";
                    int count = it.has("count") ? it.get("count").getAsInt() : 1;
                    list.add(new OfferItem(id, name, rarity, count));
                }
            }
            long newAt = o.has("at") ? o.get("at").getAsLong() : 0L;
            long newExp = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : 0L;
            int newZone = o.has("zone") ? o.get("zone").getAsInt() : 0;
            // Merge (never shrink a live merchant's offer); ingest() resets sold tracking on a fresh one.
            ingest(newAt, newExp, newZone, list);
        } catch (Exception ignored) {
            // malformed response — keep the previous cache
        }
    }

    /** Reconstruct a render stack from an offer item's base id (falls back to paper). */
    public static ItemStack stackOf(OfferItem it) {
        Identifier id = it == null ? null : Identifier.tryParse((String) it.id());
        if (id != null) {
            Item item = (Item) BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack((net.minecraft.world.level.ItemLike) item);
            }
        }
        return new ItemStack((net.minecraft.world.level.ItemLike) Items.PAPER);
    }

    public record OfferItem(String id, String name, String rarity, int count) {
    }
}
