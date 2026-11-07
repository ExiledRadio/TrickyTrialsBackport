package com.exiledradio.trickytrialsbackport.common.building;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * The stairs, slab and wall cut from one block, registered and named the modern way:
 * {@code <prefix>_stairs}, {@code <prefix>_slab} (+ {@code <prefix>_double_slab}), {@code <prefix>_wall}.
 *
 * <p>The base block need not be registered by this mod (a copy's owner may provide the real one); the
 * shapes copy strength, sound and tool from whatever base they are given.
 */
public final class Shapes {

    public final String prefix;
    @Nullable public final BlockShapedStairs stairs;
    @Nullable public final BlockShapedSlab.Half slab;
    @Nullable public final BlockShapedSlab.Double doubleSlab;
    @Nullable public final BlockShapedWall wall;

    private Shapes(String prefix, BlockShapedStairs stairs, BlockShapedSlab.Half slab, BlockShapedSlab.Double doubleSlab,
                   BlockShapedWall wall) {
        this.prefix = prefix;
        this.stairs = stairs;
        this.slab = slab;
        this.doubleSlab = doubleSlab;
        this.wall = wall;
    }

    /** Registers the asked-for shapes of a base block. */
    public static Shapes register(IForgeRegistry<Block> registry, String prefix, Block base, boolean stairs, boolean slab,
                                  boolean wall) {
        IBlockState state = base.getDefaultState();
        String tool = base.getHarvestTool(state);
        BlockShapedStairs st = null;
        BlockShapedSlab.Half half = null;
        BlockShapedSlab.Double full = null;
        BlockShapedWall wa = null;
        if (stairs) {
            st = Content.block(registry, prefix + "_stairs", new BlockShapedStairs(state), tool);
        }
        if (slab) {
            half = Content.block(registry, prefix + "_slab", new BlockShapedSlab.Half(base, state), tool);
            full = Content.block(registry, prefix + "_double_slab", new BlockShapedSlab.Double(base, state, half), tool);
            full.setCreativeTab(null);
            Content.ignoreProperties(half, BlockShapedSlab.VARIANT);
            Content.ignoreProperties(full, BlockShapedSlab.VARIANT);
        }
        if (wall) {
            wa = Content.block(registry, prefix + "_wall", new BlockShapedWall(base, state), tool);
            Content.ignoreProperties(wa, BlockWall.VARIANT);
            Content.multipartFile(wa);
        }
        return new Shapes(prefix, st, half, full, wa);
    }

    /** Registers the items: plain for stairs and wall, the joining {@link ItemSlab} for the slab. */
    public void registerItems(IForgeRegistry<Item> registry) {
        if (this.stairs != null) {
            Content.registerBlockItem(registry, this.stairs);
        }
        if (this.slab != null) {
            Content.blockItem(registry, this.slab, new ItemSlab(this.slab, (BlockSlab) this.slab, (BlockSlab) this.doubleSlab));
        }
        if (this.wall != null) {
            Content.registerBlockItem(registry, this.wall);
        }
    }
}
