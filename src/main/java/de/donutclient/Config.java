package de.donutclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Einstellungen. Werden als JSON in config/donutclient.json gespeichert. */
public class Config {
    public static final String[] TYPE_NAMES = {
            "Truhe", "Redstone-Truhe", "Endertruhe", "Fass",
            "Shulkerbox", "Trichter", "Werfer", "Spender"
    };
    public static final String[] COLOR_NAMES = {
            "Rot", "Orange", "Gelb", "Gruen", "Cyan", "Blau", "Lila", "Weiss"
    };
    public static final int[] PALETTE = {
            0xFF3B3B, 0xFF9F1C, 0xFFE14D, 0x4DFF6A, 0x2EC4FF, 0x4D7CFF, 0xB65DFF, 0xFFFFFF
    };
    public static final String[] MODE_NAMES = {"Umrandung", "Fuellung", "Beides"};

    public static Config I = new Config();

    // StorageFinder
    public boolean storageEnabled = false;
    public int storageRange = 64;
    public int storageAlpha = 35;
    public int storageMode = 0;
    public boolean[] typeOn = {true, true, true, true, true, false, false, false};
    public int[] typeColor = {1, 1, 5, 3, 6, 7, 7, 7};

    // SpawnerFinder
    public boolean spawnerEnabled = false;
    public int spawnerRange = 64;
    public int spawnerAlpha = 45;
    public int spawnerMode = 2;
    public int spawnerColor = 0;

    // AutoTotem
    public boolean totemEnabled = false;
    public int totemDelay = 2;
    public boolean totemLowHealthOnly = false;
    public int totemHealth = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutclient.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Config c = GSON.fromJson(Files.readString(f), Config.class);
                if (c != null) {
                    I = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[DONUT] Config konnte nicht geladen werden: " + e);
        }
        I.sanitize();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(I));
        } catch (Exception e) {
            System.err.println("[DONUT] Config konnte nicht gespeichert werden: " + e);
        }
    }

    private void sanitize() {
        if (typeOn == null || typeOn.length != 8) typeOn = new Config().typeOn;
        if (typeColor == null || typeColor.length != 8) typeColor = new Config().typeColor;
        for (int i = 0; i < typeColor.length; i++) {
            typeColor[i] = Math.floorMod(typeColor[i], PALETTE.length);
        }
        spawnerColor = Math.floorMod(spawnerColor, PALETTE.length);
        storageMode = Math.floorMod(storageMode, 3);
        spawnerMode = Math.floorMod(spawnerMode, 3);
        storageRange = Math.max(16, Math.min(256, storageRange));
        spawnerRange = Math.max(16, Math.min(256, spawnerRange));
        storageAlpha = Math.max(5, Math.min(100, storageAlpha));
        spawnerAlpha = Math.max(5, Math.min(100, spawnerAlpha));
        totemDelay = Math.max(0, Math.min(10, totemDelay));
        totemHealth = Math.max(1, Math.min(20, totemHealth));
    }
}
