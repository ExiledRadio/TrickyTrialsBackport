package com.exiledradio.trickytrialsbackport.nautilus;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21.11's Breath of the Nautilus: beneficial, color {@code #00FFEE}. Under water the air bar neither drops nor
 * refills (1.21.11's {@code MobEffectUtil.hasWaterBreathing} and {@code shouldEffectsRefillAirsupply}); the air is
 * held by {@link NautilusContent}'s tick handlers. Its icon is 1.21.11's 18x18 texture, drawn by the client.
 */
public class PotionBreath extends Potion {

    public static final int COLOR = 0x00FFEE;

    public PotionBreath() {
        super(false, COLOR);
        this.setPotionName("effect." + TrickyTrialsBackport.MOD_ID + ".breath_of_the_nautilus");
        this.setBeneficial();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        com.exiledradio.trickytrialsbackport.nautilus.client.NautilusClient.drawIcon(x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.trickytrialsbackport.nautilus.client.NautilusClient.drawIcon(x + 3, y + 3, alpha);
    }
}
