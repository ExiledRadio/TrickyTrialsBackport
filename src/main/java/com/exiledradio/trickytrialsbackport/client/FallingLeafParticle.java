package com.exiledradio.trickytrialsbackport.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's {@code FallingParticle} with the numbers {@code FallingLeavesParticle}'s pale oak, poplar and tinted leaves
 * providers all pass: gravity 0.07 (x 1.2 x 0.0025), wind 10, swirl on, flow-away off, size 2 x (0.05 or 0.075),
 * falling at 0.021 from the start; lives up to 300 ticks and is gone when it lands or stops against something.
 * The tinted leaf ({@code TintedLeavesProvider}) takes its color from the leaves it fell from.
 */
public final class FallingLeafParticle extends Particle {
    private final double swirlPeriod;
    private final float windBig = 10.0F;
    private float rotSpeed;
    private final float spinAcceleration;
    private int life = 300;

    public FallingLeafParticle(World world, double x, double y, double z, TextureAtlasSprite sprite, Random rand) {
        super(world, x, y, z);
        setParticleTexture(sprite);
        this.rotSpeed = (float) Math.toRadians(rand.nextBoolean() ? -30.0D : 30.0D);
        this.spinAcceleration = (float) Math.toRadians(rand.nextBoolean() ? -5.0D : 5.0D);
        this.particleMaxAge = 300;
        this.particleGravity = 0.07F * 1.2F * 0.0025F;
        float size = 2.0F * (rand.nextBoolean() ? 0.05F : 0.075F);
        // 1.12.2 draws a quad 0.1 * particleScale across; modern particles give the size directly.
        this.particleScale = size * 10.0F;
        setSize(size, size);
        this.motionX = 0.0D;
        this.motionY = -0.021D;
        this.motionZ = 0.0D;
        float f = rand.nextFloat();
        this.swirlPeriod = Math.toRadians(1000.0F + f * 3000.0F);
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
    }

    /** {@code setColor} from a {@code ColorParticleOption}'s RGB; {@code -1} (no tint) stays white. */
    public FallingLeafParticle tint(int rgb) {
        setRBGColorF((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F);
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
        if (this.life-- <= 0) {
            setExpired();
            return;
        }
        float t = Math.min((300 - this.life) / 300.0F, 1.0F);
        double ax = t * Math.cos(t * this.swirlPeriod) * this.windBig;
        double az = t * Math.sin(t * this.swirlPeriod) * this.windBig;
        this.motionX += ax * 0.0025F;
        this.motionZ += az * 0.0025F;
        this.motionY -= this.particleGravity;
        this.rotSpeed += this.spinAcceleration / 20.0F;
        this.prevParticleAngle = this.particleAngle;
        this.particleAngle += this.rotSpeed / 20.0F;
        move(this.motionX, this.motionY, this.motionZ);
        if (this.onGround || this.life < 299 && (this.motionX == 0.0D || this.motionZ == 0.0D)) {
            setExpired();
        }
    }
}
