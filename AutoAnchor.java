package de.donutclient;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * AutoAnchor: arbeitet an dem Block, auf den du zielst.
 *  - Ist es ein ungeladener Seelenanker: mit Leuchtstein aufladen.
 *  - Ist er geladen: mit einem Gegenstand, der kein Leuchtstein ist, ausloesen.
 *  - Optional: einen Anker aus der Hotbar auf den anvisierten Block setzen.
 * Die Blickrichtung wird nicht veraendert.
 */
public final class AutoAnchor {
    private static int cooldown = 0;

    private AutoAnchor() {}

    public static void tick(MinecraftClient mc) {
        Config c = Config.I;
        if (!c.anchorEnabled || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.world.getBlockState(hit.getBlockPos());

        if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges == 0) {
                int slot = findSlot(mc, true);
                if (slot < 0) return;
                use(mc, hit, slot);
            } else {
                int cur = mc.player.getInventory().getSelectedSlot();
                int slot = mc.player.getInventory().getStack(cur).isOf(Items.GLOWSTONE) ? findSlot(mc, false) : cur;
                if (slot < 0) return;
                use(mc, hit, slot);
            }
            cooldown = c.anchorDelay;
            return;
        }

        if (c.anchorPlace) {
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isOf(Items.RESPAWN_ANCHOR)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return;
            use(mc, hit, slot);
            cooldown = c.anchorDelay;
        }
    }

    private static void use(MinecraftClient mc, BlockHitResult hit, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /** Sucht in der Hotbar einen Leuchtstein (glowstone=true) oder einen Slot ohne Leuchtstein/Anker. */
    private static int findSlot(MinecraftClient mc, boolean glowstone) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (glowstone) {
                if (s.isOf(Items.GLOWSTONE)) return i;
            } else if (!s.isOf(Items.GLOWSTONE) && !s.isOf(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }
        return -1;
    }
}
