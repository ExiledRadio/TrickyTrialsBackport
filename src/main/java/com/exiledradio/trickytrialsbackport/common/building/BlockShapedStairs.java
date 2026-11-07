package com.exiledradio.trickytrialsbackport.common.building;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/**
 * Modern stairs. Vanilla's {@link BlockStairs} already does corners, upside-down placement and copying
 * its strength and sound from the block it is made of; it only hides its constructor.
 */
public final class BlockShapedStairs extends BlockStairs {

    public BlockShapedStairs(IBlockState base) {
        super(base);
        this.useNeighborBrightness = true;
        String tool = base.getBlock().getHarvestTool(base);
        if (tool != null) {
            setHarvestLevel(tool, base.getBlock().getHarvestLevel(base));
        }
    }
}
