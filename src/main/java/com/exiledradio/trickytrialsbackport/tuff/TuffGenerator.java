package com.exiledradio.trickytrialsbackport.tuff;

import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Our tuff copy's generation, only registered when we register the copy: 1.17's {@code ore_tuff}, two
 * blobs of up to 33 blocks per chunk, evenly between y 0 and 16, replacing stone (and granite, diorite
 * and andesite, which are stone in 1.12.2), in the Overworld only.
 */
final class TuffGenerator implements IWorldGenerator {

    static final int SIZE = 33;
    static final int PER_CHUNK = 2;
    static final int MAX_Y = 16;

    private final WorldGenMinable blob;

    TuffGenerator(IBlockState tuff) {
        this.blob = new WorldGenMinable(tuff, SIZE, BlockMatcher.forBlock(Blocks.STONE));
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0) {
            return;
        }
        for (int i = 0; i < PER_CHUNK; i++) {
            // Offset by 8 into the chunk, as vanilla's decorators do, so blobs never reach an ungenerated chunk.
            BlockPos at = new BlockPos(chunkX * 16 + 8 + random.nextInt(16), random.nextInt(MAX_Y + 1), chunkZ * 16 + 8 + random.nextInt(16));
            this.blob.generate(world, random, at);
        }
    }
}
