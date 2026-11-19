package com.exiledradio.trickytrialsbackport.coppergolem;

import net.minecraft.item.ItemBlock;

/**
 * A copper golem statue's item: its damage value is the pose it is placed in (1.21.9 keeps the pose in the item's
 * block state). One item in the creative menu, standing, as in 1.21.9.
 */
public class ItemCopperGolemStatue extends ItemBlock {

    public ItemCopperGolemStatue(BlockCopperGolemStatue block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return BlockCopperGolemStatue.Pose.byId(damage).ordinal();
    }
}
