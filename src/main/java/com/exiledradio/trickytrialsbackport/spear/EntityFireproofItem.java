package com.exiledradio.trickytrialsbackport.spear;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * A dropped netherite spear: 1.21.11's {@code fireResistant()}. It never burns, ignores fire damage and floats up
 * through lava as 1.16's fire-resistant items do (the same rule as Nether Update Backport's netherite drops).
 */
public final class EntityFireproofItem extends EntityItem {

    public EntityFireproofItem(World world) {
        super(world);
        this.isImmuneToFire = true;
    }

    public EntityFireproofItem(World world, Entity original, ItemStack stack) {
        super(world, original.posX, original.posY, original.posZ, stack);
        this.isImmuneToFire = true;
        this.motionX = original.motionX;
        this.motionY = original.motionY;
        this.motionZ = original.motionZ;
        if (original instanceof EntityItem) {
            EntityItem item = (EntityItem) original;
            Integer delay = ObfuscationReflectionHelper.getPrivateValue(EntityItem.class, item, "field_145804_b");
            setPickupDelay(delay != null ? delay : 10);
            setThrower(item.getThrower());
            setOwner(item.getOwner());
        }
    }

    @Override
    public void onUpdate() {
        boolean lava = this.world.isMaterialInBB(getEntityBoundingBox().shrink(0.001), Material.LAVA);
        double mx = this.motionX;
        double my = this.motionY;
        double mz = this.motionZ;
        if (lava) {
            // No gravity in lava: cancel the 0.04 the update is about to take away.
            this.motionY += 0.04;
        }
        super.onUpdate();
        if (lava && !this.isDead) {
            // 1.16's lava movement, then the usual 0.98 drag; this also undoes 1.12.2's random bounce in lava.
            this.motionX = mx * 0.95 * 0.98;
            this.motionY = (my + (my < 0.06 ? 5.0E-4 : 0.0)) * 0.98;
            this.motionZ = mz * 0.95 * 0.98;
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return !source.isFireDamage() && super.attackEntityFrom(source, amount);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound != SoundEvents.ENTITY_GENERIC_BURN) {
            super.playSound(sound, volume, pitch);
        }
    }

    @Override
    public boolean isBurning() {
        return false;
    }
}
