package com.exiledradio.trickytrialsbackport.spear;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A spear (1.21.11). Two attacks, both in {@link SpearAttacks}:
 *
 * <ul>
 *   <li><b>Jab</b> (attack): every target along the look between 2 and 4.5 blocks, for the spear's attack damage,
 *       with knockback; only at full attack charge. A spear never mines.</li>
 *   <li><b>Charge</b> (use, held): after a short delay, whatever the spear runs into is hit by speed: dismounted while
 *       the wielder is fast enough, knocked back for longer, and damaged for longest by how much faster than the
 *       target the wielder is moving.</li>
 * </ul>
 *
 * <p>Stats are 1.21.11's ({@link SpearTier}): attack damage 1 + the material's bonus, speed 1 / its swing time, the
 * material's durability and enchantability, repaired with the material.
 */
public class ItemSpear extends Item {

    private final SpearTier tier;
    /** What repairs it, read late: an ore name, or an item another mod or stream owns. */
    private final Supplier<Object> repair;

    public ItemSpear(SpearTier tier, Supplier<Object> repair) {
        this.tier = tier;
        this.repair = repair;
        setMaxStackSize(1);
        setMaxDamage(tier.durability);
        setFull3D();
    }

    public SpearTier getTier() {
        return this.tier;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> map = HashMultimap.<String, AttributeModifier>create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", (double) (0.0F + this.tier.damageBonus), 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", this.tier.attackSpeed() - 4.0, 0));
        }
        return map;
    }

    @Override
    public int getItemEnchantability() {
        return this.tier.enchantability;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack with) {
        Object r = this.repair.get();
        if (r instanceof String) {
            for (int id : OreDictionary.getOreIDs(with)) {
                if (r.equals(OreDictionary.getOreName(id))) {
                    return true;
                }
            }
            return false;
        }
        return r instanceof Item && with.getItem() == r;
    }

    /** At the table: 1.21.11's {@code enchantable/melee_weapon} and {@code enchantable/lunge} (see {@link SpearContent#takes}). */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return SpearContent.atTable(enchantment);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        SpearAttacks.useSound(player, this.tier);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase user, int remaining) {
        if (!user.world.isRemote) {
            SpearAttacks.chargeTick(user, stack, remaining);
        }
    }

    /** 1.21.11's {@code Weapon(1)}: a hit wears it by one. */
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean canHarvestBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return this.tier == SpearTier.NETHERITE && SpearContent.fireproofItem();
    }

    @Nullable
    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        return hasCustomEntity(stack) ? new EntityFireproofItem(world, location, stack) : null;
    }
}
