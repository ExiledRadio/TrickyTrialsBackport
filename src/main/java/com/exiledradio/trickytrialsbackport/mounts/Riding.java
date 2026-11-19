package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Two riding changes the mounted mobs need.
 *
 * <ul>
 * <li>{@code mounts.floatWhileRidden} (1.21.11): a ridden horse, zombie horse, donkey, mule, camel or camel husk (1.21.11's
 * {@code #can_float_while_ridden}) whose water is more than 0.4 deep at its feet rises by 0.04 a tick after moving in
 * water ({@code LivingEntity.floatInWaterWhileRidden}). It is added at the start of the next tick, before 1.12.2's water
 * push and move, which is the same order. A player's mount moves on the player's client, where this runs too.</li>
 * <li>{@code mounts.mountedReach} (1.20.5 and later): a mounted zombie reaches as far as its mount is wide. 1.21.11's
 * {@code Mob.getAttackBoundingBox} is the rider's box widened to the vehicle's and lowered to its feet, grown by 0.828
 * sideways, and a melee hit lands when it touches the target's box. 1.12.2 measures from the rider's own feet, a block
 * above the ground, so a zombie on a horse could hardly ever hit. Zombies' (and husks' and zombie villagers') attack
 * goal is swapped for one that measures so while riding, and as 1.12.2 does otherwise.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class Riding {

    static final double RISE = 0.04D;
    public static final double FLUID_JUMP_THRESHOLD = 0.4D;
    /** 1.21.11's {@code Mob.DEFAULT_ATTACK_REACH}: sqrt(2.04) - 0.6. */
    static final double ATTACK_REACH = Math.sqrt(2.04D) - 0.6D;

    static final Set<ResourceLocation> FLOATERS = new HashSet<ResourceLocation>(Arrays.asList(
            new ResourceLocation("horse"), new ResourceLocation("zombie_horse"), new ResourceLocation("mule"), new ResourceLocation("donkey"),
            new ResourceLocation("trailsandtalesbackport", "camel"), new ResourceLocation(TrickyTrialsBackport.MOD_ID, "camel_husk")));

    private Riding() {
    }

    static boolean floatOn() {
        return MountsContent.enabled() && ModConfig.mounts.floatWhileRidden;
    }

    static boolean reachOn() {
        return MountsContent.enabled() && ModConfig.mounts.mountedReach;
    }

    // ------------------------------------------------------------------ floating

    @SubscribeEvent
    public static void onUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase mount = event.getEntityLiving();
        if (mount.isBeingRidden() && mount.isInWater() && floatOn() && floats(mount) && waterAboveFeet(mount) > FLUID_JUMP_THRESHOLD) {
            mount.motionY += RISE;
        }
    }

    static boolean floats(Entity e) {
        ResourceLocation id = EntityList.getKey(e);
        return id != null && FLOATERS.contains(id);
    }

    /** How deep the water is above its feet, in its column (1.21's {@code getFluidHeight}, for the middle). */
    static double waterAboveFeet(Entity e) {
        BlockPos feet = new BlockPos(e.posX, e.posY, e.posZ);
        double top = -1.0D;
        for (BlockPos p = feet; p.getY() < feet.getY() + 4; p = p.up()) {
            IBlockState state = e.world.getBlockState(p);
            if (state.getMaterial() != Material.WATER) {
                break;
            }
            boolean waterAbove = e.world.getBlockState(p.up()).getMaterial() == Material.WATER;
            int level = state.getPropertyKeys().contains(BlockLiquid.LEVEL) ? state.getValue(BlockLiquid.LEVEL) : 0;
            top = waterAbove ? p.getY() + 1.0D : p.getY() + 1.0D - BlockLiquid.getLiquidHeightPercent(level);
        }
        return top < 0.0D ? 0.0D : top - e.posY;
    }

    // ------------------------------------------------------------------ mounted reach

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityZombie) || event.getWorld().isRemote || !reachOn()) {
            return;
        }
        EntityZombie zombie = (EntityZombie) event.getEntity();
        EntityAITasks.EntityAITaskEntry found = null;
        for (EntityAITasks.EntityAITaskEntry entry : zombie.tasks.taskEntries) {
            if (entry.action.getClass() == EntityAIZombieAttack.class) {
                found = entry;
            }
        }
        if (found == null) {
            return;
        }
        double speed = ReflectionHelper.<Double, EntityAIAttackMelee>getPrivateValue(EntityAIAttackMelee.class, (EntityAIAttackMelee) found.action,
                "speedTowardsTarget", "field_75440_e");
        boolean memory = ReflectionHelper.<Boolean, EntityAIAttackMelee>getPrivateValue(EntityAIAttackMelee.class, (EntityAIAttackMelee) found.action,
                "longMemory", "field_75437_f");
        zombie.tasks.removeTask(found.action);
        zombie.tasks.addTask(found.priority, new MountedZombieAttack(zombie, speed, memory));
    }

    /** 1.21.11's attack box for a rider; the rider's own box when not riding. */
    static AxisAlignedBB attackBox(Entity attacker) {
        AxisAlignedBB own = attacker.getEntityBoundingBox();
        Entity vehicle = attacker.getRidingEntity();
        AxisAlignedBB box = own;
        if (vehicle != null) {
            AxisAlignedBB v = vehicle.getEntityBoundingBox();
            box = new AxisAlignedBB(Math.min(own.minX, v.minX), v.minY, Math.min(own.minZ, v.minZ), Math.max(own.maxX, v.maxX), own.maxY,
                    Math.max(own.maxZ, v.maxZ));
        }
        return box.grow(ATTACK_REACH, 0.0D, ATTACK_REACH);
    }

    /** A zombie's attack that, while riding, hits whatever touches 1.21.11's attack box. */
    public static final class MountedZombieAttack extends EntityAIZombieAttack {
        private final EntityZombie zombie;

        public MountedZombieAttack(EntityZombie zombie, double speed, boolean longMemory) {
            super(zombie, speed, longMemory);
            this.zombie = zombie;
        }

        @Override
        protected double getAttackReachSqr(EntityLivingBase target) {
            if (this.zombie.getRidingEntity() == null || !reachOn()) {
                return super.getAttackReachSqr(target);
            }
            return attackBox(this.zombie).intersects(target.getEntityBoundingBox()) ? Double.MAX_VALUE : -1.0D;
        }

        /** @return {@link #getAttackReachSqr}, for the self-test */
        public double reachForTest(EntityLivingBase target) {
            return getAttackReachSqr(target);
        }
    }
}
