package com.exiledradio.trickytrialsbackport.creaking;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.TileCreakingHeart;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The creaking half of 1.21.11's {@code CreakingHeartBlockEntity}, reached by name from stream 12's
 * {@code TileCreakingHeart} (see its javadoc). The heart keeps its state, ticker and comparator; this class keeps
 * the link to its creaking (the creaking's UUID, saved in the heart's {@code Creaking} compound as
 * {@code creaking}), spawns the creaking, sends the trail particles and the hurt sounds, and tears the creaking
 * down.
 *
 * <p>Vanilla's per-heart fields that are not saved (the creaking itself once found, ticks existed, the update
 * ticker, the emitter and its target) live in a {@link WeakHashMap} keyed on the tile object; a reloaded chunk
 * makes a new tile and so starts them again, as vanilla's do.
 */
public final class CreakingHeartHook {

    static final int PLAYER_DETECTION_RANGE = 32;
    static final int DISTANCE_CREAKING_TOO_FAR = 34;
    static final int SPAWN_RANGE_XZ = 16;
    static final int SPAWN_RANGE_Y = 8;
    static final int ATTEMPTS_PER_SPAWN = 5;
    static final int HURT_CALL_TOTAL_TICKS = 100;
    static final int HURT_CALL_INTERVAL = 10;
    static final int HURT_CALL_PARTICLE_TICKS = 50;
    static final int TICKS_GRACE_PERIOD = 30;

    private static final String KEY = "creaking";

    /** Vanilla's transient fields, per tile. */
    static final class State {
        @Nullable
        EntityCreaking creaking;
        @Nullable
        UUID uuid;
        long ticksExisted;
        int ticker;
        int emitter;
        @Nullable
        Vec3d emitterTarget;
        boolean loaded;
    }

    private static final Map<TileEntity, State> STATES = new WeakHashMap<TileEntity, State>();

    private CreakingHeartHook() {
    }

    static State state(TileEntity heart, NBTTagCompound link) {
        State s = STATES.get(heart);
        if (s == null) {
            s = new State();
            STATES.put(heart, s);
        }
        if (!s.loaded) {
            s.loaded = true;
            // loadAdditional: a saved UUID starts the grace period.
            if (link.hasUniqueId(KEY)) {
                s.uuid = link.getUniqueId(KEY);
                s.ticksExisted = 0L;
            }
        }
        return s;
    }

    @Nullable
    static NBTTagCompound linkOf(TileEntity heart) {
        return heart instanceof TileCreakingHeart ? ((TileCreakingHeart) heart).link() : null;
    }

    // ---------------------------------------------------------------------------------------------
    // The hook

