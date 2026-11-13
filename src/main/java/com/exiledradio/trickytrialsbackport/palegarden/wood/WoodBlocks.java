package com.exiledradio.trickytrialsbackport.palegarden.wood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The wooden blocks of a {@link WoodKind}, each extending vanilla's own block of that shape, so placing,
 * connecting, redstone and every {@code instanceof} check in vanilla and other mods treat them as wood of
 * that shape. What they add is the wood's map color and its sounds.
 *
 * <p>Numbers are vanilla's wood: hardness 2 and blast resistance 3 (15 in 1.12.2's tripled scale) for
 * planks, logs, stairs, slabs, fences and gates; 3 for doors and trapdoors; 0.5 for buttons and pressure
 * plates.
 */
public final class WoodBlocks {

    private WoodBlocks() {
    }

    /** Planks. */
    public static class Planks extends Block {
        public Planks(WoodKind kind) {
            super(Material.WOOD, kind.planksColor);
            setHardness(2.0F);
            setResistance(5.0F);
            setSoundType(kind.sound);
        }
    }

    /**
     * Logs, wood, their stripped forms, and bamboo's blocks: a pillar on three axes. 1.12.2's vanilla
     * logs have a fourth "none" axis for the all-bark block; 1.13 made that its own block, as these are.
     */
    public static class Log extends BlockRotatedPillar {
        private final MapColor top;
        private final MapColor side;

        /** @param top the map color seen from above (the end grain) @param side the bark's */
        public Log(WoodKind kind, MapColor top, MapColor side, SoundType sound) {
            super(Material.WOOD, side);
            this.top = top;
            this.side = side;
            setHardness(2.0F);
            setSoundType(sound);
            // BlockRotatedPillar leaves the first axis value (x) as the default; upright, as vanilla logs.
            setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(AXIS) == EnumFacing.Axis.Y ? this.top : this.side;
        }

        @Override
        public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
            return true;
        }

        @Override
        public boolean isWood(IBlockAccess world, BlockPos pos) {
            return true;
        }

