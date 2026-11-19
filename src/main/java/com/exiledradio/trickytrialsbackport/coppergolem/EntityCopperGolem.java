package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 1.21.9's copper golem ({@code CopperGolem}, {@code CopperGolemAi}): 12 health, speed 0.2, steps up a block, never
 * attacks, panics when hurt, opens and closes wooden doors, breathes under water and takes no fall damage. It sorts
 * items from copper chests into chests ({@link CopperGolemTransport}).
 *
 * <h2>Oxidation</h2>
 * Every 504000-552000 ticks of world time (21-23 days) it ages one stage, even while unloaded; fully oxidized and
 * standing in air, it turns into a statue in a random pose with chance 0.0058 per tick. Honeycomb waxes it (no aging,
 * no statue), an axe takes the wax off, or else scrapes a stage off, and each lightning bolt scrapes one stage off.
 * {@code next_weather_age} is -1 for "not yet set" and -2 for waxed, as in 1.21.9.
 *
 * <h2>Its antenna</h2>
 * 1.21.9 keeps the poppy an iron golem gives it in a {@code SADDLE} slot 1.12.2 lacks; here it is its own synced
 * stack, saved as {@code antenna}. Shears take it off.
 */
public class EntityCopperGolem extends EntityGolem {

    public static final int STATE_IDLE = 0;
    public static final int STATE_GETTING_ITEM = 1;
    public static final int STATE_GETTING_NO_ITEM = 2;
    public static final int STATE_DROPPING_ITEM = 3;
    public static final int STATE_DROPPING_NO_ITEM = 4;

    static final long IGNORE_WEATHERING_TICK = -2L;
    static final long UNSET_WEATHERING_TICK = -1L;
    static final int WEATHERING_TICK_FROM = 504000;
    static final int WEATHERING_TICK_TO = 552000;
    static final float TURN_TO_STATUE_CHANCE = 0.0058F;
    static final int SPIN_ANIMATION_MIN_COOLDOWN = 200;
    static final int SPIN_ANIMATION_MAX_COOLDOWN = 240;

