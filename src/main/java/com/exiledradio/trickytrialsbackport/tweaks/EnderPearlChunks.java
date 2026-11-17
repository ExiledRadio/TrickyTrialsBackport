package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 1.21.2's ender pearls: a pearl thrown by a player keeps the 3x3 chunks around it loaded and ticking, renewing the
 * ticket when it moves into another chunk; a chunk it leaves stays loaded 40 ticks more (1.21.2's ticket timeout),
 * and so do its last chunks after it lands. When its thrower logs out the pearl leaves with them, saved with the
 * player, and comes back when they log in again.
 *
 * <p>1.12.2's chunk tickets are Forge's {@link ForgeChunkManager} ones, one per pearl; Forge keeps an entity ticking in
 * a forced chunk. Tickets are never kept over a restart: pearls in flight are reloaded with their thrower or their
 * chunk.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class EnderPearlChunks {

    /** 1.21.2's {@code TicketType.ENDER_PEARL} timeout. */
    static final int TIMEOUT = 40;
    /** Player data key for the pearls saved with a player who logged out. */
    static final String SAVED = TrickyTrialsBackport.MOD_ID + ":ender_pearls";
    static final String DIMENSION = "ender_pearl_dimension";

    private static final Map<World, Map<EntityEnderPearl, Loader>> LOADERS = new IdentityHashMap<World, Map<EntityEnderPearl, Loader>>();

    private EnderPearlChunks() {
    }

    /** Registers the (unused) callback Forge needs before any ticket; tickets left from a crash are released. */
    public static void init() {
        ForgeChunkManager.setForcedChunkLoadingCallback(TrickyTrialsBackport.instance, new ForgeChunkManager.LoadingCallback() {
            @Override
            public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                for (ForgeChunkManager.Ticket t : tickets) {
                    ForgeChunkManager.releaseTicket(t);
                }
            }
        });
    }

    /** One pearl's ticket: its 3x3 around {@link #center}, and chunks it left, each with the tick it is let go. */
    static final class Loader {
        final ForgeChunkManager.Ticket ticket;
        ChunkPos center;
        final Map<ChunkPos, Long> leaving = new java.util.HashMap<ChunkPos, Long>();
        long releaseAt = -1;

        Loader(ForgeChunkManager.Ticket ticket) {
            this.ticket = ticket;
        }
    }

    /** @return the chunks a pearl keeps loaded in this world, for the self-test */
    public static int forcedFor(World world) {
        Map<EntityEnderPearl, Loader> map = LOADERS.get(world);
        int n = 0;
        if (map != null) {
            for (Loader l : map.values()) {
                n += l.ticket.getChunkList().size();
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Tweaks.server(event.world)) {
            return;
        }
        tick(event.world);
    }

    static void tick(World world) {
        long now = world.getTotalWorldTime();
        Map<EntityEnderPearl, Loader> map = LOADERS.get(world);
        if (Tweaks.on(ModConfig.tweaks.pearlsLoadChunks)) {
            for (Entity e : new ArrayList<Entity>(world.loadedEntityList)) {
                if (e instanceof EntityEnderPearl && !e.isDead) {
                    EntityLivingBase owner = PortalTravel.owner((EntityEnderPearl) e);
                    if (owner instanceof EntityPlayerMP) {
                        if (map == null) {
                            map = new IdentityHashMap<EntityEnderPearl, Loader>();
                            LOADERS.put(world, map);
                        }
                        follow(world, (EntityEnderPearl) e, map, now);
                    }
                }
            }
        }
        if (map == null) {
            return;
        }
        for (Iterator<Map.Entry<EntityEnderPearl, Loader>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<EntityEnderPearl, Loader> entry = it.next();
            Loader l = entry.getValue();
            if ((entry.getKey().isDead || entry.getKey().world != world) && l.releaseAt < 0) {
                l.releaseAt = now + TIMEOUT;
            }
            for (Iterator<Map.Entry<ChunkPos, Long>> c = l.leaving.entrySet().iterator(); c.hasNext(); ) {
                Map.Entry<ChunkPos, Long> leave = c.next();
                if (leave.getValue() <= now) {
                    if (!near(l.center, leave.getKey())) {
                        ForgeChunkManager.unforceChunk(l.ticket, leave.getKey());
                    }
                    c.remove();
                }
            }
            if (l.releaseAt >= 0 && l.releaseAt <= now || !ModConfig.tweaks.enabled || !ModConfig.tweaks.pearlsLoadChunks) {
                ForgeChunkManager.releaseTicket(l.ticket);
                it.remove();
            }
        }
    }

    private static void follow(World world, EntityEnderPearl pearl, Map<EntityEnderPearl, Loader> map, long now) {
        ChunkPos at = new ChunkPos(pearl.chunkCoordX, pearl.chunkCoordZ);
        if (!pearl.addedToChunk) {
            at = new ChunkPos(((int) Math.floor(pearl.posX)) >> 4, ((int) Math.floor(pearl.posZ)) >> 4);
        }
        Loader l = map.get(pearl);
        if (l == null) {
            ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(TrickyTrialsBackport.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                return;
            }
            l = new Loader(ticket);
            map.put(pearl, l);
        }
        if (at.equals(l.center)) {
            return;
        }
        if (l.center != null) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    l.leaving.put(new ChunkPos(l.center.x + dx, l.center.z + dz), now + TIMEOUT);
                }
            }
        }
        l.center = at;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos c = new ChunkPos(at.x + dx, at.z + dz);
                l.leaving.remove(c);
                ForgeChunkManager.forceChunk(l.ticket, c);
            }
        }
    }

    private static boolean near(ChunkPos center, ChunkPos c) {
        return center != null && Math.abs(center.x - c.x) <= 1 && Math.abs(center.z - c.z) <= 1;
    }

    @SubscribeEvent
    public static void onUnload(WorldEvent.Unload event) {
        Map<EntityEnderPearl, Loader> map = LOADERS.remove(event.getWorld());
        if (map != null) {
            for (Loader l : map.values()) {
                ForgeChunkManager.releaseTicket(l.ticket);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Pearls leave and come back with their thrower
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || !Tweaks.on(ModConfig.tweaks.pearlsLoadChunks)) {
            return;
        }
        NBTTagList saved = new NBTTagList();
        for (WorldServer world : DimensionManager.getWorlds()) {
            for (Entity e : new ArrayList<Entity>(world.loadedEntityList)) {
                if (e instanceof EntityEnderPearl && !e.isDead && PortalTravel.owner((EntityEnderPearl) e) == event.player) {
                    NBTTagCompound tag = new NBTTagCompound();
                    if (e.writeToNBTOptional(tag)) {
                        tag.setInteger(DIMENSION, world.provider.getDimension());
                        saved.appendTag(tag);
                    }
                    e.setDead();
                }
            }
        }
        NBTTagCompound data = event.player.getEntityData();
        if (saved.isEmpty()) {
            data.removeTag(SAVED);
        } else {
            data.setTag(SAVED, saved);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        if (!(player instanceof EntityPlayerMP) || !data.hasKey(SAVED, Constants.NBT.TAG_LIST)) {
            return;
        }
        NBTTagList saved = data.getTagList(SAVED, Constants.NBT.TAG_COMPOUND);
        data.removeTag(SAVED);
        for (int i = 0; i < saved.tagCount(); i++) {
            NBTTagCompound tag = saved.getCompoundTagAt(i);
            WorldServer world = DimensionManager.getWorld(tag.getInteger(DIMENSION));
            if (world == null) {
                world = player.getServer().getWorld(tag.getInteger(DIMENSION));
            }
            Entity e = EntityList.createEntityFromNBT(tag, world);
            if (e instanceof EntityEnderPearl) {
                world.getChunk(((int) Math.floor(e.posX)) >> 4, ((int) Math.floor(e.posZ)) >> 4);
                PortalTravel.rememberOwner((EntityEnderPearl) e, player);
                world.spawnEntity(e);
            }
        }
    }
}
