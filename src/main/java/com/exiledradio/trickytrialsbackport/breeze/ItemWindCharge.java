package com.exiledradio.trickytrialsbackport.breeze;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/** The wind charge item: thrown at 1.5 blocks a tick, half a second between throws, as 1.21. */
public final class ItemWindCharge extends Item {

    static final int COOLDOWN_TICKS = 10;
    static final float SPEED = 1.5F;

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(null, player.posX, player.posY, player.posZ, Content.sound("entity.wind_charge.throw"),
                SoundCategory.NEUTRAL, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote) {
            EntityWindCharge charge = new EntityWindCharge(world, player);
            charge.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, SPEED, 1.0F);
            world.spawnEntity(charge);
        }
        player.getCooldownTracker().setCooldown(this, COOLDOWN_TICKS);
        player.addStat(StatList.getObjectUseStats(this));
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
