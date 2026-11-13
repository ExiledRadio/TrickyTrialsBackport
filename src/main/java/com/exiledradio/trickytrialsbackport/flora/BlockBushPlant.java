package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.21.5's bush ({@code BushBlock}) and firefly bush ({@code FireflyBushBlock}): bone meal grows one more beside it,
 * on the first free side (in a shuffled order) where one could stand.
 *
 * <ul>
 *   <li>Bush: replaceable, a 16-wide, 13-high outline, tinted by the grass color, dropped only to shears or Silk
 *       Touch.</li>
 *   <li>Firefly bush: light level 2, a full-block outline, always drops itself; at night it buzzes when the sky is
 *       open above it, and fireflies drift around it wherever the light is 13 or lower.</li>
 * </ul>
 */
public class BlockBushPlant extends BlockVegetation implements IGrowable {

    private final boolean fireflies;

    BlockBushPlant(boolean fireflies) {
        super(fireflies ? Material.PLANTS : Material.VINE, MapColor.FOLIAGE,
                fireflies ? FloraContent.SWEET_BERRY_BUSH : SoundType.PLANT,
                fireflies ? Block.FULL_BLOCK_AABB : column(16.0D, 0.0D, 13.0D), !fireflies, !fireflies);
        this.fireflies = fireflies;
        if (fireflies) {
            setLightLevel(2.0F / 15.0F);
        }
    }

    public boolean hasFireflies() {
        return this.fireflies;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return spreadable(world, pos, getDefaultState(), horizontal()) != null;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        BlockPos p = spreadable(world, pos, getDefaultState(), shuffledHorizontal(world.rand));
        if (p != null) {
            world.setBlockState(p, getDefaultState());
        }
    }

    /** {@code FireflyBushBlock.animateTick}. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (this.fireflies) {
            com.exiledradio.trickytrialsbackport.flora.client.FloraClient.fireflyBush(world, pos, rand);
        }
    }
}
