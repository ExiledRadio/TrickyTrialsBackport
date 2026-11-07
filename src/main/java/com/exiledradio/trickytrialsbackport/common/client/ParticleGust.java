package com.exiledradio.trickytrialsbackport.common.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** One gust puff: plays its frames once, still and full-bright, after an optional delay in ticks. */
final class ParticleGust extends Particle {

    private final TextureAtlasSprite[] frames;
    private int delay;

    ParticleGust(World world, double x, double y, double z, TextureAtlasSprite[] frames, float scale, int delay) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.frames = frames;
        this.delay = delay;
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.particleGravity = 0.0F;
        this.particleScale = scale;
        this.particleMaxAge = frames.length + this.rand.nextInt(4);
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
        if (this.delay > 0) {
            this.delay--;
            return;
        }
        if (this.particleAge++ >= this.particleMaxAge) {
            setExpired();
            return;
        }
        int frame = Math.min(this.frames.length - 1, this.particleAge * this.frames.length / this.particleMaxAge);
        setParticleTexture(this.frames[frame]);
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotX, float rotZ, float rotYZ,
                               float rotXY, float rotXZ) {
        if (this.delay <= 0) {
            super.renderParticle(buffer, entity, partialTicks, rotX, rotZ, rotYZ, rotXY, rotXZ);
        }
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 0xF000F0;
    }
}
