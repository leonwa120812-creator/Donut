package de.donutclient;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Zeichnet Umrandungen und Fuellungen um erkannte Bloecke.
 * Im Modus "Durch Waende" wird eine Render-Schicht ohne Tiefentest benutzt.
 */
public final class EspRenderer {
    private static final int FULL_LIGHT = 0xF000F0;
    private static final float EDGE = 0.03f;
    private static boolean seeThroughFailed = false;

    private EspRenderer() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        List<BlockScanner.Entry> list = BlockScanner.entries;
        if (list.isEmpty()) return;

        MatrixStack ms = ctx.matrices();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;

        float tickProgress = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d cam = mc.player.getCameraPosVec(tickProgress);
        Config c = Config.I;
        boolean see = c.throughWalls && !seeThroughFailed;

        VertexConsumer seeBuf = null;
        VertexConsumer lines = null;
        VertexConsumer fill = null;

        ms.push();
        try {
            ms.translate(-cam.x, -cam.y, -cam.z);

            for (BlockScanner.Entry e : list) {
                boolean spawner = e.type() == BlockScanner.SPAWNER;
                boolean enabled;
                int colorIdx;
                int mode;
                int alphaPct;
                int range;
                if (spawner) {
                    enabled = c.spawnerEnabled;
                    colorIdx = c.spawnerColor;
                    mode = c.spawnerMode;
                    alphaPct = c.spawnerAlpha;
                    range = c.spawnerRange;
                } else {
                    enabled = c.storageEnabled && c.typeOn[e.type()];
                    colorIdx = c.typeColor[e.type()];
                    mode = c.storageMode;
                    alphaPct = c.storageAlpha;
                    range = c.storageRange;
                }
                if (!enabled) continue;

                double dx = e.pos().getX() + 0.5 - cam.x;
                double dy = e.pos().getY() + 0.5 - cam.y;
                double dz = e.pos().getZ() + 0.5 - cam.z;
                if (dx * dx + dy * dy + dz * dz > (double) range * range) continue;

                int rgb = Config.PALETTE[colorIdx];
                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >> 8) & 0xFF) / 255f;
                float b = (rgb & 0xFF) / 255f;
                float a = alphaPct / 100f;

                double e0 = 0.002;
                float x1 = (float) (e.pos().getX() - e0), y1 = (float) (e.pos().getY() - e0), z1 = (float) (e.pos().getZ() - e0);
                float x2 = (float) (e.pos().getX() + 1 + e0), y2 = (float) (e.pos().getY() + 1 + e0), z2 = (float) (e.pos().getZ() + 1 + e0);
                Matrix4f m = ms.peek().getPositionMatrix();

                if (see) {
                    if (seeBuf == null) seeBuf = vcp.getBuffer(RenderLayers.textBackgroundSeeThrough());
                    if (mode == 1 || mode == 2) {
                        box(m, seeBuf, x1, y1, z1, x2, y2, z2, r, g, b, a, true);
                    }
                    if (mode == 0 || mode == 2) {
                        edges(m, seeBuf, x1, y1, z1, x2, y2, z2, r, g, b, 1.0f);
                    }
                } else {
                    if (mode == 0 || mode == 2) {
                        if (lines == null) lines = vcp.getBuffer(RenderLayers.lines());
                        VertexRendering.drawOutline(ms, lines, VoxelShapes.fullCube(),
                                e.pos().getX(), e.pos().getY(), e.pos().getZ(), 0xFF000000 | rgb, 2.0f);
                    }
                    if (mode == 1 || mode == 2) {
                        if (fill == null) fill = vcp.getBuffer(RenderLayers.debugFilledBox());
                        box(m, fill, x1, y1, z1, x2, y2, z2, r, g, b, a, false);
                    }
                }
            }
        } catch (RuntimeException ex) {
            if (see) {
                seeThroughFailed = true;
                System.err.println("[DONUT] Durch-Waende-Darstellung fehlgeschlagen, wechsle zur normalen: " + ex);
            }
        } finally {
            ms.pop();
        }

        if (vcp instanceof VertexConsumerProvider.Immediate imm) {
            try {
                if (seeBuf != null) imm.draw(RenderLayers.textBackgroundSeeThrough());
                if (fill != null) imm.draw(RenderLayers.debugFilledBox());
                if (lines != null) imm.draw(RenderLayers.lines());
            } catch (RuntimeException ex) {
                if (see) {
                    seeThroughFailed = true;
                    System.err.println("[DONUT] Zeichnen fehlgeschlagen: " + ex);
                }
            }
        }
    }

    private static void vtx(Matrix4f m, VertexConsumer v, float x, float y, float z,
                            float r, float g, float b, float a, boolean light) {
        VertexConsumer vc = v.vertex(m, x, y, z).color(r, g, b, a);
        if (light) vc.light(FULL_LIGHT);
    }

    private static void quad(Matrix4f m, VertexConsumer v,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a, boolean light) {
        vtx(m, v, ax, ay, az, r, g, b, a, light);
        vtx(m, v, bx, by, bz, r, g, b, a, light);
        vtx(m, v, cx, cy, cz, r, g, b, a, light);
        vtx(m, v, dx, dy, dz, r, g, b, a, light);
    }

    /** Gefuellter Quader aus sechs Flaechen. */
    private static void box(Matrix4f m, VertexConsumer v,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float r, float g, float b, float a, boolean light) {
        quad(m, v, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a, light); // unten
        quad(m, v, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a, light); // oben
        quad(m, v, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a, light); // nord
        quad(m, v, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a, light); // sued
        quad(m, v, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a, light); // west
        quad(m, v, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a, light); // ost
    }

    /** Umrandung aus zwoelf duennen Quadern (funktioniert ohne Tiefentest). */
    private static void edges(Matrix4f m, VertexConsumer v,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        float t = EDGE;
        float[] ys = {y1, y2 - t};
        float[] zs = {z1, z2 - t};
        float[] xs = {x1, x2 - t};
        for (float y : ys) {
            for (float z : zs) {
                box(m, v, x1, y, z, x2, y + t, z + t, r, g, b, a, true);
            }
        }
        for (float x : xs) {
            for (float z : zs) {
                box(m, v, x, y1, z, x + t, y2, z + t, r, g, b, a, true);
            }
        }
        for (float x : xs) {
            for (float y : ys) {
                box(m, v, x, y, z1, x + t, y + t, z2, r, g, b, a, true);
            }
        }
    }
}
