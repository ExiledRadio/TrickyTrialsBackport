package com.exiledradio.trickytrialsbackport.effects.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;

/**
 * 1.21's {@code infested} particle ({@code SpellParticle.Provider} with its own texture): spawned with 1.21's
 * effect-particle speed of (1, 1, 1), it drifts sideways and rises (gravity -0.1, friction 0.96), three
 * quarters of a particle's usual size, for 8-40 ticks, through blocks.
 */
final class ParticleInfested extends Particle {

    ParticleInfested(World world, double x, double y, double z, TextureAtlasSprite sprite) {
        super(world, x, y, z, 0.5 - Math.random(), 1.0, 0.5 - Math.random());
        this.motionY *= 0.2F;
        this.particleScale *= 0.75F;
        this.particleMaxAge = (int) (8.0 / (this.rand.nextFloat() * 0.8 + 0.2));
        this.particleGravity = -0.1F;
        this.canCollide = false;
        setParticleTexture(sprite);
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
        this.motionY -= 0.04 * this.particleGravity;
        double before = this.posY;
        move(this.motionX, this.motionY, this.motionZ);
        if (this.posY == before) {
            // speedUpWhenYMotionIsBlocked
            this.motionX *= 1.1;
            this.motionZ *= 1.1;
        }
        this.motionX *= 0.96F;
        this.motionY *= 0.96F;
        this.motionZ *= 0.96F;
    }
}
