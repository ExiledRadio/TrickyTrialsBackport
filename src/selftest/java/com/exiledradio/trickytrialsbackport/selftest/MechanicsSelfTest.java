package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.asm.MechanicsTransformer;
import com.exiledradio.trickytrialsbackport.tweaks.EnderPearlChunks;
import com.exiledradio.trickytrialsbackport.tweaks.GameRuleTweaks;
import com.exiledradio.trickytrialsbackport.tweaks.Leads;
import com.exiledradio.trickytrialsbackport.tweaks.MechanicsHooks;
import com.exiledradio.trickytrialsbackport.tweaks.PortalTravel;
import com.exiledradio.trickytrialsbackport.tweaks.Saddles;
import com.exiledradio.trickytrialsbackport.tweaks.SmallTweaks;
import com.exiledradio.trickytrialsbackport.tweaks.SpawnChunks;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Field;

/**
 * Stream 25's behavior changes from the 2026-11-16 audit: every patch applied and each change checked with its
 * setting on, and off where the old behavior is observable.
 */
public final class MechanicsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        for (String p : new String[] {"P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12", "P13", "P14"}) {
            c.ok(MechanicsTransformer.APPLIED.contains(p), "patch " + p + " applied");
        }
        boolean tt = net.minecraftforge.fml.common.Loader.isModLoaded("trailsandtalesbackport");
        if (tt) {
            c.ok(!MechanicsTransformer.APPLIED.contains("P15") && !MechanicsTransformer.APPLIED.contains("P15b"),
                    "with Trails & Tales, P15 stands aside (its P10 patches the spawn chunk sites)");
            c.eq(0, Class.forName("com.exiledradio.trailsandtalesbackport.mechanics.SpawnChunks").getField("radius").getInt(null),
                    "and its spawn chunk radius is 0 (1.21.9)");
        } else {
            c.ok(MechanicsTransformer.APPLIED.contains("P15") && MechanicsTransformer.APPLIED.contains("P15b"), "patch P15 applied");
        }
        cooldowns(c);
        ridersAndTnt(c);
        pearls(c);
        saddles(c);
        leads(c);
        knockback(c);
        smallChanges(c);
        blocks(c);
        gameRules(c);
    }

    private static void cooldowns(Check c) {
        WorldServer w = c.world;
        c.eq(10, new EntityBoat(w).getPortalCooldown(), "P5: a boat's portal cooldown is 10 ticks (1.21.2)");
        c.eq(10, new EntityMinecartEmpty(w).getPortalCooldown(), "P5: a minecart's is 10 ticks");
        c.eq(2, new EntityTippedArrow(w).getPortalCooldown(), "P5: a projectile's is 2 ticks");
        c.eq(300, new EntityPig(w).getPortalCooldown(), "P5: a pig's stays 300");
        ModConfig.tweaks.portalCooldowns = false;
        c.eq(300, new EntityBoat(w).getPortalCooldown(), "P5 off: a boat's is 1.12.2's 300");
        ModConfig.tweaks.portalCooldowns = true;
    }

    private static void ridersAndTnt(Check c) {
        WorldServer w = c.world;
        BlockPos a = c.area();
        // End portal: a pig with a zombie riding it.
        BlockPos end = a.add(4, 0, 4);
        c.fill(end.add(-1, 0, -1), end.add(1, 0, 1), Blocks.END_PORTAL.getDefaultState());
        EntityPig pig = new EntityPig(w);
        pig.setLocationAndAngles(end.getX() + 0.5, end.getY(), end.getZ() + 0.5, 0, 0);
        w.spawnEntity(pig);
        EntityZombie rider = new EntityZombie(w);
        rider.setLocationAndAngles(pig.posX, pig.posY + 1, pig.posZ, 0, 0);
        w.spawnEntity(rider);
        rider.startRiding(pig, true);
        PortalTravel.tick(w, true, false);
        c.ok(pig.isDead && rider.isDead, "a pig and its rider in an end portal both left the overworld");
        WorldServer theEnd = w.getMinecraftServer().getWorld(1);
        EntityPig there = null;
        for (EntityPig p : theEnd.getEntities(EntityPig.class, x -> x != null && x.getUniqueID().equals(pig.getUniqueID()))) {
            there = p;
        }
        c.ok(there != null, "the pig is in the End");
        c.ok(there != null && there.getPassengers().size() == 1 && there.getPassengers().get(0) instanceof EntityZombie,
                "and the zombie rides it there (1.21)");
        if (there != null) {
            there.removePassengers();
            for (Entity e : theEnd.getEntitiesWithinAABB(Entity.class, there.getEntityBoundingBox().grow(4))) {
                if (!(e instanceof EntityPlayerMP)) {
                    e.setDead();
                }
            }
        }
        c.fill(end.add(-1, 0, -1), end.add(1, 0, 1), Blocks.AIR.getDefaultState());

        // Nether portal: primed TNT goes through and carries the mark.
        BlockPos portal = a.add(12, 0, 4);
        c.fill(portal.add(-1, -1, 0), portal.add(2, 3, 0), Blocks.OBSIDIAN.getDefaultState());
        c.fill(portal, portal.add(1, 2, 0), Blocks.PORTAL.getDefaultState().withProperty(BlockPortal.AXIS, EnumFacing.Axis.X));
        EntityTNTPrimed tnt = new EntityTNTPrimed(w, portal.getX() + 0.5, portal.getY(), portal.getZ() + 0.5, null);
        tnt.setFuse(400);
        w.spawnEntity(tnt);
        PortalTravel.tick(w, false, true);
        c.ok(tnt.isDead, "primed TNT in a nether portal left the overworld (1.21)");
        WorldServer nether = w.getMinecraftServer().getWorld(-1);
        EntityTNTPrimed came = null;
        for (EntityTNTPrimed t : nether.getEntities(EntityTNTPrimed.class, x -> x != null && x.getUniqueID().equals(tnt.getUniqueID()))) {
            came = t;
        }
        c.ok(came != null && came.getEntityData().getBoolean(PortalTravel.PORTAL_TNT), "it is in the Nether, marked as having used a portal");
        if (came != null) {
            came.setDead();
        }
        c.fill(portal.add(-1, -1, 0), portal.add(2, 3, 0), Blocks.AIR.getDefaultState());

        // P14: the End platform drops what it replaces.
        BlockPos spawn = new BlockPos(100, 50, 0);
        theEnd.setBlockState(spawn, Blocks.DIRT.getDefaultState());
        EntityZombie traveller = new EntityZombie(theEnd);
        traveller.setLocationAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
        new Teleporter(theEnd).placeInPortal(traveller, 0.0F);
        c.ok(theEnd.isAirBlock(spawn) && theEnd.getBlockState(spawn.down(2)).getBlock() == Blocks.OBSIDIAN, "P14: the obsidian platform is built");
        c.eq(1, Drops.count(theEnd, spawn, net.minecraft.item.Item.getItemFromBlock(Blocks.DIRT)), "P14: the dirt it replaced dropped (1.21)");
    }

    private static void pearls(Check c) {
        WorldServer w = c.world;
        WorldServer nether = w.getMinecraftServer().getWorld(-1);
        BlockPos a = c.area();
        EntityZombie thrower = new EntityZombie(w);
        thrower.setLocationAndAngles(a.getX() + 2.5, a.getY(), a.getZ() + 2.5, 0, 0);
        w.spawnEntity(thrower);
        EntityEnderPearl pearl = new EntityEnderPearl(w, thrower);
        w.spawnEntity(pearl);
        BlockPos far = new BlockPos(a.getX() / 8 + 3, 120, a.getZ() / 8 + 3);
        nether.getChunk(far);
        Entity moved = pearl.changeDimension(-1, new PortalTravel.At(far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0, 0));
        c.ok(moved instanceof EntityEnderPearl && moved.dimension == -1, "a pearl went to the Nether");
        if (!(moved instanceof EntityEnderPearl)) {
            return;
        }
        EntityEnderPearl inNether = (EntityEnderPearl) moved;
        c.ok(PortalTravel.owner(inNether) == thrower, "the pearl in the Nether still knows its thrower (1.21)");
        ProjectileImpactEvent.Throwable impact = new ProjectileImpactEvent.Throwable(inNether,
                new RayTraceResult(RayTraceResult.Type.BLOCK, new Vec3d(far), EnumFacing.UP, far));
        PortalTravel.onImpact(impact);
        c.ok(impact.isCanceled() && inNether.isDead, "its landing is handled here, and the pearl is used up");
        c.ok(thrower.isDead, "the thrower left the overworld");
        Entity arrived = nether.getEntityFromUuid(thrower.getUniqueID());
        c.ok(arrived != null && arrived.getDistanceSq(far.getX() + 0.5, far.getY(), far.getZ() + 0.5) < 4.0,
                "and arrived at the pearl in the Nether (1.21): " + arrived);
        if (arrived != null) {
            arrived.setDead();
        }

        // 1.21.2: chunks around a player's pearl load, and the pearl leaves and returns with the player.
        EntityPlayerMP player = Players.real(w, "Pearl");
        player.setLocationAndAngles(a.getX() + 2, a.getY(), a.getZ() + 2, 0, 0);
        EntityEnderPearl flying = new EntityEnderPearl(w, player);
        flying.setLocationAndAngles(a.getX() + 400, 120, a.getZ() + 8, 0, 0);
        flying.motionX = flying.motionY = flying.motionZ = 0;
        w.getChunk(flying.getPosition());
        w.spawnEntity(flying);
        int before = EnderPearlChunks.forcedFor(w);
        EnderPearlChunks.onWorldTick(new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(
                net.minecraftforge.fml.relauncher.Side.SERVER, net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, w));
        c.eq(before + 9, EnderPearlChunks.forcedFor(w), "a player's pearl keeps its 3x3 chunks loaded (1.21.2)");
        c.ok(w.getPersistentChunks().containsKey(new net.minecraft.util.math.ChunkPos(flying.getPosition())), "its own chunk is forced, so it ticks");
        EnderPearlChunks.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        w.removeEntityDangerously(flying);
        c.ok(flying.isDead && player.getEntityData().hasKey(TrickyTrialsBackport.MOD_ID + ":ender_pearls"),
                "the pearl left with its thrower when they logged out");
        EnderPearlChunks.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        Entity back = w.getEntityFromUuid(flying.getUniqueID());
        c.ok(back instanceof EntityEnderPearl && !back.isDead, "and came back when they logged in");
        if (back != null) {
            back.setDead();
        }
    }

    private static void saddles(Check c) {
        WorldServer w = c.world;
        c.ok(CraftingManager.getRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "saddle")) != null, "the saddle recipe exists");
        Crafting.expect(c, Crafting.grid(3, 2, ItemStack.EMPTY, new ItemStack(Items.LEATHER), ItemStack.EMPTY,
                new ItemStack(Items.LEATHER), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.LEATHER)),
                new ItemStack(Items.SADDLE), "3 leather and an iron ingot make a saddle (1.21.6)");
        c.ok(CraftingManager.getRecipe(new ResourceLocation("lead")) == null, "1.12.2's lead recipe (with a slimeball) is gone");
        Crafting.expect(c, Crafting.grid(3, 3, new ItemStack(Items.STRING), new ItemStack(Items.STRING), ItemStack.EMPTY,
                new ItemStack(Items.STRING), new ItemStack(Items.STRING), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STRING)), new ItemStack(Items.LEAD, 2), "5 string make 2 leads (1.21.6)");

        for (ResourceLocation id : new ResourceLocation[] {LootTableList.CHESTS_SIMPLE_DUNGEON, LootTableList.CHESTS_DESERT_PYRAMID,
                LootTableList.CHESTS_JUNGLE_TEMPLE, LootTableList.CHESTS_STRONGHOLD_CORRIDOR}) {
            LootPool main = w.getLootTableManager().getLootTableFromLocation(id).getPool("main");
            c.ok(main != null && main.getEntry("minecraft:saddle") == null && main.getEntry("minecraft:leather") != null,
                    id + ": leather where the saddle was (1.21.6)");
        }
        LootTable witch = w.getLootTableManager().getLootTableFromLocation(LootTableList.ENTITIES_WITCH);
        c.ok(witch.getPool(TrickyTrialsBackport.MOD_ID + ":redstone") != null && witch.getPool("main").getEntry("minecraft:redstone") == null,
                "witches: redstone in a pool of its own (1.21)");

        BlockPos a = c.area();
        FakePlayer player = c.player("shears", a.add(1, 0, 1));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        EntityHorse horse = new EntityHorse(w);
        horse.setLocationAndAngles(a.getX() + 5.5, a.getY(), a.getZ() + 5.5, 0, 0);
        w.spawnEntity(horse);
        horse.setHorseTamed(true);
        IItemHandler inv = horse.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        inv.insertItem(0, new ItemStack(Items.SADDLE), false);
        inv.insertItem(1, new ItemStack(Items.IRON_HORSE_ARMOR), false);
        c.ok(horse.isHorseSaddled(), "the horse wears a saddle");
        interact(player, horse);
        c.ok(inv.getStackInSlot(1).isEmpty() && !inv.getStackInSlot(0).isEmpty(), "shears take the horse armor first");
        c.eq(1, Drops.count(w, horse.getPosition(), Items.IRON_HORSE_ARMOR), "and it drops");
        interact(player, horse);
        c.ok(!horse.isHorseSaddled() && inv.getStackInSlot(0).isEmpty(), "then the saddle (1.21.6)");
        c.eq(1, Drops.count(w, horse.getPosition(), Items.SADDLE), "which drops");
        EntityPig pig = new EntityPig(w);
        pig.setLocationAndAngles(a.getX() + 9.5, a.getY(), a.getZ() + 5.5, 0, 0);
        w.spawnEntity(pig);
        pig.setSaddled(true);
        EntityZombie rider = new EntityZombie(w);
        rider.setLocationAndAngles(pig.posX, pig.posY, pig.posZ, 0, 0);
        w.spawnEntity(rider);
        rider.startRiding(pig, true);
        interact(player, pig);
        c.ok(pig.getSaddled(), "not while something rides it");
        rider.dismountRidingEntity();
        rider.setDead();
        interact(player, pig);
        c.ok(!pig.getSaddled(), "a pig's saddle comes off too");

        // Carrots (1.21.6): an apple's worth.
        EntityHorse hungry = new EntityHorse(w);
        hungry.setLocationAndAngles(a.getX() + 14.5, a.getY(), a.getZ() + 5.5, 0, 0);
        w.spawnEntity(hungry);
        hungry.setHealth(5.0F);
        int temper = hungry.getTemper();
        c.ok(Saddles.eatCarrot(hungry), "a horse eats a carrot");
        c.ok(Math.abs(hungry.getHealth() - 8.0F) < 0.01F && hungry.getTemper() == temper + 3, "heals 3 and gains 3 temper, as an apple");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.CARROT, 4));
        hungry.setHealth(5.0F);
        interact(player, hungry);
        c.eq(3, player.getHeldItemMainhand().getCount(), "feeding one from the hand uses it up");

        // Thorns on a saddle (1.21.5).
        ItemStack thorny = new ItemStack(Items.SADDLE);
        thorny.addEnchantment(Enchantments.THORNS, 7);
        inv.insertItem(0, thorny, false);
        EntityZombie attacker = new EntityZombie(w);
        attacker.setLocationAndAngles(horse.posX + 1, horse.posY, horse.posZ, 0, 0);
        w.spawnEntity(attacker);
        horse.attackEntityFrom(DamageSource.causeMobDamage(attacker), 1.0F);
        c.ok(attacker.getHealth() < attacker.getMaxHealth(), "Thorns on a horse's saddle hurts its attacker");
        for (Entity e : new Entity[] {horse, pig, hungry, attacker}) {
            e.setDead();
        }
    }

    private static void interact(FakePlayer player, Entity target) {
        player.interactOn(target, EnumHand.MAIN_HAND);
    }

    private static void leads(Check c) {
        WorldServer w = c.world;
        c.ok(Math.abs(Leads.snapDistance() - 12.0F) < 1e-6F, "P7: leads snap at 12 blocks (1.21.6)");
        BlockPos a = c.area();
        FakePlayer player = c.player("leads", a.add(5, 0, 5));
        EntityCow led = new EntityCow(w);
        led.setLocationAndAngles(a.getX() + 6.5, a.getY(), a.getZ() + 5.5, 0, 0);
        w.spawnEntity(led);
        led.setLeashHolder(player, true);
        EntityCow post = new EntityCow(w);
        post.setLocationAndAngles(a.getX() + 9.5, a.getY(), a.getZ() + 5.5, 0, 0);
        w.spawnEntity(post);
        player.setSneaking(true);
        interact(player, post);
        player.setSneaking(false);
        c.ok(led.getLeashHolder() == post, "sneak-use ties the led cow to the other cow (1.21.6)");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        interact(player, post);
        c.ok(!led.getLeashed(), "shears on the holder cut the lead it holds");
        c.eq(1, Drops.count(w, led.getPosition(), Items.LEAD), "which drops");
        led.setDead();
        post.setDead();
    }

    private static void knockback(Check c) {
        WorldServer w = c.world;
        BlockPos a = c.area();
        EntityZombie target = new EntityZombie(w);
        target.setLocationAndAngles(a.getX() + 8.5, a.getY(), a.getZ() + 8.5, 0, 0);
        w.spawnEntity(target);
        // A dispenser's arrow (no shooter) flying toward +x.
        EntityTippedArrow arrow = new EntityTippedArrow(w);
        arrow.setPosition(target.posX - 1, target.posY + 1, target.posZ);
        arrow.motionX = 2.0;
        target.motionX = target.motionY = target.motionZ = 0;
        target.attackEntityFrom(DamageSource.causeArrowDamage(arrow, null), 1.0F);
        c.ok(target.motionX > 0.1, "P6: an arrow with no shooter knocks back, along its flight (1.21): " + target.motionX);
        // A shooter on the far side: 1.12.2 pushes away from the shooter (-x), 1.21 along the arrow (+x).
        EntitySkeleton shooter = new EntitySkeleton(w);
        shooter.setLocationAndAngles(target.posX + 5, target.posY, target.posZ, 0, 0);
        target.hurtResistantTime = 0;
        target.motionX = target.motionY = target.motionZ = 0;
        target.attackEntityFrom(DamageSource.causeArrowDamage(arrow, shooter), 1.0F);
        c.ok(target.motionX > 0.1, "P6: knockback follows the arrow, not the shooter's position: " + target.motionX);
        ModConfig.tweaks.projectileKnockback = false;
        target.hurtResistantTime = 0;
        target.motionX = target.motionY = target.motionZ = 0;
        target.attackEntityFrom(DamageSource.causeArrowDamage(arrow, shooter), 1.0F);
        c.ok(target.motionX < -0.1, "P6 off: 1.12.2 pushes away from the shooter: " + target.motionX);
        ModConfig.tweaks.projectileKnockback = true;
        target.setDead();
    }

    private static void smallChanges(Check c) throws Exception {
        WorldServer w = c.world;
        BlockPos a = c.area();
        // Axe and shield.
        FakePlayer player = c.player("axe", a.add(2, 0, 2));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.SHIELD));
        PlayerInteractEvent.RightClickBlock click = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND,
                a.add(2, -1, 3), EnumFacing.UP, new Vec3d(a.add(2, 0, 3)));
        SmallTweaks.onRightClickBlock(click);
        c.ok(click.isCanceled(), "axe and shield: the axe's use is skipped so the shield goes up (1.21)");
        player.setSneaking(true);
        click = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, a.add(2, -1, 3), EnumFacing.UP, new Vec3d(a.add(2, 0, 3)));
        SmallTweaks.onRightClickBlock(click);
        c.ok(!click.isCanceled(), "sneaking, the axe is used");
        player.setSneaking(false);

        // Panic.
        EntityCow cow = new EntityCow(w);
        cow.setLocationAndAngles(a.getX() + 10.5, a.getY(), a.getZ() + 10.5, 0, 0);
        w.spawnEntity(cow);
        SmallTweaks.Panic panic = null;
        for (EntityAITasks.EntityAITaskEntry e : cow.tasks.taskEntries) {
            if (e.action instanceof SmallTweaks.Panic) {
                panic = (SmallTweaks.Panic) e.action;
            }
        }
        c.ok(panic != null, "a cow's panic goal is 1.21's");
        if (panic != null) {
            cow.attackEntityFrom(DamageSource.FALL, 1.0F);
            c.ok(!panic.shouldPanic(), "a fall does not make it panic (not a panic cause)");
            cow.hurtResistantTime = 0;
            cow.attackEntityFrom(DamageSource.MAGIC, 1.0F);
            c.ok(panic.shouldPanic(), "magic does (1.21's #panic_causes; 1.12.2 needed an attacker)");
        }
        EntityPolarBear bear = new EntityPolarBear(w);
        bear.setLocationAndAngles(a.getX() + 14.5, a.getY(), a.getZ() + 10.5, 0, 0);
        w.spawnEntity(bear);
        EntityZombie zombie = new EntityZombie(w);
        zombie.setLocationAndAngles(a.getX() + 16.5, a.getY(), a.getZ() + 10.5, 0, 0);
        bear.attackEntityFrom(DamageSource.causeMobDamage(zombie), 1.0F);
        boolean bearPanics = false;
        for (EntityAITasks.EntityAITaskEntry e : bear.tasks.taskEntries) {
            if (e.action instanceof SmallTweaks.Panic) {
                bearPanics = ((SmallTweaks.Panic) e.action).shouldPanic();
            }
        }
        c.ok(!bearPanics, "an adult polar bear does not panic from a mob's hit");
        c.ok(SmallTweaks.panicCause(DamageSource.LIGHTNING_BOLT, true), "but would from lightning");
        cow.setDead();
        bear.setDead();

        // No spawn invulnerability (1.21.4).
        EntityPlayerMP joining = Players.real(w, "Join");
        SmallTweaks.onJoin(new EntityJoinWorldEvent(joining, w));
        Field ticks = EntityPlayerMP.class.getDeclaredField(field(EntityPlayerMP.class, "respawnInvulnerabilityTicks", "field_147101_bU"));
        ticks.setAccessible(true);
        c.eq(0, ticks.getInt(joining), "a joining player has no spawn invulnerability (1.21.4)");

        // Water Breathing refills air (1.21.4).
        BlockPos pool = a.add(20, 0, 20);
        c.fill(pool, pool.add(0, 2, 0), Blocks.WATER.getDefaultState());
        EntityZombie diver = new EntityZombie(w);
        diver.setLocationAndAngles(pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5, 0, 0);
        w.spawnEntity(diver);
        diver.setAir(100);
        diver.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 200));
        SmallTweaks.onLivingUpdate(new LivingEvent.LivingUpdateEvent(diver));
        c.eq(104, diver.getAir(), "Water Breathing adds 4 air a tick under water");
        diver.setDead();
        c.fill(pool, pool.add(0, 2, 0), Blocks.AIR.getDefaultState());

        // Elytra and a ladder (1.21.5).
        BlockPos ladder = a.add(24, 0, 2);
        w.setBlockState(ladder.east(), Blocks.STONE.getDefaultState());
        w.setBlockState(ladder, Blocks.LADDER.getDefaultState().withProperty(net.minecraft.block.BlockLadder.FACING, EnumFacing.WEST));
        EntityPlayerMP glider = Players.real(w, "Glider");
        glider.setLocationAndAngles(ladder.getX() + 0.5, ladder.getY(), ladder.getZ() + 0.5, 0, 0);
        glider.setElytraFlying();
        SmallTweaks.onLivingUpdate(new LivingEvent.LivingUpdateEvent(glider));
        c.ok(!glider.isElytraFlying(), "gliding into a ladder ends the flight");
        w.setBlockState(ladder, Blocks.VINE.getDefaultState().withProperty(net.minecraft.block.BlockVine.EAST, true));
        glider.setElytraFlying();
        SmallTweaks.onLivingUpdate(new LivingEvent.LivingUpdateEvent(glider));
        c.ok(glider.isElytraFlying(), "but vines are glided through (1.21.11)");

        // Protection adds up (1.21).
        EntityZombie armored = new EntityZombie(w);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 2);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 2);
        ItemStack legs = new ItemStack(Items.IRON_LEGGINGS);
        legs.addEnchantment(Enchantments.FIRE_PROTECTION, 2);
        legs.addEnchantment(Enchantments.BLAST_PROTECTION, 2);
        armored.setItemStackToSlot(EntityEquipmentSlot.FEET, boots);
        armored.setItemStackToSlot(EntityEquipmentSlot.LEGS, legs);
        c.eq(40, EnchantmentProtection.getFireTimeForEntity(armored, 100), "P10: two pieces of Fire Protection II burn 40% as long");
        c.ok(Math.abs(EnchantmentProtection.getBlastDamageReduction(armored, 1.0D) - 0.4D) < 1e-9, "P10: two of Blast Protection II keep 40% of the knockback");
        ModConfig.tweaks.protectionStacks = false;
        c.eq(70, EnchantmentProtection.getFireTimeForEntity(armored, 100), "P10 off: 1.12.2 counts the best piece only");
        ModConfig.tweaks.protectionStacks = true;

        // End crystals and fire (1.21).
        EntityEnderCrystal crystal = new EntityEnderCrystal(w, a.getX() + 28.5, a.getY(), a.getZ() + 28.5);
        w.spawnEntity(crystal);
        crystal.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
        c.ok(!crystal.isDead, "P11: fire does not set off an end crystal");
        crystal.setDead();

        // Splash distance (1.21.2).
        EntityPotion potion = new EntityPotion(w, a.getX() + 4.5, a.getY() + 1, a.getZ() + 20.5, new ItemStack(Items.SPLASH_POTION));
        EntityZombie near = new EntityZombie(w);
        near.setPosition(a.getX() + 7.0, a.getY(), a.getZ() + 20.5);
        double old = potion.getDistanceSq(near);
        double now = MechanicsHooks.splashDistanceSq(potion, near);
        c.ok(now < old, "P12: splash distance is box to box, shorter than 1.12.2's center to feet: " + now + " < " + old);

        // Map markers (1.21.2).
        MapData map = new MapData("selftest_map");
        FakePlayer holder = c.player("mapholder", a);
        FakePlayer hidden = c.player("maphidden", a);
        map.getMapInfo(holder);
        map.getMapInfo(hidden);
        map.mapDecorations.put(hidden.getName(), new net.minecraft.world.storage.MapDecoration(
                net.minecraft.world.storage.MapDecoration.Type.PLAYER, (byte) 0, (byte) 0, (byte) 0));
        hidden.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Blocks.PUMPKIN));
        MechanicsHooks.hideMapMarkers(map, holder);
        c.ok(!map.mapDecorations.containsKey(hidden.getName()), "P13: a player wearing a pumpkin is hidden from others' maps");
        hidden.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
    }

    private static String field(Class<?> owner, String mcp, String srg) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getName().equals(mcp) || f.getName().equals(srg)) {
                return f.getName();
            }
        }
        return mcp;
    }

    private static void blocks(Check c) {
        WorldServer w = c.world;
        BlockPos a = c.area();
        // Mixed double doors (1.21).
        BlockPos oak = a.add(4, 0, 4);
        ItemDoor.placeDoor(w, oak, EnumFacing.NORTH, Blocks.OAK_DOOR, false);
        BlockPos spruce = oak.east();
        ItemDoor.placeDoor(w, spruce, EnumFacing.NORTH, Blocks.SPRUCE_DOOR, false);
        c.eq(BlockDoor.EnumHingePosition.RIGHT, w.getBlockState(spruce.up()).getValue(BlockDoor.HINGE),
                "P8: a spruce door beside an oak door forms a double door (1.21)");
        ModConfig.tweaks.mixedDoubleDoors = false;
        BlockPos birch = oak.add(4, 0, 0);
        ItemDoor.placeDoor(w, birch, EnumFacing.NORTH, Blocks.OAK_DOOR, false);
        ItemDoor.placeDoor(w, birch.east(), EnumFacing.NORTH, Blocks.BIRCH_DOOR, false);
        c.eq(BlockDoor.EnumHingePosition.LEFT, w.getBlockState(birch.east().up()).getValue(BlockDoor.HINGE),
                "P8 off: 1.12.2 pairs only doors of one material");
        ModConfig.tweaks.mixedDoubleDoors = true;

        // Fire away from players (1.21.5).
        BlockPos fire = a.add(10, 0, 10);
        c.ok(w.playerEntities.isEmpty() || w.getClosestPlayer(fire.getX(), fire.getY(), fire.getZ(), 128, false) == null,
                "(no player near the test area)");
        c.ok(MechanicsHooks.fireIdle(Blocks.FIRE, w, fire, w.rand), "P9: fire with no player within 128 blocks only waits");
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.FIRE_AWAY, "true");
        c.ok(!MechanicsHooks.fireIdle(Blocks.FIRE, w, fire, w.rand), "allowFireTicksAwayFromPlayer: it burns as before");
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.FIRE_AWAY, "false");

        // Tool-free drops (1.21.4).
        FakePlayer hand = c.player("hand", a);
        c.ok(hand.canHarvestBlock(Blocks.IRON_DOOR.getDefaultState()), "an iron door drops when broken by hand");
        c.ok(hand.canHarvestBlock(Blocks.BREWING_STAND.getDefaultState()), "so does a brewing stand");
        c.ok(!hand.canHarvestBlock(Blocks.IRON_BLOCK.getDefaultState()), "an iron block still needs a pickaxe");

        // Head names (1.21.9).
        BlockPos head = a.add(20, 0, 4);
        FakePlayer placer = c.player("head", a);
        ItemStack named = new ItemStack(Items.SKULL, 1, 2);
        named.setStackDisplayName("Gerald");
        placer.setHeldItem(EnumHand.MAIN_HAND, named);
        w.setBlockState(head, Blocks.SKULL.getDefaultState().withProperty(net.minecraft.block.BlockSkull.FACING, EnumFacing.UP));
        TileEntity te = w.getTileEntity(head);
        if (te instanceof net.minecraft.tileentity.TileEntitySkull) {
            ((net.minecraft.tileentity.TileEntitySkull) te).setType(2);
        }
        SmallTweaks.onPlace(new BlockEvent.PlaceEvent(BlockSnapshot.getBlockSnapshot(w, head), Blocks.STONE.getDefaultState(), placer, EnumHand.MAIN_HAND));
        IBlockState skull = w.getBlockState(head);
        skull.getBlock().dropBlockAsItem(w, head, skull, 0);
        String got = null;
        for (EntityItem item : w.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(head).grow(2))) {
            if (item.getItem().getItem() == Items.SKULL) {
                got = item.getItem().getDisplayName();
            }
            item.setDead();
        }
        c.eq("Gerald", got, "a placed head keeps its name when it drops again (1.21.9)");
        w.setBlockToAir(head);

        // No spawn chunks (1.21.9).
        BlockPos spawn = w.getSpawnPoint();
        c.ok(!w.isSpawnChunk(spawn.getX() >> 4, spawn.getZ() >> 4), "P15: the spawn's chunk is no longer a spawn chunk (1.21.9)");
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("trailsandtalesbackport")) {
            ModConfig.tweaks.noSpawnChunks = false;
            c.ok(w.isSpawnChunk(spawn.getX() >> 4, spawn.getZ() >> 4), "P15 off: 1.12.2 keeps it");
            c.eq(192, SpawnChunks.prepareBlocks(), "P15 off: 1.12.2's start area");
            ModConfig.tweaks.noSpawnChunks = true;
            c.ok(SpawnChunks.prepareBlocks() < SpawnChunks.prepareStart(), "P15: no start area is prepared");
        }
    }

    private static void gameRules(Check c) {
        WorldServer w = c.world;
        for (String rule : new String[] {GameRuleTweaks.TNT_EXPLODES, GameRuleTweaks.FIRE_AWAY, GameRuleTweaks.NETHER_PORTALS,
                GameRuleTweaks.SPAWN_MONSTERS, GameRuleTweaks.PVP, GameRuleTweaks.COMMAND_BLOCKS, GameRuleTweaks.SPAWNER_BLOCKS}) {
            c.ok(w.getGameRules().hasRule(rule), "game rule " + rule + " exists");
        }
        BlockPos a = c.area();
        FakePlayer one = c.player("pvp1", a);
        FakePlayer two = c.player("pvp2", a.add(1, 0, 0));
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.PVP, "false");
        LivingAttackEvent hit = new LivingAttackEvent(two, DamageSource.causePlayerDamage(one), 1.0F);
        GameRuleTweaks.onAttack(hit);
        c.ok(hit.isCanceled(), "pvp false: players cannot hurt players");
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.PVP, "true");
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.TNT_EXPLODES, "false");
        EntityTNTPrimed tnt = new EntityTNTPrimed(w, a.getX() + 8.5, a.getY(), a.getZ() + 8.5, null);
        net.minecraftforge.event.world.ExplosionEvent.Start boom = new net.minecraftforge.event.world.ExplosionEvent.Start(w,
                new net.minecraft.world.Explosion(w, tnt, tnt.posX, tnt.posY, tnt.posZ, 4.0F, false, true));
        GameRuleTweaks.onExplosion(boom);
        c.ok(boom.isCanceled(), "tntExplodes false: TNT does not explode");
        w.getGameRules().setOrCreateGameRule(GameRuleTweaks.TNT_EXPLODES, "true");
    }
}
