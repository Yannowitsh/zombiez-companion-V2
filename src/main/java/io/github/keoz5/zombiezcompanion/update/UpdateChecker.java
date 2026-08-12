package io.github.keoz5.zombiezcompanion.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Polls a per-branch {@code update.json} manifest on GitHub (raw) every minute to detect a newer build,
 * and downloads the release-asset jar into {@code <gameDir>/zzc-updates/} on demand. Which branch to read
 * is derived from the running Minecraft version. All state is client-global and read by the UI.
 */
public final class UpdateChecker {
    private static final long CHECK_INTERVAL_MS = 60000L;
    private static final String RAW_BASE = "https://raw.githubusercontent.com/Yannowitsh/zombiez-companion-V2";
    private static final String UPDATE_DIR = "zzc-updates";

    private static volatile boolean checked;
    private static volatile boolean available;
    private static volatile String latestVersion = "";
    private static volatile String jarUrl = "";
    private static volatile boolean downloading;
    private static long lastCheckMs;
    private static boolean inFlight;

    private UpdateChecker() {
    }

    public static boolean available() {
        return available;
    }

    public static boolean checked() {
        return checked;
    }

    public static boolean downloading() {
        return downloading;
    }

    public static String latestVersion() {
        return latestVersion;
    }

    public static Path updateDir() {
        return FabricLoader.getInstance().getGameDir().resolve(UPDATE_DIR);
    }

    /** Called each client tick; throttled to one manifest fetch per minute. */
    public static void tick() {
        if (downloading || inFlight) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastCheckMs != 0L && now - lastCheckMs < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckMs = now;
        UpdateChecker.checkAsync();
    }

    private static void checkAsync() {
        inFlight = true;
        String url = RAW_BASE + "/" + UpdateChecker.branch() + "/update.json?t=" + System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(6L)).GET().build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                try {
                    if (err == null && resp != null && resp.statusCode() == 200) {
                        UpdateChecker.parse((String)resp.body());
                    }
                }
                catch (Exception exception) {
                    // ignore malformed manifest / transient errors
                }
                finally {
                    checked = true;
                    inFlight = false;
                }
            });
        }
        catch (Exception e) {
            inFlight = false;
        }
    }

    private static void parse(String body) {
        JsonObject o = JsonParser.parseString((String)body).getAsJsonObject();
        String ver = o.has("version") ? o.get("version").getAsString() : "";
        String jar = o.has("jar") ? o.get("jar").getAsString() : "";
        if (ver.isEmpty() || jar.isEmpty()) {
            return;
        }
        latestVersion = ver;
        jarUrl = jar;
        available = UpdateChecker.isNewer(ver, UpdateChecker.localVersion());
    }

    /** GitHub branch for the running Minecraft version (26.1.2 -> "26.1.2", 1.21.x -> "master"). */
    public static String branch() {
        String mc = FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("26.1.2");
        return mc.startsWith("1.21") ? "master" : mc;
    }

    public static String localVersion() {
        return FabricLoader.getInstance().getModContainer(ModInfo.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0");
    }

    static boolean isNewer(String remote, String local) {
        List<Integer> a = UpdateChecker.parseVer(remote);
        List<Integer> b = UpdateChecker.parseVer(local);
        int n = Math.max(a.size(), b.size());
        for (int i = 0; i < n; ++i) {
            int x = i < a.size() ? a.get(i) : 0;
            int y = i < b.size() ? b.get(i) : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static List<Integer> parseVer(String v) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (String part : v.split("[^0-9]+")) {
            if (part.isEmpty()) continue;
            try {
                out.add(Integer.valueOf(Integer.parseInt(part)));
            }
            catch (NumberFormatException numberFormatException) {
                // ignore
            }
        }
        return out;
    }

    /**
     * Downloads the update jar into {@code <gameDir>/zzc-updates/}. The callback receives (path, error)
     * with exactly one non-null. Uses a redirect-following client (release assets 302 to a CDN).
     */
    public static void downloadAsync(BiConsumer<Path, String> cb) {
        if (downloading || jarUrl.isEmpty()) {
            cb.accept(null, "no_update");
            return;
        }
        downloading = true;
        Path dir = UpdateChecker.updateDir();
        String name = jarUrl.substring(jarUrl.lastIndexOf(47) + 1);
        if (name.isEmpty() || !name.endsWith(".jar")) {
            name = "zombiezcompanionV2-update.jar";
        }
        Path target = dir.resolve(name);
        try {
            Files.createDirectories(dir);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(jarUrl)).timeout(Duration.ofSeconds(120L)).GET().build();
            HttpClients.DOWNLOAD.sendAsync(req, HttpResponse.BodyHandlers.ofFile(target)).whenComplete((resp, err) -> {
                downloading = false;
                if (err != null || resp == null || resp.statusCode() != 200) {
                    cb.accept(null, err != null ? err.getMessage() : "http " + (resp == null ? "?" : Integer.valueOf(resp.statusCode())));
                } else {
                    cb.accept((Path)resp.body(), null);
                }
            });
        }
        catch (Exception e) {
            downloading = false;
            cb.accept(null, e.getMessage());
        }
    }
}
