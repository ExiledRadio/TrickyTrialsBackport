package com.exiledradio.trickytrialsbackport.nautilus;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;

/**
 * 1.21.11's {@code ZombieNautilus}: an undead nautilus that never breeds or grows up, a little faster (speed 1.1), burns
 * in daylight unless it wears armor, and carries a drowned. Its warm variant, grown over with coral, is the one of warm
 * oceans (Update Aquatic's {@code warm_ocean}).
 */
public class EntityZombieNautilus extends EntityAbstractNautilus {

    public static final ResourceLocation LOOT = Content.id("entities/zombie_nautilus");
    public static final int TEMPERATE = 0;
    public static final int WARM = 1;
    /** 1.21.11's {@code #spawns_coral_variant_zombie_nautilus}, by biome path. */
    public static final String WARM_BIOME = "warm_ocean";

    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityZombieNautilus.class, DataSerializers.VARINT);

    public EntityZombieNautilus(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.1F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(VARIANT, TEMPERATE);
    }

    @Override
    protected void initEntityAI() {
        this.charge = new NautilusAi.Charge(this, NautilusAi.ZOMBIE_SPEED_WHEN_ATTACKING);
        this.tempt = new NautilusAi.Tempt(this, NautilusAi.ZOMBIE_SPEED_WHEN_TEMPTED);
        this.tasks.addTask(0, this.charge);
        this.tasks.addTask(1, new NautilusAi.FollowRiderTarget(this));
        this.tasks.addTask(2, this.tempt);
        this.tasks.addTask(3, new NautilusAi.Swim(this));
    }

    public int getVariant() {
        return this.dataManager.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataManager.set(VARIANT, variant == WARM ? WARM : TEMPERATE);
    }

    /** The variant spawned at a place: warm in a warm ocean, else temperate. */
    public static int variantAt(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        return variantFor(biome.getRegistryName() == null ? null : biome.getRegistryName().getPath());
    }

    /** By a biome's path: {@code warm_ocean} (any mod's) is warm. */
    public static int variantFor(@Nullable String biomePath) {
        return WARM_BIOME.equals(biomePath) ? WARM : TEMPERATE;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setVariant(variantAt(this.world, new BlockPos(this)));
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    public boolean isChild() {
        return false;
    }

    @Override
    public void setGrowingAge(int age) {
        super.setGrowingAge(0);
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return null;
    }

    @Override
    public boolean canMateWith(net.minecraft.entity.passive.EntityAnimal other) {
        return false;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    /** A monster for the spawn caps, as 1.21.11's category. */
    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && this.isNoDespawnRequired()) {
            return false;
        }
        return type == EnumCreatureType.MONSTER;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !this.isMobControlled() && super.canBeLeashedTo(player);
    }

    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote && this.isEntityAlive()) {
            this.burnUndead();
        }
        super.onLivingUpdate();
    }

    /**
     * 1.21.11's {@code Mob.burnUndead} for {@code #burn_in_daylight}, with the body slot as its sun protection: in
     * daylight, bright, open to the sky and dry, it catches fire for 8 seconds unless it wears armor (which has no
     * durability to lose).
     */
    void burnUndead() {
        if (!this.isSunBurnTick()) {
            return;
        }
        if (!this.getBodyArmor().isEmpty()) {
            return;
        }
        this.setFire(8);
    }

    boolean isSunBurnTick() {
        if (!this.world.isDaytime()) {
            return false;
        }
        float brightness = this.getBrightness();
        BlockPos eyes = new BlockPos(this.posX, this.posY + this.getEyeHeight(), this.posZ);
        boolean wet = this.isWet() || this.inWater;
        return brightness > 0.5F && this.rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && !wet && this.world.canSeeSky(eyes);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("variant", this.getVariant() == WARM ? "minecraft:warm" : "minecraft:temperate");
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("variant")) {
            this.setVariant("minecraft:warm".equals(tag.getString("variant")) ? WARM : TEMPERATE);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isUnderWater() ? NautilusContent.ZOMBIE_AMBIENT : NautilusContent.ZOMBIE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isUnderWater() ? NautilusContent.ZOMBIE_HURT : NautilusContent.ZOMBIE_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isUnderWater() ? NautilusContent.ZOMBIE_DEATH : NautilusContent.ZOMBIE_DEATH_LAND;
    }

    @Override
    SoundEvent getDashSound() {
        return this.isUnderWater() ? NautilusContent.ZOMBIE_DASH : NautilusContent.ZOMBIE_DASH_LAND;
    }

    @Override
    SoundEvent getDashReadySound() {
        return this.isUnderWater() ? NautilusContent.ZOMBIE_DASH_READY : NautilusContent.ZOMBIE_DASH_READY_LAND;
    }

    @Override
    void playEatingSound() {
        this.playSound(NautilusContent.ZOMBIE_EAT, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    protected SoundEvent getSwimSound() {
        return NautilusContent.ZOMBIE_SWIM;
    }
}
