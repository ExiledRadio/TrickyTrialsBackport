package com.exiledradio.trickytrialsbackport.common.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A modern wall.
 *
 * <p>Extending {@link BlockWall} is what makes it connect to vanilla's walls, other mods' walls and
 * Village &amp; Pillage's. The cost is vanilla's {@code variant} property (cobblestone or mossy), which
 * {@code BlockWall}'s constructor sets: it is pinned to its first value, saved as metadata 0, and hidden
 * from the client's state mapper ({@code Content.ignoreProperties}).
 */
public final class BlockShapedWall extends BlockWall {

    private final MapColor color;

    public BlockShapedWall(Block base, IBlockState baseState) {
        super(base);
        this.color = base.getMapColor(baseState, null, null);
        // BlockWall copies hardness and resistance; modern walls also keep their block's sound and tool.
        setSoundType(base.getSoundType());
        String tool = base.getHarvestTool(baseState);
        if (tool != null) {
            setHarvestLevel(tool, base.getHarvestLevel(baseState));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getLocalizedName() {
        // Vanilla's appends ".normal", the name of the cobblestone variant.
        return net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.color;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }
}
