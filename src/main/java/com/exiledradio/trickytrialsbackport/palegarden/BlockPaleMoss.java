package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

import java.util.Random;

/**
 * The pale moss block: 1.21.4's {@code BonemealableFeaturePlacerBlock} with {@code pale_moss_patch_bonemeal}.
 * Strength 0.1, moss sounds, broken by pistons. Bone meal on it, with air above, spreads a small patch of pale
 * moss with carpets and grass ({@link PaleGardenFeatures#mossPatch}). It is in modern {@code #dirt}, so
 * saplings, grass and flowers grow on it.
 */
public class BlockPaleMoss extends Block implements IGrowable {

    public BlockPaleMoss() {
        super(Material.GRASS, MapColor.SILVER);
        setHardness(0.1F);
        setSoundType(PaleGardenContent.MOSS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        EnumPlantType type = plantable.getPlantType(world, pos.offset(direction));
        return type == EnumPlantType.Plains || plantable instanceof BlockPaleOakSapling
                || plantable instanceof net.minecraft.block.BlockSapling;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        PaleGardenFeatures.mossPatch(world, rand, pos.up(), true);
    }
}
