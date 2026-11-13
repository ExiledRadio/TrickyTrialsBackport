package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.21.2's air bubbles ({@code Gui.renderAirBubbles}): the ten slots keep showing once air runs low, a used bubble
 * bursts for a moment then stays as an empty bubble, the empty ones wobble when air is gone, and each burst pops,
 * louder and higher as more are empty. Shown under water and while air is below full.
 *
 * <p>Drawn in place of Forge's air bar, at its place ({@code GuiIngameForge.right_height}); other mods still get
 * the bar's post event.
 */
@SideOnly(Side.CLIENT)
public final class AirBubbles {

    private static final ResourceLocation FULL = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/gui/sprites/hud/air.png");
    private static final ResourceLocation BURSTING = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/gui/sprites/hud/air_bursting.png");
    private static final ResourceLocation EMPTY = new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/gui/sprites/hud/air_empty.png");

    /** 1.12.2's air supply when full ({@code Entity.getAir} starts at 300). */
    private static final int MAX_AIR = 300;

    /** For a look in game: frames the bar was drawn. */
    public static volatile int frames;

    private final Random random = new Random();
    private int lastPopSound;

    @SubscribeEvent
    public void air(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (!(view instanceof EntityPlayer)) {
            return;
        }
        event.setCanceled(true);
        EntityPlayer player = (EntityPlayer) view;
        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();
        int right = width / 2 + 91;
        int top = height - GuiIngameForge.right_height;
        int air = Math.max(0, Math.min(MAX_AIR, player.getAir()));
        boolean underWater = player.isInsideOfMaterial(Material.WATER);
        if (underWater || air < MAX_AIR) {
            if (!underWater) {
                this.lastPopSound = 0;
            }
            int empty = 10 - Rules.airBubble(air, MAX_AIR, Rules.emptyBubbleDelay(air, underWater));
            int ticks = mc.ingameGUI.getUpdateCounter();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            for (int i = 1; i <= 10; i++) {
                int x = right - (i - 1) * 8 - 9;
                switch (Rules.bubble(i, air, MAX_AIR, underWater)) {
                    case FULL:
                        draw(mc, FULL, x, top);
                        break;
                    case BURSTING:
                        draw(mc, BURSTING, x, top);
                        if (this.lastPopSound != i && LookAndFeel.BUBBLE_POP != null) {
                            player.playSound(LookAndFeel.BUBBLE_POP, Rules.bubblePopVolume(empty), Rules.bubblePopPitch(empty));
                            this.lastPopSound = i;
                        }
                        break;
                    case EMPTY:
                        int wobble = empty == 10 && ticks % 2 == 0 ? this.random.nextInt(2) : 0;
                        draw(mc, EMPTY, x, top + wobble);
                        break;
                    default:
                        break;
                }
            }
            GlStateManager.disableBlend();
            GuiIngameForge.right_height += 10;
            frames++;
        }
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, RenderGameOverlayEvent.ElementType.AIR));
    }

    private static void draw(Minecraft mc, ResourceLocation sprite, int x, int y) {
        mc.getTextureManager().bindTexture(sprite);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 9, 9, 9.0F, 9.0F);
    }
}
