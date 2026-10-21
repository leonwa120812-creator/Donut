package de.donutclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Legt einen Totem der Unsterblichkeit in die Nebenhand, sobald dort keiner liegt. */
public final class AutoTotem {
    private static int cooldown = 0;

    private AutoTotem() {}

    public static void tick(MinecraftClient mc) {
        Config c = Config.I;
        if (!c.totemEnabled || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (c.totemLowHealthOnly && mc.player.getHealth() > c.totemHealth) return;

        int found = -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                found = i;
                break;
            }
        }
        if (found < 0) return;

        // Inventar-Index (0-8 Hotbar, 9-35 Rucksack) -> Slot im Spieler-Inventar
        int screenSlot = found < 9 ? 36 + found : found;
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId, screenSlot, 40, SlotActionType.SWAP, mc.player);
        cooldown = c.totemDelay;
    }
}
