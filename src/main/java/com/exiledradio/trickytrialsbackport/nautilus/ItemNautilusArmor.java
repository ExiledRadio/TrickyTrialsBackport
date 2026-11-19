package com.exiledradio.trickytrialsbackport.nautilus;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Nautilus armor (1.21.11's {@code Item.Properties.nautilusArmor}): one to a stack, no durability, put on a tamed
 * adult nautilus or zombie nautilus by use, through its inventory, or by a dispenser.
 */
public class ItemNautilusArmor extends Item {

    public final NautilusArmorTier tier;

    public ItemNautilusArmor(NautilusArmorTier tier) {
        this.tier = tier;
        this.setMaxStackSize(1);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, DISPENSE);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.COMMON;
    }

    /** 1.21's {@code EquipmentDispenseItemBehavior}: onto a nautilus in front that can take it, else dropped. */
    static final BehaviorDefaultDispenseItem DISPENSE = new BehaviorDefaultDispenseItem() {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos at = source.getBlockPos().offset(facing);
            List<EntityAbstractNautilus> near = source.getWorld().getEntitiesWithinAABB(EntityAbstractNautilus.class, new AxisAlignedBB(at),
                    n -> n != null && n.isEntityAlive());
            for (EntityAbstractNautilus n : near) {
                if (n.dispenseEquip(stack)) {
                    stack.shrink(1);
                    return stack;
                }
            }
            return super.dispenseStack(source, stack);
        }
    };
}
