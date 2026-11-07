package com.exiledradio.trickytrialsbackport.common;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The mod's creative tab: {@code itemGroup.trickytrialsbackport}. */
public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab TAB = new ModCreativeTab();

    /** Items that can stand for the tab, first one registered wins; switched-off streams have none. */
    private static final String[] ICONS = {"mace", "trial_spawner", "copper_bulb", "chiseled_tuff_bricks", "polished_tuff"};

    private ModCreativeTab() {
        super(TrickyTrialsBackport.MOD_ID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        for (String name : ICONS) {
            Item item = ForgeRegistries.ITEMS.getValue(Content.id(name));
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.BLAZE_ROD);
    }
}
