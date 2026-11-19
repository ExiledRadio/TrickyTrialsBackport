package com.exiledradio.trickytrialsbackport.ghast;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * A harness (1.21.6), one item per dye color, stacking to 1: put on an adult happy ghast that has none by using it on
 * the ghast or dispensing it at one (1.21's equippable {@code equip_on_interact}, {@code can_be_sheared}).
 */
public class ItemHarness extends Item {

    private final EnumDyeColor color;

    public ItemHarness(EnumDyeColor color) {
        this.color = color;
        this.setMaxStackSize(1);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, DISPENSE);
    }

    public EnumDyeColor color() {
        return this.color;
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        return equip(stack, target);
    }

    /** 1.21's {@code Equippable.equipOnTarget}: an adult, living happy ghast with no harness takes one off the stack. */
    static boolean equip(ItemStack stack, EntityLivingBase target) {
        if (!(target instanceof EntityHappyGhast)) {
            return false;
        }
        EntityHappyGhast ghast = (EntityHappyGhast) target;
        if (!ghast.canWearHarness() || ghast.isWearingHarness()) {
            return false;
        }
        if (!ghast.world.isRemote) {
            ghast.setHarness(stack.splitStack(1));
        }
        return true;
    }

    /** 1.21's {@code EquipmentDispenseItemBehavior}: onto a happy ghast in front, else dispensed as an item. */
    static final BehaviorDefaultDispenseItem DISPENSE = new BehaviorDefaultDispenseItem() {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos front = source.getBlockPos().offset(facing);
            List<EntityHappyGhast> ghasts = source.getWorld().getEntitiesWithinAABB(EntityHappyGhast.class, new AxisAlignedBB(front),
                    g -> g != null && g.canWearHarness() && !g.isWearingHarness());
            if (!ghasts.isEmpty() && equip(stack, ghasts.get(0))) {
                return stack;
            }
            return super.dispenseStack(source, stack);
        }
    };
}
