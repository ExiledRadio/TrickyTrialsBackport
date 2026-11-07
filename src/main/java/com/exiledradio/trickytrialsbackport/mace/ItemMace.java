package com.exiledradio.trickytrialsbackport.mace;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.WindBurst;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The mace (1.21): 6 attack damage, 0.6 attack speed, 500 durability, repaired with breeze rods.
 *
 * <h2>Smash attack</h2>
 * A hit after falling more than 1.5 blocks (not while gliding) adds 4 damage per block for the first 3
 * blocks, 2 per block for the next 5 and 1 per block after that, plus Density's 0.5 per block per level
 * ({@link MaceEvents#onHurt}). After the hit ({@link #hitEntity}) the wielder stops falling: no fall damage
 * for that fall, and the smash knocks back every other living thing within 3.5 blocks of the target
 * (harder after 5 blocks). Wind Burst then launches the wielder up.
 */
public final class ItemMace extends Item {

    static final int DURABILITY = 500;
    static final double DAMAGE = 6.0;
    static final double SPEED = 0.6;
    static final float SMASH_FALL = 1.5F;
    static final double SMASH_REACH = 3.5;
    /** Wind Burst's knockback multiplier per level (1.21.11's lookup; beyond III, 1.5 + 0.35 a level). */
    static final float[] WIND_BURST = {1.2F, 1.75F, 2.2F};

    public ItemMace() {
        setMaxStackSize(1);
        setMaxDamage(DURABILITY);
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> map = HashMultimap.<String, AttributeModifier>create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            // A player's base is 1 damage and 4 attacks a second.
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", DAMAGE - 1.0, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", SPEED - 4.0, 0));
        }
        return map;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public int getItemEnchantability() {
        return 15;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        Item rod = Owners.ours("breeze_rod");
        return rod != null && repair.getItem() == rod;
    }

    /** At the enchanting table also Smite, Bane of Arthropods and Fire Aspect, as 1.21's tags allow. */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return MaceEvents.acceptsOnMace(enchantment) || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0.0F) {
            stack.damageItem(2, entity);
        }
        return true;
    }

    /** @return true if this attacker's hit is a smash attack */
    static boolean canSmash(EntityLivingBase attacker) {
        return attacker.fallDistance > SMASH_FALL && !attacker.isElytraFlying();
    }

    /** The smash's bonus damage for a fall, before Density. */
    static float smashBonus(float fall) {
        if (fall <= 3.0F) {
            return 4.0F * fall;
        }
        if (fall <= 8.0F) {
            return 12.0F + 2.0F * (fall - 3.0F);
        }
        return 22.0F + fall - 8.0F;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        if (attacker.world.isRemote || !canSmash(attacker)) {
            return true;
        }
        float fall = attacker.fallDistance;
        World world = attacker.world;

        attacker.motionY = 0.01;
        attacker.fallDistance = 0.0F;
        if (attacker instanceof EntityPlayer) {
            WindBurst.trackImpulse((EntityPlayer) attacker, attacker.posY, false);
        }
        SoundEvent sound = target.onGround ? Content.sound(fall > 5.0F ? "item.mace.smash_ground_heavy" : "item.mace.smash_ground")
                : Content.sound("item.mace.smash_air");
        world.playSound(null, attacker.posX, attacker.posY, attacker.posZ, sound, attacker.getSoundCategory(), 1.0F, 1.0F);
        if (target.onGround) {
            // Vanilla's block-dust burst (event 2001 needs a block: the one the target stands on).
            BlockPos under = new BlockPos(target.posX, target.posY - 0.2, target.posZ);
            world.playEvent(2001, under, net.minecraft.block.Block.getStateId(world.getBlockState(under)));
        }
        knockback(world, attacker, target, fall);

        int windBurst = EnchantmentHelper.getEnchantmentLevel(MaceContent.windBurst, stack);
        if (windBurst > 0) {
            float multiplier = windBurst <= WIND_BURST.length ? WIND_BURST[windBurst - 1] : 1.5F + 0.35F * (windBurst - 1);
            WindBurst.burst(world, null, attacker.posX, attacker.posY, attacker.posZ,
                    new WindBurst.Settings(3.5F, multiplier, true, Content.sound("entity.wind_charge.wind_burst"), false, null));
        }
        if (attacker instanceof EntityPlayerMP) {
            ((EntityPlayerMP) attacker).connection.sendPacket(new SPacketEntityVelocity(attacker));
        }
        return true;
    }

    /** 1.21's smash knockback: everything living within 3.5 blocks of the target but the two of them and the wielder's own. */
    static void knockback(World world, EntityLivingBase attacker, EntityLivingBase target, float fall) {
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, target.getEntityBoundingBox().grow(SMASH_REACH))) {
            if (e == attacker || e == target || e.isOnSameTeam(attacker) || e instanceof EntityArmorStand && ((EntityArmorStand) e).hasMarker()
                    || e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()
                    || e instanceof EntityTameable && ((EntityTameable) e).isTamed() && ((EntityTameable) e).getOwner() == attacker
                    || e.getDistanceSq(target) > SMASH_REACH * SMASH_REACH) {
                continue;
            }
            Vec3d away = new Vec3d(e.posX - target.posX, e.posY - target.posY, e.posZ - target.posZ);
            double resist = e.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
            double power = (SMASH_REACH - away.length()) * 0.7 * (fall > 5.0F ? 2 : 1) * (1.0 - resist);
            if (power <= 0.0) {
                continue;
            }
            Vec3d push = away.normalize().scale(power);
            e.addVelocity(push.x, 0.7, push.z);
            e.velocityChanged = true;
        }
    }

    /** For the anvil: every enchantment a mace takes (see {@link MaceEvents#acceptsOnMace}). */
    static boolean takes(Enchantment e) {
        return MaceEvents.acceptsOnMace(e) || e == Enchantments.UNBREAKING || e == Enchantments.MENDING || e == Enchantments.VANISHING_CURSE;
    }
}
