package de.donutclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class DonutClient implements ClientModInitializer {
    private static boolean shiftWasDown = false;
    private static boolean rtpWasDown = false;

    /** Wird vom Menue gesetzt: die naechste gedrueckte Taste wird die /rtp-Taste. */
    public static boolean bindingRtp = false;
    private static int bindDelay = 0;

    @Override
    public void onInitializeClient() {
        Config.load();
        Heatmap.load();
        ClientTickEvents.END_CLIENT_TICK.register(DonutClient::tick);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render);
    }

    public static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null && !n.isEmpty()) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "Leertaste";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Shift links";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Strg links";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Alt links";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Strg rechts";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Alt rechts";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Feststell";
            default -> {
                if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
                    yield "F" + (key - GLFW.GLFW_KEY_F1 + 1);
                }
                if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
                    yield "Num " + (key - GLFW.GLFW_KEY_KP_0);
                }
                yield "Taste " + key;
            }
        };
    }

    private static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long win = mc.getWindow().getHandle();

        // Menue mit Right Shift
        boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !shiftWasDown && !bindingRtp) {
            if (mc.currentScreen == null) {
                mc.setScreen(new DonutScreen());
            } else if (mc.currentScreen instanceof DonutScreen) {
                mc.setScreen(null);
            }
        }
        shiftWasDown = down;

        // Taste fuer /rtp festlegen
        if (bindingRtp) {
            if (!(mc.currentScreen instanceof DonutScreen)) {
                bindingRtp = false;
            } else if (bindDelay > 0) {
                bindDelay--;
            } else {
                for (int k = 32; k <= 348; k++) {
                    if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
                    if (GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS) {
                        if (k != GLFW.GLFW_KEY_ESCAPE) {
                            Config.I.rtpKey = k;
                            Config.save();
                        }
                        bindingRtp = false;
                        if (mc.currentScreen instanceof DonutScreen s) s.refresh();
                        break;
                    }
                }
            }
        } else if (bindDelay != 0) {
            bindDelay = 0;
        }

        // /rtp-Taste
        Config c = Config.I;
        boolean rtpDown = c.rtpEnabled && mc.currentScreen == null
                && GLFW.glfwGetKey(win, c.rtpKey) == GLFW.GLFW_PRESS;
        if (rtpDown && !rtpWasDown && mc.player != null && mc.getNetworkHandler() != null) {
            if (c.rtpQuiet) {
                if (RtpSearch.active) {
                    RtpSearch.stop(mc, "Suche abgebrochen.");
                } else {
                    RtpSearch.start(mc);
                }
            } else {
                mc.getNetworkHandler().sendChatCommand("rtp");
            }
        }
        rtpWasDown = rtpDown;

        Heatmap.tick(mc);
        RtpSearch.tick(mc);
        BlockScanner.tick(mc);
        Notifier.tick(mc);
        AutoTotem.tick(mc);
        AutoCrystal.tick(mc);
        AutoAnchor.tick(mc);
    }

    /** Wird vom Menue beim Klick auf "Taste aendern" aufgerufen. */
    public static void startBinding() {
        bindingRtp = true;
        bindDelay = 5;
    }
}
