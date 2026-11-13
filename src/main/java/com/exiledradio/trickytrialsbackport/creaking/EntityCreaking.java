package com.exiledradio.trickytrialsbackport.creaking;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;

import com.google.common.base.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The creaking: 1.21.11's {@code Creaking}.
 *
 * <ul>
 *   <li>1 health, speed 0.4, 3 attack damage, follow range 32, step height 1.0625, 0.9 x 2.7 with eyes at 2.3, no
 *       experience.</li>
 *   <li>Freezes ({@code CAN_MOVE} false) while a player who can be attacked looks at it ({@link #checkCanMove}):
 *       its movement, looking, jumping, body turning, pushing and knockback all stop. A frozen look within 12
 *       blocks wakes it ({@code activate}): it targets that player, its eyes glow, and it stops making its
 *       ambient sound. With no attackable player in range it goes back to sleep.</li>
 *   <li>A heart's creaking ({@code home_pos}) cannot be hurt: anything that would hurt it (a living or projectile
 *       attacker, or a player behind the damage) plays the invulnerable animation for 8 ticks and the sway sound,
 *       and when a player was behind it, the heart sends its trails and spreads resin
 *       ({@link CreakingHeartHook#creakingHurt}). It is fire immune, never uses portals, and dies the moment its
 *       heart stops naming it. Only the void and {@code /kill} hurt it.</li>
 *   <li>The heart broken, it twitches for 45 ticks with flickering eyes, then crumbles ({@link #tearDown}).</li>
 *   <li>A spawn-egg creaking has no heart and dies to one hit.</li>
 * </ul>
 *
 * The AI is 1.21.11's brain ({@link CreakingBrain}), ticked from {@link #updateAITasks()}; the creaking has no
 * goals in {@code tasks}.
 */
public class EntityCreaking extends EntityMob {

    private static final DataParameter<Boolean> CAN_MOVE = EntityDataManager.createKey(EntityCreaking.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_ACTIVE = EntityDataManager.createKey(EntityCreaking.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_TEARING_DOWN = EntityDataManager.createKey(EntityCreaking.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<BlockPos>> HOME_POS = EntityDataManager.createKey(EntityCreaking.class, DataSerializers.OPTIONAL_BLOCK_POS);

    public static final ResourceLocation LOOT = Content.id("entities/creaking");

    static final int ATTACK_ANIMATION_DURATION = 15;
    static final double MAX_HEALTH = 1.0D;
    static final double ATTACK_DAMAGE = 3.0D;
    static final double FOLLOW_RANGE = 32.0D;
    static final double MOVEMENT_SPEED = 0.4F;
    static final float ACTIVATION_RANGE_SQ = 144.0F;
    public static final int ATTACK_INTERVAL = 40;
    public static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.3F;
    public static final int CREAKING_ORANGE = 0xFC7812;
    public static final int CREAKING_GRAY = 0x5F5F5F;
    public static final int INVULNERABILITY_ANIMATION_DURATION = 8;
    public static final int TWITCH_DEATH_DURATION = 45;
    static final int MAX_PLAYER_STUCK_COUNTER = 4;
    /** Entity events: vanilla's 66 (the invulnerable flash) and 4 (an attack); 20 is 1.12.2's poof. */
    static final byte EVENT_INVULNERABLE = 66;
    static final byte EVENT_ATTACK = 4;

    private int attackAnimationRemainingTicks;
    private int invulnerabilityAnimationRemainingTicks;
    private boolean eyesGlowing;
    private int nextFlickerTime;
    private int playerStuckCounter;

    /** Client animation states (1.21's {@code AnimationState}: the tick each started, or MIN_VALUE). */
    public int attackAnimationStart = Integer.MIN_VALUE;
    public int invulnerabilityAnimationStart = Integer.MIN_VALUE;
    public int deathAnimationStart = Integer.MIN_VALUE;

    final CreakingBrain brain = new CreakingBrain(this);

    public EntityCreaking(World world) {
        super(world);
        setSize(0.9F, 2.7F);
        this.stepHeight = 1.0625F;
        this.experienceValue = 0;
        this.moveHelper = new MoveHelper(this);
        this.jumpHelper = new JumpHelper(this);
        ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, this, new LookHelper(this), "field_70749_g");
        ((PathNavigateGround) getNavigator()).setCanSwim(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CAN_MOVE, true);
        this.dataManager.register(IS_ACTIVE, false);
        this.dataManager.register(IS_TEARING_DOWN, false);
        this.dataManager.register(HOME_POS, Optional.<BlockPos>absent());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(FOLLOW_RANGE);
    }

    @Override
    public float getEyeHeight() {
        return 2.3F;
    }

    /** {@code setTransient}: bound to a heart, with its path costs. */
    public void setTransient(BlockPos home) {
        setHomePos(home);
        setPathPriority(PathNodeType.DAMAGE_OTHER, 8.0F);
        setPathPriority(PathNodeType.LAVA, 8.0F);
        setPathPriority(PathNodeType.DAMAGE_FIRE, 0.0F);
        setPathPriority(PathNodeType.DANGER_FIRE, 0.0F);
    }

    public boolean isHeartBound() {
        return getHomePos() != null;
    }

    public boolean canMove() {
        return this.dataManager.get(CAN_MOVE);
    }

    // ---------------------------------------------------------------------------------------------
    // Combat

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!(target instanceof EntityLivingBase)) {
            return false;
        }
        this.attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
        this.world.setEntityState(this, EVENT_ATTACK);
        return super.attackEntityAsMob(target);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.world.isRemote) {
            return false;
        }
        BlockPos home = getHomePos();
        // BYPASSES_INVULNERABILITY is out_of_world and generic_kill: 1.12.2's void and /kill both use OUT_OF_WORLD.
        if (home == null || source.canHarmInCreative()) {
            return super.attackEntityFrom(source, amount);
        }
        if (isEntityInvulnerable(source) || this.invulnerabilityAnimationRemainingTicks > 0 || getHealth() <= 0.0F) {
            return false;
        }
        EntityPlayer player = blameSourceForDamage(source);
        Entity direct = source.getImmediateSource();
        if (!(direct instanceof EntityLivingBase) && !(direct instanceof IProjectile) && player == null) {
            return false;
        }
        this.invulnerabilityAnimationRemainingTicks = INVULNERABILITY_ANIMATION_DURATION;
        this.world.setEntityState(this, EVENT_INVULNERABLE);
        TileEntity heart = this.world.isBlockLoaded(home) ? this.world.getTileEntity(home) : null;
        if (heart != null && CreakingHeartHook.isProtector(heart, this)) {
            if (player != null) {
                CreakingHeartHook.creakingHurt(heart);
            }
            playHurtSound(source);
        }
        return true;
    }

    /** {@code blameSourceForDamage}: the last attacker and the player behind the damage, as a real hurt would set them. */
    @Nullable
    EntityPlayer blameSourceForDamage(DamageSource source) {
        Entity attacker = source.getTrueSource();
        if (attacker instanceof EntityLivingBase) {
            setRevengeTarget((EntityLivingBase) attacker);
        }
        EntityPlayer player = null;
        if (attacker instanceof EntityPlayer) {
            player = (EntityPlayer) attacker;
        } else if (attacker instanceof EntityTameable && ((EntityTameable) attacker).isTamed()
                && ((EntityTameable) attacker).getOwner() instanceof EntityPlayer) {
            player = (EntityPlayer) ((EntityTameable) attacker).getOwner();
        }
        if (player != null) {
            this.attackingPlayer = player;
            this.recentlyHit = 100;
        }
        return player;
    }

    @Override
    public boolean canBePushed() {
        return super.canBePushed() && canMove();
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (canMove()) {
            super.addVelocity(x, y, z);
        }
    }

    @Override
    public void knockBack(Entity entity, float strength, double xRatio, double zRatio) {
        if (canMove()) {
            super.knockBack(entity, strength, xRatio, zRatio);
        }
    }

    /** {@code fireImmune}: a heart's creaking (1.12.2's flag is a final getter over a field, set here on both sides). */
    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (HOME_POS.equals(key)) {
            this.isImmuneToFire = isHeartBound();
        }
    }

    @Override
    public void setPortal(BlockPos pos) {
        if (!isHeartBound()) {
            super.setPortal(pos);
        }
    }

    @Override
    @Nullable
    public Entity changeDimension(int dimension, net.minecraftforge.common.util.ITeleporter teleporter) {
        return isHeartBound() ? null : super.changeDimension(dimension, teleporter);
    }

    // ---------------------------------------------------------------------------------------------
    // Ticking

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("creakingBrain");
        this.brain.tick();
        this.world.profiler.endSection();
        this.brain.updateActivity();
    }

    /** {@code aiStep}'s own part, before 1.12.2's living update. */
    @Override
    public void onLivingUpdate() {
        if (this.invulnerabilityAnimationRemainingTicks > 0) {
            this.invulnerabilityAnimationRemainingTicks--;
        }
        if (this.attackAnimationRemainingTicks > 0) {
            this.attackAnimationRemainingTicks--;
        }
        if (!this.world.isRemote) {
            boolean could = this.dataManager.get(CAN_MOVE);
            boolean can = checkCanMove();
            if (can != could) {
                if (can) {
                    makeSound(CreakingContent.UNFREEZE);
                } else {
                    stopInPlace();
                    makeSound(CreakingContent.FREEZE);
                }
            }
            this.dataManager.set(CAN_MOVE, can);
        }
        super.onLivingUpdate();
    }

    private void stopInPlace() {
        getNavigator().clearPath();
        this.moveStrafing = 0.0F;
        this.moveForward = 0.0F;
        this.moveVertical = 0.0F;
        setAIMoveSpeed(0.0F);
        setJumping(false);
    }

    @Override
    public void onUpdate() {
        if (!this.world.isRemote) {
            BlockPos home = getHomePos();
            if (home != null) {
                TileEntity heart = this.world.isBlockLoaded(home) ? this.world.getTileEntity(home) : null;
                if (heart == null || !CreakingHeartHook.isProtector(heart, this)) {
                    setHealth(0.0F);
                }
            }
        }
        super.onUpdate();
        if (this.world.isRemote) {
            setupAnimationStates();
            checkEyeBlink();
        }
    }

    @Override
    protected void onDeathUpdate() {
        if (isHeartBound() && isTearingDown()) {
            this.deathTime++;
            if (!this.world.isRemote && this.deathTime > TWITCH_DEATH_DURATION && !this.isDead) {
                tearDown();
            }
        } else {
            super.onDeathUpdate();
        }
    }

    /** {@code updateWalkAnimation}: the walk runs faster than 1.12.2's (up to 3, from 25 times the distance). */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        super.travel(strafe, vertical, forward);
        // Undo 1.12.2's update (speed min(4d, 1)) and redo it as the creaking's.
        this.limbSwing -= this.limbSwingAmount;
        this.limbSwingAmount = this.prevLimbSwingAmount;
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        float speed = Math.min(MathHelper.sqrt(dx * dx + dz * dz) * 25.0F, 3.0F);
        this.limbSwingAmount += (speed - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    private void setupAnimationStates() {
        this.attackAnimationStart = animateWhen(this.attackAnimationStart, this.attackAnimationRemainingTicks > 0);
        this.invulnerabilityAnimationStart = animateWhen(this.invulnerabilityAnimationStart, this.invulnerabilityAnimationRemainingTicks > 0);
        this.deathAnimationStart = animateWhen(this.deathAnimationStart, isTearingDown());
    }

    private int animateWhen(int start, boolean on) {
        if (!on) {
            return Integer.MIN_VALUE;
        }
        return start == Integer.MIN_VALUE ? this.ticksExisted : start;
    }

    /** {@code tearDown}: crumbles into pale oak wood and an awake heart, with the death sound, and is gone. */
    public void tearDown() {
        if (this.world instanceof WorldServer) {
            AxisAlignedBB bb = getEntityBoundingBox();
            Vec3d c = CreakingHeartHook.center(bb);
            double sx = (bb.maxX - bb.minX) * 0.3D;
            double sy = (bb.maxY - bb.minY) * 0.3D;
            double sz = (bb.maxZ - bb.minZ) * 0.3D;
            CreakingFxMessage.crumble((WorldServer) this.world, c, sx, sy, sz, 100, crumbleWood());
            CreakingFxMessage.crumble((WorldServer) this.world, c, sx, sy, sz, 10, crumbleHeart());
        }
        makeSound(getDeathSound());
        setDead();
    }

    private static IBlockState crumbleWood() {
        if (PaleGardenContent.set != null && PaleGardenContent.set.wood != null) {
            return PaleGardenContent.set.wood.getDefaultState();
        }
        return Blocks.LOG.getDefaultState();
    }

    private static IBlockState crumbleHeart() {
        if (PaleGardenContent.creakingHeart != null) {
            return PaleGardenContent.creakingHeart.getDefaultState().withProperty(BlockCreakingHeart.STATE, BlockCreakingHeart.State.AWAKE);
        }
        return crumbleWood();
    }

    /** {@code creakingDeathEffects}: dies to the damage (kill credit, drops) with the twitch sound. */
    public void creakingDeathEffects(DamageSource source) {
        blameSourceForDamage(source);
        onDeath(source);
        makeSound(CreakingContent.TWITCH);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == EVENT_INVULNERABLE) {
            this.invulnerabilityAnimationRemainingTicks = INVULNERABILITY_ANIMATION_DURATION;
            playHurtSound(DamageSource.GENERIC);
        } else if (id == EVENT_ATTACK) {
            this.attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
            makeSound(CreakingContent.ATTACK);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    /** {@code playerIsStuckInYou}: a player's eyes inside the creaking for more than 4 heart updates. */
    public boolean playerIsStuckInYou() {
        List<EntityPlayer> players = this.brain.nearestPlayers;
        if (players.isEmpty()) {
            this.playerStuckCounter = 0;
            return false;
        }
        AxisAlignedBB bb = getEntityBoundingBox();
        for (EntityPlayer p : players) {
            if (bb.contains(new Vec3d(p.posX, p.posY + p.getEyeHeight(), p.posZ))) {
                this.playerStuckCounter++;
                return this.playerStuckCounter > MAX_PLAYER_STUCK_COUNTER;
            }
        }
        this.playerStuckCounter = 0;
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Freezing

    /** {@code checkCanMove}. */
    public boolean checkCanMove() {
        List<EntityPlayer> players = this.brain.nearestPlayers;
        boolean active = isActive();
        if (players.isEmpty()) {
            if (active) {
                deactivate();
            }
            return true;
        }
        boolean any = false;
        for (EntityPlayer p : players) {
            if (CreakingBrain.canAttack(this, p) && !isOnSameTeam(p)) {
                any = true;
                if ((!active || !wearsDisguise(p)) && isLookingAtMe(p)) {
                    if (active) {
                        return false;
                    }
                    if (p.getDistanceSq(this) < ACTIVATION_RANGE_SQ) {
                        activate(p);
                        return false;
                    }
                }
            }
        }
        if (!any && active) {
            deactivate();
        }
        return true;
    }

    /** {@code #gaze_disguise_equipment}: a carved pumpkin on the head. */
    static boolean wearsDisguise(EntityPlayer p) {
        ItemStack head = p.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        return !head.isEmpty() && head.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN);
    }

    /** {@code isLookingAtMe(player, 0.5, false, true, eyes, feet + 0.5, between)}. */
    public boolean isLookingAtMe(EntityPlayer p) {
        Vec3d look = p.getLook(1.0F).normalize();
        double eyeY = this.posY + getEyeHeight();
        double[] heights = {eyeY, this.posY + 0.5D, (eyeY + this.posY) / 2.0D};
        Vec3d eye = new Vec3d(p.posX, p.posY + p.getEyeHeight(), p.posZ);
        for (double h : heights) {
            Vec3d to = new Vec3d(this.posX - p.posX, h - eye.y, this.posZ - p.posZ).normalize();
            if (look.dotProduct(to) > 1.0D - 0.5D && seesThrough(this.world, eye, new Vec3d(this.posX, h, this.posZ))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code hasLineOfSight} with {@code ClipContext.Block.VISUAL}: 128 blocks at most, and glass, stained glass,
     * panes and iron bars (whose visual shape is empty) let the look through.
     */
    static boolean seesThrough(World world, Vec3d from, Vec3d to) {
        if (from.distanceTo(to) > 128.0D) {
            return false;
        }
        Vec3d start = from;
        for (int i = 0; i < 64; i++) {
            RayTraceResult hit = world.rayTraceBlocks(start, to, false, true, false);
            if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
                return true;
            }
            Block b = world.getBlockState(hit.getBlockPos()).getBlock();
            if (!(b instanceof BlockGlass || b instanceof BlockStainedGlass || b instanceof BlockPane)) {
                return false;
            }
            // Go on from just past the see-through block's far side along the same line.
            Vec3d dir = to.subtract(from).normalize();
            Vec3d past = exit(hit.getBlockPos(), start, dir);
            if (past == null || past.squareDistanceTo(from) >= to.squareDistanceTo(from)) {
                return true;
            }
            start = past.add(dir.scale(1.0E-4D));
        }
        return false;
    }

    /** Where a ray from {@code start} going {@code dir} leaves the unit cube at {@code pos}. */
    @Nullable
    private static Vec3d exit(BlockPos pos, Vec3d start, Vec3d dir) {
        double t = Double.MAX_VALUE;
        double[] o = {start.x, start.y, start.z};
        double[] d = {dir.x, dir.y, dir.z};
        int[] lo = {pos.getX(), pos.getY(), pos.getZ()};
        for (int a = 0; a < 3; a++) {
            if (d[a] > 1.0E-9D) {
                t = Math.min(t, (lo[a] + 1 - o[a]) / d[a]);
            } else if (d[a] < -1.0E-9D) {
                t = Math.min(t, (lo[a] - o[a]) / d[a]);
            }
        }
        return t == Double.MAX_VALUE || t < 0.0D ? null : start.add(dir.scale(t));
    }

    public void activate(EntityPlayer player) {
        this.brain.attackTarget = player;
        makeSound(CreakingContent.ACTIVATE);
        setIsActive(true);
    }

    public void deactivate() {
        this.brain.attackTarget = null;
        makeSound(CreakingContent.DEACTIVATE);
        setIsActive(false);
    }

    public void setIsActive(boolean active) {
        this.dataManager.set(IS_ACTIVE, active);
    }

    public boolean isActive() {
        return this.dataManager.get(IS_ACTIVE);
    }

    @Override
    @Nullable
    public EntityLivingBase getAttackTarget() {
        return this.brain.attackTarget;
    }

    @Override
    public void setAttackTarget(@Nullable EntityLivingBase target) {
        // The brain picks its own targets (players only); other mods' calls are ignored, as vanilla's brain
        // ignores setTarget.
    }

    // ---------------------------------------------------------------------------------------------
    // Data

    public void setHomePos(BlockPos pos) {
        this.dataManager.set(HOME_POS, Optional.of(pos));
        this.isImmuneToFire = true;
    }

    @Nullable
    public BlockPos getHomePos() {
        return this.dataManager.get(HOME_POS).orNull();
    }

    public void setTearingDown() {
        this.dataManager.set(IS_TEARING_DOWN, true);
    }

    public boolean isTearingDown() {
        return this.dataManager.get(IS_TEARING_DOWN);
    }

    public boolean hasGlowingEyes() {
        return this.eyesGlowing;
    }

    /** {@code checkEyeBlink}: the twitching creaking's eyes flicker, slower as it dies. */
    private void checkEyeBlink() {
        if (this.deathTime > this.nextFlickerTime) {
            int lo = this.eyesGlowing ? 2 : this.deathTime / 4;
            int hi = this.eyesGlowing ? 8 : this.deathTime / 2;
            this.nextFlickerTime = this.deathTime + lo + this.rand.nextInt(Math.max(hi - lo, 0) + 1);
            this.eyesGlowing = !this.eyesGlowing;
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("home_pos", 10)) {
            setTransient(NBTUtil.getPosFromTag(tag.getCompoundTag("home_pos")));
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        BlockPos home = getHomePos();
        if (home != null) {
            tag.setTag("home_pos", NBTUtil.createPosTag(home));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds and loot

    void makeSound(@Nullable SoundEvent sound) {
        if (sound != null) {
            playSound(sound, getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return isActive() ? null : CreakingContent.AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isHeartBound() ? CreakingContent.SWAY : SoundEvents.ENTITY_HOSTILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CreakingContent.DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(CreakingContent.STEP, 0.15F, 1.0F);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        return 0.0F;
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new Navigation(this, world);
    }

    @Override
    protected net.minecraft.entity.EntityBodyHelper createBodyHelper() {
        return new BodyHelper(this);
    }

    // ---------------------------------------------------------------------------------------------
    // Controls that stop while frozen

    static final class MoveHelper extends EntityMoveHelper {
        MoveHelper(EntityCreaking c) {
            super(c);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (((EntityCreaking) this.entity).canMove()) {
                super.onUpdateMoveHelper();
            }
        }
    }

    static final class JumpHelper extends EntityJumpHelper {
        private final EntityCreaking creaking;

        JumpHelper(EntityCreaking c) {
            super(c);
            this.creaking = c;
        }

        @Override
        public void doJump() {
            if (this.creaking.canMove()) {
                super.doJump();
            } else {
                this.creaking.setJumping(false);
            }
        }
    }

    static final class LookHelper extends EntityLookHelper {
        private final EntityCreaking creaking;

        LookHelper(EntityCreaking c) {
            super(c);
            this.creaking = c;
        }

        @Override
        public void onUpdateLook() {
            if (this.creaking.canMove()) {
                super.onUpdateLook();
            }
        }
    }

    static final class BodyHelper extends net.minecraft.entity.EntityBodyHelper {
        private final EntityCreaking creaking;

        BodyHelper(EntityCreaking c) {
            super(c);
            this.creaking = c;
        }

        @Override
        public void updateRenderAngles() {
            if (this.creaking.canMove()) {
                super.updateRenderAngles();
            }
        }
    }

    /** {@code CreakingPathNavigation}: stops while frozen; its paths stay within 32 blocks of home. */
    static final class Navigation extends PathNavigateGround {
        Navigation(EntityCreaking c, World world) {
            super(c, world);
        }

        @Override
        public void onUpdateNavigation() {
            if (((EntityCreaking) this.entity).canMove()) {
                super.onUpdateNavigation();
            }
        }

        @Override
        protected PathFinder getPathFinder() {
            this.nodeProcessor = new HomeNodeProcessor();
            this.nodeProcessor.setCanEnterDoors(true);
            return new PathFinder(this.nodeProcessor);
        }
    }

    /** {@code HomeNodeEvaluator}: a node farther than 32 blocks from home, and no nearer than the creaking, is blocked. */
    public static final class HomeNodeProcessor extends WalkNodeProcessor {
        static final double MAX_DISTANCE_TO_HOME_SQ = 1024.0D;

        @Override
        public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z, EntityLiving entity, int xSize, int ySize,
                                            int zSize, boolean canBreakDoors, boolean canEnterDoors) {
            BlockPos home = entity instanceof EntityCreaking ? ((EntityCreaking) entity).getHomePos() : null;
            if (home != null) {
                double d = home.distanceSq(x, y, z);
                if (d > MAX_DISTANCE_TO_HOME_SQ && d >= home.distanceSq(new BlockPos(entity))) {
                    return PathNodeType.BLOCKED;
                }
            }
            return super.getPathNodeType(world, x, y, z, entity, xSize, ySize, zSize, canBreakDoors, canEnterDoors);
        }
    }
}
