package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Breezes turn projectiles back, and trial mobs do not fight each other over a breeze's stray charge.
 *
 * <ul>
 *   <li><b>Deflection</b> (1.21's {@code REVERSE}): an arrow, thrown item, fireball or any other projectile
 *       about to hit a breeze is sent back at half its speed, turned 170-190 degrees, with the breeze's
 *       deflect sound. Wind charges are not deflected; they hit.</li>
 *   <li><b>No anger</b> (1.21's {@code no_anger_from_wind_charge}): a breeze, skeleton, stray, bogged, zombie,
 *       husk, spider, cave spider or slime hit by a mob's wind charge does not turn on that mob.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class BreezeEvents {

    private static final String NO_ANGER = TrickyTrialsBackport.MOD_ID + ":noAngerFrom";

    private BreezeEvents() {
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (!BreezeContent.enabled() || hit == null || hit.typeOfHit != RayTraceResult.Type.ENTITY
                || !(hit.entityHit instanceof EntityBreeze) || projectile instanceof EntityAbstractWindCharge
                || projectile.world.isRemote) {
            return;
        }
        event.setCanceled(true);
        reverse(projectile);
        hit.entityHit.world.playSound(null, hit.entityHit.posX, hit.entityHit.posY, hit.entityHit.posZ,
                Content.sound("entity.breeze.deflect"), SoundCategory.HOSTILE, 1.0F, 1.0F);
    }

    /** Back the way it came, at half speed, out of the breeze. */
    static void reverse(Entity projectile) {
        projectile.motionX *= -0.5;
        projectile.motionY *= -0.5;
        projectile.motionZ *= -0.5;
        if (projectile instanceof EntityFireball) {
            EntityFireball fireball = (EntityFireball) projectile;
            fireball.accelerationX *= -1;
            fireball.accelerationY *= -1;
            fireball.accelerationZ *= -1;
        }
        float turn = 170.0F + projectile.world.rand.nextFloat() * 20.0F;
        projectile.rotationYaw += turn;
        projectile.prevRotationYaw += turn;
        projectile.setPosition(projectile.prevPosX, projectile.prevPosY, projectile.prevPosZ);
        projectile.velocityChanged = true;
    }

    /** 1.21's no_anger_from_wind_charge list. */
    static boolean calm(Entity entity) {
        if (entity instanceof EntityBreeze || entity instanceof EntitySkeleton || entity instanceof EntityStray
                || entity instanceof EntityZombie && !(entity instanceof net.minecraft.entity.monster.EntityPigZombie)
                || entity instanceof EntitySpider || entity instanceof EntitySlime) {
            return true;
        }
        // The bogged (stream 4) is found by name.
        net.minecraft.util.ResourceLocation id = net.minecraft.entity.EntityList.getKey(entity);
        return id != null && Content.id("bogged").equals(id);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity direct = event.getSource().getImmediateSource();
        Entity owner = event.getSource().getTrueSource();
        EntityLivingBase victim = event.getEntityLiving();
        if (direct instanceof EntityAbstractWindCharge && owner instanceof EntityLivingBase && !(owner instanceof EntityPlayer)
                && calm(victim)) {
            victim.getEntityData().setInteger(NO_ANGER, owner.getEntityId());
        }
    }

    @SubscribeEvent
    public static void onUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !entity.getEntityData().hasKey(NO_ANGER)) {
            return;
        }
        int id = entity.getEntityData().getInteger(NO_ANGER);
        entity.getEntityData().removeTag(NO_ANGER);
        EntityLivingBase revenge = entity.getRevengeTarget();
        if (revenge != null && revenge.getEntityId() == id) {
            entity.setRevengeTarget(null);
        }
        if (entity instanceof EntityLiving) {
            EntityLivingBase target = ((EntityLiving) entity).getAttackTarget();
            if (target != null && target.getEntityId() == id) {
                ((EntityLiving) entity).setAttackTarget(null);
            }
        }
    }
}
