package com.exiledradio.trickytrialsbackport.effects;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * One of 1.21's four effects: harmful, a color, and its own 18x18 icon
 * ({@code textures/mob_effect/<name>.png}, as 1.21 draws it) instead of a cell of 1.12.2's inventory sheet.
 *
 * <p>Its status icon index stays -1, so vanilla draws no cell for it. The two drawing methods are client-only and stripped from a server by Forge's side transformer, so the
 * {@code Gui} in their descriptors never loads there; their bodies hand plain numbers to the client class.
 */
public class EffectPotion extends Potion {

    private final String name;

    EffectPotion(String name, int color) {
        super(true, color);
        this.name = name;
        setPotionName("effect." + TrickyTrialsBackport.MOD_ID + "." + name);
    }

    /** @return the short name, such as {@code weaving} */
    public String shortName() {
        return this.name;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        com.exiledradio.trickytrialsbackport.effects.client.EffectsClient.drawIcon(this.name, x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.trickytrialsbackport.effects.client.EffectsClient.drawIcon(this.name, x + 3, y + 3, alpha);
    }
}
