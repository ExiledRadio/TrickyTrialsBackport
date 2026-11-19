package com.exiledradio.trickytrialsbackport.spear.client;

import com.exiledradio.trickytrialsbackport.spear.SpearTier;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * 1.21.11's {@code SpearAnimations} and the easing curves they use ({@code Ease}), on 1.12.2's model parts and GL
 * matrix: the first-person charge and jab, and the third-person arm while charging and jabbing.
 */
final class SpearPoses {

    private static final float DEG = (float) (Math.PI / 180.0);

    private SpearPoses() {
    }

    static float progress(float v, float a, float b) {
        return MathHelper.clamp((v - a) / (b - a), 0.0F, 1.0F);
    }

    // --- Ease -------------------------------------------------------------------------------------

    static float inQuad(float x) {
        return x * x;
    }

    static float inOutSine(float x) {
        return -(MathHelper.cos((float) Math.PI * x) - 1.0F) / 2.0F;
    }

    static float outBack(float x) {
        float m = x - 1.0F;
        return 1.0F + 2.70158F * m * m * m + 1.70158F * m * m;
    }

    static float outQuart(float x) {
        float m = (1.0F - x) * (1.0F - x);
        return 1.0F - m * m;
    }

    static float outCubic(float x) {
        float m = 1.0F - x;
        return 1.0F - m * m * m;
    }

    static float outCirc(float x) {
        return (float) Math.sqrt(1.0F - (x - 1.0F) * (x - 1.0F));
    }

    static float inCirc(float x) {
        return (float) -Math.sqrt(1.0F - x * x) + 1.0F;
    }

    static float inOutExpo(float x) {
        if (x < 0.5F) {
            return x == 0.0F ? 0.0F : (float) (Math.pow(2.0, 20.0 * x - 10.0) / 2.0);
        }
        return x == 1.0F ? 1.0F : (float) ((2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0);
    }

    static float inOutBack(float x) {
        if (x < 0.5F) {
            return 4.0F * x * x * (7.189819F * x - 2.5949094F) / 2.0F;
        }
        float m = 2.0F * x - 2.0F;
        return (m * m * (3.5949094F * m + 2.5949094F) + 2.0F) / 2.0F;
    }

    static float inOutElastic(float x) {
        if (x == 0.0F) {
            return 0.0F;
        }
        if (x == 1.0F) {
            return 1.0F;
        }
        double s = Math.sin((20.0 * x - 11.125) * (float) Math.PI * 4.0F / 9.0F);
        return x < 0.5F ? (float) (-(Math.pow(2.0, 20.0 * x - 10.0) * s) / 2.0) : (float) (Math.pow(2.0, -20.0 * x + 10.0) * s / 2.0 + 1.0);
    }

    // --- The charge's phases ------------------------------------------------------------------------

    /** 1.21.11's {@code SpearAnimations.UseParams}: where a charge of so many ticks is in its raise, sway and lowering. */
    static final class Use {
        final float raise;
        final float raiseStart;
        final float raiseMiddle;
        final float raiseEnd;
        final float sway;
        final float lower;
        final float raiseBack;
        final float swayIntensity;
        final float swaySlow;
        final float swayFast;

        Use(SpearTier tier, float t) {
            int delay = tier.delayTicks;
            int swayStart = tier.dismount.maxDurationTicks + delay - 20;
            int knockEnd = tier.knockback.maxDurationTicks + delay;
            int swayEnd = knockEnd - 40;
            int damageEnd = tier.damage.maxDurationTicks + delay;
            this.raise = progress(t, 0.0F, delay);
            this.raiseStart = progress(this.raise, 0.0F, 0.5F);
            this.raiseMiddle = progress(this.raise, 0.5F, 0.8F);
            this.raiseEnd = progress(this.raise, 0.8F, 1.0F);
            this.sway = progress(t, swayStart, swayEnd);
            this.lower = outCubic(inOutElastic(progress(t - 20.0F, swayEnd, knockEnd)));
            this.raiseBack = progress(t, damageEnd - 5, damageEnd);
            this.swayIntensity = 2.0F * outCirc(this.sway) - 2.0F * inCirc(this.raiseBack);
            this.swaySlow = MathHelper.sin(t * 19.0F * DEG) * this.swayIntensity;
            this.swayFast = MathHelper.sin(t * 30.0F * DEG) * this.swayIntensity;
        }
    }

    static float hitFeedback(float ticks) {
        return 0.4F * (outQuart(progress(ticks, 1.0F, 3.0F)) - inOutSine(progress(ticks, 3.0F, 10.0F)));
    }

    private static void rotateAround(float degrees, float ax, float ay, float az, float x, float y, float z) {
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(degrees, ax, ay, az);
        GlStateManager.translate(-x, -y, -z);
    }

    /** 1.21.11's {@code firstPersonUse}, after its {@code translate(i * 0.56, -0.52, -0.72)}. */
    static void firstPersonUse(float feedbackTicks, float ticks, int i, SpearTier tier) {
        Use u = new Use(tier, ticks);
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

    /** 1.21.11's {@code thirdPersonHandUse}: the arm held out along the look while charging, then its sway. */
    static void thirdPersonHandUse(ModelRenderer arm, ModelRenderer head, boolean right, SpearTier tier, float ticks, boolean gliding) {
        int i = right ? 1 : -1;
        arm.rotateAngleY = -0.1F * i + head.rotateAngleY;
        arm.rotateAngleX = (float) (-Math.PI / 2) + head.rotateAngleX + 0.8F;
        if (gliding) {
            arm.rotateAngleX -= 0.9599311F;
        }
        arm.rotateAngleY = DEG * MathHelper.clamp(arm.rotateAngleY / DEG, -60.0F, 60.0F);
        arm.rotateAngleX = DEG * MathHelper.clamp(arm.rotateAngleX / DEG, -120.0F, 30.0F);
        if (ticks > 0.0F) {
            Use u = new Use(tier, ticks);
            arm.rotateAngleY += -i * u.swayFast * DEG * u.swayIntensity;
            arm.rotateAngleZ += -i * u.swaySlow * DEG * u.swayIntensity * 0.5F;
            arm.rotateAngleX += DEG * (-40.0F * u.raiseStart + 30.0F * u.raiseMiddle - 20.0F * u.raiseEnd + 20.0F * u.lower
                    + 10.0F * u.raiseBack + 0.6F * u.swaySlow * u.swayIntensity);
        }
    }

    /** 1.21.11's {@code thirdPersonAttackHand}: the jab's thrust, in place of the sword's swing. */
    static void thirdPersonAttackHand(ModelRenderer arm, float p) {
        float a = inOutSine(progress(p, 0.0F, 0.05F));
        float b = inQuad(progress(p, 0.05F, 0.2F));
        float c = inOutExpo(progress(p, 0.4F, 1.0F));
        arm.rotateAngleX += (90.0F * a - 120.0F * b + 30.0F * c) * DEG;
    }
}
