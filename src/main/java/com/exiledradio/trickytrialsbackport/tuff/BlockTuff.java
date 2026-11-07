package com.exiledradio.trickytrialsbackport.tuff;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/**
 * Tuff and every full block made from it: rock, needs a pickaxe, hardness 1.5 and blast resistance 6
 * (1.21.11's numbers for all of them), gray terracotta on maps.
 */
public final class BlockTuff extends Block {

    public BlockTuff(SoundType sound) {
        super(Material.ROCK, MapColor.GRAY_STAINED_HARDENED_CLAY);
        setHardness(1.5F);
        // 1.12.2 stores resistance tripled and reports it fifthed: 10 here is a modern 6.
        setResistance(10.0F);
        setSoundType(sound);
        setHarvestLevel("pickaxe", 0);
    }
}
