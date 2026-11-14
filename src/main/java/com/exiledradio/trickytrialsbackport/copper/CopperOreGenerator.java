package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Our copy of Caves &amp; Cliffs' copper ore veins, registered only with the copies: {@code veinsPerChunk} veins
 * (6) of up to {@code veinSize} (10), evenly between {@code minY} and {@code maxY} (1-96), in the
 * Overworld's stone (granite, diorite and andesite included), in superflat only with {@code decoration} (ores are
 * decoration there, as 1.12's). Its deepslate copper ore
 * is not copied: without that mod there is no deepslate. Placed inside the population window.
 *
 * <p>A vein is a line of overlapping blobs: a random direction and length from the vein size, with a
 * blob radius that swells toward the middle (the same shape as the owner's).
 */
public final class CopperOreGenerator implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!ModConfig.copper.generate || world.provider.getDimension() != 0 || !FlatWorlds.decoration(world)) {
            return;
        }
        int lo = Math.max(1, ModConfig.copper.minY);
        int hi = Math.max(lo + 1, Math.min(255, ModConfig.copper.maxY));
        for (int i = 0; i < ModConfig.copper.veinsPerChunk; i++) {
            BlockPos at = new BlockPos(chunkX * 16 + 8 + random.nextInt(16), lo + random.nextInt(hi - lo), chunkZ * 16 + 8 + random.nextInt(16));
            vein(world, random, at, ModConfig.copper.veinSize);
        }
    }

    /** Places one vein centered near {@code at}; @return how many blocks it set */
    public static int vein(World world, Random random, BlockPos at, int size) {
        Block ore = Owners.ourBlock("copper_ore");
        if (ore == null || size <= 0) {
            return 0;
        }
        IBlockState oreState = ore.getDefaultState();
        float angle = random.nextFloat() * (float) Math.PI;
        float pitch = (random.nextFloat() - 0.5F) * 0.8F;
        float half = size / 8.0F;
        double x0 = at.getX() + 0.5 - MathHelper.cos(angle) * half;
        double x1 = at.getX() + 0.5 + MathHelper.cos(angle) * half;
        double z0 = at.getZ() + 0.5 - MathHelper.sin(angle) * half;
        double z1 = at.getZ() + 0.5 + MathHelper.sin(angle) * half;
        double y0 = at.getY() + 0.5 - pitch * half;
        double y1 = at.getY() + 0.5 + pitch * half;
        int placed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int n = 0; n < size; n++) {
            float t = n / (float) size;
            double cx = x0 + (x1 - x0) * t;
            double cy = y0 + (y1 - y0) * t;
            double cz = z0 + (z1 - z0) * t;
            double swell = MathHelper.sin((float) Math.PI * t) + 1.0;
            double r = (random.nextDouble() * size / 16.0 * swell + 1.0) / 2.0;
            for (int x = MathHelper.floor(cx - r); x <= MathHelper.floor(cx + r); x++) {
                for (int y = MathHelper.floor(cy - r); y <= MathHelper.floor(cy + r); y++) {
                    for (int z = MathHelper.floor(cz - r); z <= MathHelper.floor(cz + r); z++) {
                        double dx = (x + 0.5 - cx) / r;
                        double dy = (y + 0.5 - cy) / r;
                        double dz = (z + 0.5 - cz) / r;
                        if (dx * dx + dy * dy + dz * dz >= 1 || y < 1 || y > 255) {
                            continue;
                        }
                        p.setPos(x, y, z);
                        if (world.getBlockState(p).getBlock() == Blocks.STONE) {
                            world.setBlockState(p, oreState, 2);
                            placed++;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
