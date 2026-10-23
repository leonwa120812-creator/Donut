package de.donutclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Merkt sich, was DEIN Client gesehen hat: wie viele verschiedene Spieler in welcher
 * 128x128-Zone und wie oft du per Teleport dort gelandet bist.
 * Das sind nur eigene Beobachtungen, keine Serverdaten.
 */
public final class Heatmap {
    public static final int CELL = 128;

    public static class Data {
        public Map<String, Integer> seen = new HashMap<>();
        public Map<String, Integer> landings = new HashMap<>();
    }

    public static Data data = new Data();
    private static final Set<String> sessionSeen = new HashSet<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean dirty = false;
    private static int timer = 0;
    private static int saveTimer = 0;
    private static double lastX = Double.NaN;
    private static double lastZ = Double.NaN;

    private Heatmap() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutclient_zones.json");
    }

    public static int cell(double v) {
        return Math.floorDiv((int) Math.floor(v), CELL);
    }

    private static String key(int cx, int cz) {
        return cx + "," + cz;
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Data d = GSON.fromJson(Files.readString(f), Data.class);
                if (d != null) {
                    data = d;
                    if (data.seen == null) data.seen = new HashMap<>();
                    if (data.landings == null) data.landings = new HashMap<>();
                }
            }
        } catch (Exception e) {
            System.err.println("[DONUT] Zonen-Daten konnten nicht geladen werden: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(data));
            dirty = false;
        } catch (Exception e) {
            System.err.println("[DONUT] Zonen-Daten konnten nicht gespeichert werden: " + e);
        }
    }

    public static void clear() {
        data = new Data();
        sessionSeen.clear();
        save();
    }

    public static int seenAt(int cx, int cz) {
        return data.seen.getOrDefault(key(cx, cz), 0);
    }

    public static int landingsAt(int cx, int cz) {
        return data.landings.getOrDefault(key(cx, cz), 0);
    }

    /** Summe der gesehenen Spieler in der 3x3-Umgebung der Zone. */
    public static int seenAround(int cx, int cz) {
        int sum = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sum += seenAt(cx + dx, cz + dz);
            }
        }
        return sum;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            lastX = Double.NaN;
            if (dirty) save();
            return;
        }

        // Teleport erkennen (grosser Sprung zwischen zwei Ticks)
        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (!Double.isNaN(lastX) && Math.hypot(x - lastX, z - lastZ) > 100) {
            data.landings.merge(key(cell(x), cell(z)), 1, Integer::sum);
            dirty = true;
        }
        lastX = x;
        lastZ = z;

        if (++timer >= 10) {
            timer = 0;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isSpectator()) continue;
                int cx = cell(p.getX());
                int cz = cell(p.getZ());
                if (sessionSeen.add(p.getUuid() + ":" + cx + ":" + cz)) {
                    data.seen.merge(key(cx, cz), 1, Integer::sum);
                    dirty = true;
                }
            }
        }

        if (dirty && ++saveTimer >= 1200) {
            saveTimer = 0;
            save();
        }
    }
}
