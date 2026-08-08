/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_5251
 */
package io.github.keoz5.zombiezcompanion.modules.dropalert;

import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_5251;

public final class DropClassifier {
    private static final Map<String, DropRarity> FOOD_DROPS = new HashMap<String, DropRarity>();
    private static final Set<String> GADGET_DROPS = new HashSet<String>();
    private static final List<Consumable> CONSUMABLES = new ArrayList<Consumable>();

    private DropClassifier() {
    }

    public static List<Consumable> consumables() {
        return CONSUMABLES;
    }

    public static boolean isConsumableHidden(String itemName, Set<String> hidden) {
        return hidden != null && !hidden.isEmpty() && hidden.contains(DropClassifier.normalizeName(itemName));
    }

    private static void registerFood(DropRarity rarity, String ... names) {
        for (String name : names) {
            FOOD_DROPS.put(DropClassifier.normalizeName(name), rarity);
            CONSUMABLES.add(new Consumable(name, true, rarity));
        }
    }

    private static void registerGadgets(String ... names) {
        for (String name : names) {
            GADGET_DROPS.add(DropClassifier.normalizeName(name));
            CONSUMABLES.add(new Consumable(name, false, DropRarity.COMMON));
        }
    }

    public static DropRarity foodRarity(String itemName) {
        return FOOD_DROPS.get(DropClassifier.normalizeName(itemName));
    }

    public static boolean isFood(String itemName) {
        return FOOD_DROPS.containsKey(DropClassifier.normalizeName(itemName));
    }

    public static boolean isGadget(String itemName) {
        return GADGET_DROPS.contains(DropClassifier.normalizeName(itemName));
    }

    public static String normalizeName(String input) {
        String text = input == null ? "" : input.replace("\u0153", "oe").replace("\u0152", "oe").replace("\u00e6", "ae").replace("\u00c6", "ae");
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static DropRarity rarityOf(class_1799 stack) {
        Integer rgb = DropClassifier.firstColor(stack.method_7964());
        return rgb == null ? DropRarity.COMMON : DropClassifier.nearestRarity(rgb);
    }

    private static Integer firstColor(class_2561 text) {
        class_5251 color = text.method_10866().method_10973();
        if (color != null) {
            return color.method_27716();
        }
        for (class_2561 sibling : text.method_10855()) {
            Integer c = DropClassifier.firstColor(sibling);
            if (c == null) continue;
            return c;
        }
        return null;
    }

    private static DropRarity nearestRarity(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        if (r > 220 && g > 220 && b > 220) {
            return DropRarity.COMMON;
        }
        if (r > 120 && r <= 210 && g < 80 && b < 80) {
            return DropRarity.PRIMAL;
        }
        if (r > 210 && g < 120 && b < 130) {
            return DropRarity.EXALTED;
        }
        if (r > 210 && g > 120 && b < 110) {
            return DropRarity.LEGENDARY;
        }
        if (r > 200 && g < 150 && b > 150) {
            return DropRarity.MYTHIC;
        }
        if (r > 110 && g < 130 && b > 130) {
            return DropRarity.EPIC;
        }
        if (b > 150 && r < 150) {
            return DropRarity.RARE;
        }
        if (g > 150 && r < 170 && b < 170) {
            return DropRarity.UNCOMMON;
        }
        DropRarity best = DropRarity.COMMON;
        int bestDistance = Integer.MAX_VALUE;
        for (DropRarity rarity : DropRarity.values()) {
            int dist = DropClassifier.colorDistance(rgb, rarity.colorRgb);
            if (dist >= bestDistance) continue;
            bestDistance = dist;
            best = rarity;
        }
        return best;
    }

    private static int colorDistance(int a, int b) {
        int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
        int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    static {
        DropClassifier.registerFood(DropRarity.LEGENDARY, "Poulet Dor\u00e9 du Fermier", "Agneau des Dieux", "C\u00f4te de Porc Parfaite", "Boeuf de Kobe Apocalyptique", "Esprit de la Ch\u00e8vre Ancestrale", "Essence du Cheval de Feu", "Patate Maudite", "Lapin Dor\u00e9 du Destin", "Seringue d'Adr\u00e9naline");
        DropClassifier.registerFood(DropRarity.EPIC, "Stim-Pack M\u00e9dical", "Fromage de Ch\u00e8vre Vieilli", "Coeur de Destrier", "Patte de Lapin Enchant\u00e9e", "Pomme Dor\u00e9e Artisanale");
        DropClassifier.registerFood(DropRarity.RARE, "Boisson \u00c9nergisante", "Festin du Survivant", "Ailes \u00c9pic\u00e9es du Phoenix", "Festin de l'\u00c9leveur", "Rago\u00fbt de l'Alpiniste", "Kit de Premiers Soins", "Oeuf Dor\u00e9", "Festin du Cavalier");
        DropClassifier.registerFood(DropRarity.UNCOMMON, "Sandwich Emball\u00e9", "Ration Militaire", "Steak Juteux", "Poulet Grill\u00e9", "Jambon Fum\u00e9");
        DropClassifier.registerFood(DropRarity.COMMON, "Pilon de Poulet", "Cuisse de Lapin", "C\u00f4te de Porc", "C\u00f4te de Ch\u00e8vre", "C\u00f4telette d'Agneau", "Bo\u00eete de Conserve", "Banane", "Barre \u00c9nerg\u00e9tique", "Viande de Cheval");
        DropClassifier.registerGadgets("Bandage", "Kit d'Adr\u00e9naline", "Bombe Incendiaire", "Tourelle Golem", "Bocal d'Acide", "Grenade TNT", "Antidote", "Pi\u00e8ge de Toiles", "Jetpack", "Leurre", "Perle Instable", "Fleur d'Infestation");
    }

    public record Consumable(String name, boolean food, DropRarity rarity) {
    }
}