    private static final DataParameter<Integer> WEATHER_STATE = EntityDataManager.createKey(EntityCopperGolem.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> GOLEM_STATE = EntityDataManager.createKey(EntityCopperGolem.class, DataSerializers.VARINT);
    private static final DataParameter<ItemStack> ANTENNA = EntityDataManager.createKey(EntityCopperGolem.class, DataSerializers.ITEM_STACK);

    long nextWeatheringTick = UNSET_WEATHERING_TICK;
    @Nullable
    private UUID lastLightningBoltUUID;
    /** 1.21's {@code TRANSPORT_ITEMS_COOLDOWN_TICKS} memory: present while above 0. */
    public int transportCooldown;
    public final CopperGolemTransport transport = new CopperGolemTransport(this);
    private Set<BlockPos> visited = Collections.emptySet();
    private long visitedExpires;
    private Set<BlockPos> unreachable = Collections.emptySet();
    private long unreachableExpires;
    /** The chests this golem has open now (both halves of a double chest), for 1.21's {@code ContainerUser}. */
    private final List<BlockPos> opened = new ArrayList<BlockPos>(2);
    @Nullable
    private BlockPos openedChestPos;

    // Client animation states (1.21's AnimationState: the tick each started, or Integer.MIN_VALUE when stopped).
    public int idleAnimationStart = Integer.MIN_VALUE;
    public int interactionAnimationStart = Integer.MIN_VALUE;
    public int interactionAnimationState = STATE_IDLE;
    private int idleAnimationStartTick;

    public EntityCopperGolem(World world) {
        super(world);
        setSize(0.49F, 0.98F);
        this.stepHeight = 1.0F;
        enablePersistence();
        PathNavigateGround nav = (PathNavigateGround) getNavigator();
        nav.setBreakDoors(true);
        nav.setEnterDoors(true);
        setPathPriority(PathNodeType.DANGER_FIRE, 16.0F);
        setPathPriority(PathNodeType.DANGER_OTHER, 16.0F);
        setPathPriority(PathNodeType.DAMAGE_FIRE, -1.0F);
        this.transportCooldown = 60 + this.rand.nextInt(40);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(WEATHER_STATE, 0);
        this.dataManager.register(GOLEM_STATE, STATE_IDLE);
        this.dataManager.register(ANTENNA, ItemStack.EMPTY);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2D);
        // 1.21's setRequiredPathLength(48).
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    /**
     * The brain's goals that 1.12.2's task list can run (its core and idle activities): panic, doors, looking at
     * players, and strolling or standing about while resting from sorting. Sorting itself runs every tick from
     * {@link #updateAITasks}.
     */
    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new Panic(this));
        this.tasks.addTask(1, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F, 1.0F / 60.0F));
        this.tasks.addTask(3, new Rest(this));
    }

    @Override
    public float getEyeHeight() {
        return 0.8125F;
    }

    // ------------------------------------------------------------------------------------------
    // Synced state
    // ------------------------------------------------------------------------------------------

    /** @return 0 unaffected, 1 exposed, 2 weathered, 3 oxidized */
    public int getWeatherState() {
        return this.dataManager.get(WEATHER_STATE);
    }

    public void setWeatherState(int stage) {
        this.dataManager.set(WEATHER_STATE, Math.max(0, Math.min(3, stage)));
    }

    public int getGolemState() {
        return this.dataManager.get(GOLEM_STATE);
    }

    public void setGolemState(int state) {
        this.dataManager.set(GOLEM_STATE, state);
    }

    public ItemStack getAntenna() {
        return this.dataManager.get(ANTENNA);
    }

    public void setAntenna(ItemStack stack) {
        this.dataManager.set(ANTENNA, stack);
    }

    public boolean isWaxed() {
        return this.nextWeatheringTick == IGNORE_WEATHERING_TICK;
    }

    public long getNextWeatheringTick() {
        return this.nextWeatheringTick;
    }

    public void setNextWeatheringTick(long tick) {
        this.nextWeatheringTick = tick;
    }

    public void setGuaranteedDrop() {
        this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
    }

    // ------------------------------------------------------------------------------------------
    // Memories
    // ------------------------------------------------------------------------------------------

    public Set<BlockPos> visitedPositions() {
        return this.world.getTotalWorldTime() < this.visitedExpires ? this.visited : Collections.<BlockPos>emptySet();
    }

    public Set<BlockPos> unreachablePositions() {
        return this.world.getTotalWorldTime() < this.unreachableExpires ? this.unreachable : Collections.<BlockPos>emptySet();
    }

    void setVisited(Set<BlockPos> set, int ticks) {
        this.visited = set;
        this.visitedExpires = this.world.getTotalWorldTime() + ticks;
    }

    void setUnreachable(Set<BlockPos> set, int ticks) {
        this.unreachable = set;
        this.unreachableExpires = this.world.getTotalWorldTime() + ticks;
    }

    void clearVisited() {
        this.visited = Collections.emptySet();
        this.unreachable = Collections.emptySet();
        this.visitedExpires = 0;
        this.unreachableExpires = 0;
    }

    /** 1.21's {@code IS_PANICKING}: hurt in the last 40 ticks (its hurt-by memory), or burning. */
    public boolean isPanicking() {
        return getLastDamageSource() != null || isBurning();
    }

    // ------------------------------------------------------------------------------------------
    // Chests it has open (1.21's ContainerUser)
    // ------------------------------------------------------------------------------------------

    /** Opens a chest (both halves) for this golem: the lid lifts and its open sound plays, as for a player. */
    void openChest(BlockPos pos) {
        clearOpenedChestPos();
        this.openedChestPos = pos;
        List<BlockPos> halves = new ArrayList<BlockPos>(2);
        halves.add(pos);
        BlockPos other = CopperGolemTransport.otherHalf(this.world, pos);
        if (other != null) {
            halves.add(other);
        }
        for (BlockPos p : halves) {
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityChest) {
                changeOpeners((TileEntityChest) te, 1);
                this.opened.add(p);
            }
        }
    }

    /** 1.12.2's chests recount their players every 200 ticks; a golem's open lid stays up through it. */
    void keepChestOpen() {
        for (BlockPos p : this.opened) {
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityChest && !(te instanceof TileEntityCopperChest) && ((TileEntityChest) te).numPlayersUsing <= 0) {
                changeOpeners((TileEntityChest) te, 1);
            }
        }
    }

    /** Closes what this golem has open (1.21 lets the chest notice within 5 ticks; here at once). */
    void clearOpenedChestPos() {
        for (BlockPos p : this.opened) {
            TileEntity te = this.world.getTileEntity(p);
            if (te instanceof TileEntityChest && ((TileEntityChest) te).numPlayersUsing > 0) {
                changeOpeners((TileEntityChest) te, -1);
            }
        }
        this.opened.clear();
        this.openedChestPos = null;
    }

    @Nullable
    public BlockPos getOpenedChestPos() {
        return this.openedChestPos;
    }

    private void changeOpeners(TileEntityChest te, int by) {
        if (te.numPlayersUsing < 0) {
            te.numPlayersUsing = 0;
        }
        te.numPlayersUsing += by;
        if (te instanceof TileEntityCopperChest) {
            ((TileEntityCopperChest) te).golemsUsing = Math.max(0, ((TileEntityCopperChest) te).golemsUsing + by);
        }
        IBlockState state = this.world.getBlockState(te.getPos());
        this.world.addBlockEvent(te.getPos(), state.getBlock(), 1, te.numPlayersUsing);
        this.world.notifyNeighborsOfStateChange(te.getPos(), state.getBlock(), false);
        if (state.getBlock() instanceof BlockChest && ((BlockChest) state.getBlock()).chestType == BlockChest.Type.TRAP) {
            this.world.notifyNeighborsOfStateChange(te.getPos().down(), state.getBlock(), false);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    /** The brain's tick: the cooldown counts down, then sorting runs (1.21's core, then idle activity). */
    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (this.transportCooldown > 0) {
            this.transportCooldown--;
        }
        if (ModConfig.copperGolem.sorting) {
            this.transport.update((WorldServer) this.world);
        } else if (this.transport.isRunning()) {
            this.transport.stop();
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            if (!isAIDisabled()) {
                setupAnimationStates();
            }
        } else if (!this.isDead) {
            updateWeathering((WorldServer) this.world, this.world.getTotalWorldTime());
        }
    }

    public void updateWeathering(WorldServer world, long gameTime) {
        if (this.nextWeatheringTick == IGNORE_WEATHERING_TICK || !ModConfig.copperGolem.oxidation) {
            return;
        }
        if (this.nextWeatheringTick == UNSET_WEATHERING_TICK) {
            this.nextWeatheringTick = gameTime + between(WEATHERING_TICK_FROM, WEATHERING_TICK_TO);
            return;
        }
        int stage = getWeatherState();
        boolean oxidized = stage == 3;
        if (gameTime >= this.nextWeatheringTick && !oxidized) {
            int next = stage + 1;
            setWeatherState(next);
            this.nextWeatheringTick = next == 3 ? 0L : this.nextWeatheringTick + between(WEATHERING_TICK_FROM, WEATHERING_TICK_TO);
        }
        if (oxidized && canTurnToStatue()) {
            turnToStatue(world);
        }
    }

    private int between(int from, int to) {
        return from + this.rand.nextInt(to - from + 1);
    }

    private boolean canTurnToStatue() {
        return ModConfig.copperGolem.statues && this.world.isAirBlock(new BlockPos(this))
                && this.world.rand.nextFloat() <= TURN_TO_STATUE_CHANCE;
    }

    /** 1.21's {@code turnToStatue}: an oxidized statue in a random pose, facing the golem's way, with its name. */
    public void turnToStatue(WorldServer world) {
        BlockPos pos = new BlockPos(this);
        BlockCopperGolemStatue block = CopperGolemContent.STATUES[0][3];
        BlockCopperGolemStatue.Pose[] poses = BlockCopperGolemStatue.Pose.values();
        IBlockState state = block.getDefaultState().withProperty(BlockCopperGolemStatue.POSE, poses[this.rand.nextInt(poses.length)])
                .withProperty(BlockCopperGolemStatue.FACING, EnumFacing.fromAngle(this.rotationYaw));
        if (!world.setBlockState(pos, state, 3)) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCopperGolemStatue) {
            ((TileEntityCopperGolemStatue) te).createStatue(this);
            dropPreservedEquipment();
            clearOpenedChestPos();
            setDead();
            playSound(CopperGolemContent.BECOME_STATUE, 1.0F, 1.0F);
            if (getLeashed()) {
                clearLeashed(true, world.getGameRules().getBoolean("doEntityDrops"));
            }
        }
    }

    /** What it was sure to drop: the stack it carried from a chest and the poppy on its antenna. */
    void dropPreservedEquipment() {
        ItemStack hand = getHeldItemMainhand();
        if (!hand.isEmpty() && this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] > 1.0F) {
            entityDropItem(hand, 0.0F);
            setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        }
        ItemStack antenna = getAntenna();
        if (!antenna.isEmpty()) {
            entityDropItem(antenna.copy(), 0.0F);
            setAntenna(ItemStack.EMPTY);
        }
    }

    /** 1.21's client {@code setupAnimationStates}: the idle head spin every 200-240 ticks, and the chest animations. */
    private void setupAnimationStates() {
        int state = getGolemState();
        if (state == STATE_IDLE) {
            this.interactionAnimationStart = Integer.MIN_VALUE;
            if (this.idleAnimationStartTick == this.ticksExisted) {
                this.idleAnimationStart = this.ticksExisted;
            } else if (this.idleAnimationStartTick == 0) {
                this.idleAnimationStartTick = this.ticksExisted + SPIN_ANIMATION_MIN_COOLDOWN
                        + this.rand.nextInt(SPIN_ANIMATION_MAX_COOLDOWN - SPIN_ANIMATION_MIN_COOLDOWN);
            }
            if (this.ticksExisted == this.idleAnimationStartTick + 10) {
                if (!isSilent()) {
                    this.world.playSound(this.posX, this.posY, this.posZ, CopperGolemContent.SPIN[getWeatherState()],
                            getSoundCategory(), 1.0F, 1.0F, false);
                }
                this.idleAnimationStartTick = 0;
            }
        } else {
            this.idleAnimationStart = Integer.MIN_VALUE;
            this.idleAnimationStartTick = 0;
            if (this.interactionAnimationStart == Integer.MIN_VALUE || this.interactionAnimationState != state) {
                this.interactionAnimationStart = this.ticksExisted;
                this.interactionAnimationState = state;
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Players
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) {
            ItemStack carried = getHeldItemMainhand();
            if (!carried.isEmpty()) {
                if (!this.world.isRemote) {
                    throwItem(carried, player.getPositionVector());
                    setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                }
                return true;
            }
        }
        if (held.getItem() == Items.SHEARS && readyForShearing()) {
            if (!this.world.isRemote) {
                shear();
                held.damageItem(1, player);
            }
            return true;
        }
        if (this.world.isRemote) {
            return false;
        }
        if (CopperGolemEvents.isWax(held) && this.nextWeatheringTick != IGNORE_WEATHERING_TICK) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, CopperGolemContent.WAX_ON, getSoundCategory(), 1.0F, 1.0F);
            this.nextWeatheringTick = IGNORE_WEATHERING_TICK;
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            return true;
        }
        if (CopperGolemEvents.isAxe(held) && this.nextWeatheringTick == IGNORE_WEATHERING_TICK) {
            playSound(CopperGolemContent.AXE_SCRAPE, 1.0F, 1.0F);
            this.nextWeatheringTick = UNSET_WEATHERING_TICK;
            held.damageItem(1, player);
            return true;
        }
        if (CopperGolemEvents.isAxe(held) && getWeatherState() > 0) {
            playSound(CopperGolemContent.AXE_SCRAPE, 1.0F, 1.0F);
            this.nextWeatheringTick = UNSET_WEATHERING_TICK;
            setWeatherState(getWeatherState() - 1);
            held.damageItem(1, player);
            return true;
        }
        return super.processInteract(player, hand);
    }

    /** 1.21's {@code BehaviorUtils.throwItem}: from just below the eyes, toward the player. */
    void throwItem(ItemStack stack, Vec3d to) {
        double y = this.posY + getEyeHeight() - 0.3D;
        EntityItem item = new EntityItem(this.world, this.posX, y, this.posZ, stack);
        item.setThrower(getName());
        Vec3d v = to.subtract(this.posX, this.posY, this.posZ).normalize().scale(0.3D);
        item.motionX = v.x;
        item.motionY = v.y;
        item.motionZ = v.z;
        item.setDefaultPickupDelay();
        this.world.spawnEntity(item);
    }

    boolean readyForShearing() {
        ItemStack a = getAntenna();
        return isEntityAlive() && a.getItem() == Item.getItemFromBlock(Blocks.RED_FLOWER) && a.getMetadata() == 0;
    }

    /** Shears take the poppy off its antenna (1.21's {@code shear}). */
    void shear() {
        this.world.playSound(null, this.posX, this.posY, this.posZ, CopperGolemContent.SHEAR, SoundCategory.PLAYERS, 1.0F, 1.0F);
        ItemStack poppy = getAntenna();
        setAntenna(ItemStack.EMPTY);
        entityDropItem(poppy.copy(), 1.5F);
    }

    // ------------------------------------------------------------------------------------------
    // Hurting, dying, lightning
    // ------------------------------------------------------------------------------------------

    @Override
    protected void damageEntity(DamageSource source, float amount) {
        float before = getHealth();
        super.damageEntity(source, amount);
        if (getHealth() != before || amount > 0.0F) {
            setGolemState(STATE_IDLE);
        }
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        // 1.21's #fall_damage_immune.
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt bolt) {
        super.onStruckByLightning(bolt);
        UUID id = bolt.getUniqueID();
        if (!id.equals(this.lastLightningBoltUUID)) {
            this.lastLightningBoltUUID = id;
            int stage = getWeatherState();
            if (stage > 0) {
                this.nextWeatheringTick = UNSET_WEATHERING_TICK;
                setWeatherState(stage - 1);
            }
        }
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return null;
    }

    /** 1.21.9's {@code entities/copper_golem}: 1-3 copper ingots, and up to one more per Looting level. */
    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int looting) {
        Item ingot = CopperGolemContent.ingot();
        if (ingot == null) {
            return;
        }
        int count = 1 + this.rand.nextInt(3) + Math.round(looting * this.rand.nextFloat());
        entityDropItem(new ItemStack(ingot, count), 0.0F);
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        ItemStack antenna = getAntenna();
        if (!antenna.isEmpty()) {
            entityDropItem(antenna.copy(), 0.0F);
            setAntenna(ItemStack.EMPTY);
        }
    }

    @Override
    public void setDead() {
        if (!this.world.isRemote) {
            clearOpenedChestPos();
        }
        super.setDead();
    }

    // ------------------------------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CopperGolemContent.HURT[getWeatherState()];
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CopperGolemContent.DEATH[getWeatherState()];
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
        playSound(CopperGolemContent.STEP[getWeatherState()], 1.0F, 1.0F);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    /** From a built golem: its stage from the copper block, and the spawn sound. */
    public void spawn(int stage) {
        setWeatherState(stage);
        playSpawnSound();
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        playSpawnSound();
        return super.onInitialSpawn(difficulty, data);
    }

    public void playSpawnSound() {
        playSound(CopperGolemContent.SPAWN, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setLong("next_weather_age", this.nextWeatheringTick);
        tag.setString("weather_state", CopperGolemContent.stateName(getWeatherState()));
        if (!getAntenna().isEmpty()) {
            tag.setTag("antenna", getAntenna().writeToNBT(new NBTTagCompound()));
        }
        if (this.transportCooldown > 0) {
            tag.setInteger("transport_items_cooldown_ticks", this.transportCooldown);
        }
        writePositions(tag, "visited_block_positions", visitedPositions(), this.visitedExpires);
        writePositions(tag, "unreachable_transport_block_positions", unreachablePositions(), this.unreachableExpires);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.nextWeatheringTick = tag.hasKey("next_weather_age") ? tag.getLong("next_weather_age") : UNSET_WEATHERING_TICK;
        setWeatherState(CopperGolemContent.stageOf(tag.getString("weather_state")));
        setAntenna(tag.hasKey("antenna", 10) ? new ItemStack(tag.getCompoundTag("antenna")) : ItemStack.EMPTY);
        this.transportCooldown = tag.getInteger("transport_items_cooldown_ticks");
        if (tag.hasKey("visited_block_positions", 10)) {
            NBTTagCompound v = tag.getCompoundTag("visited_block_positions");
            this.visited = readPositions(v);
            this.visitedExpires = v.getLong("expires");
        }
        if (tag.hasKey("unreachable_transport_block_positions", 10)) {
            NBTTagCompound u = tag.getCompoundTag("unreachable_transport_block_positions");
            this.unreachable = readPositions(u);
            this.unreachableExpires = u.getLong("expires");
        }
    }

    private static void writePositions(NBTTagCompound tag, String key, Set<BlockPos> set, long expires) {
        if (set.isEmpty()) {
            return;
        }
        NBTTagList list = new NBTTagList();
        for (BlockPos p : set) {
            list.appendTag(NBTUtil.createPosTag(p));
        }
        NBTTagCompound c = new NBTTagCompound();
        c.setTag("positions", list);
        c.setLong("expires", expires);
        tag.setTag(key, c);
    }

    private static Set<BlockPos> readPositions(NBTTagCompound c) {
        Set<BlockPos> out = new HashSet<BlockPos>();
        NBTTagList list = c.getTagList("positions", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            out.add(NBTUtil.getPosFromTag(list.getCompoundTagAt(i)));
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code AnimalPanic(1.5)}: while hurt in the last 40 ticks or burning, runs about at 1.5 times its speed. */
    static final class Panic extends EntityAIPanic {
        private final EntityCopperGolem golem;

        Panic(EntityCopperGolem golem) {
            super(golem, 1.5D);
            this.golem = golem;
        }

        @Override
        public boolean shouldExecute() {
            return this.golem.isPanicking() && findRandomPosition();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (!this.golem.isPanicking()) {
                return false;
            }
            if (this.golem.getNavigator().noPath() && findRandomPosition()) {
                this.golem.getNavigator().tryMoveToXYZ(this.randPosX, this.randPosY, this.randPosZ, this.speed);
            }
            return true;
        }
    }

    /**
     * 1.21's idle {@code RunOne}: while it rests from sorting and has nowhere to walk, either a short stroll (2
     * blocks across, 2 up or down) or 30-60 ticks of standing still, even odds.
     */
    static final class Rest extends EntityAIBase {
        private final EntityCopperGolem golem;
        private int waiting;

        Rest(EntityCopperGolem golem) {
            this.golem = golem;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return this.golem.transportCooldown > 0 && this.golem.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            if (this.golem.rand.nextBoolean()) {
                this.waiting = 0;
                Vec3d to = RandomPositionGenerator.findRandomTarget(this.golem, 2, 2);
                if (to != null) {
                    this.golem.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, 1.0D);
                }
            } else {
                this.waiting = 30 + this.golem.rand.nextInt(31);
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.waiting > 0 ? this.golem.transportCooldown > 0 : !this.golem.getNavigator().noPath();
        }

        @Override
        public void updateTask() {
            if (this.waiting > 0) {
                this.waiting--;
            }
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        return false;
    }
}
