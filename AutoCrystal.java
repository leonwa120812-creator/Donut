package de.donutclient;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * AutoCrystal: schlaegt Endkristalle in Reichweite kaputt und setzt Kristalle auf
 * Obsidian/Grundgestein, auf das du gerade zielst. Die Blickrichtung wird nicht veraendert.
 */
public final class AutoCrystal {
    private static int cooldown = 0;

    private AutoCrystal() {}

    public static void tick(MinecraftClient mc) {
        Config c = Config.I;
        if (!c.crystalEnabled || mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (c.crystalBreak && breakCrystal(mc, c)) {
            cooldown = c.crystalDelay;
            return;
        }
        if (c.crystalPlace && placeCrystal(mc)) {
            cooldown = c.crystalDelay;
        }
    }

    private static boolean breakCrystal(MinecraftClient mc, Config c) {
        double range = c.crystalRange + 0.5;
        Box box = mc.player.getBoundingBox().expand(range);
        List<Entity> found = mc.world.getOtherEntities(mc.player, box, e -> e instanceof EndCrystalEntity);

        Entity best = null;
        float bestDist = Float.MAX_VALUE;
        for (Entity e : found) {
            float d = mc.player.distanceTo(e);
            if (d > range || d < 2.0f) continue; // zu nah = Selbstschaden
            if (c.crystalOnlyNearPlayers && !nearOtherPlayer(mc, e)) continue;
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        if (best == null) return false;

        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private static boolean nearOtherPlayer(MinecraftClient mc, Entity crystal) {
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator()) continue;
            if (p.distanceTo(crystal) <= 6.0f) return true;
        }
        return false;
    }

    private static boolean placeCrystal(MinecraftClient mc) {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;

        BlockPos base = hit.getBlockPos();
        if (!(mc.world.getBlockState(base).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(base).isOf(Blocks.BEDROCK))) {
            return false;
        }
        BlockPos above = base.up();
        if (!mc.world.isAir(above) || !mc.world.isAir(above.up())) return false;
        Box space = new Box(above.getX(), above.getY(), above.getZ(), above.getX() + 1, above.getY() + 2, above.getZ() + 1);
        if (!mc.world.getOtherEntities(null, space, e -> true).isEmpty()) return false;

        Hand hand;
        if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
            hand = Hand.OFF_HAND;
        } else {
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return false;
            mc.player.getInventory().setSelectedSlot(slot);
            hand = Hand.MAIN_HAND;
        }

        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        return true;
    }
}
