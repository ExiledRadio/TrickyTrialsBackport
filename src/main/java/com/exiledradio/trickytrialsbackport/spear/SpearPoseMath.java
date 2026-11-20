package com.exiledradio.trickytrialsbackport.spear;

import net.minecraft.util.math.MathHelper;

/**
 * The spear's pose math (1.21.11's {@code SpearAnimations} and the {@code Ease} curves it uses), with no client types, so
 * the client's model hooks and the server's self-test share it.
 */
public final class SpearPoseMath {

    public static final float DEG = (float) (Math.PI / 180.0);

    private SpearPoseMath() {
    }

    public static float progress(float v, float a, float b) {
        return MathHelper.clamp((v - a) / (b - a), 0.0F, 1.0F);
    }

    // --- Ease -------------------------------------------------------------------------------------

    public static float inQuad(float x) {
        return x * x;
    }

    public static float inOutSine(float x) {
        return -(MathHelper.cos((float) Math.PI * x) - 1.0F) / 2.0F;
    }

    public static float outBack(float x) {
        float m = x - 1.0F;
        return 1.0F + 2.70158F * m * m * m + 1.70158F * m * m;
    }

    public static float outQuart(float x) {
        float m = (1.0F - x) * (1.0F - x);
        return 1.0F - m * m;
    }

    public static float outCubic(float x) {
        float m = 1.0F - x;
        return 1.0F - m * m * m;
    }

    public static float outCirc(float x) {
        return (float) Math.sqrt(1.0F - (x - 1.0F) * (x - 1.0F));
    }

    public static float inCirc(float x) {
        return (float) -Math.sqrt(1.0F - x * x) + 1.0F;
    }

    public static float inOutExpo(float x) {
        if (x < 0.5F) {
            return x == 0.0F ? 0.0F : (float) (Math.pow(2.0, 20.0 * x - 10.0) / 2.0);
        }
        return x == 1.0F ? 1.0F : (float) ((2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0);
    }

    public static float inOutBack(float x) {
        if (x < 0.5F) {
            return 4.0F * x * x * (7.189819F * x - 2.5949094F) / 2.0F;
        }
        float m = 2.0F * x - 2.0F;
        return (m * m * (3.5949094F * m + 2.5949094F) + 2.0F) / 2.0F;
    }

    public static float inOutElastic(float x) {
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
    public static final class Use {
        public final float raise;
        public final float raiseStart;
        public final float raiseMiddle;
        public final float raiseEnd;
        public final float sway;
        public final float lower;
        public final float raiseBack;
        public final float swayIntensity;
        public final float swaySlow;
        public final float swayFast;

        public Use(SpearTier tier, float t) {
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

    public static float hitFeedback(float ticks) {
        return 0.4F * (outQuart(progress(ticks, 1.0F, 3.0F)) - inOutSine(progress(ticks, 3.0F, 10.0F)));
    }

    /**
     * 1.21.11's {@code SpearAnimations.thirdPersonHandUse}: the arm held out along the look ({@code -pi/2 + head pitch
     * + 0.8}, about 44 degrees down from level for a level head), clamped, then raised and swayed as a charge of
     * {@code ticks} wears on (nothing more at 0 ticks: a mob just holding a spear).
     *
     * @return {xRot, yRot, zRot}
     */
    public static float[] handUse(float headX, float headY, float zRot, boolean right, SpearTier tier, float ticks, boolean gliding) {
        int i = right ? 1 : -1;
        float y = -0.1F * i + headY;
        float x = (float) (-Math.PI / 2) + headX + 0.8F;
        float z = zRot;
        if (gliding) {
            x -= 0.9599311F;
        }
        y = DEG * MathHelper.clamp(y / DEG, -60.0F, 60.0F);
        x = DEG * MathHelper.clamp(x / DEG, -120.0F, 30.0F);
        if (ticks > 0.0F) {
            Use u = new Use(tier, ticks);
            y += -i * u.swayFast * DEG * u.swayIntensity;
            z += -i * u.swaySlow * DEG * u.swayIntensity * 0.5F;
            x += DEG * (-40.0F * u.raiseStart + 30.0F * u.raiseMiddle - 20.0F * u.raiseEnd + 20.0F * u.lower
                    + 10.0F * u.raiseBack + 0.6F * u.swaySlow * u.swayIntensity);
        }
        return new float[] {x, y, z};
    }

    /**
     * Which arms of a held spear take 1.21.11's {@code ArmPose.SPEAR}. A mob ({@code HumanoidMobRenderer}): the arm
     * holding a spear, always, idle too; on a zombie-kind model ({@code AbstractZombieRenderer}) also the other arm, and
     * the zombie's arms-out pose is skipped. A player ({@code AvatarRenderer}): only the arm using it in a charge.
     */
    public static boolean spearArm(boolean player, boolean thisArmSpear, boolean thisArmCharging, boolean zombie, boolean otherArmSpear) {
        if (player) {
            return thisArmSpear && thisArmCharging;
        }
        return thisArmSpear || zombie && otherArmSpear;
    }
}
