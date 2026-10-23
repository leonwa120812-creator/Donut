package de.donutclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Wiederholt /rtp, bis du in einer Zone landest, in der dein Client (fast) keine
 * anderen Spieler gesehen hat. Nutzt nur deine eigenen Beobachtungen.
 */
public final class RtpSearch {
    public static boolean active = false;

    private static int attempts = 0;
    private static int phase = 0; // 1 = warte auf Teleport, 2 = warte auf Chunks/Spieler
    private static int wait = 0;
    private static double lastX;
    private static double lastZ;

    private RtpSearch() {}

    public static void start(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        active = true;
        attempts = 0;
        msg(mc, "§9[DONUT] §fSuche ruhige Zone (max. " + Config.I.rtpMaxAttempts + " Versuche) ...");
        send(mc);
    }

    public static void stop(MinecraftClient mc, String reason) {
        if (!active) return;
        active = false;
        phase = 0;
        msg(mc, "§9[DONUT] §f" + reason);
    }

    private static void send(MinecraftClient mc) {
        attempts++;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        mc.getNetworkHandler().sendChatCommand("rtp");
        phase = 1;
        wait = Config.I.rtpWaitSec * 20;
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(s), false);
    }

    public static void tick(MinecraftClient mc) {
        if (!active) return;
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            active = false;
            phase = 0;
            return;
        }
        Config c = Config.I;

        if (phase == 1) {
            if (Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ) > 64) {
                phase = 2;
                wait = 80; // 4 Sekunden, damit Chunks und Spieler geladen werden
            } else if (--wait <= 0) {
                if (attempts >= c.rtpMaxAttempts) {
                    stop(mc, "Keine Teleportation erfolgt. Suche beendet.");
                } else {
                    send(mc);
                }
            }
            return;
        }

        if (phase == 2) {
            if (--wait > 0) return;
            int cx = Heatmap.cell(mc.player.getX());
            int cz = Heatmap.cell(mc.player.getZ());
            int around = Heatmap.seenAround(cx, cz);
            int px = (int) mc.player.getX();
            int pz = (int) mc.player.getZ();
            if (around <= c.rtpTolerance) {
                stop(mc, "Ruhige Zone gefunden bei §b" + px + " " + pz + "§f (gesehene Spieler: " + around
                        + ", Versuch " + attempts + ").");
            } else if (attempts >= c.rtpMaxAttempts) {
                stop(mc, "Maximale Versuche erreicht. Letzte Zone: " + px + " " + pz + " (gesehene Spieler: " + around + ").");
            } else {
                msg(mc, "§9[DONUT] §fZone " + px + " " + pz + " belebt (" + around
                        + " gesehen). Naechster Versuch " + (attempts + 1) + "/" + c.rtpMaxAttempts + " ...");
                send(mc);
            }
        }
    }
}
