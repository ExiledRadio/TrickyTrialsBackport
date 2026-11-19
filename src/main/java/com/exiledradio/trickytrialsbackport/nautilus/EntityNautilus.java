package com.exiledradio.trickytrialsbackport.nautilus;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import com.exiledradio.trickytrialsbackport.common.Content;

/**
 * 1.21.11's {@code Nautilus}: 15 health, neutral, tamed with pufferfish, bred with fish, dries out on land (300 air,
 * 2 damage each second once it runs out). A water creature of the oceans.
 */
public class EntityNautilus extends EntityAbstractNautilus {

    public static final ResourceLocation LOOT = Content.id("entities/nautilus");
    public static final int TOTAL_AIR_SUPPLY = 300;
    /** Dry-out damage (1.13's {@code dryOut}; 1.12.2 has no such source). */
    public static final DamageSource DRY_OUT = new DamageSource("dryout");

    public EntityNautilus(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        this.charge = new NautilusAi.Charge(this, NautilusAi.SPEED_WHEN_ATTACKING);
        this.tempt = new NautilusAi.Tempt(this, NautilusAi.SPEED_WHEN_TEMPTED);
        this.tasks.addTask(0, this.charge);
        this.tasks.addTask(1, new NautilusAi.Panic(this));
        this.tasks.addTask(2, new EntityAIMate(this, NautilusAi.SPEED_WHEN_MAKING_LOVE));
        this.tasks.addTask(3, this.tempt);
        this.tasks.addTask(5, new NautilusAi.Swim(this));
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        EntityNautilus baby = new EntityNautilus(this.world);
        if (this.isTamed()) {
            baby.setOwnerId(this.getOwnerId());
            baby.setTamed(true);
            baby.enablePersistence();
        }
        return baby;
    }

    /** 1.21.11's {@code checkNautilusSpawnRules}: 5 to 25 below sea level, water under it and above it. */
    @Override
    public boolean getCanSpawnHere() {
        return checkSpawnRules(this.world, new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ));
    }

    public static boolean checkSpawnRules(World world, BlockPos pos) {
        int sea = world.getSeaLevel();
        return pos.getY() >= sea - 25 && pos.getY() <= sea - 5
                && world.getBlockState(pos.down()).getMaterial() == net.minecraft.block.material.Material.WATER
                && world.getBlockState(pos.up()).getMaterial() == net.minecraft.block.material.Material.WATER;
    }

    /** A water creature for the spawn caps, as 1.21.11's category. */
    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && this.isNoDespawnRequired()) {
            return false;
        }
        return type == EnumCreatureType.WATER_CREATURE;
    }

    /** 1.21.11's {@code handleAirSupply}: out of water it loses air; at -20, 2 damage and air back to 0. */
    @Override
    public void onEntityUpdate() {
        int air = this.getAir();
        super.onEntityUpdate();
        if (!this.isAIDisabled() && !this.world.isRemote) {
            this.handleAirSupply(air);
        }
    }

    void handleAirSupply(int air) {
        if (this.isEntityAlive() && !this.isInWater()) {
            this.setAir(air - 1);
            if (this.getAir() <= -20) {
                this.setAir(0);
                this.attackEntityFrom(DRY_OUT, 2.0F);
            }
        } else {
            this.setAir(TOTAL_AIR_SUPPLY);
        }
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    // Sounds: under water or on land, baby or adult.

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isChild()) {
            return this.isUnderWater() ? NautilusContent.BABY_AMBIENT : NautilusContent.BABY_AMBIENT_LAND;
        }
        return this.isUnderWater() ? NautilusContent.AMBIENT : NautilusContent.AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isChild()) {
            return this.isUnderWater() ? NautilusContent.BABY_HURT : NautilusContent.BABY_HURT_LAND;
        }
        return this.isUnderWater() ? NautilusContent.HURT : NautilusContent.HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (this.isChild()) {
            return this.isUnderWater() ? NautilusContent.BABY_DEATH : NautilusContent.BABY_DEATH_LAND;
        }
        return this.isUnderWater() ? NautilusContent.DEATH : NautilusContent.DEATH_LAND;
    }

    @Override
    SoundEvent getDashSound() {
        return this.isUnderWater() ? NautilusContent.DASH : NautilusContent.DASH_LAND;
    }

    @Override
    SoundEvent getDashReadySound() {
        return this.isUnderWater() ? NautilusContent.DASH_READY : NautilusContent.DASH_READY_LAND;
    }

    @Override
    void playEatingSound() {
        this.playSound(this.isChild() ? NautilusContent.BABY_EAT : NautilusContent.EAT, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    protected SoundEvent getSwimSound() {
        return this.isChild() ? NautilusContent.BABY_SWIM : NautilusContent.SWIM;
    }
}
