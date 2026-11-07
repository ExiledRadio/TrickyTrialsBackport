package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.building.BlockShapedSlab;
import com.exiledradio.trickytrialsbackport.common.building.BlockShapedStairs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Every copper block shape: Caves &amp; Cliffs' four (copied, the same classes' behavior: hardness 3, blast
 * resistance 6, a stone pickaxe) and 1.21's five. Each ages on random ticks through {@link Weathering}
 * unless it is waxed or fully oxidized.
 */
public final class CopperBlocks {

    private CopperBlocks() {
    }

    static void copper(Block b, boolean ages) {
        b.setHardness(3.0F);
        // 1.12.2 stores resistance tripled and reports it fifthed: 10 here is a modern 6.
        b.setResistance(10.0F);
        b.setHarvestLevel("pickaxe", 1);
        b.setTickRandomly(ages);
    }

    /** A block of copper, cut copper or chiseled copper. */
    public static class Full extends Block {
        public Full(CopperStage stage, SoundType sound, boolean ages) {
            super(Material.IRON, stage.color);
            setSoundType(sound);
            copper(this, ages);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }
    }

    /** A copper grate (1.21): a see-through full block that lets light through. */
    public static class Grate extends Full {
        public Grate(CopperStage stage, SoundType sound, boolean ages) {
            super(stage, sound, ages);
            setLightOpacity(0);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
            return false;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getRenderLayer() {
            return BlockRenderLayer.CUTOUT;
        }

        @Override
        @SuppressWarnings("deprecation")
        @SideOnly(Side.CLIENT)
        public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
            IBlockState next = world.getBlockState(pos.offset(side));
            return next.getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
        }
    }

    /**
     * Cut copper stairs. Vanilla stairs pass random ticks to their model block, which would turn the stairs
     * into that block's next stage as a full block, so stairs tick for themselves.
     */
    public static class Stairs extends BlockShapedStairs {
        public Stairs(IBlockState model, boolean ages) {
            super(model);
            setTickRandomly(ages);
            setHarvestLevel("pickaxe", 1);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        }
    }

    public static class Slab extends BlockShapedSlab.Half {
        public Slab(Block full, boolean ages) {
            super(full, full.getDefaultState());
            setTickRandomly(ages);
            setHarvestLevel("pickaxe", 1);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }
    }

    public static class DoubleSlab extends BlockShapedSlab.Double {
        public DoubleSlab(Block full, BlockShapedSlab.Half half, boolean ages) {
            super(full, full.getDefaultState(), half);
            setTickRandomly(ages);
            setHarvestLevel("pickaxe", 1);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }
    }

    /**
     * The copper bulb (1.21): each rising redstone edge switches it on or off, in the same tick (1.20.4
     * dropped the delay). Light 15, 12, 8 or 4 by stage while lit; a comparator reads 15 while lit.
     */
    public static class Bulb extends Block {
        public static final PropertyBool LIT = PropertyBool.create("lit");
        public static final PropertyBool POWERED = PropertyBool.create("powered");
        static final int[] LIGHT = {15, 12, 8, 4};

        private final int light;

        public Bulb(CopperStage stage, SoundType sound, boolean ages) {
            super(Material.IRON, stage.color);
            setSoundType(sound);
            copper(this, ages);
            this.light = LIGHT[stage.ordinal()];
            setDefaultState(this.blockState.getBaseState().withProperty(LIT, false).withProperty(POWERED, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, LIT, POWERED);
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(LIT, (meta & 1) != 0).withProperty(POWERED, (meta & 2) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return (state.getValue(LIT) ? 1 : 0) | (state.getValue(POWERED) ? 2 : 0);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getLightValue(IBlockState state) {
            return state.getValue(LIT) ? this.light : 0;
        }

        @Override
        public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
            check(world, pos, state);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            check(world, pos, state);
        }

        /** 1.21's {@code checkAndFlip}: a change of power stores it; a rising one also flips the light. */
        void check(World world, BlockPos pos, IBlockState state) {
            if (world.isRemote) {
                return;
            }
            boolean powered = world.isBlockPowered(pos);
            if (powered == state.getValue(POWERED)) {
                return;
            }
            IBlockState next = state.withProperty(POWERED, powered);
            if (powered) {
                boolean lit = !state.getValue(LIT);
                next = next.withProperty(LIT, lit);
                world.playSound(null, pos, Content.sound(lit ? "block.copper_bulb.turn_on" : "block.copper_bulb.turn_off"),
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            world.setBlockState(pos, next, 3);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean hasComparatorInputOverride(IBlockState state) {
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
            return state.getValue(LIT) ? 15 : 0;
        }
    }

    /** The copper door (1.21): opened by hand and by wind, unlike iron; drops its own item. */
    public static class Door extends BlockDoor {
        Item item;
        private final CopperStage stage;

        public Door(CopperStage stage, SoundType sound, boolean ages) {
            super(Material.IRON);
            this.stage = stage;
            setSoundType(sound);
            copper(this, ages);
        }

        /** 1.21.4: copper doors drop with any tool or none. */
        @Override
        public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public net.minecraft.block.material.MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.stage.color;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                        float hitX, float hitY, float hitZ) {
            BlockPos lower = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
            IBlockState here = pos.equals(lower) ? state : world.getBlockState(lower);
            if (here.getBlock() != this) {
                return false;
            }
            boolean open = !here.getValue(OPEN);
            world.setBlockState(lower, here.withProperty(OPEN, open), 10);
            world.markBlockRangeForRenderUpdate(lower, pos);
            world.playSound(player, pos, Content.sound(open ? "block.copper_door.open" : "block.copper_door.close"), SoundCategory.BLOCKS, 1.0F,
                    world.rand.nextFloat() * 0.1F + 0.9F);
            return true;
        }

        @Override
        public void toggleDoor(World world, BlockPos pos, boolean open) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != this) {
                return;
            }
            BlockPos lower = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
            IBlockState here = pos.equals(lower) ? state : world.getBlockState(lower);
            if (here.getBlock() == this && here.getValue(OPEN) != open) {
                world.setBlockState(lower, here.withProperty(OPEN, open), 10);
                world.markBlockRangeForRenderUpdate(lower, pos);
                world.playSound(null, pos, Content.sound(open ? "block.copper_door.open" : "block.copper_door.close"), SoundCategory.BLOCKS,
                        1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
            }
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : this.item;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.item);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }
    }

    /** The copper trapdoor (1.21): opened by hand and by wind, unlike iron. */
    public static class Trapdoor extends BlockTrapDoor {
        private final CopperStage stage;

        public Trapdoor(CopperStage stage, SoundType sound, boolean ages) {
            super(Material.IRON);
            this.stage = stage;
            setSoundType(sound);
            copper(this, ages);
        }

        @Override
        @SuppressWarnings("deprecation")
        public net.minecraft.block.material.MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.stage.color;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                        float hitX, float hitY, float hitZ) {
            boolean open = !state.getValue(OPEN);
            world.setBlockState(pos, state.withProperty(OPEN, open), 2);
            world.playSound(player, pos, Content.sound(open ? "block.copper_trapdoor.open" : "block.copper_trapdoor.close"),
                    SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
            return true;
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
            Weathering.randomTick(world, pos, state, rand);
        }
    }
}
