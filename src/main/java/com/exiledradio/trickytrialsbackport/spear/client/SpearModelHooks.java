package com.exiledradio.trickytrialsbackport.spear.client;

import com.exiledradio.trickytrialsbackport.spear.ItemSpear;
import com.exiledradio.trickytrialsbackport.spear.SpearContent;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;

/**
 * Patch P21 (client): the spear's third-person arm. {@code ModelBiped.setRotationAngles} calls {@link #pre} at its
 * head and {@link #post} before it returns.
 *
 * <ul>
 *   <li>A jab is 1.21.11's thrust ({@code STAB}), not the sword's swing: {@link #pre} hides the swing from 1.12.2's own
 *       angles, and {@link #post} thrusts the arm instead.</li>
 *   <li>A charge holds the arm out along the look and sways it as the charge wears on ({@code ArmPose.SPEAR}).</li>
 * </ul>
 * Armor models are {@code ModelBiped}s posed from the same entity, so they follow; the player's sleeves copy the arm
 * after this returns. Zombie-like models set their own arms after {@code ModelBiped}'s and keep them.
 */
public final class SpearModelHooks {

    private static ModelBiped stashedModel;
    private static float stashedSwing;

    private SpearModelHooks() {
    }

    private static boolean active() {
        return SpearContent.enabled();
    }

    public static void pre(ModelBiped model, Entity entity) {
        stashedModel = null;
        if (!active() || !(entity instanceof EntityLivingBase) || model.swingProgress <= 0.0F) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        EnumHand hand = living.swingingHand == null ? EnumHand.MAIN_HAND : living.swingingHand;
        if (living.getHeldItem(hand).getItem() instanceof ItemSpear) {
            stashedModel = model;
            stashedSwing = model.swingProgress;
            model.swingProgress = 0.0F;
        }
    }

    public static void post(ModelBiped model, Entity entity) {
        if (!active() || !(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        if (living.isHandActive() && living.getActiveItemStack().getItem() instanceof ItemSpear) {
            ItemStack stack = living.getActiveItemStack();
            boolean right = side(living, living.getActiveHand()) == EnumHandSide.RIGHT;
            SpearPoses.thirdPersonHandUse(right ? model.bipedRightArm : model.bipedLeftArm, model.bipedHead, right,
                    ((ItemSpear) stack.getItem()).getTier(), living.getItemInUseMaxCount(), living.isElytraFlying());
        }
        if (stashedModel == model) {
            stashedModel = null;
            model.swingProgress = stashedSwing;
            EnumHand hand = living.swingingHand == null ? EnumHand.MAIN_HAND : living.swingingHand;
            ModelRenderer arm = side(living, hand) == EnumHandSide.RIGHT ? model.bipedRightArm : model.bipedLeftArm;
            SpearPoses.thirdPersonAttackHand(arm, stashedSwing);
        }
    }

    private static EnumHandSide side(EntityLivingBase e, EnumHand hand) {
        return hand == EnumHand.MAIN_HAND ? e.getPrimaryHand() : e.getPrimaryHand().opposite();
    }
}
