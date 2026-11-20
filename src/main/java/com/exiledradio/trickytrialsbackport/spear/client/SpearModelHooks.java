package com.exiledradio.trickytrialsbackport.spear.client;

import com.exiledradio.trickytrialsbackport.spear.ItemSpear;
import com.exiledradio.trickytrialsbackport.spear.SpearContent;
import com.exiledradio.trickytrialsbackport.spear.SpearPoseMath;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.model.ModelZombieVillager;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
    /** The entity {@link #pre} was handed, for {@link #post}; see that method. */
    private static ModelBiped entityModel;
    private static Entity entityStash;
    /** The spear arms a zombie-kind model keeps, for {@link #zombieArms}. */
    private static ModelBiped zombieModel;
    private static final float[] zombieArms = new float[6];

    private SpearModelHooks() {
    }

    private static boolean active() {
        return SpearContent.enabled();
    }

    public static void pre(ModelBiped model, Entity entity) {
        entityModel = model;
        entityStash = entity;
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

    /**
     * Takes only the model: Mojang's stack-map frames mark the entity parameter unusable by the method's end, so
     * reading it there is a VerifyError in a real client. The entity comes from {@link #pre}'s stash.
     */
    public static void post(ModelBiped model) {
        Entity entity = entityModel == model ? entityStash : null;
        entityModel = null;
        entityStash = null;
        if (!active() || !(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        boolean player = living instanceof EntityPlayer;
        boolean zombie = model instanceof ModelZombie || model instanceof ModelZombieVillager;
        boolean any = false;
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = living.getHeldItem(hand);
            ItemStack other = living.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
            boolean charging = living.isHandActive() && living.getActiveHand() == hand && stack.getItem() instanceof ItemSpear;
            if (!SpearPoseMath.spearArm(player, stack.getItem() instanceof ItemSpear, charging, zombie, other.getItem() instanceof ItemSpear)) {
                continue;
            }
            ItemStack spear = stack.getItem() instanceof ItemSpear ? stack : other;
            boolean right = side(living, hand) == EnumHandSide.RIGHT;
            SpearPoses.thirdPersonHandUse(right ? model.bipedRightArm : model.bipedLeftArm, model.bipedHead, right,
                    ((ItemSpear) spear.getItem()).getTier(), charging ? living.getItemInUseMaxCount() : 0.0F, living.isElytraFlying());
            any = true;
        }
        if (stashedModel == model) {
            stashedModel = null;
            model.swingProgress = stashedSwing;
            EnumHand hand = living.swingingHand == null ? EnumHand.MAIN_HAND : living.swingingHand;
            ModelRenderer arm = side(living, hand) == EnumHandSide.RIGHT ? model.bipedRightArm : model.bipedLeftArm;
            SpearPoses.thirdPersonAttackHand(arm, stashedSwing);
        }
        if (zombie && any) {
            // 1.21.11's zombies skip their arms-out pose while holding a spear; 1.12.2's set it after this returns.
            zombieModel = model;
            zombieArms[0] = model.bipedRightArm.rotateAngleX;
            zombieArms[1] = model.bipedRightArm.rotateAngleY;
            zombieArms[2] = model.bipedRightArm.rotateAngleZ;
            zombieArms[3] = model.bipedLeftArm.rotateAngleX;
            zombieArms[4] = model.bipedLeftArm.rotateAngleY;
            zombieArms[5] = model.bipedLeftArm.rotateAngleZ;
        }
    }

    /**
     * Patch P21, before every return of {@code ModelZombie.setRotationAngles} and
     * {@code ModelZombieVillager.setRotationAngles}: puts back the spear arms {@link #post} set, over the zombie's
     * arms-out pose. Update Aquatic's drowned model extends {@code ModelZombie}, so it follows, with its swimming after.
     */
    public static void zombieArms(ModelBiped model) {
        if (zombieModel != model) {
            return;
        }
        zombieModel = null;
        model.bipedRightArm.rotateAngleX = zombieArms[0];
        model.bipedRightArm.rotateAngleY = zombieArms[1];
        model.bipedRightArm.rotateAngleZ = zombieArms[2];
        model.bipedLeftArm.rotateAngleX = zombieArms[3];
        model.bipedLeftArm.rotateAngleY = zombieArms[4];
        model.bipedLeftArm.rotateAngleZ = zombieArms[5];
    }

    private static EnumHandSide side(EntityLivingBase e, EnumHand hand) {
        return hand == EnumHand.MAIN_HAND ? e.getPrimaryHand() : e.getPrimaryHand().opposite();
    }
}
