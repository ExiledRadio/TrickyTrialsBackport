package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.breeze.BreezeContent;
import com.exiledradio.trickytrialsbackport.breeze.EntityBreeze;
import com.exiledradio.trickytrialsbackport.breeze.EntityBreezeWindCharge;
import com.exiledradio.trickytrialsbackport.breeze.EntityWindCharge;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.WindBurst;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Stream 3: the breeze, wind charges and the shared wind burst. */
public final class BreezeSelfTest implements SelfTest {

    @Override
    public String name() {
        return "breeze";
    }

    @Override
    public void run(Check c) {
        registration(c);
        breeze(c);
        flight(c);
        blocks(c);
        knockback(c);
        falls(c);
        deflection(c);
        ai(c);
        jumps(c);
    }

    private void registration(Check c) {
        c.eq(Content.id("breeze_rod"), BreezeContent.breezeRod.getRegistryName(), "breeze rod");
        c.eq(Content.id("wind_charge"), BreezeContent.windCharge.getRegistryName(), "wind charge item");
        EntityBreeze breeze = new EntityBreeze(c.world);
        c.eq(Content.id("breeze"), EntityList.getKey(breeze), "breeze entity name");
        c.eq(Content.id("wind_charge"), EntityList.getKey(new EntityWindCharge(c.world)), "wind charge entity name");
        c.eq(Content.id("breeze_wind_charge"), EntityList.getKey(new EntityBreezeWindCharge(c.world)), "breeze wind charge entity name");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("breeze")), "breeze has a spawn egg");
        ItemStack rod = new ItemStack(BreezeContent.breezeRod);
        Crafting.expect(c, Crafting.grid(1, 1, rod), new ItemStack(BreezeContent.windCharge, 4), "a breeze rod makes four wind charges");
        IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(BreezeContent.windCharge);
        c.ok(behavior != null && behavior != IBehaviorDispenseItem.DEFAULT_BEHAVIOR, "dispensers shoot wind charges");
        c.ok(CriteriaTriggers.get(Content.id("fall_after_wind_charge")) != null, "fall trigger registered");
        c.ok(c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/blowback")) != null,
                "Blowback loaded");
        c.ok(c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/who_needs_rockets")) != null,
                "Who Needs Rockets? loaded");
    }

    private void breeze(Check c) {
        BlockPos area = c.area();
        EntityBreeze breeze = spawnBreeze(c, area.add(16, 1, 16));
        c.eq(30.0, breeze.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "30 health");
        c.ok(!breeze.attackEntityFrom(DamageSource.FALL, 10.0F), "no fall damage");
        breeze.fall(30.0F, 1.0F);
        c.eq(30.0F, breeze.getHealth(), "a long fall leaves it whole");
        c.ok(!breeze.isEntityInvulnerable(DamageSource.GENERIC), "not invulnerable");

        // Saved and loaded back as a breeze.
        NBTTagCompound tag = new NBTTagCompound();
        breeze.writeToNBTOptional(tag);
        Entity back = EntityList.createEntityFromNBT(tag, c.world);
        c.ok(back instanceof EntityBreeze && Math.abs(((EntityBreeze) back).getHealth() - 30.0F) < 1e-3, "NBT round trip");

        // Drops: 1-2 breeze rods when a player kills it, none otherwise.
        FakePlayer player = c.player("breeze", area.up());
        int min = 99;
        int max = 0;
        for (int i = 0; i < 30; i++) {
            EntityBreeze b = spawnBreeze(c, area.add(16, 1, 16));
            b.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000.0F);
            int n = Drops.count(c.world, b.getPosition(), BreezeContent.breezeRod);
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        c.ok(min == 1 && max == 2, "player kills drop 1-2 rods: " + min + "-" + max);
        EntityBreeze unowned = spawnBreeze(c, area.add(16, 1, 16));
        unowned.attackEntityFrom(DamageSource.GENERIC, 1000.0F);
        c.eq(0, Drops.count(c.world, unowned.getPosition(), BreezeContent.breezeRod), "no rods without a player");
    }

    private static EntityBreeze spawnBreeze(Check c, BlockPos at) {
        EntityBreeze b = new EntityBreeze(c.world);
        b.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        b.onInitialSpawn(c.world.getDifficultyForLocation(at), null);
        c.world.spawnEntity(b);
        return b;
    }

    private void flight(Check c) {
        BlockPos area = c.area();
        EntityWindCharge charge = new EntityWindCharge(c.world, area.getX() + 2.5, 130, area.getZ() + 2.5);
        charge.shoot(1, 0, 0, 1.5F, 0.0F);
        c.world.spawnEntity(charge);
        double y0 = charge.posY;
        for (int i = 0; i < 10; i++) {
            charge.onUpdate();
        }
        c.ok(Math.abs(charge.motionX - 1.5) < 1e-6, "no drag in air: " + charge.motionX);
        c.ok(Math.abs(charge.posY - y0) < 1e-6, "no gravity");
        c.ok(Math.abs(charge.posX - (area.getX() + 2.5 + 15)) < 1e-3, "flew 15 blocks in 10 ticks: " + charge.posX);

        // A direct hit deals one damage and bursts.
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(area.getX() + 10.5, 100, area.getZ() + 10.5);
        c.world.spawnEntity(pig);
        float health = pig.getHealth();
        EntityWindCharge hit = new EntityWindCharge(c.world, pig.posX - 1, 100.5, pig.posZ);
        c.world.spawnEntity(hit);
        invokeImpact(hit, new RayTraceResult(pig));
        c.eq(health - 1.0F, pig.getHealth(), "a direct hit does 1 damage");
        c.ok(hit.isDead, "the charge is gone after bursting");

        // Hitting a charge turns it the way the hitter looks, at its own speed, and makes them its owner.
        FakePlayer player = c.player("deflector", area.up());
        player.rotationYaw = 180.0F; // north
        player.rotationPitch = 0.0F;
        EntityBreezeWindCharge bc = new EntityBreezeWindCharge(c.world);
        bc.setPosition(area.getX() + 5, 120, area.getZ() + 5);
        bc.motionX = 0.7;
        c.ok(bc.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F), "a hit deflects the charge");
        c.ok(Math.abs(bc.motionZ + 0.7) < 1e-3 && Math.abs(bc.motionX) < 1e-3, "now flying north at 0.7: " + bc.motionX + "," + bc.motionZ);
        c.ok(bc.getThrower() == player, "the deflector owns it");
        charge.setDead();
    }

    /** EntityThrowable.onImpact is protected; the test calls it the way the entity's own tick does. */
    private static void invokeImpact(EntityWindCharge charge, RayTraceResult result) {
        try {
            java.lang.reflect.Method m = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(
                    net.minecraft.entity.projectile.EntityThrowable.class, "onImpact", "func_70184_a", RayTraceResult.class);
            m.invoke(charge, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void blocks(Check c) {
        BlockPos area = c.area();
        // Each block on its own, 4 apart, with a burst against its west face as a direct hit makes.
        BlockPos oak = area.add(2, 0, 2);
        ItemDoor.placeDoor(c.world, oak, EnumFacing.EAST, Blocks.OAK_DOOR, false);
        BlockPos iron = area.add(6, 0, 2);
        ItemDoor.placeDoor(c.world, iron, EnumFacing.EAST, Blocks.IRON_DOOR, false);
        BlockPos trap = area.add(10, 0, 2);
        c.world.setBlockState(trap, Blocks.TRAPDOOR.getDefaultState());
        BlockPos gate = area.add(14, 0, 2);
        c.world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());
        BlockPos button = area.add(18, 0, 2);
        c.world.setBlockState(button, Blocks.WOODEN_BUTTON.getDefaultState().withProperty(BlockButton.FACING, EnumFacing.UP));
        BlockPos lever = area.add(22, 0, 2);
        c.world.setBlockState(lever, Blocks.LEVER.getDefaultState().withProperty(BlockLever.FACING, BlockLever.EnumOrientation.UP_X));
        BlockPos flower = area.add(26, 0, 2);
        c.world.setBlockState(flower.down(), Blocks.END_STONE.getDefaultState());
        c.world.setBlockState(flower, Blocks.CHORUS_FLOWER.getDefaultState());
        BlockPos powered = area.add(2, 0, 10);
        ItemDoor.placeDoor(c.world, powered, EnumFacing.EAST, Blocks.OAK_DOOR, false);
        c.world.setBlockState(powered.north(), Blocks.REDSTONE_BLOCK.getDefaultState());
        boolean poweredOpen = BlockDoor.isOpen(c.world, powered);

        for (BlockPos p : new BlockPos[] {oak, iron, trap, gate, button, lever, flower, powered}) {
            hitWest(c, p);
        }
        c.ok(BlockDoor.isOpen(c.world, oak), "oak door opens");
        c.ok(!BlockDoor.isOpen(c.world, iron), "iron door stays shut");
        c.ok(c.world.getBlockState(trap).getValue(BlockTrapDoor.OPEN), "trapdoor opens");
        c.ok(c.world.getBlockState(gate).getValue(BlockFenceGate.OPEN), "fence gate opens");
        c.ok(c.world.getBlockState(button).getBlock() == Blocks.WOODEN_BUTTON && c.world.getBlockState(button).getValue(BlockButton.POWERED),
                "button pressed");
        c.ok(c.world.getBlockState(lever).getBlock() == Blocks.LEVER && c.world.getBlockState(lever).getValue(BlockLever.POWERED),
                "lever flipped");
        c.ok(c.world.getBlockState(flower).getBlock() != Blocks.CHORUS_FLOWER, "chorus flower broken");
        c.ok(c.world.getBlockState(area.add(2, -1, 2)).getBlock() == Blocks.STONE, "nothing else broken");
        c.eq(poweredOpen, BlockDoor.isOpen(c.world, powered), "a door held by redstone is not flipped");

        hitWest(c, oak);
        c.ok(!BlockDoor.isOpen(c.world, oak), "a second burst closes the door");
    }

    /** A wind charge's burst a quarter block west of a block, at its middle height. */
    private static void hitWest(Check c, BlockPos p) {
        WindBurst.burst(c.world, null, p.getX() - 0.25, p.getY() + 0.5, p.getZ() + 0.5, settings(1.2F));
    }

    private static WindBurst.Settings settings(float radius) {
        return new WindBurst.Settings(radius, 1.22F, false, Content.sound("entity.wind_charge.wind_burst"), true,
                e -> e instanceof EntityBreeze);
    }

    private void knockback(Check c) {
        BlockPos area = c.area();
        BlockPos center = area.add(16, 0, 16);
        EntityPig near = pig(c, center.getX() + 1.5, center.getY(), center.getZ() + 0.5);
        EntityPig hidden = pig(c, center.getX() - 1.5, center.getY(), center.getZ() + 0.5);
        c.fill(center.add(-1, 0, -2), center.add(-1, 3, 2), Blocks.OBSIDIAN.getDefaultState());
        EntityPig far = pig(c, center.getX() + 6.5, center.getY(), center.getZ() + 0.5);
        EntityBreeze breeze = spawnBreeze(c, center.add(0, 0, 2));
        breeze.motionX = breeze.motionY = breeze.motionZ = 0;
        WindBurst.burst(c.world, null, center.getX() + 0.5, center.getY() + 0.2, center.getZ() + 0.5, settings(1.2F));
        c.ok(near.motionX > 0.3 && near.motionY > 0.0, "a pig beside the burst flies away: " + near.motionX + "," + near.motionY);
        c.ok(Math.abs(hidden.motionX) < 1e-6, "a pig behind obsidian is not pushed: " + hidden.motionX);
        c.ok(Math.abs(far.motionX) < 1e-6, "a pig beyond twice the radius is not pushed");
        c.ok(Math.abs(breeze.motionZ) < 1e-6, "a breeze ignores wind charges");

        // The breeze's own charge reaches 6 blocks.
        EntityPig farther = pig(c, center.getX() + 0.5, center.getY(), center.getZ() + 4.5);
        WindBurst.burst(c.world, null, center.getX() + 0.5, center.getY() + 0.2, center.getZ() + 0.5,
                new WindBurst.Settings(3.0F, 1.0F, true, Content.sound("entity.breeze.wind_burst"), true, e -> e instanceof EntityBreeze));
        c.ok(farther.motionZ > 0.05, "a breeze charge pushes a pig 4 blocks away: " + farther.motionZ);
    }

    private static EntityPig pig(Check c, double x, double y, double z) {
        EntityPig p = new EntityPig(c.world);
        p.setPosition(x, y, z);
        c.world.spawnEntity(p);
        p.motionX = p.motionY = p.motionZ = 0;
        return p;
    }

    private void falls(Check c) {
        BlockPos area = c.area();
        // A real player in the world: fake players are not in its entity list, so no burst finds them.
        net.minecraft.entity.player.EntityPlayerMP player = Players.real(c.world, "rocket");
        player.setPosition(area.getX() + 16.5, area.getY(), area.getZ() + 16.5);
        c.world.spawnEntity(player);
        player.motionX = player.motionY = player.motionZ = 0;
        player.onGround = true;
        WindBurst.burst(c.world, null, player.posX, player.posY + 0.1, player.posZ, settings(1.2F));
        c.ok(player.motionY > 0.6, "a burst under a player launches them: " + player.motionY);
        c.ok(WindBurst.launched(player), "the launch is remembered");

        // Landing higher than the burst: no fall damage, whatever the fall.
        player.setPosition(player.posX, area.getY() + 5, player.posZ);
        LivingFallEvent high = new LivingFallEvent(player, 12.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(high);
        c.eq(0.0F, high.getDistance(), "landing above the burst counts no fall");
        // Landing 4 blocks below it: only those 4 count.
        player.setPosition(player.posX, area.getY() - 4.3, player.posZ);
        LivingFallEvent low = new LivingFallEvent(player, 12.0F, 1.0F);
        MinecraftForge.EVENT_BUS.post(low);
        c.ok(Math.abs(low.getDistance() - 4.4F) < 0.01F, "only the fall below the burst (at +0.1) counts: " + low.getDistance());

        // Landing 7.5 blocks above the burst grants Who Needs Rockets?.
        net.minecraft.advancements.Advancement rockets = c.world.getMinecraftServer().getAdvancementManager()
                .getAdvancement(Content.id("adventure/who_needs_rockets"));
        c.ok(!player.getAdvancements().getProgress(rockets).isDone(), "not yet earned");
        player.setPosition(player.posX, area.getY() + 0.1 + 7.5, player.posZ);
        player.onGround = true;
        player.ticksExisted += 5;
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, player));
        c.ok(player.getAdvancements().getProgress(rockets).isDone(), "Who Needs Rockets? earned on landing 7.5 up");
        c.ok(!WindBurst.launched(player), "the launch is forgotten on landing");
        c.world.removeEntityDangerously(player);
    }

    private void deflection(Check c) {
        BlockPos area = c.area();
        EntityBreeze breeze = spawnBreeze(c, area.add(16, 0, 16));
        breeze.setNoAI(true);
        EntityTippedArrow arrow = new EntityTippedArrow(c.world, breeze.posX - 3, breeze.posY + 1.0, breeze.posZ);
        arrow.shoot(1, 0, 0, 1.5F, 0.0F);
        c.world.spawnEntity(arrow);
        boolean reversed = false;
        for (int i = 0; i < 10 && !reversed; i++) {
            arrow.onUpdate();
            reversed = arrow.motionX < 0;
        }
        c.ok(reversed, "a breeze turns an arrow back: " + arrow.motionX);
        c.eq(30.0F, breeze.getHealth(), "the arrow did no harm");

        // A breeze's charge striking a zombie does not turn it on the breeze.
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(area.getX() + 4.5, area.getY(), area.getZ() + 4.5);
        c.world.spawnEntity(zombie);
        EntityBreezeWindCharge charge = new EntityBreezeWindCharge(c.world, breeze);
        zombie.attackEntityFrom(charge.damageSource(), 1.0F);
        c.ok(zombie.getRevengeTarget() == breeze, "hit by the breeze's charge");
        zombie.onUpdate();
        c.ok(zombie.getRevengeTarget() != breeze && zombie.getAttackTarget() != breeze, "no anger from a wind charge");
        // A player's charge does anger it.
        FakePlayer player = c.player("angry", area.up());
        EntityWindCharge mine = new EntityWindCharge(c.world, player);
        zombie.hurtResistantTime = 0;
        zombie.attackEntityFrom(mine.damageSource(), 1.0F);
        zombie.onUpdate();
        c.ok(zombie.getRevengeTarget() == player, "a player's charge still angers");
        zombie.setDead();
    }

    private void ai(Check c) {
        BlockPos area = c.area();
        EntityBreeze breeze = spawnBreeze(c, area.add(4, 0, 16));
        EntityPig target = pig(c, area.getX() + 16.5, area.getY(), area.getZ() + 16.5);
        target.setNoAI(true);
        double startY = breeze.posY;
        double maxY = startY;
        int charges = 0;
        boolean jumped = false;
        for (int tick = 0; tick < 300; tick++) {
            breeze.setAttackTarget(target);
            breeze.ticksExisted++;
            breeze.onUpdate();
            for (Entity e : c.world.getEntitiesWithinAABB(EntityBreezeWindCharge.class, breeze.getEntityBoundingBox().grow(20))) {
                if (!e.getEntityData().getBoolean("ttbCounted")) {
                    e.getEntityData().setBoolean("ttbCounted", true);
                    charges++;
                }
                if (!e.isDead) {
                    e.ticksExisted++;
                    e.onUpdate();
                }
            }
            if (breeze.pose() == EntityBreeze.Pose.LONG_JUMPING) {
                jumped = true;
            }
            maxY = Math.max(maxY, breeze.posY);
            target.heal(100);
            target.setPosition(area.getX() + 16.5, area.getY(), area.getZ() + 16.5);
        }
        com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.LOGGER.info("SELFTEST breeze ai: rose {} blocks, {} charges, ends {} from its target",
                String.format("%.1f", maxY - startY), charges, String.format("%.1f", breeze.getDistance(target)));
        c.ok(jumped && maxY > startY + 1.5, "the breeze long-jumps toward a far target: rose " + (maxY - startY));
        c.ok(charges >= 2, "the breeze shoots wind charges: " + charges);
        c.ok(breeze.isEntityAlive() && breeze.getHealth() == 30.0F, "unhurt by its own jumps and charges");
        // 1.21's breeze does not close in: it jumps behind its target and slides 4-8 blocks from it (jumps(), below).
    }

    /** 1.21.11's LongJump and the brain's memories, by vanilla's numbers. */
    private void jumps(Check c) {
        c.eq(10, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.JUMP_INHALE_TICKS, "LongJump: 10 ticks inhaling");
        c.eq(10, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.JUMP_COOLDOWN_TICKS, "LongJump: 10 ticks of cooldown");
        c.eq(2, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.JUMP_COOLDOWN_WHEN_HURT_TICKS, "LongJump: 2 when hurt");
        c.eq(4, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.REQUIRED_AIR_BLOCKS_ABOVE, "LongJump: four air blocks above");
        c.eq(java.util.Arrays.asList(40, 55, 60, 75, 80), com.exiledradio.trickytrialsbackport.breeze.BreezeAI.ALLOWED_ANGLES, "LongJump angles");
        c.ok(Math.abs(com.exiledradio.trickytrialsbackport.breeze.BreezeAI.MAX_JUMP_VELOCITY_MULTIPLIER * 24.0F - 1.4F) < 1e-5F,
                "the fastest jump: 1.4 blocks a tick at follow range 24");
        c.eq(100, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.SHOOT_WINDOW_AFTER_JUMP, "BREEZE_SHOOT after a jump: 100 ticks");
        c.eq(60, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.SHOOT_WINDOW_AFTER_SLIDE, "BREEZE_SHOOT after a slide: 60 ticks");
        c.eq(20, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.SHOOT_DURATION_TICKS, "Shoot: 15 + 1 + 4 ticks");
        c.eq(10, com.exiledradio.trickytrialsbackport.breeze.BreezeAI.SHOOT_COOLDOWN_TICKS, "Shoot: 10 ticks of cooldown");
        // LongJumpUtil.calculateJumpVectorForAngle, worked by hand: 6 blocks across on the level at 40 degrees.
        net.minecraft.util.math.Vec3d v = com.exiledradio.trickytrialsbackport.breeze.BreezeAI.jumpVectorForAngle(
                new net.minecraft.util.math.Vec3d(0, 0, 0), new net.minecraft.util.math.Vec3d(6.5, 0, 0), 1.4F, 40);
        double speed = Math.sqrt(6.0 * 0.08 / Math.sin(2 * Math.toRadians(40)));
        c.ok(v != null && Math.abs(v.x - speed * Math.cos(Math.toRadians(40)) * 0.95) < 1e-4 && Math.abs(v.y - speed * Math.sin(Math.toRadians(40)) * 0.95) < 1e-4
                && Math.abs(v.z) < 1e-9, "a 40 degree jump of 6 blocks: " + v);
        c.ok(com.exiledradio.trickytrialsbackport.breeze.BreezeAI.jumpVectorForAngle(new net.minecraft.util.math.Vec3d(0, 0, 0),
                new net.minecraft.util.math.Vec3d(40.5, 0, 0), 1.4F, 40) == null, "too far for 1.4 blocks a tick: no jump");

        BlockPos area = c.area();
        EntityBreeze breeze = spawnBreeze(c, area.add(6, 0, 16));
        // Each jump's landing spot is drawn from the breeze's own random (behind the target or in the middle circle);
        // a run of unreachable draws halves the count about one run in twenty. A fixed seed makes it the same every run.
        breeze.getRNG().setSeed(11L);
        EntityPig target = pig(c, area.getX() + 16.5, area.getY(), area.getZ() + 16.5);
        target.setNoAI(true);
        int charges = 0;
        double flightX = Double.NaN;
        boolean noDrag = true;
        boolean sawFlight = false;
        String dragSeen = "";
        for (int tick = 0; tick < 600; tick++) {
            breeze.setAttackTarget(target);
            breeze.ticksExisted++;
            double mx = breeze.motionX;
            double my = breeze.motionY;
            boolean flying = breeze.discardsFriction() && !breeze.onGround;
            breeze.onUpdate();
            // Both versions zero a motion under 0.003 (LivingEntity.aiStep), so only real motion is compared.
            if (flying && breeze.discardsFriction() && !breeze.onGround && !breeze.collidedHorizontally && Math.abs(mx) >= 0.003
                    && Math.abs(my) >= 0.003 && Math.abs(my - 0.08) >= 0.003) {
                sawFlight = true;
                // No drag: across stays, down gains exactly gravity.
                boolean same = Math.abs(breeze.motionX - mx) < 1e-9 && Math.abs(breeze.motionY - (my - 0.08)) < 1e-9;
                if (!same && noDrag) {
                    dragSeen = String.format("x %.5f -> %.5f, y %.5f -> %.5f", mx, breeze.motionX, my, breeze.motionY);
                }
                noDrag &= same;
            }
            for (Entity e : c.world.getEntitiesWithinAABB(EntityBreezeWindCharge.class, breeze.getEntityBoundingBox().grow(30))) {
                if (!e.getEntityData().getBoolean("ttbCounted")) {
                    e.getEntityData().setBoolean("ttbCounted", true);
                    charges++;
                }
                e.setDead();
            }
            target.heal(100);
            target.setPosition(area.getX() + 16.5, area.getY(), area.getZ() + 16.5);
        }
        com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.LOGGER.info("SELFTEST breeze jumps: {} jumps and {} charges in 600 ticks", breeze.jumps, charges);
        c.ok(sawFlight && noDrag, "a long jump flies without drag, as 1.21's discardFriction " + dragSeen);
        // Vanilla's cycle: 10 ticks inhaling, the flight, one shot (20 ticks), then the next jump: about one a 50 ticks.
        c.ok(breeze.jumps >= 6, "it jumps as often as 1.21's breeze: " + breeze.jumps + " jumps in 30 seconds");
        c.ok(charges >= breeze.jumps - 1 && charges <= breeze.jumps + 2, "one shot after each landing: " + charges + " charges, " + breeze.jumps + " jumps");
        breeze.setDead();
        target.setDead();
    }
}
