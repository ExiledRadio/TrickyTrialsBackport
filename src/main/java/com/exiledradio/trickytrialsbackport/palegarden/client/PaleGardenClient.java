package com.exiledradio.trickytrialsbackport.palegarden.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.client.FallingLeafParticle;
import com.exiledradio.trickytrialsbackport.palegarden.BiomePaleGarden;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;
import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodSign;
import com.exiledradio.trickytrialsbackport.palegarden.wood.client.WoodSignRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Client parts of the pale garden: the falling pale oak leaf, the sign renderer (the boats are Backport Core's), and the pale
 * garden's gray fog.
 */
public final class PaleGardenClient {

    private static final TextureAtlasSprite[] SPRITES = new TextureAtlasSprite[12];

    private PaleGardenClient() {
    }

    public static void init() {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Events());
        ClientRegistry.bindTileEntitySpecialRenderer(WoodSign.Tile.class, new WoodSignRenderer());
    }

    /** Called from the leaves' random display tick: one leaf just under the block ({@code spawnParticleBelow}). */
    public static void leaf(World world, BlockPos pos, Random rand) {
        if (SPRITES[0] == null) {
            return;
        }
        Minecraft.getMinecraft().effectRenderer.addEffect(new FallingLeafParticle(world, pos.getX() + rand.nextDouble(),
                pos.getY() - 0.05D, pos.getZ() + rand.nextDouble(), SPRITES[rand.nextInt(SPRITES.length)], rand));
    }

    public static final class Events {
        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            for (int i = 0; i < SPRITES.length; i++) {
                SPRITES[i] = event.getMap().registerSprite(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "particle/pale_oak_" + i));
            }
        }

        /**
         * The pale garden's fog color, {@code #817770}, blended in by how much of the area around the camera
         * (a 5x5 of 4-block cells, as modern fog samples biomes) is pale garden, with the overworld's
         * day-night darkening applied as 1.12.2 applies it to its own fog color.
         */
        @SubscribeEvent
        public void fog(EntityViewRenderEvent.FogColors event) {
            BiomePaleGarden pale = PaleGardenContent.biome;
            Entity view = event.getEntity();
            if (pale == null || view == null || view.world == null || !view.world.provider.isSurfaceWorld()) {
                return;
            }
            World world = view.world;
            int px = MathHelper.floor(view.posX);
            int pz = MathHelper.floor(view.posZ);
            int count = 0;
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    at.setPos(px + dx * 4, 64, pz + dz * 4);
                    Biome b = world.getBiome(at);
                    if (b == pale) {
                        count++;
                    }
                }
            }
            if (count == 0) {
                return;
            }
            float share = count / 25.0F;
            float angle = world.getCelestialAngle((float) event.getRenderPartialTicks());
            float light = MathHelper.clamp(MathHelper.cos(angle * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
            float r = (BiomePaleGarden.FOG >> 16 & 255) / 255.0F * (light * 0.94F + 0.06F);
            float g = (BiomePaleGarden.FOG >> 8 & 255) / 255.0F * (light * 0.94F + 0.06F);
            float b = (BiomePaleGarden.FOG & 255) / 255.0F * (light * 0.91F + 0.09F);
            event.setRed(event.getRed() + (r - event.getRed()) * share);
            event.setGreen(event.getGreen() + (g - event.getGreen()) * share);
            event.setBlue(event.getBlue() + (b - event.getBlue()) * share);
        }
    }
}
