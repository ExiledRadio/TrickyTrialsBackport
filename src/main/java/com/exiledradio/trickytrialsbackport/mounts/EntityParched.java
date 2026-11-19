package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRangedBow;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;

/**
 * The parched (1.21.11's {@code Parched}): a desert skeleton. 16 health, a bow drawn as a skeleton's with 1.21.11's
 * slower shots (50 ticks between them on Hard, 70 otherwise, as the bogged), arrows of Weakness (30 s). It does not burn
 * in daylight (it is not in 1.21.11's {@code #burn_in_daylight}) and Weakness never takes hold of it.
 */
public class EntityParched extends AbstractSkeleton {

    public static final ResourceLocation LOOT = Content.id("entities/parched");

    static final int HARD_COOLDOWN = 50;
    static final int COOLDOWN = 70;
    static final int WEAKNESS_TICKS = 600;

    /**
     * True from the start of {@link #onLivingUpdate} until 1.12.2's daylight check in {@code AbstractSkeleton} is past
     * (the next call it makes is {@link #updateArmSwingProgress}), so that check cannot set it on fire. Fire from
     * anything else (lava, fire blocks, Fire Aspect) comes later in the tick or from elsewhere and still burns it.
     */
    private boolean inDaylightCheck;

    public EntityParched(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
    }

    @Override
    public void setCombatTask() {
        super.setCombatTask();
        if (this.world == null || this.world.isRemote) {
            return;
        }
        EntityAIAttackRangedBow<?> bow = ReflectionHelper.getPrivateValue(AbstractSkeleton.class, this, "aiArrowAttack", "field_85037_d");
        if (bow != null) {
            bow.setAttackCooldown(this.world.getDifficulty() == EnumDifficulty.HARD ? HARD_COOLDOWN : COOLDOWN);
        }
    }

    @Override
    public void onLivingUpdate() {
        this.inDaylightCheck = true;
        try {
            super.onLivingUpdate();
        } finally {
            this.inDaylightCheck = false;
        }
    }

    @Override
    protected void updateArmSwingProgress() {
        this.inDaylightCheck = false;
        super.updateArmSwingProgress();
    }

    @Override
    public void setFire(int seconds) {
        if (!this.inDaylightCheck) {
            super.setFire(seconds);
        }
    }

    /** @return true while the daylight check runs; for the self-test */
    boolean inDaylightCheck() {
        return this.inDaylightCheck;
    }

    @Override
    protected EntityArrow getArrow(float distanceFactor) {
        EntityArrow arrow = super.getArrow(distanceFactor);
        if (arrow instanceof EntityTippedArrow) {
            ((EntityTippedArrow) arrow).addEffect(new PotionEffect(MobEffects.WEAKNESS, WEAKNESS_TICKS));
        }
        return arrow;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return effect.getPotion() != MobEffects.WEAKNESS && super.isPotionApplicable(effect);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Content.sound("entity.parched.ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return Content.sound("entity.parched.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Content.sound("entity.parched.death");
    }

    @Override
    protected SoundEvent getStepSound() {
        return Content.sound("entity.parched.step");
    }
}
