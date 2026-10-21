package de.donutclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Hauptmenue (Right Shift). Schwarz/Marineblau mit blauen Akzenten. */
public class DonutScreen extends Screen {
    private static final int PANEL_W = 460;
    private static final int PANEL_H = 270;
    private static final int MENU_W = 110;

    private static final int BG_DIM = 0xB0000000;
    private static final int BG_PANEL = 0xF0080C16;
    private static final int BG_MENU = 0xF00B1426;
    private static final int ACCENT = 0xFF2D7DFF;
    private static final int TEXT = 0xFFE6EEFF;
    private static final int TEXT_DIM = 0xFF7F93B8;

    private static int page = 0; // 0 Storage, 1 Spawner, 2 AutoTotem, 3 Info

    private int px, py;

    public DonutScreen() {
        super(Text.literal("DONUT Client"));
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        clearChildren();

        String[] names = {"StorageFinder", "SpawnerFinder", "AutoTotem", "Info"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            String label = (page == i ? "> " : "") + names[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                page = idx;
                init();
            }).dimensions(px + 8, py + 44 + i * 26, MENU_W - 16, 20).build());
        }

        int cx = px + MENU_W + 14;
        int cw = PANEL_W - MENU_W - 28;
        int y = py + 44;
        Config c = Config.I;

        switch (page) {
            case 0 -> buildStorage(c, cx, cw, y);
            case 1 -> buildSpawner(c, cx, cw, y);
            case 2 -> buildTotem(c, cx, cw, y);
            default -> { }
        }
    }

    private void buildStorage(Config c, int cx, int cw, int y) {
        toggle(cx, y, cw, "StorageFinder", () -> c.storageEnabled, v -> c.storageEnabled = v);
        y += 24;
        cycle(cx, y, cw, "Darstellung", Config.MODE_NAMES, () -> c.storageMode, v -> c.storageMode = v);
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Reichweite", 16, 256, c.storageRange, " Bloecke", v -> c.storageRange = v));
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Transparenz", 5, 100, c.storageAlpha, " %", v -> c.storageAlpha = v));
        y += 30;

        int colW = (cw - 8) / 2;
        for (int i = 0; i < 8; i++) {
            final int t = i;
            int col = i / 4;
            int row = i % 4;
            int bx = cx + col * (colW + 8);
            int by = y + row * 24;
            int toggleW = colW - 52;
            addDrawableChild(ButtonWidget.builder(typeLabel(t), b -> {
                c.typeOn[t] = !c.typeOn[t];
                b.setMessage(typeLabel(t));
            }).dimensions(bx, by, toggleW, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(Config.COLOR_NAMES[c.typeColor[t]]), b -> {
                c.typeColor[t] = (c.typeColor[t] + 1) % Config.PALETTE.length;
                b.setMessage(Text.literal(Config.COLOR_NAMES[c.typeColor[t]]));
            }).dimensions(bx + toggleW + 4, by, 48, 20).build());
        }
    }

    private Text typeLabel(int t) {
        return Text.literal(Config.TYPE_NAMES[t] + ": " + (Config.I.typeOn[t] ? "AN" : "AUS"));
    }

    private void buildSpawner(Config c, int cx, int cw, int y) {
        toggle(cx, y, cw, "SpawnerFinder", () -> c.spawnerEnabled, v -> c.spawnerEnabled = v);
        y += 24;
        cycle(cx, y, cw, "Darstellung", Config.MODE_NAMES, () -> c.spawnerMode, v -> c.spawnerMode = v);
        y += 24;
        cycle(cx, y, cw, "Farbe", Config.COLOR_NAMES, () -> c.spawnerColor, v -> c.spawnerColor = v);
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Reichweite", 16, 256, c.spawnerRange, " Bloecke", v -> c.spawnerRange = v));
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Transparenz", 5, 100, c.spawnerAlpha, " %", v -> c.spawnerAlpha = v));
    }

    private void buildTotem(Config c, int cx, int cw, int y) {
        toggle(cx, y, cw, "AutoTotem", () -> c.totemEnabled, v -> c.totemEnabled = v);
        y += 24;
        toggle(cx, y, cw, "Nur bei wenig Leben", () -> c.totemLowHealthOnly, v -> c.totemLowHealthOnly = v);
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Leben-Schwelle", 1, 20, c.totemHealth, " HP", v -> c.totemHealth = v));
        y += 24;
        addDrawableChild(new Slider(cx, y, cw, 20, "Verzoegerung", 0, 10, c.totemDelay, " Ticks", v -> c.totemDelay = v));
    }

    private void toggle(int x, int y, int w, String name, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(toggleText(name, get.get()), b -> {
            set.accept(!get.get());
            b.setMessage(toggleText(name, get.get()));
        }).dimensions(x, y, w, 20).build());
    }

    private Text toggleText(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "AN" : "AUS"));
    }

    private void cycle(int x, int y, int w, String name, String[] values, Supplier<Integer> get, IntConsumer set) {
        addDrawableChild(ButtonWidget.builder(Text.literal(name + ": " + values[get.get()]), b -> {
            set.accept((get.get() + 1) % values.length);
            b.setMessage(Text.literal(name + ": " + values[get.get()]));
        }).dimensions(x, y, w, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // bewusst leer: eigenes Design, kein Weichzeichner
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG_DIM);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, BG_PANEL);
        ctx.fill(px, py, px + MENU_W, py + PANEL_H, BG_MENU);
        ctx.fill(px, py, px + PANEL_W, py + 2, ACCENT);
        ctx.fill(px + MENU_W, py + 2, px + MENU_W + 1, py + PANEL_H, 0xFF14243F);

        ctx.drawText(textRenderer, "DONUT", px + 10, py + 10, ACCENT, true);
        ctx.drawText(textRenderer, "CLIENT", px + 10, py + 21, TEXT, true);

        String[] titles = {"StorageFinder", "SpawnerFinder", "AutoTotem", "Info"};
        ctx.drawText(textRenderer, titles[page], px + MENU_W + 14, py + 14, TEXT, true);
        ctx.fill(px + MENU_W + 14, py + 32, px + PANEL_W - 14, py + 33, 0xFF14243F);

        if (page == 3) {
            int x = px + MENU_W + 14;
            int y = py + 44;
            String[] lines = {
                    "Right Shift: Menue oeffnen / schliessen",
                    "",
                    "StorageFinder und SpawnerFinder markieren nur",
                    "Bloecke in clientseitig geladenen Chunks.",
                    "Nicht geladene Bereiche werden nicht gefunden.",
                    "",
                    "Einstellungen werden automatisch gespeichert.",
                    "",
                    "Beachte die Regeln deines Servers: Hilfsmittel",
                    "wie diese koennen dort verboten sein."
            };
            for (String l : lines) {
                ctx.drawText(textRenderer, l, x, y, l.isEmpty() ? TEXT : TEXT_DIM, false);
                y += 12;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Schieberegler fuer ganze Zahlen. */
    private static class Slider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final String unit;
        private final IntConsumer setter;

        Slider(int x, int y, int w, int h, String label, int min, int max, int cur, String unit, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (cur - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.setter = setter;
            updateMessage();
        }

        private int val() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Text.literal(label + ": " + val() + unit));
        }

        @Override
        protected void applyValue() {
            setter.accept(val());
        }
    }
}
