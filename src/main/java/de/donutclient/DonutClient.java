package de.donutclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class DonutClient implements ClientModInitializer {
    private static boolean shiftWasDown = false;

    @Override
    public void onInitializeClient() {
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(DonutClient::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EspRenderer::render);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;

        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !shiftWasDown) {
            if (mc.currentScreen == null) {
                mc.setScreen(new DonutScreen());
            } else if (mc.currentScreen instanceof DonutScreen) {
                mc.setScreen(null);
            }
        }
        shiftWasDown = down;

        BlockScanner.tick(mc);
        AutoTotem.tick(mc);
    }
}
