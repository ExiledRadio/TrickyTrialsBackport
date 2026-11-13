package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Trial Omen and (without Village &amp; Pillage) our copy of Bad Omen: neutral effects that do nothing by
 * themselves; trial spawners read them. Each draws 1.21's own icon, as 1.12.2's shared effect sheet has
 * no room for more.
 */
public final class TrialEffect extends Potion {

    private final ResourceLocation icon;

    TrialEffect(String name, int color) {
        super(false, color);
        setPotionName("effect." + Content.MOD_ID + "." + name);
        setRegistryName(Content.id(name));
        this.icon = Content.id("textures/mob_effect/" + name + ".png");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z) {
        com.exiledradio.trickytrialsbackport.trial.client.TrialFx.drawIcon(this.icon, x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.trickytrialsbackport.trial.client.TrialFx.drawIcon(this.icon, x + 3, y + 3, alpha);
    }
}
