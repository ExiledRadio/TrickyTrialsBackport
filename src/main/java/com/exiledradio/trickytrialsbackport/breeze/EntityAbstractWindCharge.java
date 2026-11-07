package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.WindBurst;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A wind charge in flight, the player's or the breeze's: no gravity, no drag in air, one damage to what it
 * hits directly, then a {@link WindBurst}. Hitting it (a melee attack or another projectile) sends it the
 * way the attacker is looking, at its own speed, as 1.21 deflects fireballs; the attacker becomes its owner.
 *
 * <p>It never hits its owner or another wind charge, and bursts once it climbs 30 blocks above the build
 * limit.
 */
public abstract class EntityAbstractWindCharge extends EntityThrowable {

    protected EntityAbstractWindCharge(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    protected EntityAbstractWindCharge(World world, EntityLivingBase owner) {
        super(world, owner);
        setSize(0.3125F, 0.3125F);
    }

    protected EntityAbstractWindCharge(World world, double x, double y, double z) {
        super(world, x, y, z);
        setSize(0.3125F, 0.3125F);
    }

    /** The burst this charge sets off. */
    protected abstract WindBurst.Settings burst();

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }

    @Override
    public void onUpdate() {
        double mx = this.motionX;
        double my = this.motionY;
        double mz = this.motionZ;
        super.onUpdate();
        if (!this.isDead && !isInWater()) {
            // EntityThrowable slows everything by 1% a tick; a wind charge keeps its speed in air.
            this.motionX = mx;
            this.motionY = my;
            this.motionZ = mz;
        }
        if (!this.world.isRemote && !this.isDead && this.posY > this.world.getHeight() + 30) {
            burstHere(this.posX, this.posY, this.posZ);
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (this.world.isRemote || this.isDead) {
            return;
        }
        if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity hit = result.entityHit;
            if (hit == null || hit == getThrower() || hit instanceof EntityAbstractWindCharge) {
                return;
            }
            hit.attackEntityFrom(damageSource(), 1.0F);
            burstHere(this.posX, this.posY, this.posZ);
        } else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            Vec3d at = result.hitVec;
            double x = at.x + result.sideHit.getXOffset() * 0.25;
            double y = at.y + result.sideHit.getYOffset() * 0.25;
            double z = at.z + result.sideHit.getZOffset() * 0.25;
            burstHere(x, y, z);
        }
    }

    /** Bursts at a point and is gone. */
    public void burstHere(double x, double y, double z) {
        WindBurst.burst(this.world, this, x, y, z, burst());
        setDead();
    }

    /** 1.21's {@code wind_charge} damage type: message "mob", a projectile. */
    public DamageSource damageSource() {
        Entity owner = getThrower();
        return new EntityDamageSourceIndirect("mob", this, owner == null ? this : owner).setProjectile();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source) || this.world.isRemote) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (attacker == null) {
            return false;
        }
        markVelocityChanged();
        double speed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        Vec3d look = attacker.getLookVec();
        this.motionX = look.x * speed;
        this.motionY = look.y * speed;
        this.motionZ = look.z * speed;
        if (attacker instanceof EntityLivingBase) {
            this.thrower = (EntityLivingBase) attacker;
            this.ignoreEntity = attacker;
        }
        return true;
    }
}
