package com.exiledradio.trickytrialsbackport.trial.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * 1.21's trial particles, drawn from sprites stitched into the block atlas: the flame kinds (small flame,
 * soul fire flame), the spawner's detection puffs, Trial Omen's swirl, the ominous spawning streaks and
 * the vault's connection motes, each with its 1.21 motion, size and lifetime.
 */
final class ParticleTrial extends Particle {

    enum Kind {
        FLAME, DETECTION, SPELL, FLY_STRAIGHT, FLY_TOWARD
    }

    private final Kind kind;
    private final TextureAtlasSprite[] frames;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double dx;
    private final double dy;
    private final double dz;
    private final float baseScale;
    private int startColor;
    private int endColor;

    ParticleTrial(World world, Kind kind, TextureAtlasSprite[] frames, double x, double y, double z, double vx, double vy, double vz, float scale) {
        super(world, x, y, z, vx, vy, vz);
        this.kind = kind;
        this.frames = frames;
        this.dx = vx;
        this.dy = vy;
        this.dz = vz;
        this.canCollide = false;
        this.particleGravity = 0.0F;
        setParticleTexture(frames[0]);
        switch (kind) {
            case FLAME:
                this.motionX = this.motionX * 0.01 + vx;
                this.motionY = this.motionY * 0.01 + vy;
                this.motionZ = this.motionZ * 0.01 + vz;
                setPosition(x + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F, y + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F,
                        z + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
                this.particleMaxAge = (int) (8.0 / (this.rand.nextFloat() * 0.8 + 0.2)) + 4;
                this.particleScale *= scale;
                break;
            case DETECTION:
                this.motionX = vx;
                this.motionY = this.motionY * 0.9 + vy;
                this.motionZ = vz;
                this.particleScale *= 0.75F * scale;
                this.particleMaxAge = Math.max(1, (int) (8.0F / (0.5F + this.rand.nextFloat() * 0.5F) * scale));
                this.canCollide = true;
                break;
            case SPELL:
                this.motionX = (0.5 - this.rand.nextDouble()) * 0.1;
                this.motionY = this.motionY * 0.2;
                this.motionZ = (0.5 - this.rand.nextDouble()) * 0.1;
                this.particleScale *= 0.75F;
                this.particleMaxAge = (int) (8.0 / (this.rand.nextFloat() * 0.8 + 0.2));
                this.particleGravity = -0.1F;
                break;
            case FLY_STRAIGHT:
            case FLY_TOWARD:
            default:
                this.motionX = 0.0;
                this.motionY = 0.0;
                this.motionZ = 0.0;
                setPosition(x + vx, y + vy, z + vz);
                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.particleScale = (this.rand.nextFloat() * 0.5F + 0.2F) * scale;
                if (kind == Kind.FLY_STRAIGHT) {
                    this.particleMaxAge = (int) (this.rand.nextFloat() * 5.0F) + 25;
                } else {
                    this.particleMaxAge = (int) (this.rand.nextFloat() * 10.0F) + 30;
                    float f = this.rand.nextFloat() * 0.6F + 0.4F;
                    setRBGColorF(0.9F * f, 0.9F * f, f);
                }
                break;
        }
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.baseScale = this.particleScale;
    }

    /** The ominous spawning streak runs from one color to another. */
    ParticleTrial colors(int from, int to) {
        this.startColor = from;
        this.endColor = to;
        return this;
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
        float t = (float) this.particleAge / this.particleMaxAge;
        switch (this.kind) {
            case FLY_STRAIGHT: {
                float left = 1.0F - t;
                setPosition(this.startX + this.dx * left, this.startY + this.dy * left, this.startZ + this.dz * left);
                int c = lerpColor(t, this.startColor, this.endColor);
                setRBGColorF((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
                setAlphaF((c >>> 24) / 255.0F);
                return;
            }
            case FLY_TOWARD: {
                float left = 1.0F - t;
                float drop = t * t;
                drop *= drop;
                setPosition(this.startX + this.dx * left, this.startY + this.dy * left - drop * 1.2F, this.startZ + this.dz * left);
                // LifetimeAlpha(0, 0.6, 0.25, 1): fade in over the first quarter, out at the end.
                float alpha = t < 0.25F ? 0.6F * t / 0.25F : 0.6F * (1.0F - (t - 0.25F) / 0.75F);
                setAlphaF(MathHelper.clamp(alpha, 0.0F, 0.6F));
                return;
            }
            default:
                break;
        }
        this.motionY -= 0.04 * this.particleGravity;
        move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.96;
        this.motionY *= 0.96;
        this.motionZ *= 0.96;
        if (this.frames.length > 1) {
            setParticleTexture(this.frames[Math.min(this.frames.length - 1, this.particleAge * this.frames.length / Math.max(1, this.particleMaxAge))]);
        }
        if (this.kind == Kind.FLAME) {
            this.particleScale = this.baseScale * (1.0F - t * t * 0.5F);
        } else if (this.kind == Kind.DETECTION) {
            this.particleScale = this.baseScale * MathHelper.clamp(t * 32.0F, 0.0F, 1.0F);
        }
    }

    private static int lerpColor(float t, int a, int b) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = a >>> shift & 255;
            int cb = b >>> shift & 255;
            out |= (int) (ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        if (this.kind == Kind.SPELL) {
            return super.getBrightnessForRender(partialTick);
        }
        return 0xF000F0;
    }
}
