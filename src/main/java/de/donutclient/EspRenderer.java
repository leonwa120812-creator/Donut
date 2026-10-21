package de.donutclient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/** Zeichnet Umrandungen und Fuellungen um erkannte Bloecke. */
public final class EspRenderer {
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

        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer lines = null;
        VertexConsumer fill = null;

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

            double e0 = 0.002;
            double x1 = e.pos().getX() - e0, y1 = e.pos().getY() - e0, z1 = e.pos().getZ() - e0;
            double x2 = e.pos().getX() + 1 + e0, y2 = e.pos().getY() + 1 + e0, z2 = e.pos().getZ() + 1 + e0;

            if (mode == 0 || mode == 2) {
                if (lines == null) lines = vcp.getBuffer(RenderLayer.getLines());
                VertexRendering.drawBox(ms, lines, x1, y1, z1, x2, y2, z2, r, g, b, 1.0f);
            }
            if (mode == 1 || mode == 2) {
                if (fill == null) fill = vcp.getBuffer(RenderLayer.getDebugFilledBox());
                filledBox(ms.peek().getPositionMatrix(), fill,
                        (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2,
                        r, g, b, alphaPct / 100f);
            }
        }

        ms.pop();

        if (vcp instanceof VertexConsumerProvider.Immediate imm) {
            if (fill != null) imm.draw(RenderLayer.getDebugFilledBox());
            if (lines != null) imm.draw(RenderLayer.getLines());
        }
    }

    private static void quad(Matrix4f m, VertexConsumer v,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a) {
        v.vertex(m, ax, ay, az).color(r, g, b, a);
        v.vertex(m, bx, by, bz).color(r, g, b, a);
        v.vertex(m, cx, cy, cz).color(r, g, b, a);
        v.vertex(m, dx, dy, dz).color(r, g, b, a);
    }

    private static void filledBox(Matrix4f m, VertexConsumer v,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float r, float g, float b, float a) {
        quad(m, v, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a); // unten
        quad(m, v, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a); // oben
        quad(m, v, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a); // nord
        quad(m, v, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a); // sued
        quad(m, v, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a); // west
        quad(m, v, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a); // ost
    }
}
