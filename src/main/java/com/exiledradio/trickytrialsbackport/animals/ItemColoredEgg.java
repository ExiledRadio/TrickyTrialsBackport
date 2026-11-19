package com.exiledradio.trickytrialsbackport.animals;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * 1.21.5's blue and brown eggs: 1.12.2's egg (16 to a stack, thrown at 1.5) whose thrown egg hatches chicks of its
 * variant ({@link EntityColoredEgg}).
 */
public final class ItemColoredEgg extends Item {

    public final FarmVariant variant;

    public ItemColoredEgg(FarmVariant variant) {
        this.variant = variant;
        this.maxStackSize = 16;
        this.setCreativeTab(CreativeTabs.MATERIALS);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS,
                0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote) {
            EntityColoredEgg egg = new EntityColoredEgg(world, player, this.variant);
            egg.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 1.5F, 1.0F);
            world.spawnEntity(egg);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
