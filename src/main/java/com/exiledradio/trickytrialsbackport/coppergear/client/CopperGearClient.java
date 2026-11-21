package com.exiledradio.trickytrialsbackport.coppergear.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
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

    /**
     * 1.12.2's {@code ParticleFlame} (motion, shrinking scale, brightening toward full bright), drawn from the block
     * atlas with the copper sprite. It extends {@link Particle}, not {@code ParticleFlame}: that constructor calls
     * {@code setParticleTextureIndex(48)}, which throws on any particle whose {@code getFXLayer()} is not 0, and a
     * subclass's layer override already applies inside the super constructor.
     */
    static final class CopperFlame extends Particle {
        private final float flameScale;

        CopperFlame(World world, double x, double y, double z) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            this.motionX *= 0.009999999776482582D;
            this.motionY *= 0.009999999776482582D;
            this.motionZ *= 0.009999999776482582D;
            this.posX += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F;
            this.posY += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F;
            this.posZ += (this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F;
            this.flameScale = this.particleScale;
            this.particleRed = 1.0F;
            this.particleGreen = 1.0F;
            this.particleBlue = 1.0F;
            this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
            setParticleTexture(flame);
        }

        @Override
        public int getFXLayer() {
            return 1;
        }

        @Override
        public void move(double x, double y, double z) {
            setBoundingBox(getBoundingBox().offset(x, y, z));
            resetPositionToBB();
        }

        @Override
        public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ,
                float rotationYZ, float rotationXY, float rotationXZ) {
            float f = (this.particleAge + partialTicks) / this.particleMaxAge;
            this.particleScale = this.flameScale * (1.0F - f * f * 0.5F);
            super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        }

        @Override
        public int getBrightnessForRender(float partialTicks) {
            float f = MathHelper.clamp((this.particleAge + partialTicks) / this.particleMaxAge, 0.0F, 1.0F);
            int i = super.getBrightnessForRender(partialTicks);
            int block = Math.min((i & 255) + (int) (f * 15.0F * 16.0F), 240);
            return block | (i >> 16 & 255) << 16;
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
            }
            move(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9599999785423279D;
            this.motionY *= 0.9599999785423279D;
            this.motionZ *= 0.9599999785423279D;
            if (this.onGround) {
                this.motionX *= 0.699999988079071D;
                this.motionZ *= 0.699999988079071D;
            }
        }
    }
}
