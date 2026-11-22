package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.creaking.CreakingContent;
import com.exiledradio.trickytrialsbackport.creaking.CreakingHeartHook;
import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;
import com.exiledradio.trickytrialsbackport.creaking.client.CreakingPoser;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.PartPose;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;
import com.exiledradio.trickytrialsbackport.palegarden.TileCreakingHeart;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Stream 13: the creaking. */
public final class CreakingSelfTest implements SelfTest {

    private static final String[] SOUNDS = {"entity.creaking.activate", "entity.creaking.ambient", "entity.creaking.attack",
            "entity.creaking.deactivate", "entity.creaking.death", "entity.creaking.freeze", "entity.creaking.spawn",
            "entity.creaking.step", "entity.creaking.sway", "entity.creaking.twitch", "entity.creaking.unfreeze",
            "block.creaking_heart.hurt", "block.creaking_heart.spawn"};

    @Override
    public String name() {
        return "creaking";
    }

    @Override
    public void run(Check c) {
        EnumDifficulty was = c.world.getDifficulty();
        c.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
        long time = c.world.getWorldTime();
        try {
            registration(c);
            standalone(c);
            freezing(c);
            heart(c);
            heartGone(c);
            daybreak(c);
            transplant(c);
            pathing(c);
            animation(c);
            clientSide(c);
        } finally {
            c.world.getWorldInfo().setDifficulty(was);
            c.world.setWorldTime(time);
        }
    }

    private static EntityCreaking spawn(Check c, BlockPos at) {
        EntityCreaking e = new EntityCreaking(c.world);
        e.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        e.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        c.world.spawnEntity(e);
        return e;
    }

    /** A real survival player in the world (fake players are not in the player list), looking at a point. */
    private static EntityPlayerMP player(Check c, String name, BlockPos at) {
        EntityPlayerMP p = Players.real(c.world, name);
        p.setGameType(GameType.SURVIVAL);
        p.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        c.world.spawnEntity(p);
        return p;
    }

