package com.exiledradio.trickytrialsbackport.bogged;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRangedBow;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The bogged (1.21): a mossy swamp skeleton. 16 health, a bow drawn as a skeleton's but 4.5 seconds between
 * shots (1.21's 50-tick cooldown on Hard, 70 otherwise, after the 20-tick draw), arrows of Poison (5 s).
 * Shears take its two mushrooms, once.
 */
public class EntityBogged extends AbstractSkeleton implements IShearable {

    private static final DataParameter<Boolean> SHEARED = EntityDataManager.createKey(EntityBogged.class, DataSerializers.BOOLEAN);

    public static final ResourceLocation LOOT = Content.id("entities/bogged");

    static final int HARD_COOLDOWN = 50;
    static final int COOLDOWN = 70;
    static final int POISON_TICKS = 100;

    public EntityBogged(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SHEARED, false);
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
    protected EntityArrow getArrow(float distanceFactor) {
        EntityArrow arrow = super.getArrow(distanceFactor);
        if (arrow instanceof EntityTippedArrow) {
            ((EntityTippedArrow) arrow).addEffect(new PotionEffect(MobEffects.POISON, POISON_TICKS));
        }
        return arrow;
    }

    public boolean isSheared() {
        return this.dataManager.get(SHEARED);
    }

    public void setSheared(boolean sheared) {
        this.dataManager.set(SHEARED, sheared);
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return !isSheared() && isEntityAlive();
    }

    /** 1.21's {@code shearing/bogged}: two rolls, each a brown or a red mushroom. */
    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        List<ItemStack> drops = new ArrayList<ItemStack>();
        if (isSheared()) {
            return drops;
        }
        setSheared(true);
        this.world.playSound(null, this.posX, this.posY, this.posZ, Content.sound("entity.bogged.shear"), SoundCategory.HOSTILE, 1.0F, 1.0F);
        for (int i = 0; i < 2; i++) {
            drops.add(new ItemStack(this.rand.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM));
        }
        return drops;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("sheared", isSheared());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setSheared(tag.getBoolean("sheared"));
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Content.sound("entity.bogged.ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return Content.sound("entity.bogged.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Content.sound("entity.bogged.death");
    }

    @Override
    protected SoundEvent getStepSound() {
        return Content.sound("entity.bogged.step");
    }
}
