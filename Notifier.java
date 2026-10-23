package de.donutclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Meldet Ansammlungen von Behaeltern und Spawner und zeigt Infos zur aktuellen Zone. */
public final class Notifier {
    public record Find(int x, int y, int z, int containers, int spawners) {}

    public static final List<Find> finds = new ArrayList<>();
    private static final Set<Long> notified = new HashSet<>();
    private static int timer = 0;
    private static long lastZoneKey = Long.MIN_VALUE;

    private Notifier() {}

    public static void clearFinds() {
        finds.clear();
        notified.clear();
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            lastZoneKey = Long.MIN_VALUE;
            return;
        }
        Config c = Config.I;

        if (c.zoneInfo) zoneInfo(mc);

        if (!c.notifyEnabled) return;
        if (++timer < 40) return;
        timer = 0;

        // Eintraege nach 32x32-Bloecken gruppieren
        Map<Long, int[]> groups = new HashMap<>(); // {behaelter, spawner, sumX, sumY, sumZ, n}
        for (BlockScanner.Entry e : BlockScanner.entries) {
            boolean spawner = e.type() == BlockScanner.SPAWNER;
            if (spawner && !c.notifySpawner) continue;
            if (!spawner && !c.typeOn[e.type()]) continue;
            long gx = Math.floorDiv(e.pos().getX(), 32);
            long gz = Math.floorDiv(e.pos().getZ(), 32);
            long k = (gx << 32) ^ (gz & 0xFFFFFFFFL);
            int[] g = groups.computeIfAbsent(k, kk -> new int[6]);
            if (spawner) g[1]++; else g[0]++;
            g[2] += e.pos().getX();
            g[3] += e.pos().getY();
            g[4] += e.pos().getZ();
            g[5]++;
        }

        for (Map.Entry<Long, int[]> en : groups.entrySet()) {
            int[] g = en.getValue();
            boolean hit = g[0] >= c.clusterMin || (c.notifySpawner && g[1] > 0);
            if (!hit || !notified.add(en.getKey())) continue;

            int x = g[2] / g[5];
            int y = g[3] / g[5];
            int z = g[4] / g[5];
            finds.add(0, new Find(x, y, z, g[0], g[1]));
            while (finds.size() > 50) finds.remove(finds.size() - 1);

            String extra = g[1] > 0 ? ", " + g[1] + " Spawner" : "";
            mc.player.sendMessage(Text.literal("§9[DONUT] §fMoegliche Basis bei §b"
                    + x + " " + y + " " + z + "§f: " + g[0] + " Behaelter" + extra), false);
        }
    }

    private static void zoneInfo(MinecraftClient mc) {
        int cx = Heatmap.cell(mc.player.getX());
        int cz = Heatmap.cell(mc.player.getZ());
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        if (key == lastZoneKey) return;
        lastZoneKey = key;

        int around = Heatmap.seenAround(cx, cz);
        String rating = around == 0 ? "§aruhig" : around <= 3 ? "§egeringe Aktivitaet" : "§cbelebt";
        mc.player.sendMessage(Text.literal("§9Zone " + (cx * Heatmap.CELL) + "/" + (cz * Heatmap.CELL)
                + " §f| gesehene Spieler: " + Heatmap.seenAt(cx, cz) + " (Umgebung " + around
                + ") | eigene Landungen: " + Heatmap.landingsAt(cx, cz) + " | " + rating), true);
    }
}