    private static void look(EntityPlayerMP p, double x, double y, double z) {
        double dx = x - p.posX;
        double dy = y - (p.posY + p.getEyeHeight());
        double dz = z - p.posZ;
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180D / Math.PI));
        p.rotationYaw = yaw;
        p.rotationYawHead = yaw;
        p.rotationPitch = pitch;
    }

    private static void tick(EntityCreaking e, int n) {
        for (int i = 0; i < n && !e.isDead; i++) {
            e.onUpdate();
        }
    }

    private void registration(Check c) {
        BlockPos area = c.area();
        EntityCreaking e = spawn(c, area.add(16, 0, 16));
        c.eq(Content.id("creaking"), EntityList.getKey(e), "creaking entity name");
        c.eq(EntityCreaking.class, EntityList.getClass(Content.id("creaking")), "registered class");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("creaking")), "creaking has a spawn egg");
        c.eq(0x5F5F5F, EntityList.ENTITY_EGGS.get(Content.id("creaking")).primaryColor, "egg gray");
        c.eq(0xFC7812, EntityList.ENTITY_EGGS.get(Content.id("creaking")).secondaryColor, "egg orange");
        c.eq(1.0D, e.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "1 health");
        c.eq((double) 0.4F, e.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue(), "speed 0.4F");
        c.eq(3.0D, e.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "3 damage");
        c.eq(32.0D, e.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getBaseValue(), "follow range 32");
        c.eq(0.9F, e.width, "width 0.9");
        c.eq(2.7F, e.height, "height 2.7");
        c.eq(2.3F, e.getEyeHeight(), "eyes at 2.3");
        c.eq(1.0625F, e.stepHeight, "step height 1.0625");
        c.ok(e.canMove(), "can move at first");
        c.ok(!e.isActive(), "not active at first");
        c.ok(!e.isHeartBound(), "a creaking from nowhere has no heart");
        for (String s : SOUNDS) {
            SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(Content.id(s));
            c.ok(ev != null, "sound " + s);
        }
        c.ok(LootTableList.getAll().contains(EntityCreaking.LOOT), "loot table registered");
        c.ok(net.minecraft.advancements.CriteriaTriggers.get(Content.id("heart_transplanted")) != null, "Heart Transplanter trigger");
        e.setDead();
    }

    private void standalone(Check c) {
        BlockPos area = c.area();
        EntityCreaking e = spawn(c, area.add(16, 0, 16));
        c.ok(!e.isImmuneToFire(), "a creaking without a heart burns");
        EntityPlayerMP p = player(c, "standalone", area.add(16, 0, 10));
        c.ok(e.attackEntityFrom(DamageSource.causePlayerDamage(p), 1.0F), "a creaking without a heart is hurt");
        c.ok(e.getHealth() <= 0.0F, "and dies to one hit");
        c.world.removeEntityDangerously(p);
        e.setDead();
    }

    private void freezing(Check c) {
        BlockPos area = c.area();
        EntityCreaking e = spawn(c, area.add(16, 0, 16));
        // A player 6 blocks south, looking at it: the sensor runs within 20 ticks; then it freezes and wakes.
        EntityPlayerMP p = player(c, "watcher", area.add(16, 0, 22));
        look(p, e.posX, e.posY + 1.5D, e.posZ);
        c.ok(e.isLookingAtMe(p), "the player looks at the creaking");
        tick(e, 25);
        c.ok(!e.canMove(), "frozen while looked at");
        c.ok(e.isActive(), "woken by a look within 12 blocks");
        c.eq(p, e.getAttackTarget(), "targets the player who woke it");
        // Frozen completely: the head, body and walk stay put while the player moves around it, still watching.
        float headYaw = e.rotationYawHead;
        float headPitch = e.rotationPitch;
        float bodyYaw = e.renderYawOffset;
        double fx = e.posX;
        double fz = e.posZ;
        for (int i = 0; i < 10; i++) {
            p.setPosition(p.posX + 0.3D, p.posY, p.posZ);
            look(p, e.posX, e.posY + 1.5D, e.posZ);
            e.onUpdate();
        }
        c.ok(!e.canMove(), "still frozen while watched");
        c.eq(headYaw, e.rotationYawHead, "frozen: the head does not turn");
        c.eq(headPitch, e.rotationPitch, "frozen: the head does not tilt");
        c.eq(bodyYaw, e.renderYawOffset, "frozen: the body does not turn");
        c.ok(Math.abs(e.posX - fx) < 1.0E-6D && Math.abs(e.posZ - fz) < 1.0E-6D, "frozen: it does not move");
        c.ok(e.limbSwingAmount < 0.01F, "frozen: the walk has stopped (" + e.limbSwingAmount + ")");
        p.setPosition(area.getX() + 16.5D, area.getY(), area.getZ() + 22.5D);
        // Turned away: it moves again and stays active.
        look(p, e.posX, e.posY + 1.5D, e.posZ + 40.0D);
        c.ok(!e.isLookingAtMe(p), "turned away");
        tick(e, 2);
        c.ok(e.canMove(), "moves when no one looks");
        // It resumes smoothly: the walk speed eases up by 1.21's 0.4 lerp, never jumping to full.
        float eased = e.limbSwingAmount;
        c.ok(eased < 1.5F, "the walk eases back in (" + eased + " after 2 ticks)");
        c.ok(e.isActive(), "still active");
        // Unwatched, it walks to the player and hits for 3 every 40 ticks.
        double before = e.getDistance(p);
        // A new server player is invulnerable for 60 ticks until it ticks; this one never ticks.
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0,
                "respawnInvulnerabilityTicks", "field_147101_bU");
        float hp = p.getHealth();
        // The walk feed, tick by tick: 1.21's updateWalkAnimation (speed toward min(25 d, 3) by 0.4, position += speed).
        int feedBad = 0;
        float maxAmount = 0.0F;
        double maxStep = 0.0D;
        for (int i = 0; i < 100 && !e.isDead; i++) {
            float amount0 = e.limbSwingAmount;
            float swing0 = e.limbSwing;
            e.onUpdate();
            double dx = e.posX - e.prevPosX;
            double dz = e.posZ - e.prevPosZ;
            float want = amount0 + (Math.min(MathHelper.sqrt(dx * dx + dz * dz) * 25.0F, 3.0F) - amount0) * 0.4F;
            if (Math.abs(e.limbSwingAmount - want) > 1.0E-4F || Math.abs(e.limbSwing - (swing0 + want)) > 1.0E-3F) {
                feedBad++;
            }
            maxAmount = Math.max(maxAmount, e.limbSwingAmount);
            maxStep = Math.max(maxStep, Math.sqrt(dx * dx + dz * dz));
        }
        com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.LOGGER.info("SELFTEST creaking walk feed: top speed " + maxAmount + ", top step " + maxStep);
        c.eq(0, feedBad, "the walk speed and position follow 1.21's creaking formula every tick");
        c.ok(maxAmount > 1.0F, "a chasing creaking's walk runs at full strength (" + maxAmount + ")");
        c.ok(e.getDistance(p) < before - 2.0D, "it closes in while unwatched (" + before + " -> " + e.getDistance(p) + ")");
        c.ok(hp - p.getHealth() >= 3.0F - 0.001F, "and hits for 3 (" + (hp - p.getHealth()) + ")");
        p.setHealth(p.getMaxHealth());
        p.hurtResistantTime = 0;
        e.setPosition(area.getX() + 16.5D, area.getY(), area.getZ() + 16.5D);
        // Looking through glass still freezes it; through stone it does not.
        look(p, e.posX, e.posY + 1.5D, e.posZ);
        c.fill(area.add(13, 0, 19), area.add(19, 5, 19), Blocks.GLASS.getDefaultState());
        tick(e, 2);
        c.ok(!e.canMove(), "glass does not hide it");
        c.fill(area.add(13, 0, 19), area.add(19, 5, 19), Blocks.STONE.getDefaultState());
        tick(e, 2);
        c.ok(e.canMove(), "stone hides it");
        c.fill(area.add(13, 0, 19), area.add(19, 5, 19), Blocks.AIR.getDefaultState());
        // A carved pumpkin: an active creaking ignores the look.
        p.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Blocks.PUMPKIN));
        tick(e, 2);
        c.ok(e.canMove(), "a carved pumpkin keeps an active creaking moving");
        p.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
        tick(e, 2);
        c.ok(!e.canMove(), "frozen again without the pumpkin");
        // Frozen: no knockback, no pushing.
        e.motionX = 0.0D;
        e.knockBack(p, 1.0F, 1.0D, 0.0D);
        c.eq(0.0D, e.motionX, "no knockback while frozen");
        c.ok(!e.canBePushed(), "not pushable while frozen");
        // A creative player is not a target: it goes back to sleep.
        p.setGameType(GameType.CREATIVE);
        tick(e, 25);
        c.ok(e.canMove(), "creative players do not freeze it");
        c.ok(!e.isActive(), "and it deactivates");
        c.world.removeEntityDangerously(p);
        e.setDead();
    }

    /** A heart between two logs; @return its position */
    private static BlockPos heartAt(Check c, BlockPos area) {
        BlockPos at = area.add(16, 6, 16);
        Block log = PaleGardenContent.set.log;
        c.world.setBlockState(at.up(), log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y));
        c.world.setBlockState(at.down(), log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y));
        c.world.setBlockState(at, PaleGardenContent.creakingHeart.getDefaultState());
        PaleGardenContent.creakingHeart.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        return at;
    }

    private static void tickHeart(Check c, BlockPos at, int n) {
        for (int i = 0; i < n; i++) {
            TileEntity t = c.world.getTileEntity(at);
            if (t instanceof TileCreakingHeart) {
                ((TileCreakingHeart) t).update();
            }
        }
    }

    private void heart(Check c) {
        BlockPos area = c.area();
        c.world.setWorldTime(14000L);
        BlockPos at = heartAt(c, area);
        // No player in 32 blocks: nothing.
        tickHeart(c, at, 60);
        c.ok(CreakingHeartHook.creaking(c.world.getTileEntity(at)) == null, "no creaking without a player near");
        EntityPlayerMP p = player(c, "hearty", area.add(4, 0, 4));
        tickHeart(c, at, 60);
        c.eq(BlockCreakingHeart.State.AWAKE, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "awake at night");
        EntityCreaking e = CreakingHeartHook.creaking(c.world.getTileEntity(at));
        if (!c.ok(e != null, "an awake heart spawns a creaking with a player near")) {
            c.world.removeEntityDangerously(p);
            return;
        }
        c.eq(at, e.getHomePos(), "the creaking's home is the heart");
        c.ok(e.isImmuneToFire(), "a heart's creaking does not burn");
        c.ok(Math.abs(e.posX - at.getX()) <= 17 && Math.abs(e.posZ - at.getZ()) <= 17, "within 16 blocks across");
        c.eq(100.0D, e.posY, "stands on the floor");
        double d = Math.sqrt(e.getDistanceSq(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D));
        tickHeart(c, at, 1);
        c.eq(15 - MathHelper.floor(Math.min(d, 32.0D) / 32.0D * 15.0D),
                c.world.getBlockState(at).getComparatorInputOverride(c.world, at), "comparator by distance");
        tickHeart(c, at, 60);
        c.eq(e, CreakingHeartHook.creaking(c.world.getTileEntity(at)), "only one creaking per heart");

        // Saved and loaded: the link and home survive.
        NBTTagCompound tag = new NBTTagCompound();
        e.writeToNBT(tag);
        EntityCreaking copy = new EntityCreaking(c.world);
        copy.readFromNBT(tag);
        c.eq(at, copy.getHomePos(), "home_pos saved");
        NBTTagCompound heartTag = c.world.getTileEntity(at).writeToNBT(new NBTTagCompound());
        c.ok(heartTag.getCompoundTag("Creaking").hasUniqueId("creaking"), "the heart saves the creaking's UUID");
        c.eq(e.getUniqueID(), heartTag.getCompoundTag("Creaking").getUniqueId("creaking"), "the right UUID");

        // Invulnerable: a player's hit flashes, sways and calls the heart.
        float health = e.getHealth();
        c.ok(e.attackEntityFrom(DamageSource.causePlayerDamage(p), 10.0F), "a player's hit counts as a hit");
        c.eq(health, e.getHealth(), "but does no damage");
        c.eq(100, CreakingHeartHook.emitter(c.world.getTileEntity(at)), "the heart sends its trails for 100 ticks");
        c.ok(!e.attackEntityFrom(DamageSource.causePlayerDamage(p), 10.0F), "no second hit within 8 ticks");
        tick(e, 9);
        c.ok(!e.attackEntityFrom(DamageSource.IN_FIRE, 10.0F), "fire is not a hit");
        c.ok(!e.attackEntityFrom(DamageSource.LAVA, 10.0F), "lava is not a hit");
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(e.posX + 1, e.posY, e.posZ);
        c.ok(e.attackEntityFrom(DamageSource.causeMobDamage(zombie), 10.0F), "a mob's hit flashes too");
        c.eq(health, e.getHealth(), "and does no damage");
        tickHeart(c, at, 120);

        // Breaking the heart: the creaking twitches 45 ticks, then crumbles.
        p.setPosition(at.getX() + 0.5D, at.getY() - 5, at.getZ() + 2.5D);
        p.interactionManager.tryHarvestBlock(at);
        c.eq(Blocks.AIR, c.world.getBlockState(at).getBlock(), "heart broken");
        c.ok(e.isTearingDown(), "the creaking twitches");
        c.ok(e.getHealth() <= 0.0F, "dying");
        tick(e, 40);
        c.ok(!e.isDead, "still twitching after 40 ticks");
        tick(e, 10);
        c.ok(e.isDead, "gone after 45 ticks");
        c.world.removeEntityDangerously(p);
        c.world.setWorldTime(1000L);
    }

    private void heartGone(Check c) {
        BlockPos area = c.area();
        c.world.setWorldTime(14000L);
        BlockPos at = heartAt(c, area);
        EntityPlayerMP p = player(c, "gone", area.add(4, 0, 4));
        tickHeart(c, at, 60);
        EntityCreaking e = CreakingHeartHook.creaking(c.world.getTileEntity(at));
        if (c.ok(e != null, "spawned for the removal test")) {
            c.world.setBlockToAir(at);
            tick(e, 1);
            c.ok(e.getHealth() <= 0.0F, "a creaking whose heart is gone dies");
            c.ok(!e.isTearingDown(), "without the twitch when nobody broke the heart");
            c.ok(e.attackEntityFrom(DamageSource.OUT_OF_WORLD, 1000.0F) || e.getHealth() <= 0.0F, "the void still works");
        }
        c.world.removeEntityDangerously(p);
        c.world.setWorldTime(1000L);
    }

    private void daybreak(Check c) {
        BlockPos area = c.area();
        c.world.setWorldTime(14000L);
        BlockPos at = heartAt(c, area);
        EntityPlayerMP p = player(c, "dawn", area.add(4, 0, 4));
        tickHeart(c, at, 60);
        EntityCreaking e = CreakingHeartHook.creaking(c.world.getTileEntity(at));
        if (c.ok(e != null, "spawned for the dawn test")) {
            c.world.setWorldTime(1000L);
            tickHeart(c, at, 60);
            c.eq(BlockCreakingHeart.State.DORMANT, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "dormant by day");
            c.ok(e.isDead, "day crumbles the creaking");
            c.ok(CreakingHeartHook.creaking(c.world.getTileEntity(at)) == null, "and unlinks it");
            tickHeart(c, at, 60);
            c.ok(CreakingHeartHook.creaking(c.world.getTileEntity(at)) == null, "no new creaking by day");
        }
        c.world.removeEntityDangerously(p);
    }

    private void transplant(Check c) {
        BlockPos area = c.area();
        Block log = PaleGardenContent.set.log;
        BlockPos at = area.add(8, 4, 8);
        c.world.setWorldTime(1000L);
        c.world.setBlockState(at.east(), log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        c.world.setBlockState(at.west(), log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        c.ok(!CreakingContent.transplants(c.world, at, PaleGardenContent.creakingHeart.getDefaultState()), "an uprooted heart does not count");
        c.ok(CreakingContent.transplants(c.world, at, PaleGardenContent.creakingHeart.getDefaultState()
                .withProperty(BlockCreakingHeart.AXIS, EnumFacing.Axis.X).withProperty(BlockCreakingHeart.STATE, BlockCreakingHeart.State.DORMANT)),
                "a dormant heart counts");
        c.world.setBlockState(at, PaleGardenContent.creakingHeart.getDefaultState().withProperty(BlockCreakingHeart.AXIS, EnumFacing.Axis.X));
        c.ok(CreakingContent.transplants(c.world, at.east(), c.world.getBlockState(at.east())), "the log that completes a heart counts");
        c.world.setBlockState(at, PaleGardenContent.creakingHeart.getDefaultState().withProperty(BlockCreakingHeart.AXIS, EnumFacing.Axis.Y));
        c.ok(!CreakingContent.transplants(c.world, at.east(), c.world.getBlockState(at.east())), "not across a heart of another axis");
    }

    private void pathing(Check c) {
        BlockPos area = c.area();
        EntityCreaking e = spawn(c, area.add(16, 0, 16));
        e.setTransient(area.add(16, 0, 16));
        c.eq(8.0F, e.getPathPriority(PathNodeType.LAVA), "lava costs 8");
        c.eq(0.0F, e.getPathPriority(PathNodeType.DANGER_FIRE), "fire is no danger");
        EntityCreaking.HomeNodeProcessor nodes = new EntityCreaking.HomeNodeProcessor();
        BlockPos near = area.add(20, 0, 16);
        BlockPos far = area.add(16 + 33, 0, 16);
        c.ok(nodes.getPathNodeType(c.world, near.getX(), near.getY(), near.getZ(), e, 1, 3, 1, false, true) != PathNodeType.BLOCKED,
                "a node near home is open");
        c.eq(PathNodeType.BLOCKED, nodes.getPathNodeType(c.world, far.getX(), far.getY(), far.getZ(), e, 1, 3, 1, false, true),
                "a node over 32 blocks from home, farther than the creaking, is blocked");
        e.setPosition(far.getX() + 2.5D, far.getY(), far.getZ() + 0.5D);
        c.ok(nodes.getPathNodeType(c.world, far.getX(), far.getY(), far.getZ(), e, 1, 3, 1, false, true) != PathNodeType.BLOCKED,
                "but not when it brings a lost creaking nearer home");
        e.setDead();
    }

    private static final float DEG = (float) (Math.PI / 180.0D);
    private static final int OFF = Integer.MIN_VALUE;

    /** One part's angles (degrees) against vanilla's, within 0.01 degree. */
    private static void angles(Check c, CreakingPoser p, String part, float x, float y, float z, String what) {
        PartPose q = p.part(part);
        boolean ok = Math.abs(q.xRot - x * DEG) < 2.0E-4F && Math.abs(q.yRot - y * DEG) < 2.0E-4F && Math.abs(q.zRot - z * DEG) < 2.0E-4F;
        c.ok(ok, what + " (" + part + " " + q.xRot / DEG + ", " + q.yRot / DEG + ", " + q.zRot / DEG + " against " + x + ", " + y + ", " + z + ")");
    }

    private static void pivot(Check c, CreakingPoser p, String part, float x, float y, float z, String what) {
        PartPose q = p.part(part);
        boolean ok = Math.abs(q.x - x) < 1.0E-3F && Math.abs(q.y - y) < 1.0E-3F && Math.abs(q.z - z) < 1.0E-3F;
        c.ok(ok, what + " (" + part + " at " + q.x + ", " + q.y + ", " + q.z + " against " + x + ", " + y + ", " + z + ")");
    }

    /** The walk position that is this many seconds into the cycle (applyWalk: 50 ms per unit). */
    private static float at(float seconds) {
        return seconds * 1000.0F / 50.0F;
    }

    /**
     * The model's pose math against 1.21.11's {@code CreakingModel} and {@code CreakingAnimation}, with the expected
     * numbers typed in from the decompiled source (not from our converted copy): pivots, keyframes at their
     * timestamps, linear interpolation between them, the walk's speed scaling and looping, the head, freezing, and
     * the attack, invulnerable and death animations from their start ticks.
     */
    private void animation(Check c) {
        CreakingPoser p = new CreakingPoser();
        // Rest pose: createMesh's pivots.
        p.pose(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        pivot(c, p, "root", 0.0F, 24.0F, 0.0F, "rest pivot");
        pivot(c, p, "upper_body", -1.0F, -19.0F, 0.0F, "rest pivot");
        pivot(c, p, "head", -3.0F, -11.0F, 0.0F, "rest pivot");
        pivot(c, p, "body", 0.0F, -7.0F, 1.0F, "rest pivot");
        pivot(c, p, "right_arm", -7.0F, -9.5F, 1.5F, "rest pivot");
        pivot(c, p, "left_arm", 6.0F, -9.0F, 0.5F, "rest pivot");
        pivot(c, p, "left_leg", 1.5F, -16.0F, 0.5F, "rest pivot");
        pivot(c, p, "right_leg", -1.0F, -17.5F, 0.5F, "rest pivot");
        angles(c, p, "upper_body", 0.0F, 0.0F, 0.0F, "no walk at speed 0");

        // Walk at full speed, on keyframes.
        p.pose(at(0.0F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "upper_body", 26.8802F, -23.399F, -9.0616F, "walk 0 s");
        angles(c, p, "right_arm", 12.5F, 0.0F, 0.0F, "walk 0 s");
        pivot(c, p, "left_leg", 1.5F, -16.0F, 0.5F + 2.0F, "walk 0 s: posVec(0, 0, 2)");
        p.pose(at(0.125F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "upper_body", -2.2093F, 5.9119F, 0.0675F, "walk 0.125 s");
        p.pose(at(0.25F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "right_arm", -32.0F, 0.0F, 0.0F, "walk 0.25 s");
        // posVec negates y: posVec(0, 0.1846, 0.5979) moves the pivot up.
        pivot(c, p, "left_leg", 1.5F, -16.0F - 0.1846F, 0.5F + 0.5979F, "walk 0.25 s: posVec(0, 0.1846, 0.5979)");
        p.pose(at(0.875F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "upper_body", 7.5F, 0.0F, 0.0F, "walk 0.875 s");
        angles(c, p, "right_arm", 12.0F, 0.0F, 0.0F, "walk 0.875 s");
        // Linear between keyframes: right arm 0.25 s (-32) to 0.875 s (12), at 0.5 s 40% of the way.
        p.pose(at(0.5F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "right_arm", -32.0F + 0.4F * (12.0F + 32.0F), 0.0F, 0.0F, "walk 0.5 s, linear");
        // Looping: 1.125 s further on is the same pose.
        p.pose(at(0.5F + 1.125F), 1.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "right_arm", -32.0F + 0.4F * (12.0F + 32.0F), 0.0F, 0.0F, "walk loops every 1.125 s");
        // Speed scales every channel; above 1 it is capped.
        p.pose(at(0.0F), 0.5F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "upper_body", 26.8802F * 0.5F, -23.399F * 0.5F, -9.0616F * 0.5F, "walk at half speed");
        pivot(c, p, "left_leg", 1.5F, -16.0F, 0.5F + 1.0F, "walk at half speed, position too");
        p.pose(at(0.0F), 3.0F, 0.0F, 0.0F, 0.0F, true, OFF, OFF, OFF);
        angles(c, p, "upper_body", 26.8802F, -23.399F, -9.0616F, "walk speed capped at 1");

        // The head follows the look, and the walk adds to it.
        p.pose(at(0.0F), 0.0F, 0.0F, 30.0F, -20.0F, true, OFF, OFF, OFF);
        angles(c, p, "head", -20.0F, 30.0F, 0.0F, "head pitch and yaw");
        // Walk positions become whole milliseconds (applyWalk's long), so 0.0417 s is 41 ms: 41/41.7 of the twitch.
        p.pose(at(0.0417F), 1.0F, 0.0F, 30.0F, -20.0F, true, OFF, OFF, OFF);
        float f = 0.041F / 0.0417F;
        angles(c, p, "head", -20.0F - 17.5F * f, 30.0F - 62.5F * f, 0.0F, "head twitch at 41 ms on top of the look");

        // Frozen: no walk at all, whatever the walk position and speed.
        p.pose(at(0.25F), 1.0F, 0.0F, 0.0F, 0.0F, false, OFF, OFF, OFF);
        angles(c, p, "right_arm", 0.0F, 0.0F, 0.0F, "frozen: no walk");
        pivot(c, p, "left_leg", 1.5F, -16.0F, 0.5F, "frozen: legs at rest");

        // Death, from its start tick (whole ticks, 50 ms each): head keyframes at 0.5 s and 1.0 s.
        p.pose(0.0F, 0.0F, 110.0F, 0.0F, 0.0F, false, OFF, OFF, 100);
        angles(c, p, "head", 2.5F, 0.0F, 0.0F, "death 0.5 s");
        p.pose(0.0F, 0.0F, 120.0F, 0.0F, 0.0F, false, OFF, OFF, 100);
        angles(c, p, "head", 10.773F, -29.5608F, -5.3627F, "death 1.0 s");
        // Death does not loop: past 2.25 s it holds its last keyframe.
        p.pose(0.0F, 0.0F, 160.0F, 0.0F, 0.0F, false, OFF, OFF, 100);
        angles(c, p, "head", 10.0F, 0.0F, 0.0F, "death holds after 2.25 s");
        // Attack: the head stretches to 1.3 high at 0.5 s.
        p.pose(0.0F, 0.0F, 60.0F, 0.0F, 0.0F, true, 50, OFF, OFF);
        c.ok(Math.abs(p.part("head").yScale - 1.3F) < 1.0E-4F, "attack 0.5 s: head 1.3 high (" + p.part("head").yScale + ")");
        c.ok(Math.abs(p.part("head").xScale - 1.0F) < 1.0E-4F, "attack 0.5 s: head as wide");
        // Invulnerable: upper body -5 at 0.0833 s to 5 at 0.1667 s; at 0.1 s (2 ticks) 20% of the way.
        p.pose(0.0F, 0.0F, 52.0F, 0.0F, 0.0F, true, OFF, 50, OFF);
        angles(c, p, "upper_body", -5.0F + 10.0F * (0.1F - 0.0833F) / (0.1667F - 0.0833F), 0.0F, 0.0F, "invulnerable 0.1 s, linear");
        // Not started: nothing.
        p.pose(0.0F, 0.0F, 52.0F, 0.0F, 0.0F, false, OFF, OFF, OFF);
        angles(c, p, "upper_body", 0.0F, 0.0F, 0.0F, "no animation without a start");
    }

    /** A client world for one test: isRemote, reading the server's chunks (so blocks are there and nothing generates). */
    private static final class ClientWorld extends net.minecraft.world.World {
        private final net.minecraft.world.WorldServer real;

        ClientWorld(net.minecraft.world.WorldServer real) {
            super(real.getSaveHandler(), real.getWorldInfo(), real.provider, real.profiler, true);
            this.real = real;
            this.chunkProvider = real.getChunkProvider();
        }

        @Override
        protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() {
            return this.real.getChunkProvider();
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return this.real.getChunkProvider().chunkExists(x, z);
        }
    }

    /** The client's copy of a creaking, moved as 1.12.2's client moves it (setPositionAndRotationDirect, 3 steps). */
    private static final class ClientCreaking extends EntityCreaking {
        ClientCreaking(net.minecraft.world.World world) {
            super(world);
        }

        void interpolateTo(double x, double y, double z, float yaw, float pitch) {
            this.interpTargetX = x;
            this.interpTargetY = y;
            this.interpTargetZ = z;
            this.interpTargetYaw = yaw;
            this.interpTargetPitch = pitch;
            this.newPosRotationIncrements = 3;
        }
    }

    /** The tracker's entity data packet: every entry of the server copy onto the client copy. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sync(EntityCreaking from, EntityCreaking to) {
        for (net.minecraft.network.datasync.EntityDataManager.DataEntry entry : from.getDataManager().getAll()) {
            to.getDataManager().set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The client side of the walk and the freeze, on a client world (isRemote) beside the server's: the server copy
     * chases a player; every 3 ticks the client copy gets its position (offset into a second test area) and its
     * entity data, as the tracker sends them, and ticks as a client entity. The walk must run on the client from the
     * client's own positions, the client must take "frozen" only from the synced data, and the model fed as
     * RenderLivingBase feeds it must swing its legs while the server copy moves.
     */
    private void clientSide(Check c) {
        BlockPos area = c.area();
        BlockPos mirror = c.area();
        double ox = mirror.getX() - area.getX();
        double oz = mirror.getZ() - area.getZ();
        EntityCreaking e = spawn(c, area.add(16, 0, 16));
        EntityPlayerMP p = player(c, "clientwatch", area.add(16, 0, 26));
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0,
                "respawnInvulnerabilityTicks", "field_147101_bU");
        ClientWorld cw = new ClientWorld(c.world);
        c.ok(cw.isRemote, "the client world is a client world");
        ClientCreaking client = new ClientCreaking(cw);
        client.setLocationAndAngles(e.posX + ox, e.posY, e.posZ + oz, e.rotationYaw, e.rotationPitch);
        sync(e, client);

        // Wake it with a look, then turn away: the server copy chases.
        look(p, e.posX, e.posY + 1.5D, e.posZ);
        tick(e, 25);
        c.ok(e.isActive() && !e.canMove(), "server copy woken and frozen");
        look(p, e.posX, e.posY + 1.5D, e.posZ + 40.0D);

        CreakingPoser poser = new CreakingPoser();
        int feedBad = 0;
        int canMoveBad = 0;
        int clientWrites = 0;
        int swinging = 0;
        float maxAmount = 0.0F;
        for (int i = 0; i < 60; i++) {
            e.onUpdate();
            if (i % 3 == 0) {
                client.interpolateTo(e.posX + ox, e.posY, e.posZ + oz, e.rotationYaw, e.rotationPitch);
                sync(e, client);
            }
            boolean before = client.canMove();
            float amount0 = client.limbSwingAmount;
            float swing0 = client.limbSwing;
            client.onUpdate();
            if (client.canMove() != before) {
                clientWrites++;
            }
            if (client.canMove() != e.canMove()) {
                canMoveBad++;
            }
            double dx = client.posX - client.prevPosX;
            double dz = client.posZ - client.prevPosZ;
            float want = amount0 + (Math.min(MathHelper.sqrt(dx * dx + dz * dz) * 25.0F, 3.0F) - amount0) * 0.4F;
            if (Math.abs(client.limbSwingAmount - want) > 1.0E-4F || Math.abs(client.limbSwing - (swing0 + want)) > 1.0E-3F) {
                feedBad++;
            }
            maxAmount = Math.max(maxAmount, client.limbSwingAmount);
            // RenderLivingBase at half a tick: amount lerped and capped at 1, swing behind by half the amount.
            float f5 = Math.min(client.prevLimbSwingAmount + (client.limbSwingAmount - client.prevLimbSwingAmount) * 0.5F, 1.0F);
            float f6 = client.limbSwing - client.limbSwingAmount * 0.5F;
            poser.pose(f6, f5, client.ticksExisted + 0.5F, 0.0F, 0.0F, client.canMove(),
                    client.attackAnimationStart, client.invulnerabilityAnimationStart, client.deathAnimationStart);
            if (Math.abs(poser.part("left_leg").xRot) > 10.0F * DEG || Math.abs(poser.part("right_leg").xRot) > 10.0F * DEG) {
                swinging++;
            }
        }
        com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.LOGGER.info("SELFTEST creaking client copy: top walk speed "
                + maxAmount + ", legs swung past 10 degrees in " + swinging + " of 60 ticks");
        c.ok(e.canMove(), "server copy moving while unwatched");
        c.eq(0, feedBad, "the client copy's walk follows 1.21's formula from its own positions every tick");
        c.ok(maxAmount > 1.0F, "the client copy walks at full strength (" + maxAmount + ")");
        c.eq(0, clientWrites, "the client never decides the freeze itself");
        c.ok(canMoveBad <= 3, "the client's can-move is the server's, within one data update (" + canMoveBad + " ticks behind)");
        c.ok(swinging >= 20, "the model's legs swing while it moves (" + swinging + " of 60 ticks past 10 degrees)");

        // Watched again: the server freezes it, the client copy follows and draws no walk.
        look(p, e.posX, e.posY + 1.5D, e.posZ);
        for (int i = 0; i < 12; i++) {
            e.onUpdate();
            if (i % 3 == 0) {
                client.interpolateTo(e.posX + ox, e.posY, e.posZ + oz, e.rotationYaw, e.rotationPitch);
                sync(e, client);
            }
            client.onUpdate();
        }
        c.ok(!e.canMove() && !client.canMove(), "frozen on both sides");
        poser.pose(client.limbSwing, Math.min(client.limbSwingAmount, 1.0F), client.ticksExisted, 0.0F, 0.0F, client.canMove(),
                OFF, OFF, OFF);
        angles(c, poser, "left_leg", 0.0F, 0.0F, 0.0F, "a frozen client copy stands still");
        c.world.removeEntityDangerously(p);
        e.setDead();
    }
}
