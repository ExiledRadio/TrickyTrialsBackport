package com.exiledradio.trickytrialsbackport.copper;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Our copy of Caves &amp; Cliffs' copper ore (registered only without it): hardness 3, blast resistance 3,
 * a stone pickaxe, 2-5 raw copper with Fortune's multiplying rule, no experience.
 */
public final class BlockCopperOre extends Block {

    private final Supplier<Item> drop;

    public BlockCopperOre(Supplier<Item> drop) {
        super(Material.ROCK, MapColor.STONE);
        setSoundType(SoundType.STONE);
        setHardness(3.0F);
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 1);
        this.drop = drop;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        Item item = this.drop.get();
        return item != null ? item : Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return 2 + random.nextInt(4);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        int base = quantityDropped(random);
        if (fortune <= 0) {
            return base;
        }
        int i = random.nextInt(fortune + 2) - 1;
        return base * (Math.max(i, 0) + 1);
    }
}
