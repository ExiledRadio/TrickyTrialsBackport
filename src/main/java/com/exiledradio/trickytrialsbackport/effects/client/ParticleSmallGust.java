package com.exiledradio.trickytrialsbackport.effects.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;

/**
 * 1.21's {@code small_gust} ({@code GustParticle.SmallProvider}): still, full-bright, 12-15 ticks through
 * its seven frames, 0.15 blocks across.
 */
final class ParticleSmallGust extends Particle {

    private final TextureAtlasSprite[] frames;

    ParticleSmallGust(World world, double x, double y, double z, TextureAtlasSprite[] frames) {
        super(world, x, y, z);
        this.frames = frames;
        this.particleMaxAge = 12 + this.rand.nextInt(4);
        // 1.12.2 draws a particle 0.1 x scale across.
        this.particleScale = 1.5F;
        this.canCollide = false;
        setParticleTexture(frames[0]);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) {
            setExpired();
            return;
        }
        setParticleTexture(this.frames[this.particleAge * (this.frames.length - 1) / this.particleMaxAge]);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 0xF000F0;
    }
}
