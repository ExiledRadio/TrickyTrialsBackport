package com.exiledradio.trickytrialsbackport.nautilus;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

/** The self-test's way into the stream's package-private state; compiled only into test builds. */
public final class NautilusTestAccess {

    private NautilusTestAccess() {
    }

    public static void tryToTame(EntityAbstractNautilus n, EntityPlayer p) {
        n.tryToTame(p);
    }

    public static float pendingDash(EntityAbstractNautilus n) {
        return n.playerJumpPendingScale;
    }

    public static void tickRidden(EntityAbstractNautilus n, EntityPlayer p) {
        n.tickRidden(p);
    }

    public static void openRidden(EntityPlayerMP p) {
        NautilusContent.openRidden(p);
    }

    public static EntityPlayer temptingPlayer(EntityAbstractNautilus n) {
        return NautilusAi.temptingPlayer(n);
    }

    public static boolean panicking(EntityAbstractNautilus n) {
        return n.panicking;
    }

    public static UUID angryAt(EntityAbstractNautilus n) {
        return n.angryAt;
    }

    public static long angryUntil(EntityAbstractNautilus n) {
        return n.angryUntil;
    }

    public static EntityLivingBase findTarget(EntityAbstractNautilus n) {
        return NautilusAi.findNearestValidAttackTarget(n);
    }

    public static int chargeCooldown(EntityAbstractNautilus n) {
        return n.chargeCooldown;
    }

    public static void setChargeCooldown(EntityAbstractNautilus n, int ticks) {
        n.chargeCooldown = ticks;
    }

    public static int attackTargetCooldown(EntityAbstractNautilus n) {
        return n.attackTargetCooldown;
    }

    public static void setAttackTargetCooldown(EntityAbstractNautilus n, int ticks) {
        n.attackTargetCooldown = ticks;
    }

    public static boolean randomSwim(EntityAbstractNautilus n, int xz, int y) {
        return NautilusAi.randomSwimmablePos(n, xz, y) != null;
    }

    public static void handleAir(EntityNautilus n, int air) {
        n.handleAirSupply(air);
    }

    public static void checkRestriction(EntityAbstractNautilus n) {
        n.checkRestriction();
    }

    public static void burnUndead(EntityZombieNautilus z) {
        z.burnUndead();
    }
}
