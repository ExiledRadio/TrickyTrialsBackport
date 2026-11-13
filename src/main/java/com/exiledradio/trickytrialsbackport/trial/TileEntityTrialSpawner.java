package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The trial spawner: 1.21's {@code TrialSpawner}, {@code TrialSpawnerStateData} and the state machine of
 * {@code TrialSpawnerState}, with the same numbers and the same NBT names.
 *
 * <h2>How a trial runs</h2>
 * <ul>
 *   <li>Waiting for players, it looks every second for players in survival or adventure within
 *       {@code required_player_range} (14) blocks that it can see; finding one, it becomes active.</li>
 *   <li>Active, it spawns {@code total_mobs + total_mobs_added_per_player x (players - 1)} mobs, at most
 *       {@code simultaneous_mobs + ...} alive at once, one every {@code ticks_between_spawn}; a mob dead,
 *       unloaded or more than 47 blocks away no longer counts. Each player it sees joins the trial.</li>
 *   <li>With every mob of the wave dead it waits two seconds, opens, and ejects one loot table's roll per
 *       player who took part, every 1.5 seconds, then cools down for {@code target_cooldown_length}
 *       (30 minutes).</li>
 *   <li>A player with Bad Omen (theirs turned into Trial Omen, 15 minutes per level) or Trial Omen makes
 *       it ominous: its mobs vanish, the ominous config takes over, and while active it drops ominous item
 *       spawners above players and mobs every 8 seconds. After the cooldown it turns normal again.</li>
 * </ul>
 *
 * <h2>Configs</h2>
 * {@code normal_config} and {@code ominous_config} are the names of configs in vanilla's
 * {@code trial_spawner/} data ({@link SpawnerConfig#named}), or configs written inline. Stream 10's trial
 * chambers name one pool's pair, as vanilla's structures do; a player-placed spawner has the empty
 * default, and a spawn egg sets its mob.
 */
public class TileEntityTrialSpawner extends TileEntity implements ITickable {

    public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
    private static final int MAX_MOB_TRACKING_DISTANCE = 47;
    private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
    private static final int TIME_BETWEEN_EACH_EJECTION = 30;

    // Full config.
    private SpawnerConfig normal = SpawnerConfig.DEFAULT;
    private SpawnerConfig ominous = SpawnerConfig.DEFAULT;
    private int targetCooldownLength = 36000;
    private int requiredPlayerRange = 14;

    // State data.
    final Set<UUID> detectedPlayers = new HashSet<UUID>();
    final Set<UUID> currentMobs = new HashSet<UUID>();
    long cooldownEndsAt;
    long nextMobSpawnsAt;
    int totalMobsSpawned;
    @Nullable
    SpawnData nextSpawnData;
    @Nullable
    String ejectingLootTable;
    @Nullable
    private Entity displayEntity;
    @Nullable
    private Weighted<ItemStack> dispensing;
    public double spin;
    public double oSpin;

    private boolean isOminous;
    private boolean overridePeacefulAndMobSpawnRule;
    @Nullable
    private List<? extends EntityPlayer> onlyPlayers;

    // ------------------------------------------------------------------------------------------
    // Config
    // ------------------------------------------------------------------------------------------

    public SpawnerConfig activeConfig() {
        return this.isOminous ? this.ominous : this.normal;
    }

    public SpawnerConfig normalConfig() {
        return this.normal;
    }

    public SpawnerConfig ominousConfig() {
        return this.ominous;
    }

    /**
     * Sets the spawner's configs by name, as a trial chamber does: for example
     * {@code minecraft:trial_chamber/melee/zombie/normal} and {@code .../ominous}.
     *
     * @return false when either name has no config
     */
    public boolean configure(String normalConfig, String ominousConfig) {
        SpawnerConfig n = SpawnerConfig.named(normalConfig);
        SpawnerConfig o = SpawnerConfig.named(ominousConfig);
        if (n == null || o == null) {
            return false;
        }
        this.normal = n;
        this.ominous = o;
        resetData();
        markUpdated();
        return true;
    }

    public void setTargetCooldownLength(int ticks) {
        this.targetCooldownLength = Math.max(0, ticks);
        markDirty();
    }

    public int getTargetCooldownLength() {
        return this.targetCooldownLength;
    }

    public int getRequiredPlayerRange() {
        return this.requiredPlayerRange;
    }

    public boolean isOminous() {
        return this.isOminous;
    }

    /** For tests, as vanilla's own: spawn in peaceful and without the mob-spawning rule. */
    public void overridePeacefulAndMobSpawnRule() {
        this.overridePeacefulAndMobSpawnRule = true;
    }

    /** For tests, as vanilla's {@code EntitySelector.onlySelectPlayers}: only these count as players. */
    public void onlySelectPlayers(@Nullable List<? extends EntityPlayer> players) {
        this.onlyPlayers = players;
    }

    // ------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------

    public TrialSpawnerPhase getPhase() {
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getBlock() instanceof BlockTrialSpawner ? state.getValue(BlockTrialSpawner.STATE) : TrialSpawnerPhase.INACTIVE;
    }

    private void setPhase(TrialSpawnerPhase phase) {
        markDirty();
        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockTrialSpawner) {
            this.world.setBlockState(this.pos, state.withProperty(BlockTrialSpawner.STATE, phase), 3);
        }
    }

    void markUpdated() {
        markDirty();
        if (this.world != null) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    public Set<UUID> detectedPlayers() {
        return this.detectedPlayers;
    }

    public Set<UUID> currentMobs() {
        return this.currentMobs;
    }

    public long nextMobSpawnsAt() {
        return this.nextMobSpawnsAt;
    }

    public long cooldownEndsAt() {
        return this.cooldownEndsAt;
    }

    public int totalMobsSpawned() {
        return this.totalMobsSpawned;
    }

    private void applyOminous() {
        IBlockState state = this.world.getBlockState(this.pos);
        this.world.setBlockState(this.pos, state.withProperty(BlockTrialSpawner.OMINOUS, true), 3);
        TrialMessage.send(this.world, this.pos, TrialMessage.BECOME_OMINOUS, 1);
        this.isOminous = true;
        resetAfterBecomingOminous();
    }

    private void removeOminous() {
        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof BlockTrialSpawner) {
            this.world.setBlockState(this.pos, state.withProperty(BlockTrialSpawner.OMINOUS, false), 3);
        }
        this.isOminous = false;
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world == null) {
            return;
        }
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockTrialSpawner)) {
            return;
        }
        boolean ominous = state.getValue(BlockTrialSpawner.OMINOUS);
        if (this.world.isRemote) {
            com.exiledradio.trickytrialsbackport.trial.client.TrialFx.tickSpawner(this, ominous);
        } else {
            tickServer((WorldServer) this.world, ominous);
        }
    }

    private void tickServer(WorldServer world, boolean ominous) {
        this.isOminous = ominous;
        TrialSpawnerPhase phase = getPhase();
        boolean removed = false;
        for (Iterator<UUID> it = this.currentMobs.iterator(); it.hasNext(); ) {
            if (shouldMobBeUntracked(world, it.next())) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            this.nextMobSpawnsAt = world.getTotalWorldTime() + activeConfig().ticksBetweenSpawn;
        }
        TrialSpawnerPhase next = tickAndGetNext(world, phase);
        if (next != phase) {
            setPhase(next);
        }
    }

    private boolean shouldMobBeUntracked(WorldServer world, UUID id) {
        Entity e = world.getEntityFromUuid(id);
        return e == null || !e.isEntityAlive() || e.world != world
                || e.getPosition().distanceSq(this.pos) > MAX_MOB_TRACKING_DISTANCE * MAX_MOB_TRACKING_DISTANCE;
    }

    private TrialSpawnerPhase tickAndGetNext(WorldServer world, TrialSpawnerPhase phase) {
        SpawnerConfig config = activeConfig();
        long time = world.getTotalWorldTime();
        switch (phase) {
            case INACTIVE:
                return getOrCreateDisplayEntity(TrialSpawnerPhase.WAITING_FOR_PLAYERS) == null ? phase : TrialSpawnerPhase.WAITING_FOR_PLAYERS;
            case WAITING_FOR_PLAYERS:
                if (!canSpawnInLevel(world)) {
                    resetStatistics();
                    return phase;
                }
                if (!hasMobToSpawn(world.rand)) {
                    return TrialSpawnerPhase.INACTIVE;
                }
                tryDetectPlayers(world);
                return this.detectedPlayers.isEmpty() ? phase : TrialSpawnerPhase.ACTIVE;
            case ACTIVE: {
                if (!canSpawnInLevel(world)) {
                    resetStatistics();
                    return TrialSpawnerPhase.WAITING_FOR_PLAYERS;
                }
                if (!hasMobToSpawn(world.rand)) {
                    return TrialSpawnerPhase.INACTIVE;
                }
                int additional = Math.max(0, this.detectedPlayers.size() - 1);
                tryDetectPlayers(world);
                if (this.isOminous) {
                    spawnOminousItemSpawner(world);
                }
                if (this.totalMobsSpawned >= config.targetTotalMobs(additional)) {
                    if (this.currentMobs.isEmpty()) {
                        this.cooldownEndsAt = time + this.targetCooldownLength;
                        this.totalMobsSpawned = 0;
                        this.nextMobSpawnsAt = 0L;
                        return TrialSpawnerPhase.WAITING_FOR_REWARD_EJECTION;
                    }
                } else if (time >= this.nextMobSpawnsAt && this.currentMobs.size() < config.targetSimultaneousMobs(additional)) {
                    UUID spawned = spawnMob(world);
                    if (spawned != null) {
                        this.currentMobs.add(spawned);
                        this.totalMobsSpawned++;
                        this.nextMobSpawnsAt = time + config.ticksBetweenSpawn;
                        SpawnData next = config.spawnPotentials.pick(world.rand);
                        if (next != null) {
                            this.nextSpawnData = next;
                            markUpdated();
                        }
                    }
                }
                return phase;
            }
            case WAITING_FOR_REWARD_EJECTION:
                if (time >= this.cooldownEndsAt - this.targetCooldownLength + DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB) {
                    world.playSound(null, this.pos, TrialContent.sound("block.trial_spawner.open_shutter"), SoundCategory.BLOCKS, 1.0F, 1.0F);
                    return TrialSpawnerPhase.EJECTING_REWARD;
                }
                return phase;
            case EJECTING_REWARD:
                if ((float) (time - (this.cooldownEndsAt - this.targetCooldownLength)) % TIME_BETWEEN_EACH_EJECTION != 0.0F) {
                    return phase;
                }
                if (this.detectedPlayers.isEmpty()) {
                    world.playSound(null, this.pos, TrialContent.sound("block.trial_spawner.close_shutter"), SoundCategory.BLOCKS, 1.0F, 1.0F);
                    this.ejectingLootTable = null;
                    return TrialSpawnerPhase.COOLDOWN;
                }
                if (this.ejectingLootTable == null) {
                    this.ejectingLootTable = config.lootTablesToEject.pick(world.rand);
                }
                if (this.ejectingLootTable != null) {
                    ejectReward(world, this.ejectingLootTable);
                }
                this.detectedPlayers.remove(this.detectedPlayers.iterator().next());
                return phase;
            case COOLDOWN:
            default:
                tryDetectPlayers(world);
                if (!this.detectedPlayers.isEmpty()) {
                    this.totalMobsSpawned = 0;
                    this.nextMobSpawnsAt = 0L;
                    return TrialSpawnerPhase.ACTIVE;
                }
                if (time >= this.cooldownEndsAt) {
                    removeOminous();
                    resetData();
                    return TrialSpawnerPhase.WAITING_FOR_PLAYERS;
                }
                return phase;
        }
    }

    private boolean canSpawnInLevel(World world) {
        if (this.overridePeacefulAndMobSpawnRule) {
            return true;
        }
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && world.getGameRules().getBoolean("doMobSpawning");
    }

    // ------------------------------------------------------------------------------------------
    // State data
    // ------------------------------------------------------------------------------------------

    private void resetData() {
        this.currentMobs.clear();
        this.nextSpawnData = null;
        this.displayEntity = null;
        resetStatistics();
    }

    private void resetStatistics() {
        this.detectedPlayers.clear();
        this.totalMobsSpawned = 0;
        this.nextMobSpawnsAt = 0L;
        this.cooldownEndsAt = 0L;
    }

    private boolean hasMobToSpawn(Random random) {
        return getOrCreateNextSpawnData(random).hasId() || !activeConfig().spawnPotentials.isEmpty();
    }

    SpawnData getOrCreateNextSpawnData(Random random) {
        if (this.nextSpawnData == null) {
            SpawnData picked = activeConfig().spawnPotentials.pick(random);
            this.nextSpawnData = picked == null ? SpawnData.empty() : picked;
            markUpdated();
        }
        return this.nextSpawnData;
    }

    /** The mob shown spinning inside: the next one to spawn. On the server only its presence matters. */
    @Nullable
    public Entity getOrCreateDisplayEntity(TrialSpawnerPhase phase) {
        if (!phase.hasSpinningMob()) {
            return null;
        }
        if (this.displayEntity == null) {
            SpawnData data = getOrCreateNextSpawnData(this.world.rand);
            if (data.hasId()) {
                this.displayEntity = data.create(this.world);
            }
        }
        return this.displayEntity;
    }

    private void tryDetectPlayers(WorldServer world) {
        if ((this.pos.toLong() + world.getTotalWorldTime()) % 20L != 0L) {
            return;
        }
        if (getPhase() == TrialSpawnerPhase.COOLDOWN && this.isOminous) {
            return;
        }
        List<UUID> seen = detect(world, true);
        boolean becameOminous = false;
        if (!this.isOminous && !seen.isEmpty()) {
            EntityPlayer omen = null;
            boolean trial = false;
            for (UUID id : seen) {
                EntityPlayer p = player(world, id);
                if (p == null) {
                    continue;
                }
                if (Omens.hasTrialOmen(p)) {
                    omen = p;
                    trial = true;
                    break;
                }
                if (Omens.hasBadOmen(p)) {
                    omen = p;
                }
            }
            if (omen != null) {
                if (!trial) {
                    Omens.transformBadOmenIntoTrialOmen(omen);
                }
                TrialMessage.send(world, new BlockPos(omen.posX, omen.posY + omen.getEyeHeight(), omen.posZ), TrialMessage.BECOME_OMINOUS, 0);
                applyOminous();
                becameOminous = true;
            }
        }
        if (getPhase() != TrialSpawnerPhase.COOLDOWN || becameOminous) {
            List<UUID> add = this.detectedPlayers.isEmpty() ? seen : detect(world, false);
            if (this.detectedPlayers.addAll(add)) {
                this.nextMobSpawnsAt = Math.max(world.getTotalWorldTime() + DETECT_PLAYER_SPAWN_BUFFER, this.nextMobSpawnsAt);
                if (!becameOminous) {
                    TrialMessage.send(world, this.pos, this.isOminous ? TrialMessage.DETECT_PLAYER_OMINOUS : TrialMessage.DETECT_PLAYER,
                            this.detectedPlayers.size());
                }
            }
        }
    }

    private List<? extends EntityPlayer> players(World world) {
        return this.onlyPlayers != null ? this.onlyPlayers : world.playerEntities;
    }

    @Nullable
    private EntityPlayer player(World world, UUID id) {
        for (EntityPlayer p : players(world)) {
            if (p.getUniqueID().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** 1.21's {@code PlayerDetector.NO_CREATIVE_PLAYERS}. */
    private List<UUID> detect(World world, boolean lineOfSight) {
        List<UUID> out = new ArrayList<UUID>();
        double range = this.requiredPlayerRange;
        Vec3d center = new Vec3d(this.pos).add(0.5, 0.5, 0.5);
        for (EntityPlayer p : players(world)) {
            if (p.isSpectator() || p.isCreative() || p.getPosition().distanceSq(this.pos) >= range * range) {
                continue;
            }
            if (lineOfSight && !inLineOfSight(world, center, new Vec3d(p.posX, p.posY + p.getEyeHeight(), p.posZ), this.pos)) {
                continue;
            }
            out.add(p.getUniqueID());
        }
        return out;
    }

    /** Sight from {@code from} to the center of {@code target}: nothing in between, or the target itself. */
    static boolean inLineOfSight(World world, Vec3d center, Vec3d from, BlockPos target) {
        RayTraceResult hit = world.rayTraceBlocks(from, center, false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS || target.equals(hit.getBlockPos());
    }

    private void resetAfterBecomingOminous() {
        WorldServer world = (WorldServer) this.world;
        for (UUID id : this.currentMobs) {
            Entity e = world.getEntityFromUuid(id);
            if (e != null) {
                TrialMessage.send(world, e.getPosition(), TrialMessage.SPAWN_MOB, 0);
                if (e instanceof EntityLiving) {
                    dropPreservedEquipment((EntityLiving) e);
                }
                e.setDead();
            }
        }
        if (!this.ominous.spawnPotentials.isEmpty()) {
            this.nextSpawnData = null;
            this.displayEntity = null;
        }
        this.totalMobsSpawned = 0;
        this.currentMobs.clear();
        this.nextMobSpawnsAt = world.getTotalWorldTime() + this.ominous.ticksBetweenSpawn;
        markUpdated();
        this.cooldownEndsAt = world.getTotalWorldTime() + this.ominous.ticksBetweenItemSpawners();
    }

    /** Equipment a mob picked up (drop chance above 1) falls when it vanishes, as in 1.21. */
    private static void dropPreservedEquipment(EntityLiving mob) {
        float[] hands = DropChances.hands(mob);
        float[] armor = DropChances.armor(mob);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            float[] chances = slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? hands : armor;
            ItemStack stack = mob.getItemStackFromSlot(slot);
            if (chances != null && !stack.isEmpty() && chances[slot.getIndex()] > 1.0F) {
                mob.setItemStackToSlot(slot, ItemStack.EMPTY);
                mob.entityDropItem(stack, 0.0F);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Spawning and rewards
    // ------------------------------------------------------------------------------------------

    @Nullable
    private UUID spawnMob(WorldServer world) {
        Random random = world.rand;
        SpawnData data = getOrCreateNextSpawnData(random);
        Entity entity = data.create(world);
        if (entity == null) {
            return null;
        }
        double x;
        double y;
        double z;
        if (data.entity.hasKey("Pos", 9) && data.entity.getTagList("Pos", 6).tagCount() == 3) {
            x = data.entity.getTagList("Pos", 6).getDoubleAt(0);
            y = data.entity.getTagList("Pos", 6).getDoubleAt(1);
            z = data.entity.getTagList("Pos", 6).getDoubleAt(2);
        } else {
            SpawnerConfig config = activeConfig();
            x = this.pos.getX() + (random.nextDouble() - random.nextDouble()) * config.spawnRange + 0.5;
            y = this.pos.getY() + random.nextInt(3) - 1;
            z = this.pos.getZ() + (random.nextDouble() - random.nextDouble()) * config.spawnRange + 0.5;
        }
        entity.setLocationAndAngles(x, y, z, random.nextFloat() * 360.0F, 0.0F);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        if (!world.getCollisionBoxes(null, box).isEmpty()) {
            return null;
        }
        Vec3d at = new Vec3d(x, y, z);
        if (!inLineOfSight(world, new Vec3d(this.pos).add(0.5, 0.5, 0.5), at, this.pos)) {
            return null;
        }
        BlockPos standing = new BlockPos(at);
        if (!data.lightAllows(world, standing)) {
            return null;
        }
        if (entity instanceof EntityLiving) {
            EntityLiving mob = (EntityLiving) entity;
            if (!mob.isNotColliding()) {
                return null;
            }
            if (data.onlyId() && !ForgeEventFactory.doSpecialSpawn(mob, world, (float) x, (float) y, (float) z, null)) {
                mob.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(mob)), null);
            }
            mob.enablePersistence();
            data.equip(mob);
        }
        if (!world.spawnEntity(entity)) {
            return null;
        }
        int flame = this.isOminous ? 1 : 0;
        TrialMessage.send(world, this.pos, TrialMessage.SPAWN_PARTICLES, flame);
        TrialMessage.send(world, standing, TrialMessage.SPAWN_MOB, flame);
        return entity.getUniqueID();
    }

    private void ejectReward(WorldServer world, String table) {
        List<ItemStack> items = ModernLoot.roll(world, table, world.rand, 0.0F);
        if (items.isEmpty()) {
            return;
        }
        for (ItemStack stack : items) {
            spawnItemUp(world, stack, this.pos);
        }
        TrialMessage.send(world, this.pos, TrialMessage.EJECT_ITEM, 0);
    }

    /** 1.21's {@code DefaultDispenseItemBehavior.spawnItem} upward from 1.2 blocks above the block's bottom. */
    static void spawnItemUp(World world, ItemStack stack, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.2 - 0.125;
        double z = pos.getZ() + 0.5;
        EntityItem item = new EntityItem(world, x, y, z, stack);
        double spread = 0.0172275 * 2;
        Random r = world.rand;
        r.nextDouble();
        item.motionX = triangle(r, 0.0, spread);
        item.motionY = triangle(r, 0.2, spread);
        item.motionZ = triangle(r, 0.0, spread);
        world.spawnEntity(item);
    }

    private static double triangle(Random r, double mean, double spread) {
        return mean + spread * (r.nextDouble() - r.nextDouble());
    }

    // ------------------------------------------------------------------------------------------
    // Ominous item spawners
    // ------------------------------------------------------------------------------------------

    private void spawnOminousItemSpawner(WorldServer world) {
        Weighted<ItemStack> items = getDispensingItems(world);
        ItemStack stack = items.pick(world.rand);
        if (stack == null || stack.isEmpty() || world.getTotalWorldTime() < this.cooldownEndsAt) {
            return;
        }
        Vec3d at = positionToSpawnSpawner(world);
        if (at == null) {
            return;
        }
        EntityOminousItemSpawner spawner = EntityOminousItemSpawner.create(world, stack.copy());
        spawner.setPosition(at.x, at.y, at.z);
        world.spawnEntity(spawner);
        float pitch = (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F;
        world.playSound(null, new BlockPos(at), TrialContent.sound("block.trial_spawner.spawn_item_begin"), SoundCategory.BLOCKS, 1.0F, pitch);
        this.cooldownEndsAt = world.getTotalWorldTime() + this.ominous.ticksBetweenItemSpawners();
    }

    @Nullable
    private Vec3d positionToSpawnSpawner(WorldServer world) {
        double range = this.requiredPlayerRange;
        Vec3d center = new Vec3d(this.pos).add(0.5, 0.5, 0.5);
        List<Entity> players = new ArrayList<Entity>();
        for (UUID id : this.detectedPlayers) {
            EntityPlayer p = player(world, id);
            if (p != null && !p.isCreative() && !p.isSpectator() && p.isEntityAlive()
                    && p.getDistanceSq(center.x, center.y, center.z) <= range * range) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            return null;
        }
        List<Entity> mobs = new ArrayList<Entity>();
        for (UUID id : this.currentMobs) {
            Entity e = world.getEntityFromUuid(id);
            if (e != null && e.isEntityAlive() && e.getDistanceSq(center.x, center.y, center.z) <= range * range) {
                mobs.add(e);
            }
        }
        List<Entity> pool = world.rand.nextBoolean() ? mobs : players;
        if (pool.isEmpty()) {
            return null;
        }
        Entity above = pool.size() == 1 ? pool.get(0) : pool.get(world.rand.nextInt(pool.size()));
        Vec3d from = above.getPositionVector();
        Vec3d to = from.add(0.0, above.height + 2.0F + world.rand.nextInt(4), 0.0);
        RayTraceResult hit = world.rayTraceBlocks(from, to, false, true, false);
        BlockPos block = hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.getBlockPos() : new BlockPos(to);
        Vec3d spot = new Vec3d(block).add(0.5, -0.5, 0.5);
        BlockPos spotBlock = new BlockPos(spot);
        IBlockState state = world.getBlockState(spotBlock);
        return state.getCollisionBoundingBox(world, spotBlock) != null ? null : spot;
    }

    private Weighted<ItemStack> getDispensingItems(WorldServer world) {
        if (this.dispensing == null) {
            long seed = world.getSeed() + new BlockPos(MathHelper.floor(this.pos.getX() / 30.0F), MathHelper.floor(this.pos.getY() / 20.0F),
                    MathHelper.floor(this.pos.getZ() / 30.0F)).toLong();
            Weighted<ItemStack> items = new Weighted<ItemStack>();
            for (ItemStack stack : ModernLoot.roll(world, activeConfig().itemsToDropWhenOminous, new Random(seed), 0.0F)) {
                ItemStack one = stack.copy();
                one.setCount(1);
                items.add(one, stack.getCount());
            }
            this.dispensing = items;
        }
        return this.dispensing;
    }

    // ------------------------------------------------------------------------------------------
    // Spawn egg
    // ------------------------------------------------------------------------------------------

    /** A spawn egg's mob for both configs, as 1.21's {@code overrideEntityToSpawn}. */
    public void overrideEntityToSpawn(String entityId) {
        resetData();
        this.normal = this.normal.withSpawning(entityId);
        this.ominous = this.ominous.withSpawning(entityId);
        setPhase(TrialSpawnerPhase.INACTIVE);
        markUpdated();
    }

    // ------------------------------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        Json.readUuids(tag, "registered_players", this.detectedPlayers);
        Json.readUuids(tag, "current_mobs", this.currentMobs);
        this.cooldownEndsAt = tag.getLong("cooldown_ends_at");
        this.nextMobSpawnsAt = tag.getLong("next_mob_spawns_at");
        this.totalMobsSpawned = Math.max(0, tag.getInteger("total_mobs_spawned"));
        this.nextSpawnData = tag.hasKey("spawn_data", 10) ? SpawnData.read(tag.getCompoundTag("spawn_data")) : null;
        this.ejectingLootTable = tag.hasKey("ejecting_loot_table", 8) ? tag.getString("ejecting_loot_table") : null;
        this.normal = readConfig(tag, "normal_config");
        this.ominous = readConfig(tag, "ominous_config");
        this.targetCooldownLength = tag.hasKey("target_cooldown_length") ? Math.max(0, tag.getInteger("target_cooldown_length")) : 36000;
        this.requiredPlayerRange = tag.hasKey("required_player_range")
                ? MathHelper.clamp(tag.getInteger("required_player_range"), 1, 128) : 14;
        this.displayEntity = null;
        this.dispensing = null;
    }

    private static SpawnerConfig readConfig(NBTTagCompound tag, String key) {
        if (tag.hasKey(key, 8)) {
            SpawnerConfig named = SpawnerConfig.named(tag.getString(key));
            if (named == null) {
                TrickyTrialsBackport.LOGGER.warn("Trial spawner config {} not found", tag.getString(key));
                return SpawnerConfig.DEFAULT;
            }
            return named;
        }
        if (tag.hasKey(key, 10)) {
            return new SpawnerConfig(Json.toJson(tag.getCompoundTag(key)).getAsJsonObject(), null);
        }
        return SpawnerConfig.DEFAULT;
    }

    private static void writeConfig(NBTTagCompound tag, String key, SpawnerConfig config) {
        if (config.id != null) {
            tag.setString(key, config.id);
        } else if (config != SpawnerConfig.DEFAULT) {
            tag.setTag(key, Json.toNbt(config.json));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("registered_players", Json.uuids(this.detectedPlayers));
        tag.setTag("current_mobs", Json.uuids(this.currentMobs));
        tag.setLong("cooldown_ends_at", this.cooldownEndsAt);
        tag.setLong("next_mob_spawns_at", this.nextMobSpawnsAt);
        tag.setInteger("total_mobs_spawned", this.totalMobsSpawned);
        if (this.nextSpawnData != null) {
            tag.setTag("spawn_data", this.nextSpawnData.write());
        }
        if (this.ejectingLootTable != null) {
            tag.setString("ejecting_loot_table", this.ejectingLootTable);
        }
        writeConfig(tag, "normal_config", this.normal);
        writeConfig(tag, "ominous_config", this.ominous);
        tag.setInteger("target_cooldown_length", this.targetCooldownLength);
        tag.setInteger("required_player_range", this.requiredPlayerRange);
        return tag;
    }

    /** As 1.21: the client gets the next mob (to show it) and, while active, when it spawns (for the spin). */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        if (getPhase() == TrialSpawnerPhase.ACTIVE) {
            tag.setLong("next_mob_spawns_at", this.nextMobSpawnsAt);
        }
        if (this.nextSpawnData != null) {
            tag.setTag("spawn_data", this.nextSpawnData.write());
        }
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readClient(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readClient(packet.getNbtCompound());
    }

    private void readClient(NBTTagCompound tag) {
        this.nextMobSpawnsAt = tag.getLong("next_mob_spawns_at");
        SpawnData data = tag.hasKey("spawn_data", 10) ? SpawnData.read(tag.getCompoundTag("spawn_data")) : null;
        String before = this.nextSpawnData == null ? null : this.nextSpawnData.entity.toString();
        String after = data == null ? null : data.entity.toString();
        if (before == null ? after != null : !before.equals(after)) {
            this.displayEntity = null;
        }
        this.nextSpawnData = data;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