    /** Every server tick while the heart is not uprooted. @return whether a creaking is linked */
    public static boolean tick(TileEntity heart, NBTTagCompound link) {
        World world = heart.getWorld();
        if (!(world instanceof WorldServer) || !CreakingContent.enabled()) {
            return false;
        }
        WorldServer server = (WorldServer) world;
        State s = state(heart, link);
        s.ticksExisted++;
        BlockPos pos = heart.getPos();

        if (s.emitter > 0) {
            if (s.emitter > HURT_CALL_PARTICLE_TICKS) {
                emitParticles(server, heart, link, s, 1, true);
                emitParticles(server, heart, link, s, 1, false);
            }
            if (s.emitter % HURT_CALL_INTERVAL == 0 && s.emitterTarget != null) {
                EntityCreaking c = protector(server, link, s);
                if (c != null) {
                    s.emitterTarget = center(c.getEntityBoundingBox());
                }
                Vec3d at = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                float f = 0.2F + 0.8F * (HURT_CALL_TOTAL_TICKS - s.emitter) / (float) HURT_CALL_TOTAL_TICKS;
                Vec3d p = at.subtract(s.emitterTarget).scale(f).add(s.emitterTarget);
                float volume = s.emitter / 2.0F / HURT_CALL_TOTAL_TICKS + 0.5F;
                server.playSound(null, new BlockPos(p), CreakingContent.HEART_HURT, SoundCategory.BLOCKS, volume, 1.0F);
            }
            s.emitter--;
        }

        if (s.ticker-- < 0) {
            s.ticker = server.rand.nextInt(5) + 20;
            IBlockState state = server.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockCreakingHeart)) {
                return linked(s);
            }
            if (!linked(s)) {
                if (state.getValue(BlockCreakingHeart.STATE) == BlockCreakingHeart.State.AWAKE && spawningMonsters(server)
                        && server.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), PLAYER_DETECTION_RANGE, false) != null) {
                    EntityCreaking c = spawnProtector(server, pos);
                    if (c != null) {
                        setCreaking(link, s, c);
                        c.makeSound(CreakingContent.SPAWN);
                        server.playSound(null, pos, CreakingContent.HEART_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                }
            } else {
                EntityCreaking c = protector(server, link, s);
                if (c != null && (!BlockCreakingHeart.active(server) && !c.isNoDespawnRequired()
                        || distanceToCreaking(server, pos, link, s) > DISTANCE_CREAKING_TOO_FAR || c.playerIsStuckInYou())) {
                    removeProtector(heart, link, null);
                }
            }
        }
        return linked(s);
    }

    /** The comparator output: 15 at the heart, falling to 0 at 32 blocks; 0 without a creaking. */
    public static int signal(TileEntity heart, NBTTagCompound link) {
        World world = heart.getWorld();
        if (world == null || world.isRemote) {
            return 0;
        }
        State s = state(heart, link);
        if (linked(s) && protector(world, link, s) != null) {
            double d = MathHelper.clamp(distanceToCreaking(world, heart.getPos(), link, s), 0.0D, 32.0D) / 32.0D;
            return 15 - MathHelper.floor(d * 15.0D);
        }
        return 0;
    }

    /** The heart was broken by a player or blown up: the creaking dies as if killed. */
    public static void removed(TileEntity heart, NBTTagCompound link, @Nullable Entity by) {
        DamageSource source;
        if (by instanceof EntityPlayer) {
            source = DamageSource.causePlayerDamage((EntityPlayer) by);
        } else {
            source = DamageSource.causeExplosionDamage(by instanceof EntityLivingBase ? (EntityLivingBase) by : null);
        }
        removeProtector(heart, link, source);
    }

    // ---------------------------------------------------------------------------------------------
    // Vanilla's helpers

    static boolean linked(State s) {
        return s.creaking != null || s.uuid != null;
    }

    static boolean spawningMonsters(WorldServer world) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && world.getMinecraftServer() != null
                && world.getMinecraftServer().allowSpawnMonsters();
    }

    private static void setCreaking(NBTTagCompound link, State s, EntityCreaking c) {
        s.creaking = c;
        s.uuid = c.getUniqueID();
        link.setUniqueId(KEY, s.uuid);
    }

    private static void clear(NBTTagCompound link, State s) {
        s.creaking = null;
        s.uuid = null;
        link.removeTag(KEY + "Most");
        link.removeTag(KEY + "Least");
    }

    /** {@code getCreakingProtector}. */
    @Nullable
    static EntityCreaking protector(World world, NBTTagCompound link, State s) {
        if (s.creaking != null) {
            if (!s.creaking.isDead) {
                return s.creaking;
            }
            // Removed: look it up again by UUID (it may have moved with its chunk), as vanilla.
            s.uuid = s.creaking.getUniqueID();
            s.creaking = null;
            s.ticksExisted = 0L;
        }
        if (world instanceof WorldServer && s.uuid != null) {
            Entity e = ((WorldServer) world).getEntityFromUuid(s.uuid);
            if (e instanceof EntityCreaking && !e.isDead) {
                s.creaking = (EntityCreaking) e;
                return s.creaking;
            }
            if (s.ticksExisted >= TICKS_GRACE_PERIOD) {
                clear(link, s);
            }
        }
        return null;
    }

    /** The hurt call's ticks left (100 after a player hits the creaking, 0 when quiet); for tests. */
    public static int emitter(TileEntity heart) {
        NBTTagCompound link = linkOf(heart);
        return link == null ? 0 : state(heart, link).emitter;
    }

    /** The linked creaking, if loaded; for tests. */
    @Nullable
    public static EntityCreaking creaking(TileEntity heart) {
        NBTTagCompound link = linkOf(heart);
        return link == null || heart.getWorld() == null ? null : protector(heart.getWorld(), link, state(heart, link));
    }

    /** {@code isProtector}: the creaking's own check, every tick. */
    public static boolean isProtector(TileEntity heart, EntityCreaking creaking) {
        NBTTagCompound link = linkOf(heart);
        if (link == null) {
            return false;
        }
        return protector(heart.getWorld(), link, state(heart, link)) == creaking;
    }

    private static double distanceToCreaking(World world, BlockPos pos, NBTTagCompound link, State s) {
        EntityCreaking c = protector(world, link, s);
        return c == null ? 0.0D : Math.sqrt(c.getDistanceSq(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }

    /** {@code spawnProtector}: {@code SpawnUtil.trySpawnMob} with 5 tries, 16 across, 8 up and down, on top of a collider that is not leaves. */
    @Nullable
    static EntityCreaking spawnProtector(WorldServer world, BlockPos heart) {
        Random rand = world.rand;
        for (int i = 0; i < ATTEMPTS_PER_SPAWN; i++) {
            int dx = MathHelper.getInt(rand, -SPAWN_RANGE_XZ, SPAWN_RANGE_XZ);
            int dz = MathHelper.getInt(rand, -SPAWN_RANGE_XZ, SPAWN_RANGE_XZ);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(heart.getX() + dx, heart.getY() + SPAWN_RANGE_Y, heart.getZ() + dz);
            if (!world.getWorldBorder().contains(at) || !moveToPossibleSpawnPosition(world, SPAWN_RANGE_Y, at)) {
                continue;
            }
            EntityCreaking c = new EntityCreaking(world);
            c.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, rand.nextFloat() * 360.0F, 0.0F);
            // noCollision with the spawn box, then checkSpawnRules (always true for the creaking) and checkSpawnObstruction.
            if (!world.getCollisionBoxes(null, c.getEntityBoundingBox()).isEmpty() || !c.isNotColliding()) {
                continue;
            }
            c.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(c)), null);
            world.spawnEntity(c);
            c.playLivingSound();
            world.setEntityState(c, (byte) 20);
            c.setTransient(heart);
            return c;
        }
        return null;
    }

    /** {@code SpawnUtil.moveToPossibleSpawnPosition} with {@code ON_TOP_OF_COLLIDER_NO_LEAVES}. */
    static boolean moveToPossibleSpawnPosition(World world, int range, BlockPos.MutableBlockPos pos) {
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos(pos);
        IBlockState aboveState = world.getBlockState(above);
        for (int y = range; y >= -range; y--) {
            pos.setPos(pos.getX(), pos.getY() - 1, pos.getZ());
            above.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
            IBlockState state = world.getBlockState(pos);
            if (aboveState.getCollisionBoundingBox(world, above) == null && !state.getMaterial().equals(net.minecraft.block.material.Material.LEAVES)
                    && state.getBlockFaceShape(world, pos, net.minecraft.util.EnumFacing.UP) == net.minecraft.block.state.BlockFaceShape.SOLID) {
                pos.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
                return true;
            }
            aboveState = state;
        }
        return false;
    }

    /** {@code creakingHurt}: a player hurt the creaking; the heart calls for it. */
    static void creakingHurt(TileEntity heart) {
        NBTTagCompound link = linkOf(heart);
        if (link == null || !(heart.getWorld() instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) heart.getWorld();
        State s = state(heart, link);
        EntityCreaking c = protector(world, link, s);
        if (c == null || s.emitter > 0) {
            return;
        }
        emitParticles(world, heart, link, s, 20, false);
        spreadResin(heart);
        s.emitter = HURT_CALL_TOTAL_TICKS;
        s.emitterTarget = center(c.getEntityBoundingBox());
    }

    private static boolean resinLooked;
    @Nullable
    private static Method resinHurt;

    /** The resin half of {@code creakingHurt} is stream 14's, by name. */
    private static void spreadResin(TileEntity heart) {
        if (!resinLooked) {
            resinLooked = true;
            try {
                resinHurt = Class.forName("com.exiledradio.trickytrialsbackport.resin.ResinSpread").getMethod("creakingHurt", TileEntity.class);
            } catch (ClassNotFoundException e) {
                // No resin stream in this build.
            } catch (ReflectiveOperationException | RuntimeException e) {
                TrickyTrialsBackport.LOGGER.warn("The resin stream's ResinSpread is not usable; hurt creakings spread no resin.", e);
            }
        }
        if (resinHurt != null) {
            try {
                resinHurt.invoke(null, heart);
            } catch (ReflectiveOperationException | RuntimeException e) {
                TrickyTrialsBackport.LOGGER.warn("Spreading resin failed at {}.", heart.getPos(), e);
            }
        }
    }

    /** {@code emitParticles}: trails between the creaking's box and the heart's block (orange to it, gray from it). */
    static void emitParticles(WorldServer world, TileEntity heart, NBTTagCompound link, State s, int count, boolean toCreaking) {
        EntityCreaking c = protector(world, link, s);
        if (c == null) {
            return;
        }
        int color = toCreaking ? EntityCreaking.CREAKING_ORANGE : EntityCreaking.CREAKING_GRAY;
        Random rand = world.rand;
        BlockPos pos = heart.getPos();
        List<double[]> trails = new ArrayList<double[]>(count);
        for (int i = 0; i < count; i++) {
            AxisAlignedBB bb = c.getEntityBoundingBox();
            Vec3d from = new Vec3d(bb.minX + rand.nextDouble() * (bb.maxX - bb.minX), bb.minY + rand.nextDouble() * (bb.maxY - bb.minY),
                    bb.minZ + rand.nextDouble() * (bb.maxZ - bb.minZ));
            Vec3d to = new Vec3d(pos.getX() + rand.nextDouble(), pos.getY() + rand.nextDouble(), pos.getZ() + rand.nextDouble());
            if (toCreaking) {
                Vec3d t = from;
                from = to;
                to = t;
            }
            trails.add(new double[]{from.x, from.y, from.z, to.x, to.y, to.z, rand.nextInt(40) + 10});
        }
        CreakingFxMessage.trails(world, pos, color, trails);
    }

    /** {@code removeProtector}: null tears the creaking down; a damage source kills it with the twitching death. */
    static void removeProtector(TileEntity heart, NBTTagCompound link, @Nullable DamageSource source) {
        if (heart.getWorld() == null || heart.getWorld().isRemote) {
            return;
        }
        State s = state(heart, link);
        EntityCreaking c = protector(heart.getWorld(), link, s);
        if (c != null) {
            if (source == null) {
                c.tearDown();
            } else {
                c.creakingDeathEffects(source);
                c.setTearingDown();
                c.setHealth(0.0F);
            }
            clear(link, s);
            heart.markDirty();
        }
    }

    static Vec3d center(AxisAlignedBB bb) {
        return new Vec3d((bb.minX + bb.maxX) / 2.0D, (bb.minY + bb.maxY) / 2.0D, (bb.minZ + bb.maxZ) / 2.0D);
    }
}
