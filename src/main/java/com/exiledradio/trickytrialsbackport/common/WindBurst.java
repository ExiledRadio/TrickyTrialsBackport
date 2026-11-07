package com.exiledradio.trickytrialsbackport.common;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * 1.21's wind burst: the explosion of a wind charge, the breeze's charge, the mace's Wind Burst and the
 * Wind Charged effect. It breaks and damages nothing; it knocks entities back and triggers blocks.
 *
 * <h2>What it does</h2>
 * <ul>
 *   <li><b>Knockback</b> by vanilla explosion's rule: every entity within twice the radius is pushed from
 *       the center, by (1 - distance / (2 radius)) times how much of it the burst can see, times the
 *       burst's knockback; Blast Protection reduces it. Creative players flying and spectators are left
 *       alone.</li>
 *   <li><b>Blocks</b> reached by vanilla explosion's rays (so walls shield what is behind them): wooden
 *       doors, trapdoors and fence gates flip unless powered by redstone; buttons press; levers flip;
 *       chorus flowers and decorated pots break; lit candles go out. Iron doors and trapdoors are not
 *       moved, and neither is anything in {@link #isImmovable}. Blocks a later stream makes openable by
 *       wind (copper doors) join with {@link #openable}.</li>
 *   <li><b>Fall damage</b> (players): after a burst launches a player, only the part of the next fall
 *       that ends below the burst counts. Landing at least 7 blocks away vertically from a wind charge's
 *       burst fires {@link #FALL_AFTER_WIND_CHARGE} (<i>Who Needs Rockets?</i>).</li>
 *   <li><b>Gust particles</b> for every player within 64 blocks (one packet).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class WindBurst {

    /** {@code trickytrialsbackport:fall_after_wind_charge}: landing after a wind charge launched you. */
    public static final HeightTrigger FALL_AFTER_WIND_CHARGE = new HeightTrigger(Content.id("fall_after_wind_charge"));

    /** Blocks that open like wooden doors though their material is metal (copper doors and trapdoors). */
    private static final Set<Block> OPENABLE = Collections.synchronizedSet(new HashSet<Block>());

    /** A player's last launch by a burst, until they land. */
    private static final Map<EntityPlayer, Launch> LAUNCHES = new WeakHashMap<EntityPlayer, Launch>();

    private WindBurst() {
    }

    /** One burst's settings. */
    public static final class Settings {
        final float radius;
        final float knockback;
        final boolean large;
        final SoundEvent sound;
        /** True for a wind charge (player's or breeze's): counts for the advancement. */
        final boolean windCharge;
        @Nullable final Predicate<Entity> ignore;

        public Settings(float radius, float knockback, boolean large, SoundEvent sound, boolean windCharge,
                        @Nullable Predicate<Entity> ignore) {
            this.radius = radius;
            this.knockback = knockback;
            this.large = large;
            this.sound = sound;
            this.windCharge = windCharge;
            this.ignore = ignore;
        }
    }

    /** Registers a metal block that wind opens, like 1.21's copper doors and trapdoors. */
    public static void openable(Block block) {
        OPENABLE.add(block);
    }

    /** Barrier and bedrock, as 1.21's {@code blocks_wind_charge_explosions}: they stop the rays. */
    public static boolean isImmovable(IBlockState state) {
        return state.getBlock() == Blocks.BARRIER || state.getBlock() == Blocks.BEDROCK;
    }

    /**
     * Sets off a burst. Server side only; does nothing on a client.
     *
     * @param source the entity bursting (the charge), never pushed; may be null
     */
    public static void burst(World world, @Nullable Entity source, double x, double y, double z, Settings s) {
        if (world.isRemote) {
            return;
        }
        world.playSound(null, x, y, z, s.sound, SoundCategory.BLOCKS, 1.0F,
                (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        NetworkHandler.channel.sendToAllAround(new GustMessage(x, y, z, s.large),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, 64));

        Explosion explosion = new Explosion(world, source, x, y, z, s.radius, false, false);
        Set<BlockPos> doors = new HashSet<BlockPos>();
        for (BlockPos pos : rayBlocks(world, source, explosion, x, y, z, s.radius)) {
            affect(world, pos, doors);
        }

        float reach = s.radius * 2.0F;
        Vec3d center = new Vec3d(x, y, z);
        AxisAlignedBB box = new AxisAlignedBB(x - reach - 1, y - reach - 1, z - reach - 1, x + reach + 1, y + reach + 1, z + reach + 1);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(source, box)) {
            if (entity.isImmuneToExplosions() || (s.ignore != null && s.ignore.test(entity))) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.isSpectator() || (player.isCreative() && player.capabilities.isFlying)) {
                    continue;
                }
            }
            double d = entity.getDistance(x, y, z) / reach;
            if (d > 1.0) {
                continue;
            }
            double dx = entity.posX - x;
            double dy = entity.posY + entity.getEyeHeight() - y;
            double dz = entity.posZ - z;
            double len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0.0) {
                continue;
            }
            double seen = world.getBlockDensity(center, entity.getEntityBoundingBox());
            double push = (1.0 - d) * seen * s.knockback;
            if (entity instanceof EntityLivingBase) {
                push = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, push);
            }
            entity.motionX += dx / len * push;
            entity.motionY += dy / len * push;
            entity.motionZ += dz / len * push;
            entity.velocityChanged = true;
            if (entity instanceof EntityPlayerMP && push > 0.0) {
                synchronized (LAUNCHES) {
                    LAUNCHES.put((EntityPlayer) entity, new Launch(y, s.windCharge, entity.ticksExisted));
                }
            }
        }
    }

    /** Vanilla explosion's rays: the blocks a burst of this radius reaches, stopped by resistant blocks. */
    static Set<BlockPos> rayBlocks(World world, @Nullable Entity source, Explosion explosion, double x, double y, double z, float radius) {
        Set<BlockPos> out = new LinkedHashSet<BlockPos>();
        Random random = world.rand;
        for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
                for (int l = 0; l < 16; l++) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) {
                        continue;
                    }
                    double dx = j / 15.0F * 2.0F - 1.0F;
                    double dy = k / 15.0F * 2.0F - 1.0F;
                    double dz = l / 15.0F * 2.0F - 1.0F;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float power = radius * (0.7F + random.nextFloat() * 0.6F);
                    double px = x;
                    double py = y;
                    double pz = z;
                    for (; power > 0.0F; power -= 0.22500001F) {
                        BlockPos pos = new BlockPos(px, py, pz);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getMaterial() != Material.AIR) {
                            float resistance = isImmovable(state) ? 3600000.0F
                                    : source != null ? source.getExplosionResistance(explosion, world, pos, state)
                                    : state.getBlock().getExplosionResistance(world, pos, null, explosion);
                            power -= (resistance + 0.3F) * 0.3F;
                            if (power > 0.0F) {
                                out.add(pos);
                            }
                        }
                        px += dx * 0.30000001192092896D;
                        py += dy * 0.30000001192092896D;
                        pz += dz * 0.30000001192092896D;
                    }
                }
            }
        }
        return out;
    }

    /**
     * What a burst does to one block it reaches.
     *
     * @param doors lower halves of doors this burst has already flipped: a door is two blocks, flipped once
     */
    static void affect(World world, BlockPos pos, Set<BlockPos> doors) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        boolean wooden = state.getMaterial() != Material.IRON || OPENABLE.contains(block);
        if (block instanceof BlockDoor) {
            if (!wooden) {
                return;
            }
            BlockPos lower = state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
            if (!doors.add(lower) || world.isBlockPowered(lower) || world.isBlockPowered(lower.up())) {
                return;
            }
            ((BlockDoor) block).toggleDoor(world, lower, !BlockDoor.isOpen(world, lower));
        } else if (block instanceof BlockTrapDoor) {
            if (!wooden || world.isBlockPowered(pos)) {
                return;
            }
            boolean open = !state.getValue(BlockTrapDoor.OPEN);
            world.setBlockState(pos, state.withProperty(BlockTrapDoor.OPEN, open), 2);
            world.playEvent(null, open ? 1007 : 1013, pos, 0);
        } else if (block instanceof BlockFenceGate) {
            if (world.isBlockPowered(pos)) {
                return;
            }
            boolean open = !state.getValue(BlockFenceGate.OPEN);
            world.setBlockState(pos, state.withProperty(BlockFenceGate.OPEN, open), 10);
            world.playEvent(null, open ? 1008 : 1014, pos, 0);
        } else if (block instanceof BlockButton) {
            if (!state.getValue(BlockButton.POWERED)) {
                block.onBlockActivated(world, pos, state, null, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
            }
        } else if (block instanceof BlockLever) {
            block.onBlockActivated(world, pos, state, null, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        } else if (block == Blocks.CHORUS_FLOWER || "trailsandtalesbackport:decorated_pot".equals(String.valueOf(block.getRegistryName()))) {
            world.destroyBlock(pos, true);
        } else if (block.getRegistryName() != null && block.getRegistryName().getPath().endsWith("candle")
                || block.getRegistryName() != null && block.getRegistryName().getPath().endsWith("candle_cake")) {
            for (IProperty<?> property : state.getPropertyKeys()) {
                if ("lit".equals(property.getName()) && property instanceof PropertyBool && state.getValue((PropertyBool) property)) {
                    world.setBlockState(pos, state.withProperty((PropertyBool) property, false), 11);
                    world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Falls after a launch
    // ------------------------------------------------------------------------------------------

    private static final class Launch {
        final double burstY;
        final boolean windCharge;
        final int tick;

        Launch(double burstY, boolean windCharge, int tick) {
            this.burstY = burstY;
            this.windCharge = windCharge;
            this.tick = tick;
        }
    }

    /** @return true while a burst's launch is still in the air (for the tests) */
    public static boolean launched(EntityPlayer player) {
        synchronized (LAUNCHES) {
            return LAUNCHES.containsKey(player);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        Launch launch;
        synchronized (LAUNCHES) {
            launch = LAUNCHES.get(event.getEntityLiving());
        }
        if (launch != null) {
            float below = (float) Math.max(0.0, launch.burstY - event.getEntityLiving().posY);
            event.setDistance(Math.min(event.getDistance(), below));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || !(event.player.world instanceof WorldServer)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Launch launch;
        synchronized (LAUNCHES) {
            launch = LAUNCHES.get(player);
        }
        if (launch == null || player.ticksExisted - launch.tick <= 2) {
            return;
        }
        if (player.onGround) {
            if (launch.windCharge) {
                FALL_AFTER_WIND_CHARGE.trigger(player, Math.abs(player.posY - launch.burstY));
            }
            forget(player);
        } else if (player.isInWater() || player.capabilities.isFlying || player.isElytraFlying() || player.isRiding()) {
            forget(player);
        }
    }

    private static void forget(EntityPlayer player) {
        synchronized (LAUNCHES) {
            LAUNCHES.remove(player);
        }
    }
}
