package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.lookfeel.Rarities;

import net.minecraft.item.EnumRarity;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21.2's item rarities ({@link Rarities}) in tooltips: the name line takes the color of the item's 1.21.11 rarity.
 *
 * <p>1.12.2's {@code GuiScreen.getItemToolTip} puts the old rarity's color in front of the name after this event;
 * a color code later in a line wins, so the new color goes right in front of the name (before any italic of a
 * renamed item). The item's own rarity is not changed: other mods asking for it get 1.12.2's.
 */
@SideOnly(Side.CLIENT)
public final class ItemRarityTooltips {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void tooltip(ItemTooltipEvent event) {
        if (event.getToolTip().isEmpty() || !com.exiledradio.backportcore.presentation.Presentation.isAtLeast("1.21.2")) {
            return;
        }
        EnumRarity rarity = Rarities.of(event.getItemStack());
        if (rarity == null || rarity == event.getItemStack().getRarity()) {
            return;
        }
        event.getToolTip().set(0, rarity.color + event.getToolTip().get(0));
    }
}