        /**
         * Tells leaves around it to check whether they still reach a log, as vanilla's {@code BlockLog}
         * does - but out to 7 blocks, 1.20's leaf distance, since 1.20 trees spread that far.
         */
        @Override
        public void breakBlock(World world, BlockPos pos, IBlockState state) {
            if (world.isAreaLoaded(pos.add(-8, -8, -8), pos.add(8, 8, 8))) {
                for (BlockPos at : BlockPos.getAllInBoxMutable(pos.add(-7, -7, -7), pos.add(7, 7, 7))) {
                    IBlockState near = world.getBlockState(at);
                    if (near.getBlock().isLeaves(near, world, at)) {
                        near.getBlock().beginLeavesDecay(near, world, at.toImmutable());
                    }
                }
            }
        }
    }

    public static class Stairs extends BlockStairs {
        public Stairs(WoodKind kind, IBlockState planks) {
            super(planks);
            setHardness(2.0F);
            setResistance(5.0F);
            setSoundType(kind.sound);
            this.useNeighborBrightness = true;
        }
    }

    /**
     * A slab in 1.12.2's two-block form: {@link Half} and {@link Double}, with the one-value variant
     * property {@link BlockSlab} insists on (it is how vanilla packs several slabs into one block).
     * {@link #isDouble()} is answered by the subclass, since {@code BlockSlab}'s constructor calls it
     * before any field here is assigned.
     */
    public abstract static class Slab extends BlockSlab {

        public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

        public enum Variant implements IStringSerializable {
            DEFAULT;

            @Override
            public String getName() {
                return "default";
            }
        }

        /** The half slab every one of these drops and is picked as. */
        protected Block half;

        Slab(WoodKind kind) {
            super(Material.WOOD, kind.planksColor);
            setHardness(2.0F);
            setResistance(5.0F);
            setSoundType(kind.sound);
            IBlockState state = this.blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
            if (!isDouble()) {
                state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
            }
            setDefaultState(state);
            this.useNeighborBrightness = !isDouble();
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
        }

        @Override
        public String getTranslationKey(int meta) {
            return getTranslationKey();
        }

        @Override
        public IProperty<?> getVariantProperty() {
            return VARIANT;
        }

        @Override
        public Comparable<?> getTypeForItem(ItemStack stack) {
            return Variant.DEFAULT;
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            IBlockState state = getDefaultState();
            if (!isDouble()) {
                state = state.withProperty(HALF, (meta & 8) != 0 ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
            }
            return state;
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return Item.getItemFromBlock(this.half);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.half);
        }

        public static final class Half extends Slab {
            public Half(WoodKind kind) {
                super(kind);
                this.half = this;
            }

            @Override
            public boolean isDouble() {
                return false;
            }
        }

        public static final class Double extends Slab {
            public Double(WoodKind kind, Half half) {
                super(kind);
                this.half = half;
            }

            @Override
            public boolean isDouble() {
                return true;
            }
        }
    }

    public static class Fence extends BlockFence {
        public Fence(WoodKind kind) {
            super(Material.WOOD, kind.planksColor);
            setHardness(2.0F);
            setResistance(5.0F);
            setSoundType(kind.sound);
        }
    }

    /** A fence gate; opening and closing play the wood's own sounds instead of vanilla's events. */
    public static class FenceGate extends BlockFenceGate {
        private final WoodKind kind;

        public FenceGate(WoodKind kind) {
            super(BlockPlanks.EnumType.OAK);
            this.kind = kind;
            setHardness(2.0F);
            setResistance(5.0F);
            setSoundType(kind.sound);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.kind.planksColor;
        }

        /** Vanilla's, with the wood's sound in place of world events 1008 and 1014. */
        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                        EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (state.getValue(OPEN)) {
                state = state.withProperty(OPEN, false);
            } else {
                EnumFacing look = EnumFacing.fromAngle(player.rotationYaw);
                if (state.getValue(FACING) == look.getOpposite()) {
                    state = state.withProperty(FACING, look);
                }
                state = state.withProperty(OPEN, true);
            }
            world.setBlockState(pos, state, 10);
            playSound(world, player, pos, state.getValue(OPEN));
            return true;
        }

        /** Redstone opening, as vanilla's, with the wood's sound. */
        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (world.isRemote) {
                return;
            }
            boolean powered = world.isBlockPowered(pos);
            if (state.getValue(POWERED) != powered) {
                world.setBlockState(pos, state.withProperty(POWERED, powered).withProperty(OPEN, powered), 2);
                if (state.getValue(OPEN) != powered) {
                    playSound(world, null, pos, powered);
                }
            }
        }

        private void playSound(World world, @Nullable EntityPlayer player, BlockPos pos, boolean open) {
            world.playSound(player, pos, open ? this.kind.gateOpen : this.kind.gateClose, SoundCategory.BLOCKS,
                    1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    /**
     * A wooden door. Vanilla's door drops and is picked as {@code Items.OAK_DOOR} unless told otherwise,
     * so the item is set once registered.
     */
    public static class Door extends BlockDoor {
        private final WoodKind kind;
        private Item item;

        public Door(WoodKind kind) {
            super(Material.WOOD);
            this.kind = kind;
            setHardness(3.0F);
            setSoundType(kind.sound);
            disableStats();
        }

        public void setItem(Item item) {
            this.item = item;
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.kind.planksColor;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : this.item;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.item);
        }

        /** Vanilla's, with the wood's sound in place of world events 1006 and 1012. */
        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                        EnumFacing facing, float hitX, float hitY, float hitZ) {
            BlockPos lower = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
            IBlockState door = pos.equals(lower) ? state : world.getBlockState(lower);
            if (door.getBlock() != this) {
                return false;
            }
            door = door.cycleProperty(OPEN);
            world.setBlockState(lower, door, 10);
            world.markBlockRangeForRenderUpdate(lower, pos);
            playSound(world, player, pos, door.getValue(OPEN));
            return true;
        }

        /** Redstone: vanilla's toggleDoor plays world events, so this sets the state and plays ours. */
        @Override
        public void toggleDoor(World world, BlockPos pos, boolean open) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != this) {
                return;
            }
            BlockPos lower = state.getValue(HALF) == EnumDoorHalf.LOWER ? pos : pos.down();
            IBlockState door = pos == lower ? state : world.getBlockState(lower);
            if (door.getBlock() == this && door.getValue(OPEN) != open) {
                world.setBlockState(lower, door.withProperty(OPEN, open), 10);
                world.markBlockRangeForRenderUpdate(lower, pos);
                playSound(world, null, pos, open);
            }
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (state.getValue(HALF) == EnumDoorHalf.UPPER) {
                // Vanilla's upper half only passes the update down, or breaks without its lower half.
                super.neighborChanged(state, world, pos, block, from);
                return;
            }
            BlockPos up = pos.up();
            IBlockState upper = world.getBlockState(up);
            if (upper.getBlock() != this || !world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) {
                // Vanilla's own handling: breaking, dropping. No sound involved.
                super.neighborChanged(state, world, pos, block, from);
                return;
            }
            boolean powered = world.isBlockPowered(pos) || world.isBlockPowered(up);
            if (block != this && (powered || block.getDefaultState().canProvidePower())
                    && powered != upper.getValue(POWERED)) {
                world.setBlockState(up, upper.withProperty(POWERED, powered), 2);
                if (powered != state.getValue(OPEN)) {
                    world.setBlockState(pos, state.withProperty(OPEN, powered), 2);
                    world.markBlockRangeForRenderUpdate(pos, pos);
                    playSound(world, null, pos, powered);
                }
            }
        }

        private void playSound(World world, @Nullable EntityPlayer player, BlockPos pos, boolean open) {
            world.playSound(player, pos, open ? this.kind.doorOpen : this.kind.doorClose, SoundCategory.BLOCKS,
                    1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    public static class TrapDoor extends BlockTrapDoor {
        private final WoodKind kind;

        public TrapDoor(WoodKind kind) {
            super(Material.WOOD);
            this.kind = kind;
            setHardness(3.0F);
            setSoundType(kind.sound);
            disableStats();
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.kind.planksColor;
        }

        @Override
        protected void playSound(@Nullable EntityPlayer player, World world, BlockPos pos, boolean open) {
            world.playSound(player, pos, open ? this.kind.trapdoorOpen : this.kind.trapdoorClose, SoundCategory.BLOCKS,
                    1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    public static class PressurePlate extends BlockPressurePlate {
        private final WoodKind kind;

        public PressurePlate(WoodKind kind) {
            super(Material.WOOD, Sensitivity.EVERYTHING);
            this.kind = kind;
            setHardness(0.5F);
            setSoundType(kind.sound);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.kind.planksColor;
        }

        @Override
        protected void playClickOnSound(World world, BlockPos pos) {
            if (this.kind.vanilla) {
                super.playClickOnSound(world, pos);
                return;
            }
            world.playSound(null, pos, this.kind.plateOn, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        protected void playClickOffSound(World world, BlockPos pos) {
            if (this.kind.vanilla) {
                super.playClickOffSound(world, pos);
                return;
            }
            world.playSound(null, pos, this.kind.plateOff, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static class Button extends BlockButtonWood {
        private final WoodKind kind;

        public Button(WoodKind kind) {
            this.kind = kind;
            setHardness(0.5F);
            setSoundType(kind.sound);
        }

        @Override
        protected void playClickSound(@Nullable EntityPlayer player, World world, BlockPos pos) {
            if (this.kind.vanilla) {
                super.playClickSound(player, world, pos);
                return;
            }
            world.playSound(player, pos, this.kind.buttonOn, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        protected void playReleaseSound(World world, BlockPos pos) {
            if (this.kind.vanilla) {
                super.playReleaseSound(world, pos);
                return;
            }
            world.playSound(null, pos, this.kind.buttonOff, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}
