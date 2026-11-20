package com.exiledradio.trickytrialsbackport.spear.client;

import com.exiledradio.trickytrialsbackport.spear.SpearPoseMath;
import com.exiledradio.trickytrialsbackport.spear.SpearTier;

import static com.exiledradio.trickytrialsbackport.spear.SpearPoseMath.*;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * 1.21.11's {@code SpearAnimations} and the easing curves they use ({@code Ease}), on 1.12.2's model parts and GL
 * matrix: the first-person charge and jab, and the third-person arm while charging and jabbing.
 */
final class SpearPoses {

    private SpearPoses() {
    }

    private static void rotateAround(float degrees, float ax, float ay, float az, float x, float y, float z) {
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(degrees, ax, ay, az);
        GlStateManager.translate(-x, -y, -z);
    }

    /** 1.21.11's {@code firstPersonUse}, after its {@code translate(i * 0.56, -0.52, -0.72)}. */
    static void firstPersonUse(float feedbackTicks, float ticks, int i, SpearTier tier) {
        SpearPoseMath.Use u = new SpearPoseMath.Use(tier, ticks);
        GlStateManager.translate(i * (u.raise * 0.15F + u.raiseEnd * -0.05F + u.sway * -0.1F + u.swaySlow * 0.005F),
                u.raise * -0.075F + u.raiseMiddle * 0.075F + u.swayFast * 0.01F,
                u.raiseStart * 0.05F + u.raiseEnd * -0.05F + u.swaySlow * 0.005F);
        rotateAround(-65.0F * inOutBack(u.raise) - 35.0F * u.lower + 100.0F * u.raiseBack + -0.5F * u.swayFast, 1.0F, 0.0F, 0.0F,
                0.0F, 0.1F, 0.0F);
        rotateAround(i * (-90.0F * progress(u.raise, 0.5F, 0.55F) + 90.0F * u.sway + 2.0F * u.swaySlow), 0.0F, -1.0F, 0.0F,
                i * 0.15F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, -hitFeedback(feedbackTicks), 0.0F);
    }

    /** 1.21.11's {@code firstPersonAttack} (the jab), after the hand's usual placement. */
    static void firstPersonAttack(float p, int i) {
        float a = inOutSine(progress(p, 0.0F, 0.05F));
        float b = outBack(progress(p, 0.05F, 0.2F));
        float c = inOutExpo(progress(p, 0.4F, 1.0F));
        GlStateManager.translate(i * 0.1F * (a - b), -0.075F * (a - c), 0.65F * (a - b));
        GlStateManager.rotate(-70.0F * (a - c), 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, -0.25F * (c - b));
    }

    /** 1.21.11's {@code thirdPersonHandUse} on a model part ({@link SpearPoseMath#handUse}). */
    static void thirdPersonHandUse(ModelRenderer arm, ModelRenderer head, boolean right, SpearTier tier, float ticks, boolean gliding) {
        float[] r = SpearPoseMath.handUse(head.rotateAngleX, head.rotateAngleY, arm.rotateAngleZ, right, tier, ticks, gliding);
        arm.rotateAngleX = r[0];
        arm.rotateAngleY = r[1];
        arm.rotateAngleZ = r[2];
    }

    /** 1.21.11's {@code thirdPersonAttackHand}: the jab's thrust, in place of the sword's swing. */
    static void thirdPersonAttackHand(ModelRenderer arm, float p) {
        float a = inOutSine(progress(p, 0.0F, 0.05F));
        float b = inQuad(progress(p, 0.05F, 0.2F));
        float c = inOutExpo(progress(p, 0.4F, 1.0F));
        arm.rotateAngleX += (90.0F * a - 120.0F * b + 30.0F * c) * DEG;
    }
}
