package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.creaking.CreakingContent;
import com.exiledradio.trickytrialsbackport.creaking.CreakingHeartHook;
import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;
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
        // Turned away: it moves again and stays active.
        look(p, e.posX, e.posY + 1.5D, e.posZ + 40.0D);
        c.ok(!e.isLookingAtMe(p), "turned away");
        tick(e, 2);
        c.ok(e.canMove(), "moves when no one looks");
        c.ok(e.isActive(), "still active");
        // Unwatched, it walks to the player and hits for 3 every 40 ticks.
        double before = e.getDistance(p);
        // A new server player is invulnerable for 60 ticks until it ticks; this one never ticks.
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0,
                "respawnInvulnerabilityTicks", "field_147101_bU");
        float hp = p.getHealth();
        tick(e, 100);
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
}
