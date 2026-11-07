package com.exiledradio.trickytrialsbackport.common.client;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Client side of {@code GustMessage}: 1.21's gust emitters. A large one (the breeze's charge, Wind Burst)
 * puffs twelve 12-frame gusts over a 1.5-block ball; a small one (a thrown wind charge) seven 7-frame
 * small gusts over 0.6 blocks. The frames are stitched into the block atlas, so the particles draw with
 * vanilla's own particle layer.
 */
public final class ClientGust {

    static final TextureAtlasSprite[] GUST = new TextureAtlasSprite[12];
    static final TextureAtlasSprite[] SMALL_GUST = new TextureAtlasSprite[7];

    private ClientGust() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientGust());
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        for (int i = 0; i < GUST.length; i++) {
            GUST[i] = map.registerSprite(Content.id("particle/gust_" + i));
        }
        for (int i = 0; i < SMALL_GUST.length; i++) {
            SMALL_GUST[i] = map.registerSprite(Content.id("particle/small_gust_" + i));
        }
    }

    /** From the network thread: spawn on the client thread. */
    public static void schedule(final double x, final double y, final double z, final boolean large) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                spawn(mc.world, x, y, z, large);
            }
        });
    }

    static void spawn(World world, double x, double y, double z, boolean large) {
        if (world == null || GUST[0] == null) {
            return;
        }
        Random random = world.rand;
        int count = large ? 12 : 7;
        double spread = large ? 1.5 : 0.6;
        for (int i = 0; i < count; i++) {
            double px = x + (random.nextDouble() * 2 - 1) * spread;
            double py = y + (random.nextDouble() * 2 - 1) * spread;
            double pz = z + (random.nextDouble() * 2 - 1) * spread;
            Particle p = new ParticleGust(world, px, py, pz, large ? GUST : SMALL_GUST, large ? 10.0F : 4.0F, i / 3);
            Minecraft.getMinecraft().effectRenderer.addEffect(p);
        }
    }
}
