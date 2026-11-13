package com.exiledradio.trickytrialsbackport.creaking.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/** Client side of {@code CreakingFxMessage}: the trail and block crumble particles. */
public final class CreakingParticles {

    private static final ParticleDigging.Factory DIGGING = new ParticleDigging.Factory();

    private CreakingParticles() {
    }

    /** From the network thread: spawn on the client thread. */
    public static void schedule(final byte kind, final int color, final int count, final int stateId, final double[] data) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.world != null) {
                    if (kind == 0) {
                        trails(mc, mc.world, color, count, data);
                    } else {
                        crumble(mc, mc.world, count, stateId, data);
                    }
                }
            }
        });
    }

    static void trails(Minecraft mc, World world, int color, int count, double[] d) {
        for (int i = 0; i < count && i * 7 + 6 < d.length; i++) {
            int o = i * 7;
            mc.effectRenderer.addEffect(new Trail(world, d[o], d[o + 1], d[o + 2], d[o + 3], d[o + 4], d[o + 5], color, (int) d[o + 6]));
        }
    }

    /** {@code block_crumble}: terrain particles that do not move, living 1-10 ticks. */
    static void crumble(Minecraft mc, World world, int count, int stateId, double[] d) {
        if (d.length < 6) {
            return;
        }
        Random rand = world.rand;
        for (int i = 0; i < count; i++) {
            double x = d[0] + rand.nextGaussian() * d[3];
            double y = d[1] + rand.nextGaussian() * d[4];
            double z = d[2] + rand.nextGaussian() * d[5];
            Particle p = DIGGING.createParticle(0, world, x, y, z, 0.0D, 0.0D, 0.0D, stateId);
            if (p != null) {
                p.multiplyVelocity(0.0F);
                p.setMaxAge(rand.nextInt(10) + 1);
                mc.effectRenderer.addEffect(p);
            }
        }
    }

    /** 1.21.4's {@code TrailParticle}: glides to its target, arriving as it dies; full bright, size 0.26. */
    static final class Trail extends Particle {
        private final double tx;
        private final double ty;
        private final double tz;

        Trail(World world, double x, double y, double z, double tx, double ty, double tz, int color, int duration) {
            super(world, x, y, z);
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            Random r = this.rand;
            this.particleRed = scale((color >> 16) & 255, 0.875F + r.nextFloat() * 0.25F);
            this.particleGreen = scale((color >> 8) & 255, 0.875F + r.nextFloat() * 0.25F);
            this.particleBlue = scale(color & 255, 0.875F + r.nextFloat() * 0.25F);
            // 1.21's quadSize is half the side; 1.12.2's half side is 0.1 times particleScale.
            this.particleScale = 2.6F;
            this.particleMaxAge = duration;
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            setParticleTextureIndex(0);
        }

        /** {@code ARGB.scaleRGB}: a channel times a factor, clamped to 255. */
        private static float scale(int channel, float f) {
            return MathHelper.clamp((int) (channel * f), 0, 255) / 255.0F;
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
            int left = this.particleMaxAge - this.particleAge;
            // Vanilla divides by zero on its last tick; arriving is what that tick means.
            double t = left <= 0 ? 1.0D : 1.0D / left;
            setPosition(this.posX + (this.tx - this.posX) * t, this.posY + (this.ty - this.posY) * t, this.posZ + (this.tz - this.posZ) * t);
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            return 0xF000F0;
        }
    }
}
