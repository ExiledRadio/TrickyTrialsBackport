package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Our copy of the ominous bottle, registered only without Village &amp; Pillage (whose bottle it is):
 * drink it for 100 minutes of Bad Omen, I to V by its metadata (0-4), as 1.21's amplifier component.
 */
public final class ItemOminousBottle extends Item {

    public static final int LEVELS = 5;
    private static final int DURATION = 120000;

    public ItemOminousBottle() {
        setMaxStackSize(64);
        setHasSubtypes(true);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (!world.isRemote) {
            world.playSound(null, entity.posX, entity.posY, entity.posZ, TrialContent.sound("item.ominous_bottle.dispose"),
                    entity.getSoundCategory(), 1.0F, 1.0F);
            Potion bad = Omens.badOmen();
            if (bad != null) {
                entity.addPotionEffect(new PotionEffect(bad, DURATION, level(stack), false, false));
                world.playSound(null, entity.posX, entity.posY, entity.posZ, TrialContent.sound("event.mob_effect.bad_omen"),
                        entity.getSoundCategory(), 1.0F, 1.0F);
            }
        }
        if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return stack;
    }

    public static int level(ItemStack stack) {
        return Math.max(0, Math.min(LEVELS - 1, stack.getMetadata()));
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int level = 0; level < LEVELS; level++) {
                items.add(new ItemStack(this, 1, level));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Potion bad = Omens.badOmen();
        if (bad == null) {
            return;
        }
        StringBuilder line = new StringBuilder(I18n.format(bad.getName()));
        int level = level(stack);
        if (level > 0) {
            line.append(' ').append(I18n.format("enchantment.level." + (level + 1)));
        }
        line.append(" (").append(Potion.getPotionDurationString(new PotionEffect(bad, DURATION), 1.0F)).append(')');
        tooltip.add(TextFormatting.BLUE + line.toString());
    }
}
