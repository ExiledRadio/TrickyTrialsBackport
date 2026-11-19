package com.exiledradio.trickytrialsbackport.animals;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A thrown blue or brown egg (entity {@code egg}, id 40): 1.21.5's {@code ThrownEgg} carrying its item's chicken
 * variant. One time in eight it hatches a chick of that variant, and one time in 32 of those four.
 */
public final class EntityColoredEgg extends EntityThrowable {

    private static final DataParameter<Byte> VARIANT = EntityDataManager.createKey(EntityColoredEgg.class, DataSerializers.BYTE);

    public EntityColoredEgg(World world) {
        super(world);
    }

    public EntityColoredEgg(World world, EntityLivingBase thrower, FarmVariant variant) {
        super(world, thrower);
        setVariant(variant);
    }

    public EntityColoredEgg(World world, double x, double y, double z, FarmVariant variant) {
        super(world, x, y, z);
        setVariant(variant);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(VARIANT, (byte) FarmVariant.COLD.ordinal());
    }

    public FarmVariant getVariant() {
        return FarmVariant.byOrdinal(this.dataManager.get(VARIANT));
    }

    public void setVariant(FarmVariant variant) {
        this.dataManager.set(VARIANT, (byte) variant.ordinal());
    }

    /** @return this egg's item, for the particles and the renderer; null if the eggs are not registered */
    public Item item() {
        return AnimalsContent.egg(getVariant());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 3 && item() != null) {
            for (int i = 0; i < 8; ++i) {
                this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX, this.posY, this.posZ,
                        ((double) this.rand.nextFloat() - 0.5D) * 0.08D, ((double) this.rand.nextFloat() - 0.5D) * 0.08D,
                        ((double) this.rand.nextFloat() - 0.5D) * 0.08D, Item.getIdFromItem(item()));
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null) {
            result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 0.0F);
        }
        if (!this.world.isRemote) {
            hatch();
            this.world.setEntityState(this, (byte) 3);
            this.setDead();
        }
    }

    /** @return how many chicks hatched (0, 1 or 4) */
    int hatch() {
        if (this.rand.nextInt(8) != 0) {
            return 0;
        }
        int n = this.rand.nextInt(32) == 0 ? 4 : 1;
        for (int i = 0; i < n; ++i) {
            EntityChicken chick = new EntityChicken(this.world);
            chick.setGrowingAge(-24000);
            chick.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F);
            FarmAnimals.setVariant(chick, getVariant());
            this.world.spawnEntity(chick);
        }
        return n;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("variant", getVariant().id.toString());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        FarmVariant v = FarmVariant.byId(tag.getString("variant"));
        if (v != null) {
            setVariant(v);
        }
    }
}
