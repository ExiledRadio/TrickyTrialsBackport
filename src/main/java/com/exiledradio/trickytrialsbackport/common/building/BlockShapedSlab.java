package com.exiledradio.trickytrialsbackport.common.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A modern slab in 1.12.2's two-block form, the family's layout (Village &amp; Pillage's slabs are the same).
 *
 * <h2>Two blocks per slab</h2>
 * 1.12.2 has no {@code type=double} state: a slab ({@code <name>_slab}) and the full block two of them make
 * ({@code <name>_double_slab}) are separate blocks, and {@link net.minecraft.item.ItemSlab} swaps one for
 * the other when a second slab is placed on the first. The double has no item and drops two halves.
 *
 * <h2>The one-value variant</h2>
 * {@link BlockSlab} insists on a variant property, which {@code ItemSlab} compares before joining two
 * slabs. These slabs are one block each, so it has one value; the client's state mapper hides it
 * ({@code Content.ignoreProperties}).
 *
 * <p>{@link #isDouble()} is answered by the subclass rather than read from a field, because
 * {@code BlockSlab}'s constructor calls it before any field of this class is assigned.
 */
public abstract class BlockShapedSlab extends BlockSlab {

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    /** The only variant there is. */
    public enum Variant implements IStringSerializable {
        DEFAULT;

        @Override
        public String getName() {
            return "default";
        }
    }

    /** The half slab every one of these drops and is picked as; set once both halves exist. */
    Block half;

    BlockShapedSlab(Block base, IBlockState baseState) {
        super(baseState.getMaterial(), base.getMapColor(baseState, null, null));
        // Modern slabs copy their strength from their block. Resistance is stored tripled and reported
        // fifthed, hence the conversion.
        setHardness(base.getBlockHardness(baseState, null, null));
        setResistance(base.getExplosionResistance(null) * 5.0F / 3.0F);
        setSoundType(base.getSoundType());
        String tool = base.getHarvestTool(baseState);
        if (tool != null) {
            setHarvestLevel(tool, base.getHarvestLevel(baseState));
        }

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
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.half);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.half);
    }

    /** @return the half slab (itself, or the double's half) */
    public Block half() {
        return this.half;
    }

    /** The slab itself. */
    public static class Half extends BlockShapedSlab {
        public Half(Block base, IBlockState baseState) {
            super(base, baseState);
            this.half = this;
        }

        @Override
        public boolean isDouble() {
            return false;
        }
    }

    /** Two slabs stacked into one block space. */
    public static class Double extends BlockShapedSlab {
        public Double(Block base, IBlockState baseState, Half half) {
            super(base, baseState);
            this.half = half;
        }

        @Override
        public boolean isDouble() {
            return true;
        }
    }
}
