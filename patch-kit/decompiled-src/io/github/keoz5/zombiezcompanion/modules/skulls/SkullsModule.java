/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.event.player.AttackBlockCallback
 *  net.fabricmc.fabric.api.event.player.UseBlockCallback
 *  net.minecraft.class_1269
 *  net.minecraft.class_1937
 *  net.minecraft.class_2190
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_2680
 *  net.minecraft.class_310
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.skulls;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.config.SkullsConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsOptionsScreen;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.class_1269;
import net.minecraft.class_1937;
import net.minecraft.class_2190;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_437;

public final class SkullsModule
implements Module {
    public static final String ID = "skulls";
    private static final String WAYPOINT_PREFIX = "skull-";
    public static final int BEACON_COLOR = 16766282;
    private ConfigManager configManager;
    private int routeZone = -1;
    private final List<String> routeOrder = new ArrayList<String>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Cr\u00e2nes";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.skulls.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PROGRESSION;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("cr\u00e2ne", "crane", "t\u00eate", "skull", "balise", "route", "progression", "visit\u00e9", "collection", "zone");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            this.handleClick(world, hit.method_17777());
            return class_1269.field_5811;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            this.handleClick(world, pos);
            return class_1269.field_5811;
        });
    }

    private void handleClick(class_1937 world, class_2338 pos) {
        if (this.configManager == null || pos == null || world == null) {
            return;
        }
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null || !mm.isEnabled(ID)) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        class_2680 state = world.method_8320(pos);
        if (!(state.method_26204() instanceof class_2190)) {
            return;
        }
        ZombieZMapData.Point skull = SkullsModule.nearestSkull(pos, 2.5);
        if (skull == null) {
            return;
        }
        if (this.isVisited(skull.id())) {
            return;
        }
        this.setVisited(skull.id(), true);
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.field_1724 != null) {
            mc.field_1724.method_5783((class_3414)class_3417.field_15015.comp_349(), 0.4f, 1.6f);
        }
    }

    private static ZombieZMapData.Point nearestSkull(class_2338 pos, double maxDist) {
        double bestSq = maxDist * maxDist;
        ZombieZMapData.Point best = null;
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point p : z.skulls()) {
                double dz;
                double dy;
                double dx = p.x() - (double)pos.method_10263();
                double sq = dx * dx + (dy = p.y() - (double)pos.method_10264()) * dy + (dz = p.z() - (double)pos.method_10260()) * dz;
                if (!(sq < bestSq)) continue;
                bestSq = sq;
                best = p;
            }
        }
        return best;
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
        return new SkullsOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        if (this.configManager != null) {
            this.removeAllSkullWaypoints();
        }
        this.routeZone = -1;
        this.routeOrder.clear();
    }

    public SkullsConfig config() {
        return this.configManager.get().skulls;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public static SkullsModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof SkullsModule)) continue;
            SkullsModule s = (SkullsModule)m;
            return s;
        }
        return null;
    }

    public boolean isVisited(String skullId) {
        return this.config().visited.contains(skullId);
    }

    public void setVisited(String skullId, boolean visited) {
        boolean changed;
        SkullsConfig cfg = this.config();
        boolean bl = changed = visited ? cfg.visited.add(skullId) : cfg.visited.remove(skullId);
        if (changed) {
            if (visited) {
                this.removeWaypointFor(skullId);
                this.advanceGuideIfNeeded(skullId);
            }
            this.configManager.save();
        }
    }

    public void markZoneVisited(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return;
        }
        SkullsConfig cfg = this.config();
        for (ZombieZMapData.Point p : z.skulls()) {
            cfg.visited.add(p.id());
            this.removeWaypointFor(p.id());
        }
        this.configManager.save();
    }

    public void unmarkZoneVisited(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return;
        }
        SkullsConfig cfg = this.config();
        for (ZombieZMapData.Point p : z.skulls()) {
            cfg.visited.remove(p.id());
        }
        this.configManager.save();
    }

    public void resetAllVisited() {
        this.config().visited.clear();
        this.configManager.save();
    }

    public static String skullNumber(ZombieZMapData.Point p) {
        int hash = p.id().indexOf(35);
        return hash >= 0 ? p.id().substring(hash + 1) : "?";
    }

    public int visitedCount(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return 0;
        }
        int n = 0;
        for (ZombieZMapData.Point p : z.skulls()) {
            if (!this.isVisited(p.id())) continue;
            ++n;
        }
        return n;
    }

    public int totalSkullsCount(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        return z == null ? 0 : z.skulls().length;
    }

    public int totalVisited() {
        int n = 0;
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point p : z.skulls()) {
                if (!this.isVisited(p.id())) continue;
                ++n;
            }
        }
        return n;
    }

    public int totalSkulls() {
        int n = 0;
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            n += z.skulls().length;
        }
        return n;
    }

    public boolean hasZoneWaypoints(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return false;
        }
        MapConfig map = this.configManager.get().map;
        for (ZombieZMapData.Point p : z.skulls()) {
            if (SkullsModule.waypointFor(map, p.id()) == null) continue;
            return true;
        }
        return false;
    }

    public void addZoneWaypoints(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return;
        }
        SkullsConfig cfg = this.config();
        MapConfig map = this.configManager.get().map;
        for (ZombieZMapData.Point p : z.skulls()) {
            if (cfg.hideVisitedBeacons && this.isVisited(p.id())) continue;
            this.addSkullWaypoint(p);
        }
        this.configManager.save();
    }

    public boolean hasWaypoint(ZombieZMapData.Point p) {
        return SkullsModule.waypointFor(this.configManager.get().map, p.id()) != null;
    }

    public void toggleSkullWaypoint(ZombieZMapData.Point p) {
        if (this.hasWaypoint(p)) {
            this.removeWaypointFor(p.id());
        } else {
            this.addSkullWaypoint(p);
        }
        this.configManager.save();
    }

    private void addSkullWaypoint(ZombieZMapData.Point p) {
        this.addSkullWaypoint(p, p.label());
    }

    private void addSkullWaypoint(ZombieZMapData.Point p, String label) {
        MapConfig map = this.configManager.get().map;
        if (SkullsModule.waypointFor(map, p.id()) != null) {
            return;
        }
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = WAYPOINT_PREFIX + p.id();
        wp.label = label;
        wp.x = p.x();
        wp.y = p.y();
        wp.z = p.z();
        wp.colorRgb = 16766282;
        wp.createdAt = System.currentTimeMillis();
        wp.visible = true;
        map.waypoints.add(wp);
    }

    public ZombieZMapData.Point nearestUnvisited(double px, double pz) {
        ZombieZMapData.Point best = null;
        double bestSq = Double.MAX_VALUE;
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point p : z.skulls()) {
                double dz;
                double dx;
                double d;
                if (this.isVisited(p.id()) || !((d = (dx = p.x() - px) * dx + (dz = p.z() - pz) * dz) < bestSq)) continue;
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    public boolean guideToNearestUnvisited() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null) {
            return false;
        }
        ZombieZMapData.Point p = this.nearestUnvisited(mc.field_1724.method_23317(), mc.field_1724.method_23321());
        if (p == null) {
            return false;
        }
        this.routeZone = -1;
        this.setGuide(p);
        this.configManager.save();
        return true;
    }

    private void setGuide(ZombieZMapData.Point p) {
        MapConfig.GuideTarget gt = new MapConfig.GuideTarget();
        gt.label = p.label();
        gt.type = "Crane";
        gt.x = p.x();
        gt.y = p.y();
        gt.z = p.z();
        gt.colorRgb = 16766282;
        this.configManager.get().map.guideTarget = gt;
    }

    private static ZombieZMapData.Point pointById(String id) {
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point p : z.skulls()) {
                if (!p.id().equals(id)) continue;
                return p;
            }
        }
        return null;
    }

    private void advanceGuideIfNeeded(String visitedId) {
        ZombieZMapData.Point next;
        MapConfig map = this.configManager.get().map;
        MapConfig.GuideTarget gt = map.guideTarget;
        if (gt == null || !"Crane".equals(gt.type)) {
            return;
        }
        ZombieZMapData.Point p = SkullsModule.pointById(visitedId);
        if (p == null) {
            return;
        }
        if (Math.abs(gt.x - p.x()) > 0.6 || Math.abs(gt.z - p.z()) > 0.6) {
            return;
        }
        ZombieZMapData.Point point = next = this.routeZone >= 0 ? this.nextInRoute() : this.nearestUnvisited(p.x(), p.z());
        if (next != null) {
            this.setGuide(next);
        } else {
            map.guideTarget = null;
            this.routeZone = -1;
            this.routeOrder.clear();
        }
    }

    private ZombieZMapData.Point nextInRoute() {
        for (String id : this.routeOrder) {
            ZombieZMapData.Point p;
            if (this.isVisited(id) || (p = SkullsModule.pointById(id)) == null) continue;
            return p;
        }
        return null;
    }

    public boolean buildZoneRoute(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return false;
        }
        this.removeZoneWaypoints(zoneNum);
        class_310 mc = class_310.method_1551();
        double px = mc.field_1724 != null ? mc.field_1724.method_23317() : 0.0;
        double pz = mc.field_1724 != null ? mc.field_1724.method_23321() : 0.0;
        ArrayList<ZombieZMapData.Point> pts = new ArrayList<ZombieZMapData.Point>();
        for (ZombieZMapData.Point p : z.skulls()) {
            if (this.isVisited(p.id())) continue;
            pts.add(p);
        }
        if (pts.isEmpty()) {
            this.routeZone = -1;
            this.routeOrder.clear();
            this.configManager.get().map.guideTarget = null;
            this.configManager.save();
            return false;
        }
        List<ZombieZMapData.Point> ordered = SkullsModule.optimizeRoute(pts, px, pz);
        this.routeOrder.clear();
        for (ZombieZMapData.Point p : ordered) {
            this.routeOrder.add(p.id());
        }
        this.routeZone = zoneNum;
        this.setGuide(ordered.get(0));
        this.configManager.save();
        return true;
    }

    private static List<ZombieZMapData.Point> optimizeRoute(List<ZombieZMapData.Point> pts, double startX, double startZ) {
        int n = pts.size();
        boolean[] used = new boolean[n];
        ArrayList<ZombieZMapData.Point> route = new ArrayList<ZombieZMapData.Point>(n);
        double cx = startX;
        double cz = startZ;
        for (int i = 0; i < n; ++i) {
            int best = -1;
            double bestSq = Double.MAX_VALUE;
            for (int j = 0; j < n; ++j) {
                double dz;
                double dx;
                double d;
                if (used[j] || !((d = (dx = pts.get(j).x() - cx) * dx + (dz = pts.get(j).z() - cz) * dz) < bestSq)) continue;
                bestSq = d;
                best = j;
            }
            used[best] = true;
            route.add(pts.get(best));
            cx = pts.get(best).x();
            cz = pts.get(best).z();
        }
        SkullsModule.twoOpt(route, startX, startZ);
        return route;
    }

    private static void twoOpt(List<ZombieZMapData.Point> route, double startX, double startZ) {
        int n = route.size();
        if (n < 3) {
            return;
        }
        boolean improved = true;
        int guard = 0;
        while (improved && guard++ < 60) {
            improved = false;
            for (int i = 0; i < n - 1; ++i) {
                double ax = i == 0 ? startX : route.get(i - 1).x();
                double az = i == 0 ? startZ : route.get(i - 1).z();
                double bx = route.get(i).x();
                double bz = route.get(i).z();
                for (int k = i + 1; k < n; ++k) {
                    double ckx = route.get(k).x();
                    double ckz = route.get(k).z();
                    boolean hasNext = k < n - 1;
                    double nx = hasNext ? route.get(k + 1).x() : 0.0;
                    double nz = hasNext ? route.get(k + 1).z() : 0.0;
                    double removed = SkullsModule.dist(ax, az, bx, bz) + (hasNext ? SkullsModule.dist(ckx, ckz, nx, nz) : 0.0);
                    double added = SkullsModule.dist(ax, az, ckx, ckz) + (hasNext ? SkullsModule.dist(bx, bz, nx, nz) : 0.0);
                    if (!(added + 1.0E-6 < removed)) continue;
                    SkullsModule.reverse(route, i, k);
                    improved = true;
                    bx = route.get(i).x();
                    bz = route.get(i).z();
                }
            }
        }
    }

    private static void reverse(List<ZombieZMapData.Point> route, int i, int k) {
        while (i < k) {
            ZombieZMapData.Point tmp = route.get(i);
            route.set(i, route.get(k));
            route.set(k, tmp);
            ++i;
            --k;
        }
    }

    private static double dist(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public ZombieZMapData.Point nearestUnvisitedInZone(int zoneNum, double px, double pz) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return null;
        }
        ZombieZMapData.Point best = null;
        double bestSq = Double.MAX_VALUE;
        for (ZombieZMapData.Point p : z.skulls()) {
            double dz;
            double dx;
            double d;
            if (this.isVisited(p.id()) || !((d = (dx = p.x() - px) * dx + (dz = p.z() - pz) * dz) < bestSq)) continue;
            bestSq = d;
            best = p;
        }
        return best;
    }

    public void removeZoneWaypoints(int zoneNum) {
        ZombieZMapData.Zone z = SkullsModule.zone(zoneNum);
        if (z == null) {
            return;
        }
        MapConfig map = this.configManager.get().map;
        for (ZombieZMapData.Point p : z.skulls()) {
            this.removeWaypointFor(p.id());
        }
        this.configManager.save();
    }

    public void removeAllSkullWaypoints() {
        MapConfig map = this.configManager.get().map;
        map.waypoints.removeIf(w -> w.id != null && w.id.startsWith(WAYPOINT_PREFIX));
        this.configManager.save();
    }

    private void removeWaypointFor(String skullId) {
        MapConfig map = this.configManager.get().map;
        String wpId = WAYPOINT_PREFIX + skullId;
        map.waypoints.removeIf(w -> wpId.equals(w.id));
    }

    private static MapConfig.Waypoint waypointFor(MapConfig map, String skullId) {
        String wpId = WAYPOINT_PREFIX + skullId;
        for (MapConfig.Waypoint w : map.waypoints) {
            if (!wpId.equals(w.id)) continue;
            return w;
        }
        return null;
    }

    public static boolean isSkullWaypointId(String waypointId) {
        return waypointId != null && waypointId.startsWith(WAYPOINT_PREFIX);
    }

    public List<ZombieZMapData.Zone> zonesWithSkulls() {
        ArrayList<ZombieZMapData.Zone> list = new ArrayList<ZombieZMapData.Zone>();
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            if (z.skulls() == null || z.skulls().length <= 0) continue;
            list.add(z);
        }
        return list;
    }

    private static ZombieZMapData.Zone zone(int zoneNum) {
        for (ZombieZMapData.Zone z : ZombieZMapData.ZONES) {
            if (z.num() != zoneNum) continue;
            return z;
        }
        return null;
    }
}

