package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.client.FogPipeline;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21.x's fog steps on Backport Core's fog pipeline. Both apply only to the open-air fog (not under water, in
 * lava or blind), as 1.21.11's {@code AtmosphericFogEnvironment} does.
 */
@SideOnly(Side.CLIENT)
public final class Fog {

    private Fog() {
    }

    /** True when the camera's fog is the open-air one. */
    static boolean atmospheric(FogPipeline.Context c) {
        if (c.state != null && (c.state.getMaterial() == Material.WATER || c.state.getMaterial() == Material.LAVA)) {
            return false;
        }
        return !(c.entity instanceof EntityLivingBase) || !((EntityLivingBase) c.entity).isPotionActive(MobEffects.BLINDNESS);
    }

    /**
     * 1.21.6 ({@code AtmosphericFogEnvironment.setupFog}): in rain the fog starts up to 160 blocks nearer and ends up to
     * 256 blocks nearer (never nearer than 96), by the rain level times the sky light above 8 (halved in a biome
     * without rain), eased in and out at a fifth per tick.
     */
    public static final FogPipeline.Step RAIN = new FogPipeline.Step() {
        private float multiplier;
        private long last = -1L;

        @Override
        public void distance(FogPipeline.Distance d) {
            Entity entity = d.entity;
            if (d.fogMode != 0 || entity == null || entity.world == null || !entity.world.provider.isSurfaceWorld()) {
                return;
            }
            World world = entity.world;
            long now = Minecraft.getSystemTime();
            float ticks = this.last < 0 ? 0.0F : Math.min(20.0F, (now - this.last) / 50.0F);
            this.last = now;
            BlockPos pos = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            Biome biome = world.getBiome(pos);
            float target = atmospheric(d) ? Rules.rainFogTarget(world.getRainStrength((float) d.partialTicks),
                    world.getLightFor(EnumSkyBlock.SKY, pos), biome.canRain() || biome.getEnableSnow()) : 0.0F;
            this.multiplier += (target - this.multiplier) * Math.min(1.0F, ticks * 0.2F);
            if (this.multiplier < 0.001F || !atmospheric(d)) {
                return;
            }
            // 1.12.2's own open-air fog is 75% to 100% of the render distance; an earlier version's step may have set
            // other distances.
            float start = Float.isNaN(d.start()) ? d.far * 0.75F : d.start();
            float end = Float.isNaN(d.end()) ? d.far : d.end();
            d.set(Rules.rainFogStart(start, this.multiplier), Rules.rainFogEnd(end, this.multiplier));
        }
    };

    /**
     * 1.21.11 ({@code WeatherAttributes}): rain multiplies the sunrise glow's red and green by 0.5 and blue by 0.6, and
     * thunder by 0.25, 0.25 and 0.3, each eased in by its level (rain by what is not thunder); and the clouds drift by
     * world time, the same for every player (Backport Core's P24 and P25).
     */
    public static final FogPipeline.Step SKY = new FogPipeline.Step() {
        @Override
        public void sunrise(FogPipeline.Sunrise s) {
            World world = Minecraft.getMinecraft().world;
            if (s.rgba == null || world == null || world.provider != s.provider) {
                return;
            }
            float thunder = world.getThunderStrength(s.partialTicks);
            float rain = world.getRainStrength(s.partialTicks) - thunder;
            Rules.weatherSunrise(s.rgba, rain, thunder);
        }

        @Override
        public boolean cloudsFollowWorldTime() {
            return true;
        }
    };

    /**
     * 1.21.11 ({@code the_nether}'s {@code fog_start_distance} 10 and {@code fog_end_distance} 96): the nether's fog no
     * longer follows the render distance (1.12.2's 5% to 50% of it, capped at 192); it reaches no farther than the
     * render distance itself.
     */
    public static final FogPipeline.Step NETHER = new FogPipeline.Step() {
        @Override
        public void distance(FogPipeline.Distance d) {
            Entity entity = d.entity;
            if (entity == null || entity.world == null || entity.world.provider.getDimensionType() != DimensionType.NETHER
                    || !atmospheric(d)) {
                return;
            }
            d.set(Math.min(Rules.NETHER_FOG_START, d.far), Math.min(Rules.NETHER_FOG_END, d.far));
        }
    };
}
