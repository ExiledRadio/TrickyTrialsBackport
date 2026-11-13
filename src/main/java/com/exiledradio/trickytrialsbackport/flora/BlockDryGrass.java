package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.21.5's short and tall dry grass ({@code ShortDryGrassBlock}, {@code TallDryGrassBlock}, both
 * {@code DryVegetationBlock}s): they stand on {@code #dry_vegetation_may_place_on} (sand, terracotta, dirt, farmland),
 * are offset in x, y and z like grass, replaceable, and drop only to shears or Silk Touch.
 *
 * <ul>
 *   <li>Short: 12 wide, 10 high; bone meal turns it into tall dry grass.</li>
 *   <li>Tall: 14 wide, a full block high; bone meal grows a short one beside it.</li>
 *   <li>Both: now and then, on sand or terracotta two blocks deep, the wind in the dry grass is heard
 *       ({@code AmbientDesertBlockSoundsPlayer.playAmbientDryGrassSounds}).</li>
 * </ul>
 */
public class BlockDryGrass extends BlockVegetation implements IGrowable {

    private final boolean tall;

    BlockDryGrass(boolean tall) {
        super(Material.VINE, MapColor.YELLOW, SoundType.PLANT, tall ? column(14.0D, 0.0D, 16.0D) : column(12.0D, 0.0D, 10.0D),
                true, true);
        this.tall = tall;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return isDryVegetationSoil(state);
    }

    /** Desert, so another mod's sand that answers for desert plants holds dry grass too. */
    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Desert;
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XYZ;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return !this.tall || FloraContent.shortDryGrass != null
                && spreadable(world, pos, FloraContent.shortDryGrass.getDefaultState(), horizontal()) != null;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (!this.tall) {
            world.setBlockState(pos, FloraContent.tallDryGrass.getDefaultState());
            return;
        }
        IBlockState shortGrass = FloraContent.shortDryGrass.getDefaultState();
        BlockPos p = spreadable(world, pos, shortGrass, shuffledHorizontal(world.rand));
        if (p != null) {
            world.setBlockState(p, shortGrass);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        com.exiledradio.trickytrialsbackport.flora.client.FloraClient.dryGrass(world, pos, rand);
    }
}
