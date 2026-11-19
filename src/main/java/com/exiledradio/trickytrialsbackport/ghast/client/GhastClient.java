package com.exiledradio.trickytrialsbackport.ghast.client;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.ghast.EntityHappyGhast;
import com.exiledradio.trickytrialsbackport.ghast.GhastContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The happy ghast's client side, from the mod's pre-init on a client only: its renderer, the riding sound
 * (1.21.6's {@code RidingEntitySoundInstance}: looping, everywhere at once, 5 times the ghast's speed up to full,
 * silent under water) and the rider's camera, 8 blocks back in third person instead of 4 (1.21.6's camera distance
 * attribute).
 */
public final class GhastClient {

    private static RidingSound sound;
    private static Entity soundVehicle;

    private GhastClient() {
    }

    public static void init() {
        if (GhastContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityHappyGhast.class, RenderHappyGhast::new);
            MinecraftForge.EVENT_BUS.register(new GhastClient.Events());
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getMinecraft();
            if (event.phase == TickEvent.Phase.START) {
                if (mc.player != null && mc.world != null && !mc.isGamePaused()) {
                    EntityHappyGhast.liftOntoTop(mc.player);
                }
                return;
            }
            EntityPlayerSP player = mc.player;
            Entity vehicle = player == null ? null : player.getRidingEntity();
            if (vehicle instanceof EntityHappyGhast && vehicle != soundVehicle) {
                soundVehicle = vehicle;
                sound = new RidingSound(player, (EntityHappyGhast) vehicle);
                mc.getSoundHandler().playSound(sound);
            } else if (!(vehicle instanceof EntityHappyGhast)) {
                soundVehicle = null;
            }
        }

        @SubscribeEvent
        public void camera(EntityViewRenderEvent.CameraSetup event) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity view = event.getEntity();
            if (!ModConfig.happyGhast.riderCameraDistance || mc.gameSettings.thirdPersonView == 0 || mc.gameSettings.debugCamEnable
                    || !(view.getRidingEntity() instanceof EntityHappyGhast) || ((EntityHappyGhast) view.getRidingEntity()).isChild()) {
                return;
            }
            float pt = (float) event.getRenderPartialTicks();
            double near = clippedDistance(mc, view, pt, 4.0D);
            double far = clippedDistance(mc, view, pt, 8.0D);
            if (far > near) {
                // In the front view vanilla's last turn flips z, so the extra distance goes the other way.
                float back = (float) (far - near);
                GlStateManager.translate(0.0F, 0.0F, mc.gameSettings.thirdPersonView == 2 ? back : -back);
            }
        }
    }

    /** 1.12.2's {@code orientCamera} distance: the camera pulled in before the nearest block on eight rays. */
    static double clippedDistance(Minecraft mc, Entity entity, float partialTicks, double distance) {
        double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
        double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        float yaw = entity.rotationYaw;
        float pitch = entity.rotationPitch;
        if (mc.gameSettings.thirdPersonView == 2) {
            pitch += 180.0F;
        }
        double dx = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F) * distance;
        double dz = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F) * distance;
        double dy = -MathHelper.sin(pitch * 0.017453292F) * distance;
        double d = distance;
        for (int i = 0; i < 8; i++) {
            float fx = ((i & 1) * 2 - 1) * 0.1F;
            float fy = ((i >> 1 & 1) * 2 - 1) * 0.1F;
            float fz = ((i >> 2 & 1) * 2 - 1) * 0.1F;
            RayTraceResult hit = mc.world.rayTraceBlocks(new Vec3d(x + fx, y + fy, z + fz), new Vec3d(x - dx + fx + fz, y - dy + fy, z - dz + fz));
            if (hit != null) {
                double h = hit.hitVec.distanceTo(new Vec3d(x, y, z));
                if (h < d) {
                    d = h;
                }
            }
        }
        return d;
    }

    /** 1.21.6's {@code RidingEntitySoundInstance} for {@code entity.happy_ghast.riding}. */
    static final class RidingSound extends MovingSound {
        private final EntityPlayerSP player;
        private final EntityHappyGhast ghast;

        RidingSound(EntityPlayerSP player, EntityHappyGhast ghast) {
            super(GhastContent.RIDING, ghast.getSoundCategory());
            this.player = player;
            this.ghast = ghast;
            this.attenuationType = ISound.AttenuationType.NONE;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void update() {
            if (this.ghast.isDead || !this.player.isRiding() || this.player.getRidingEntity() != this.ghast) {
                this.donePlaying = true;
            } else if (this.ghast.isSilent() || this.ghast.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)) {
                this.volume = 0.0F;
            } else {
                float speed = MathHelper.sqrt(this.ghast.motionX * this.ghast.motionX + this.ghast.motionY * this.ghast.motionY
                        + this.ghast.motionZ * this.ghast.motionZ);
                this.volume = speed >= 0.01F ? 5.0F * MathHelper.clamp(speed, 0.0F, 1.0F) : 0.0F;
            }
        }
    }
}
