package com.exiledradio.trickytrialsbackport.decor;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An armor trim smithing template (Flow, Bolt): "Smithing Template" with 1.21's four lines below it.
 *
 * <p>The trim itself is Trails &amp; Tales' (its smithing stream applies trims and draws them); it finds this item by
 * the {@code armorTrimTemplate} ore name and its {@code <pattern>_armor_trim_smithing_template} registry name, and
 * the pattern's grayscale textures at {@code trickytrialsbackport:textures/trims/entity/humanoid[_leggings]/<pattern>.png}
 * ({@code docs/CORE_REQUESTS.md} Decor-2). Without Trails &amp; Tales the template does nothing, but duplicates.
 */
public class ItemTrimTemplate extends Item {

    private final String pattern;

    ItemTrimTemplate(String pattern) {
        this.pattern = pattern;
    }

    /** @return the trim pattern's name, such as {@code flow} */
    public String pattern() {
        return this.pattern;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        String key = "item.trickytrialsbackport.smithing_template";
        tooltip.add(TextFormatting.GRAY + I18n.format("trim_pattern.trickytrialsbackport." + this.pattern));
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + I18n.format(key + ".applies_to"));
        tooltip.add(" " + TextFormatting.BLUE + I18n.format(key + ".armor_trim.applies_to"));
        tooltip.add(TextFormatting.GRAY + I18n.format(key + ".ingredients"));
        tooltip.add(" " + TextFormatting.BLUE + I18n.format(key + ".armor_trim.ingredients"));
    }
}
