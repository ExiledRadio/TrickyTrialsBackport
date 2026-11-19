package com.exiledradio.trickytrialsbackport.coppergear.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** The copper torch's green flame (1.21.9's {@code copper_fire_flame}: a flame particle with its own sprite). */
public final class CopperGearClient {

    private static TextureAtlasSprite flame;

    private CopperGearClient() {
    }

    public static void init() {
        if (CopperGearContent.enabled()) {
            MinecraftForge.EVENT_BUS.register(new Events());
        }
    }

    /** Spawns one copper flame, as the torch spawns its flame. */
    public static void flame(World world, double x, double y, double z) {
        if (flame != null) {
            Minecraft.getMinecraft().effectRenderer.addEffect(new CopperFlame(world, x, y, z));
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            flame = event.getMap().registerSprite(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "particle/copper_fire_flame"));
        }
    }

    /** 1.12.2's flame (its motion, shrinking and full brightness), drawn from the block atlas with the copper sprite. */
    static final class CopperFlame extends ParticleFlame {
        CopperFlame(World world, double x, double y, double z) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            setParticleTexture(flame);
        }

        @Override
        public int getFXLayer() {
            return 1;
        }
    }
}
