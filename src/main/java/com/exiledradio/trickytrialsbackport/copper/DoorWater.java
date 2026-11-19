package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

/**
 * Patch P25's hook (docs/PATCHES.md): water never flows into our doors, as in vanilla, where {@code
 * FlowingFluid.canHoldFluid} refuses a {@code DoorBlock} (1.12.2's {@code BlockDynamicLiquid.isBlocked}).
 *
 * <p>Called from Fluidlogged API's flow, for a door, sign, ladder or sugar cane it was about to let water displace.
 * Our doors are never waterlogged (Backport Core keeps the family's doors dry, as 1.13 and later do), so displacing
 * one would replace it with water: a door placed in water broke at once, and one beside water as soon as it spread.
 */
public final class DoorWater {

    private DoorWater() {
    }

    /**
     * @return false for our doors (water stops at them); true, Fluidlogged API's own answer, for everything else
     */
    public static boolean displaced(IBlockState state) {
        Block block = state.getBlock();
        return !(block instanceof CopperBlocks.Door || block instanceof WoodBlocks.Door);
    }
}
