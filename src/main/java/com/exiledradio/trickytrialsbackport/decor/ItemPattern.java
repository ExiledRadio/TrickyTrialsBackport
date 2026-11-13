package com.exiledradio.trickytrialsbackport.decor;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A banner pattern item when Village &amp; Pillage's loom is not installed: one to a stack, named "Banner Pattern"
 * with the design below (Flow, Guster) or by its own name (Field Masoned, Bordure Indented), and left in the crafting
 * grid when a banner recipe uses it, as a loom leaves it.
 */
public class ItemPattern extends Item {

    @Nullable
    private final BannerPattern pattern;
    private final boolean description;
    private final EnumRarity rarity;

    ItemPattern(@Nullable BannerPattern pattern, boolean description, EnumRarity rarity) {
        this.pattern = pattern;
        this.description = description;
        this.rarity = rarity;
        setMaxStackSize(1);
    }

    @Nullable
    public BannerPattern getPattern() {
        return this.pattern;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return this.rarity;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (this.description) {
            tooltip.add(TextFormatting.GRAY + I18n.format(getTranslationKey(stack) + ".desc"));
        }
    }
}
