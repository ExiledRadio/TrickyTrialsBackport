package com.exiledradio.trickytrialsbackport.decor.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.decor.DecorContent;
import com.exiledradio.trickytrialsbackport.decor.PaintingItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/**
 * 1.19.4's painting tooltip ({@code HangingEntityItem.appendHoverText}): a painting with a variant shows its title in
 * yellow, its author in gray and its size; a random one shows "Random variant" in gray, in creative only.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID, value = Side.CLIENT)
public final class PaintingTooltip {

    private PaintingTooltip() {
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!DecorContent.enabled() || event.getItemStack().getItem() != Items.PAINTING) {
            return;
        }
        List<String> lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        PaintingItems.Entry entry = PaintingItems.of(event.getItemStack());
        if (entry == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null && mc.player.capabilities.isCreativeMode) {
                lines.add(at, TextFormatting.GRAY + I18n.format("painting.random"));
            }
            return;
        }
        String key = "painting.minecraft." + entry.name;
        lines.add(at++, TextFormatting.YELLOW + I18n.format(key + ".title"));
        lines.add(at++, TextFormatting.GRAY + I18n.format(key + ".author"));
        lines.add(at, I18n.format("painting.dimensions", entry.width, entry.height));
    }
}
