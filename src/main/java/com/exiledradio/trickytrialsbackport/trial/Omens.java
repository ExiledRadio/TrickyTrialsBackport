package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;

/**
 * Bad Omen and Trial Omen.
 *
 * <p>Bad Omen is Village &amp; Pillage's ({@code villagerbackport:bad_omen}) when it is installed: our ominous
 * bottle gives it, and its raids turn it into Raid Omen in a village. Without it, this stream registers
 * its own Bad Omen (same name and levels), which does nothing but feed trials.
 * Either way, a player with Bad Omen whom a trial spawner detects has it turned into Trial Omen.
 */
public final class Omens {

    /** 1.21: 15 minutes of Trial Omen per level of Bad Omen. */
    public static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;

    private Omens() {
    }

    @Nullable
    public static Potion badOmen() {
        return Owners.effect(Owners.VILLAGE_AND_PILLAGE, "bad_omen");
    }

    @Nullable
    public static Potion trialOmen() {
        return TrialContent.trialOmen;
    }

    static boolean hasTrialOmen(EntityPlayer player) {
        return trialOmen() != null && player.isPotionActive(trialOmen());
    }

    static boolean hasBadOmen(EntityPlayer player) {
        Potion bad = badOmen();
        return bad != null && player.isPotionActive(bad);
    }

    /** 1.21's {@code transformBadOmenIntoTrialOmen}. */
    static void transformBadOmenIntoTrialOmen(EntityPlayer player) {
        Potion bad = badOmen();
        PotionEffect effect = bad == null ? null : player.getActivePotionEffect(bad);
        if (effect != null && trialOmen() != null) {
            int duration = TRIAL_OMEN_PER_BAD_OMEN_LEVEL * (effect.getAmplifier() + 1);
            player.removePotionEffect(bad);
            player.addPotionEffect(new PotionEffect(trialOmen(), duration, 0));
            player.world.playSound(null, player.posX, player.posY, player.posZ, TrialContent.sound("event.mob_effect.trial_omen"),
                    player.getSoundCategory(), 1.0F, 1.0F);
        }
    }
}
