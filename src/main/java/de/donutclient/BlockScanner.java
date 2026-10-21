package de.donutclient;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Sucht ausschliesslich in clientseitig geladenen Chunks nach Block-Entities.
 * Nicht geladene Chunks werden nicht erfasst.
 */
public final class BlockScanner {
    public static final int SPAWNER = 8;

    public record Entry(BlockPos pos, int type) {}

    public static volatile List<Entry> entries = List.of();
    private static int timer = 0;

    private BlockScanner() {}

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            entries = List.of();
            return;
        }
        if (++timer < 10) return;
        timer = 0;

        Config c = Config.I;
        if (!c.storageEnabled && !c.spawnerEnabled) {
            entries = List.of();
            return;
        }

        int maxRange = 0;
        if (c.storageEnabled) maxRange = Math.max(maxRange, c.storageRange);
        if (c.spawnerEnabled) maxRange = Math.max(maxRange, c.spawnerRange);
        int radius = (maxRange >> 4) + 1;

        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;

        List<Entry> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = mc.world.getChunkManager().getChunk(pcx + dx, pcz + dz, ChunkStatus.FULL, false);
                if (!(chunk instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    int t = classify(be);
                    if (t >= 0) out.add(new Entry(be.getPos().toImmutable(), t));
                }
            }
        }
        entries = out;
    }

    private static int classify(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return 1;
        if (be instanceof ChestBlockEntity) return 0;
        if (be instanceof EnderChestBlockEntity) return 2;
        if (be instanceof BarrelBlockEntity) return 3;
        if (be instanceof ShulkerBoxBlockEntity) return 4;
        if (be instanceof HopperBlockEntity) return 5;
        if (be instanceof DropperBlockEntity) return 7;
        if (be instanceof DispenserBlockEntity) return 6;
        if (be instanceof MobSpawnerBlockEntity) return SPAWNER;
        return -1;
    }
}
