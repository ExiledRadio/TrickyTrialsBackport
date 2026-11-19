package com.exiledradio.trickytrialsbackport.ghast;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** The self-test's way into the stream's package-private parts; compiled only with {@code -Pselftest}. */
public final class GhastSelfTestAccess {

    private GhastSelfTestAccess() {
    }

    public static boolean isBabyBrain(EntityHappyGhast g) {
        for (EntityAITasks.EntityAITaskEntry e : g.tasks.taskEntries) {
            if (e.action instanceof GhastAi.Brain) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdultControls(EntityHappyGhast g) {
        boolean goals = false;
        for (EntityAITasks.EntityAITaskEntry e : g.tasks.taskEntries) {
            goals |= e.action instanceof GhastAi.RandomFloatAroundGoal;
        }
        return goals && !isBabyBrain(g) && g.getMoveHelper() instanceof GhastAi.MoveHelper && g.getLookHelper() instanceof GhastAi.LookHelper;
    }

    public static boolean equip(ItemStack stack, EntityHappyGhast g) {
        return ItemHarness.equip(stack, g);
    }

    public static void dispense(World world, BlockPos pos) {
        TileEntityDispenser te = (TileEntityDispenser) world.getTileEntity(pos);
        ItemStack stack = te.getStackInSlot(0);
        ItemStack rest = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(stack.getItem()).dispense(new BlockSourceImpl(world, pos), stack);
        te.setInventorySlotContents(0, rest);
    }

    public static void setStill(EntityHappyGhast g, int ticks) {
        g.setServerStillTimeout(ticks);
    }

    public static void tickRidden(EntityHappyGhast g, EntityPlayer rider) {
        g.tickRidden(rider);
    }

    public static void heal(EntityHappyGhast g) {
        g.continuousHeal();
    }

    public static void restrict(EntityHappyGhast g) {
        g.checkRestriction();
    }

    public static void leashTick(EntityHappyGhast g) {
        g.updateLeashedState();
    }

    public static boolean tempted(EntityHappyGhast g) {
        return GhastAi.temptingPlayer(g) != null;
    }

    /** Whether the adult's float-around and float goals would start now. */
    public static boolean goalsWouldStart(EntityHappyGhast g) {
        return new GhastAi.RandomFloatAroundGoal(g, 16).shouldExecute() || new GhastAi.TemptGoal(g, 1.0D, 7.0D).shouldExecute();
    }

    public static boolean scanAbove(EntityHappyGhast g) {
        return g.scanPlayerAboveGhast();
    }

    public static void setJumping(net.minecraft.entity.EntityLivingBase e, boolean jumping) {
        e.setJumping(jumping);
    }

    public static boolean canReach(EntityHappyGhast g, Vec3d d) {
        return ((GhastAi.MoveHelper) g.getMoveHelper()).canReach(d);
    }

    public static Vec3d flyTo(EntityHappyGhast g) {
        return GhastAi.suitableFlyToPosition(g, 16);
    }

    public static void setFloating(EntityPlayerMP p, boolean floating, int ticks) throws IllegalAccessException {
        GhastContent.setFloating(p.connection, floating, ticks);
    }

    public static int floatingTicks(EntityPlayerMP p) throws IllegalAccessException {
        return GhastContent.floatingTicks(p.connection);
    }
}
