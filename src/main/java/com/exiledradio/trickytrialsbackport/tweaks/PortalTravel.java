package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.IProjectile;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.21's portal changes for things 1.12.2 keeps out of portals or strands on the far side:
 * <ul>
 *   <li>a mount or vehicle goes through a nether or end portal with its riders (1.12.2's portal blocks skip anything
 *       ridden or riding); the vehicle travels first, as 1.12.2 moves it, then each rider follows to its new spot and
 *       mounts it again, which is where 1.21's {@code Entity.teleportCrossDimension} puts them;</li>
 *   <li>an ender pearl that went through a portal still teleports its thrower, now across dimensions (1.12.2's pearl
 *       drops its thrower when it changes dimension and only teleports a thrower in its own world);</li>
 *   <li>primed TNT goes through nether portals (1.12.2's TNT never runs the portal countdown) and, once through, cannot
 *       break portal blocks;</li>
 *   <li>the cooldowns of 1.21.2 ({@link #cooldown}, patch P5).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class PortalTravel {

    /** Set on TNT that came through a portal (Forge entity data, so it follows the TNT through saving and travel). */
    public static final String PORTAL_TNT = TrickyTrialsBackport.MOD_ID + ":portal_tnt";

    /** Thrown pearl -> its thrower, kept for the pearl's later copies in other dimensions. */
    private static final Map<UUID, UUID> PEARL_OWNERS = new ConcurrentHashMap<UUID, UUID>();

    private static final Field THROWER = Tweaks.field(EntityThrowable.class, "thrower", "field_70192_c");
    private static final Field SEEN_CREDITS = Tweaks.field(EntityPlayerMP.class, "seenCredits", "field_192040_cp");
    private static final Field DIMENSION_CHANGE = Tweaks.field(EntityPlayerMP.class, "invulnerableDimensionChange", "field_184851_cj");

    private PortalTravel() {
    }

    // ------------------------------------------------------------------------------------------
    // P5: cooldowns (1.21.2)
    // ------------------------------------------------------------------------------------------

    /**
     * {@code Entity.getPortalCooldown}'s 300 (patch P5). 1.21.2's {@code getDimensionChangingDelay}: a mount whose
     * first rider is a player waits that player's 10 ticks, boats and minecarts 10, projectiles 2, anything else 300.
     */
    public static int cooldown(Entity entity) {
        if (!Tweaks.on(ModConfig.tweaks.portalCooldowns)) {
            return 300;
        }
        List<Entity> riders = entity.getPassengers();
        if (!riders.isEmpty() && riders.get(0) instanceof EntityPlayer) {
            return riders.get(0).getPortalCooldown();
        }
        if (entity instanceof EntityBoat || entity instanceof EntityMinecart) {
            return 10;
        }
        if (entity instanceof IProjectile || entity instanceof EntityFireball || entity instanceof EntityThrowable
                || entity instanceof EntityArrow) {
            return 2;
        }
        return 300;
    }

    // ------------------------------------------------------------------------------------------
    // Riders and TNT, checked after each world's entities have moved
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Tweaks.server(event.world) || !ModConfig.tweaks.enabled) {
            return;
        }
        boolean riders = ModConfig.tweaks.ridersThroughPortals;
        boolean tnt = ModConfig.tweaks.tntThroughPortals;
        if (!riders && !tnt) {
            return;
        }
        tick(event.world, riders, tnt);
    }

    /** One world's check, also run by the self-test. */
    public static void tick(World world, boolean riders, boolean tnt) {
        for (Entity e : new ArrayList<Entity>(world.loadedEntityList)) {
            if (e.isDead || e.world != world) {
                continue;
            }
            if (riders && e.isBeingRidden() && !e.isRiding() && !(e instanceof EntityPlayer)) {
                vehicle(world, e);
            } else if (tnt && e instanceof EntityTNTPrimed) {
                tnt(world, (EntityTNTPrimed) e);
            }
        }
    }

    private static void vehicle(World world, Entity vehicle) {
        BlockPos portal = touching(world, vehicle, Blocks.PORTAL);
        if (portal != null) {
            if (!world.getMinecraftServer().getAllowNether()) {
                return;
            }
            if (vehicle.timeUntilPortal > 0) {
                vehicle.timeUntilPortal = vehicle.getPortalCooldown();
                return;
            }
            int from = world.provider.getDimension();
            int to = from == -1 ? 0 : -1;
            if (to == -1 && !GameRuleTweaks.netherPortalsAllowed(world)) {
                return;
            }
            // setPortal records where and how the entity entered, which the nether teleporter reads.
            vehicle.setPortal(portal);
            Tweaks.set(Tweaks.IN_PORTAL, vehicle, false);
            vehicle.timeUntilPortal = vehicle.getPortalCooldown();
            carry(vehicle, to, null);
            return;
        }
        BlockPos end = touchingEndPortal(world, vehicle);
        if (end != null) {
            carry(vehicle, 1, null);
        }
    }

    private static void tnt(World world, EntityTNTPrimed tnt) {
        // 1.12.2's TNT never runs Entity.onEntityUpdate, so its cooldown is counted here.
        if (tnt.timeUntilPortal > 0) {
            tnt.timeUntilPortal--;
        }
        BlockPos portal = touching(world, tnt, Blocks.PORTAL);
        if (portal == null || tnt.timeUntilPortal > 0 || !world.getMinecraftServer().getAllowNether()) {
            if (portal != null && tnt.timeUntilPortal > 0) {
                tnt.timeUntilPortal = tnt.getPortalCooldown();
            }
            return;
        }
        int to = world.provider.getDimension() == -1 ? 0 : -1;
        if (to == -1 && !GameRuleTweaks.netherPortalsAllowed(world)) {
            return;
        }
        tnt.setPortal(portal);
        Tweaks.set(Tweaks.IN_PORTAL, tnt, false);
        tnt.timeUntilPortal = tnt.getPortalCooldown();
        tnt.getEntityData().setBoolean(PORTAL_TNT, true);
        tnt.changeDimension(to);
    }

    /** TNT that came through a portal leaves portal blocks standing (1.21's PrimedTnt). */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Entity exploder = Tweaks.exploder(event.getExplosion());
        if (exploder instanceof EntityTNTPrimed && exploder.getEntityData().getBoolean(PORTAL_TNT)
                && Tweaks.on(ModConfig.tweaks.tntThroughPortals)) {
            for (Iterator<BlockPos> it = event.getAffectedBlocks().iterator(); it.hasNext(); ) {
                if (event.getWorld().getBlockState(it.next()).getBlock() == Blocks.PORTAL) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Sends an entity and its riders to a dimension: the entity first, by {@code teleporter} (the world's own when
     * null), then each rider to the entity's new position, mounted again.
     *
     * @return the entity in its new dimension, or null when it did not go
     */
    @Nullable
    public static Entity carry(Entity entity, int dimension, @Nullable ITeleporter teleporter) {
        int from = entity.dimension;
        List<Entity> riders = new ArrayList<Entity>(entity.getPassengers());
        if (from == 1 && dimension == 1) {
            // 1.21's canTeleport: leaving the End, a player rider who has not seen the credits keeps everyone there.
            for (Entity r : entity.getRecursivePassengers()) {
                if (r instanceof EntityPlayerMP && !Boolean.TRUE.equals(Tweaks.get(SEEN_CREDITS, r))) {
                    return null;
                }
            }
        }
        Entity moved = teleporter == null ? entity.changeDimension(dimension) : entity.changeDimension(dimension, teleporter);
        if (moved == null || moved.dimension == from) {
            return null;
        }
        for (Entity rider : riders) {
            if (rider.isDead) {
                continue;
            }
            float yaw = moved.rotationYaw + (rider.rotationYaw - entity.rotationYaw);
            Entity there = carry(rider, moved.dimension, new At(moved.posX, moved.posY, moved.posZ, yaw, rider.rotationPitch));
            if (there != null && !there.isDead) {
                there.startRiding(moved, true);
            }
        }
        return moved;
    }

    @Nullable
    static BlockPos touching(World world, Entity e, Block block) {
        AxisAlignedBB box = e.getEntityBoundingBox().shrink(0.001D);
        int x0 = MathHelper.floor(box.minX), x1 = MathHelper.floor(box.maxX);
        int y0 = MathHelper.floor(box.minY), y1 = MathHelper.floor(box.maxY);
        int z0 = MathHelper.floor(box.minZ), z1 = MathHelper.floor(box.maxZ);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (world.getBlockState(p.setPos(x, y, z)).getBlock() == block) {
                        return p.toImmutable();
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos touchingEndPortal(World world, Entity e) {
        BlockPos at = touching(world, e, Blocks.END_PORTAL);
        if (at == null) {
            return null;
        }
        IBlockState state = world.getBlockState(at);
        // BlockEndPortal's own test: the entity's box meets the portal's 0.75-high box.
        return e.getEntityBoundingBox().intersects(state.getBoundingBox(world, at).offset(at)) ? at : null;
    }

    /** Puts an entity at a fixed spot in its new world: a rider where its mount came out. */
    public static final class At implements ITeleporter {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public At(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public void placeEntity(World world, Entity entity, float yaw) {
            entity.motionX = entity.motionY = entity.motionZ = 0.0D;
            entity.setLocationAndAngles(this.x, this.y, this.z, this.yaw, this.pitch);
        }

        @Override
        public boolean isVanilla() {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Ender pearls across dimensions (1.21)
    // ------------------------------------------------------------------------------------------

    /** Remembers each pearl's thrower, and gives a pearl's copy in another dimension its thrower back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityEnderPearl) || !Tweaks.server(event.getWorld())) {
            return;
        }
        EntityEnderPearl pearl = (EntityEnderPearl) event.getEntity();
        EntityLivingBase thrower = pearl.getThrower();
        if (thrower != null) {
            PEARL_OWNERS.put(pearl.getUniqueID(), thrower.getUniqueID());
            return;
        }
        UUID owner = PEARL_OWNERS.get(pearl.getUniqueID());
        if (owner != null) {
            Entity found = find(owner);
            if (found instanceof EntityLivingBase) {
                Tweaks.set(THROWER, pearl, found);
            }
        }
    }

    /** @return the thrower a pearl was thrown by, in whichever world it now is */
    @Nullable
    public static EntityLivingBase owner(EntityEnderPearl pearl) {
        EntityLivingBase thrower = pearl.getThrower();
        if (thrower != null) {
            return thrower;
        }
        UUID owner = PEARL_OWNERS.get(pearl.getUniqueID());
        Entity found = owner == null ? null : find(owner);
        return found instanceof EntityLivingBase ? (EntityLivingBase) found : null;
    }

    static void rememberOwner(EntityEnderPearl pearl, Entity owner) {
        PEARL_OWNERS.put(pearl.getUniqueID(), owner.getUniqueID());
        Tweaks.set(THROWER, pearl, owner);
    }

    @Nullable
    private static Entity find(UUID id) {
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
        if (player != null) {
            return player;
        }
        for (WorldServer w : DimensionManager.getWorlds()) {
            Entity e = w.getEntityFromUuid(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /**
     * A pearl landing in another dimension than its thrower's: 1.21's {@code ThrownEnderpearl.onHit} sends the thrower
     * there (5 damage for a player, 5% endermite chance), where 1.12.2's pearl only teleports a thrower in its world.
     */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent.Throwable event) {
        if (!(event.getThrowable() instanceof EntityEnderPearl) || !Tweaks.server(event.getThrowable().world)) {
            return;
        }
        EntityEnderPearl pearl = (EntityEnderPearl) event.getThrowable();
        RayTraceResult hit = event.getRayTraceResult();
        if (hit.typeOfHit == RayTraceResult.Type.BLOCK && pearl.world.getBlockState(hit.getBlockPos()).getBlock() == Blocks.END_GATEWAY) {
            return;
        }
        EntityLivingBase owner = owner(pearl);
        if (owner == null || owner.world == pearl.world || !Tweaks.on(ModConfig.tweaks.pearlsAcrossDimensions)) {
            if (hit.entityHit != owner) {
                PEARL_OWNERS.remove(pearl.getUniqueID());
            }
            return;
        }
        event.setCanceled(true);
        if (hit.entityHit != null) {
            hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(pearl, owner), 0.0F);
        }
        PEARL_OWNERS.remove(pearl.getUniqueID());
        pearl.setDead();
        if (!owner.isEntityAlive()) {
            return;
        }
        double x = pearl.posX, y = pearl.posY, z = pearl.posZ;
        At at = new At(x, y, z, owner.rotationYaw, owner.rotationPitch);
        if (owner instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) owner;
            if (!player.connection.getNetworkManager().isChannelOpen()) {
                return;
            }
            EnderTeleportEvent teleport = new EnderTeleportEvent(player, x, y, z, 5.0F);
            if (MinecraftForge.EVENT_BUS.post(teleport)) {
                return;
            }
            if (pearl.world.rand.nextFloat() < 0.05F && pearl.world.getGameRules().getBoolean("doMobSpawning")) {
                EntityEndermite mite = new EntityEndermite(player.world);
                mite.setSpawnedByPlayer(true);
                mite.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
                player.world.spawnEntity(mite);
            }
            if (pearl.timeUntilPortal > 0) {
                player.timeUntilPortal = player.getPortalCooldown();
            }
            if (player.isRiding()) {
                player.dismountRidingEntity();
            }
            player.changeDimension(pearl.dimension, at);
            if (player.dimension == pearl.dimension) {
                player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
                player.fallDistance = 0.0F;
                // 1.21 lets the pearl's own damage through the dimension change's grace (ServerPlayer.isInvulnerableTo).
                Object grace = Tweaks.get(DIMENSION_CHANGE, player);
                Tweaks.set(DIMENSION_CHANGE, player, false);
                player.attackEntityFrom(DamageSource.FALL, teleport.getAttackDamage());
                if (grace != null) {
                    Tweaks.set(DIMENSION_CHANGE, player, grace);
                }
            }
        } else {
            if (owner.isRiding()) {
                owner.dismountRidingEntity();
            }
            Entity moved = owner.changeDimension(pearl.dimension, at);
            if (moved != null) {
                moved.fallDistance = 0.0F;
            }
        }
    }
}
