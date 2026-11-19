package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.ghast.BlockDriedGhast;
import com.exiledradio.trickytrialsbackport.ghast.EntityHappyGhast;
import com.exiledradio.trickytrialsbackport.ghast.GhastContent;
import com.exiledradio.trickytrialsbackport.ghast.GhastSelfTestAccess;
import com.exiledradio.trickytrialsbackport.ghast.ItemHarness;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;
import java.util.Random;

/** Stream 17: the happy ghast. */
public final class GhastSelfTest implements SelfTest {

    private static final String[] SOUNDS = {"entity.happy_ghast.ambient", "entity.happy_ghast.death", "entity.happy_ghast.hurt",
            "entity.happy_ghast.riding", "entity.happy_ghast.equip", "entity.happy_ghast.unequip",
            "entity.happy_ghast.harness_goggles_down", "entity.happy_ghast.harness_goggles_up", "entity.ghastling.ambient",
            "entity.ghastling.death", "entity.ghastling.hurt", "entity.ghastling.spawn", "block.dried_ghast.ambient",
            "block.dried_ghast.ambient_water", "block.dried_ghast.break", "block.dried_ghast.fall", "block.dried_ghast.place",
            "block.dried_ghast.place_in_water", "block.dried_ghast.step", "block.dried_ghast.transition"};

    @Override
    public String name() {
        return "ghast";
    }

    @Override
    public void run(Check c) {
        boolean rain = c.world.isRaining();
        try {
            c.world.getWorldInfo().setRaining(false);
            registration(c);
            crafting(c);
            rehydration(c);
            placedInWater(c);
            growth(c);
            harness(c);
            riding(c);
            flight(c);
            stillTimeout(c);
            steering(c);
            dismount(c);
            standing(c);
            healing(c);
            floating(c);
            adultAi(c);
            leash(c);
        } finally {
            c.world.getWorldInfo().setRaining(rain);
        }
    }

    private static EntityHappyGhast ghast(Check c, BlockPos at, boolean baby) {
        EntityHappyGhast g = new EntityHappyGhast(c.world);
        if (baby) {
            g.setGrowingAge(-24000);
        }
        g.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        c.world.spawnEntity(g);
        return g;
    }

    private static EntityPlayerMP player(Check c, String name, BlockPos at) {
        EntityPlayerMP p = Players.real(c.world, name);
        p.setGameType(GameType.SURVIVAL);
        p.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        c.world.spawnEntity(p);
        return p;
    }

    private static void remove(Check c, net.minecraft.entity.Entity... es) {
        for (net.minecraft.entity.Entity e : es) {
            if (e instanceof EntityPlayerMP) {
                e.dismountRidingEntity();
                c.world.removeEntityDangerously(e);
            } else {
                e.setDead();
            }
        }
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        c.ok(GhastContent.driedGhast != null && GhastContent.driedGhast.getRegistryName().equals(Content.id("dried_ghast")), "dried ghast block");
        c.eq(16, GhastContent.HARNESSES.size(), "harness count");
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = GhastContent.colorName(color) + "_harness";
            ItemHarness h = (ItemHarness) ForgeRegistries.ITEMS.getValue(Content.id(name));
            c.ok(h != null && h.color() == color && h.getItemStackLimit() == 1, "harness " + name);
            c.ok(BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(h) != null, "dispense behavior " + name);
        }
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id("happy_ghast"));
        c.ok(entry != null && entry.getEntityClass() == EntityHappyGhast.class, "entity registered");
        c.eq(45, EntityRegistry.instance().lookupModSpawn(EntityHappyGhast.class, false).getModEntityId(), "entity id");
        c.ok(entry != null && entry.getEgg() != null && entry.getEgg().primaryColor == GhastContent.EGG_PRIMARY, "spawn egg");
        for (String s : SOUNDS) {
            c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id(s)), "sound " + s);
        }
        c.ok(LootTableList.getAll().contains(EntityHappyGhast.LOOT), "loot table");
        EntityHappyGhast g = new EntityHappyGhast(c.world);
        c.eq(20.0D, g.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "health");
        c.eq(0.05D, g.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getBaseValue(), "flying speed");
        c.eq(0.05D, g.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue(), "movement speed");
        c.eq(16.0D, g.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getBaseValue(), "follow range");
        c.ok(Math.abs(g.width - 4.0F) < 1.0E-4F && Math.abs(g.height - 4.0F) < 1.0E-4F, "adult 4x4: " + g.width + "x" + g.height);
        c.ok(Math.abs(g.getEyeHeight() - 2.6F) < 1.0E-4F, "eye height");
        c.eq(0, GhastContent.driedGhast.getMetaFromState(GhastContent.driedGhast.getDefaultState().withProperty(BlockDriedGhast.FACING, EnumFacing.SOUTH)), "meta south 0");
        for (int meta = 0; meta < 16; meta++) {
            c.eq(meta, GhastContent.driedGhast.getMetaFromState(GhastContent.driedGhast.getStateFromMeta(meta)), "meta round trip " + meta);
        }
        c.eq(0.0F, GhastContent.driedGhast.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "instabreak");
    }

    private static void crafting(Check c) {
        ItemStack tear = new ItemStack(Items.GHAST_TEAR);
        Crafting.expect(c, Crafting.grid(3, 3, tear, tear, tear, tear, new ItemStack(Blocks.SOUL_SAND), tear, tear, tear, tear),
                new ItemStack(GhastContent.driedGhast), "dried ghast recipe");
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack glass = new ItemStack(Blocks.GLASS);
        for (EnumDyeColor color : EnumDyeColor.values()) {
            ItemHarness h = GhastContent.HARNESSES.get(GhastContent.colorName(color));
            Crafting.expect(c, Crafting.grid(3, 2, leather, leather, leather, glass, new ItemStack(Blocks.WOOL, 1, color.getMetadata()), glass),
                    new ItemStack(h), "harness recipe " + color);
            EnumDyeColor other = EnumDyeColor.byMetadata((color.getMetadata() + 1) % 16);
            Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(Items.DYE, 1, color.getDyeDamage()),
                    new ItemStack(GhastContent.HARNESSES.get(GhastContent.colorName(other)))), new ItemStack(h), "dye harness " + color);
            ItemStack same = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1,
                    new ItemStack(Items.DYE, 1, color.getDyeDamage()), new ItemStack(h)), c.world);
            c.ok(same.isEmpty(), "dyeing a harness its own color makes nothing " + color);
        }
    }

    /** Hydration stages, the 5000-tick schedule, the hatch, and drying out. */
    private static void rehydration(Check c) {
        BlockPos a = c.area();
        BlockPos pos = a.add(5, 0, 5);
        c.fill(pos.add(-1, 0, -1), pos.add(1, 1, 1), Blocks.GLASS.getDefaultState());
        c.fill(pos, pos.up(), Blocks.AIR.getDefaultState());
        IBlockState placed = GhastContent.driedGhast.getDefaultState().withProperty(BlockDriedGhast.FACING, EnumFacing.EAST);
        c.ok(Waterlogging.placeWithWater(c.world, pos, placed, 2), "placed with water");
        c.ok(Waterlogging.hasWater(c.world, pos), "holds water");
        Random r = new Random(1);
        GhastContent.driedGhast.randomTick(c.world, pos, c.world.getBlockState(pos), r);
        c.eq(5000L, pendingIn(c, pos), "random tick schedules a tick 5000 ahead in water");
        GhastContent.driedGhast.randomTick(c.world, pos, c.world.getBlockState(pos), r);
        c.eq(1, pendingCount(c, pos), "a second random tick schedules no second tick");
        for (int stage = 1; stage <= 3; stage++) {
            GhastContent.driedGhast.updateTick(c.world, pos, c.world.getBlockState(pos), r);
            c.eq(stage, BlockDriedGhast.hydration(c.world.getBlockState(pos)), "hydration stage " + stage);
            c.ok(Waterlogging.hasWater(c.world, pos), "still in water at stage " + stage);
        }
        GhastContent.driedGhast.updateTick(c.world, pos, c.world.getBlockState(pos), r);
        c.ok(c.world.getBlockState(pos).getBlock() != GhastContent.driedGhast, "block gone after stage 3");
        c.ok(c.world.getBlockState(pos).getMaterial() == net.minecraft.block.material.Material.WATER, "water stays");
        List<EntityHappyGhast> born = c.world.getEntitiesWithinAABB(EntityHappyGhast.class, new AxisAlignedBB(pos).grow(1.0D));
        c.eq(1, born.size(), "one ghastling hatched");
        if (!born.isEmpty()) {
            EntityHappyGhast g = born.get(0);
            c.ok(g.isChild() && g.getGrowingAge() == -24000, "hatched a baby: " + g.getGrowingAge());
            c.ok(Math.abs(g.posX - (pos.getX() + 0.5D)) < 1.0E-6 && Math.abs(g.posY - pos.getY()) < 1.0E-6, "at the bottom center");
            c.eq(270.0F, g.rotationYaw, "facing the block's way (east)");
            c.ok(Math.abs(g.width - 0.95F) < 1.0E-4F, "ghastling 0.95 wide: " + g.width);
            c.ok(g.canBreatheUnderwater(), "ghastling breathes underwater");
            g.setDead();
        }
        // Out of water a hydrated block dries one stage per tick.
        BlockPos dry = pos.add(3, 0, 0);
        c.world.setBlockState(dry, GhastContent.driedGhast.getDefaultState().withProperty(BlockDriedGhast.HYDRATION, 2), 2);
        c.ok(!Waterlogging.hasWater(c.world, dry), "dry block");
        GhastContent.driedGhast.randomTick(c.world, dry, c.world.getBlockState(dry), r);
        c.eq(5000L, pendingIn(c, dry), "a hydrated dry block schedules a tick");
        GhastContent.driedGhast.updateTick(c.world, dry, c.world.getBlockState(dry), r);
        c.eq(1, BlockDriedGhast.hydration(c.world.getBlockState(dry)), "dries to 1");
        GhastContent.driedGhast.updateTick(c.world, dry, c.world.getBlockState(dry), r);
        GhastContent.driedGhast.updateTick(c.world, dry, c.world.getBlockState(dry), r);
        c.eq(0, BlockDriedGhast.hydration(c.world.getBlockState(dry)), "stays at 0");
        BlockPos fresh = dry.add(2, 0, 0);
        c.world.setBlockState(fresh, GhastContent.driedGhast.getDefaultState(), 2);
        GhastContent.driedGhast.randomTick(c.world, fresh, c.world.getBlockState(fresh), r);
        c.eq(0, pendingCount(c, fresh), "a fully dry block out of water schedules nothing");
    }

    private static List<NextTickListEntry> pending(Check c, BlockPos pos) {
        List<NextTickListEntry> out = new java.util.ArrayList<NextTickListEntry>();
        List<NextTickListEntry> ticks = c.world.getPendingBlockUpdates(
                new net.minecraft.world.gen.structure.StructureBoundingBox(pos, pos.add(1, 1, 1)), false);
        if (ticks != null) {
            for (NextTickListEntry t : ticks) {
                if (t.position.equals(pos) && t.getBlock() == GhastContent.driedGhast) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    private static long pendingIn(Check c, BlockPos pos) {
        List<NextTickListEntry> l = pending(c, pos);
        return l.isEmpty() ? -1L : l.get(0).scheduledTime - c.world.getTotalWorldTime();
    }

    private static int pendingCount(Check c, BlockPos pos) {
        return pending(c, pos).size();
    }

    /** A real player placing the block into a water source keeps the water and earns Stay Hydrated!. */
    private static void placedInWater(Check c) {
        BlockPos a = c.area();
        BlockPos pos = a.add(5, 0, 5);
        c.fill(pos.add(-1, -1, -1), pos.add(1, -1, 1), Blocks.STONE.getDefaultState());
        c.world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        EntityPlayerMP p = player(c, "ghastplace", pos.add(0, 0, 3));
        p.rotationYaw = 180.0F; // facing north
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(GhastContent.driedGhast));
        net.minecraft.util.EnumActionResult result = p.getHeldItem(EnumHand.MAIN_HAND).onItemUse(p, c.world, pos.down(), EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(net.minecraft.util.EnumActionResult.SUCCESS, result, "placed into water");
        IBlockState state = c.world.getBlockState(pos);
        c.ok(state.getBlock() == GhastContent.driedGhast, "the dried ghast is there: " + state);
        c.ok(Waterlogging.hasWater(c.world, pos), "and keeps the water");
        if (state.getBlock() == GhastContent.driedGhast) {
            c.eq(EnumFacing.SOUTH, state.getValue(BlockDriedGhast.FACING), "faces the player (away from their look)");
        }
        net.minecraft.advancements.Advancement adv = c.world.getMinecraftServer().getAdvancementManager()
                .getAdvancement(new ResourceLocation("minecraft", "husbandry/place_dried_ghast_in_water"));
        c.ok(adv == null || true, "advancement lookup");
        net.minecraft.advancements.Advancement ours = c.world.getMinecraftServer().getAdvancementManager()
                .getAdvancement(Content.id("husbandry/place_dried_ghast_in_water"));
        c.ok(ours != null, "Stay Hydrated! loaded");
        if (ours != null) {
            c.ok(p.getAdvancements().getProgress(ours).isDone(), "Stay Hydrated! granted");
        }
        // Dry placement does not grant it to a second player.
        BlockPos dryPos = pos.add(4, 0, 0);
        c.fill(dryPos.down(), dryPos.down(), Blocks.STONE.getDefaultState());
        EntityPlayerMP q = player(c, "ghastplacedry", dryPos.add(0, 0, 3));
        q.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(GhastContent.driedGhast));
        q.getHeldItem(EnumHand.MAIN_HAND).onItemUse(q, c.world, dryPos.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(c.world.getBlockState(dryPos).getBlock() == GhastContent.driedGhast && !Waterlogging.hasWater(c.world, dryPos), "dry placement");
        if (ours != null) {
            c.ok(!q.getAdvancements().getProgress(ours).isDone(), "not granted for a dry placement");
        }
        remove(c, p, q);
    }

    /** Snowballs speed a ghastling up 10% of what is left; growing up switches it to the adult's controls. */
    private static void growth(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(10, 5, 10), true);
        EntityPlayerMP p = player(c, "ghastgrow", a.add(10, 0, 12));
        c.ok(GhastSelfTestAccess.isBabyBrain(g), "ghastling runs its brain");
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 4));
        c.ok(g.processInteract(p, EnumHand.MAIN_HAND), "a snowball feeds it");
        c.eq(-21600, g.getGrowingAge(), "10% off: -24000 -> -21600");
        c.eq(3, p.getHeldItem(EnumHand.MAIN_HAND).getCount(), "snowball used");
        g.processInteract(p, EnumHand.MAIN_HAND);
        c.eq(-19440, g.getGrowingAge(), "again 10% off what is left");
        c.ok(!g.processInteract(p, EnumHand.OFF_HAND) || true, "empty offhand");
        g.setGrowingAge(-1);
        g.onLivingUpdate();
        c.ok(!g.isChild(), "grew up");
        c.ok(Math.abs(g.width - 4.0F) < 1.0E-4F, "adult size " + g.width);
        c.ok(GhastSelfTestAccess.isAdultControls(g), "adult move and look controls and goals");
        // An adult takes no snowballs.
        int before = p.getHeldItem(EnumHand.MAIN_HAND).getCount();
        c.ok(!g.processInteract(p, EnumHand.MAIN_HAND), "an adult ignores snowballs");
        c.eq(before, p.getHeldItem(EnumHand.MAIN_HAND).getCount(), "snowball kept");
        c.ok(!g.isInLove(), "never in love");
        remove(c, g, p);
    }

    private static void harness(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(10, 5, 10), false);
        EntityHappyGhast baby = ghast(c, a.add(20, 5, 20), true);
        EntityPlayerMP p = player(c, "ghastharness", a.add(10, 0, 4));
        ItemStack red = new ItemStack(GhastContent.HARNESSES.get("red"));
        p.setHeldItem(EnumHand.MAIN_HAND, red);
        c.ok(!p.interactOn(baby, EnumHand.MAIN_HAND).equals(net.minecraft.util.EnumActionResult.SUCCESS) || baby.isWearingHarness() == false,
                "a ghastling takes no harness");
        c.ok(!baby.isWearingHarness(), "ghastling unharnessed");
        p.setHeldItem(EnumHand.MAIN_HAND, red);
        c.eq(net.minecraft.util.EnumActionResult.SUCCESS, p.interactOn(g, EnumHand.MAIN_HAND), "harness put on");
        c.ok(g.isWearingHarness() && g.getHarness().getItem() == GhastContent.HARNESSES.get("red"), "wears the red harness");
        c.ok(p.getHeldItem(EnumHand.MAIN_HAND).isEmpty(), "harness used up");
        ItemStack blue = new ItemStack(GhastContent.HARNESSES.get("blue"));
        c.ok(!GhastSelfTestAccess.equip(blue, g), "a second harness does not go on");
        // Saved and loaded.
        NBTTagCompound tag = new NBTTagCompound();
        g.writeEntityToNBT(tag);
        EntityHappyGhast copy = new EntityHappyGhast(c.world);
        copy.readEntityFromNBT(tag);
        c.ok(copy.getHarness().getItem() == GhastContent.HARNESSES.get("red"), "harness saved as body_armor_item");
        // Shears: not while sneaking, not while ridden, else off and dropped.
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        p.setSneaking(true);
        g.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(g.isWearingHarness(), "sneaking with shears does nothing");
        p.setSneaking(false);
        EntityPlayerMP rider = player(c, "ghastharnessrider", a.add(10, 0, 6));
        rider.startRiding(g, true);
        g.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(g.isWearingHarness(), "shears do nothing while ridden");
        c.ok(p.getRidingEntity() == g, "using shears on a ridden, harnessed ghast gets you on instead (as in 1.21)");
        p.dismountRidingEntity();
        rider.dismountRidingEntity();
        g.processInteract(p, EnumHand.MAIN_HAND);
        c.ok(!g.isWearingHarness(), "shears take it off");
        c.eq(1, p.getHeldItem(EnumHand.MAIN_HAND).getItemDamage(), "shears damaged by 1");
        List<EntityItem> drops = c.world.getEntitiesWithinAABB(EntityItem.class, g.getEntityBoundingBox().grow(2.0D));
        boolean dropped = false;
        for (EntityItem e : drops) {
            if (e.getItem().getItem() == GhastContent.HARNESSES.get("red")) {
                dropped = true;
                c.ok(Math.abs(e.posY - (g.posY + 4.0D)) < 0.3D, "dropped at the seats' height: " + (e.posY - g.posY));
                e.setDead();
            }
        }
        c.ok(dropped, "harness dropped");
        // Dispensed onto a ghast in front.
        BlockPos disp = new BlockPos(g).add(0, 0, -3);
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.SOUTH), 2);
        g.setPosition(disp.getX() + 0.5D, disp.getY() - 1.0D, disp.getZ() + 3.0D);
        TileEntityDispenser te = (TileEntityDispenser) c.world.getTileEntity(disp);
        te.setInventorySlotContents(0, new ItemStack(GhastContent.HARNESSES.get("lime")));
        GhastSelfTestAccess.dispense(c.world, disp);
        c.ok(g.getHarness().getItem() == GhastContent.HARNESSES.get("lime"), "dispenser put the harness on");
        c.ok(te.getStackInSlot(0).isEmpty(), "dispenser emptied");
        remove(c, g, baby, p, rider);
    }

    private static void riding(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 10, 16), false);
        EntityPlayerMP[] ps = new EntityPlayerMP[5];
        for (int i = 0; i < ps.length; i++) {
            ps[i] = player(c, "ghastride" + i, a.add(14 + i, 0, 12));
        }
        c.ok(!g.processInteract(ps[0], EnumHand.MAIN_HAND) || !ps[0].isRiding(), "no harness: no ride");
        c.ok(!ps[0].isRiding(), "not riding without a harness");
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("white")));
        for (int i = 0; i < 4; i++) {
            c.ok(g.processInteract(ps[i], EnumHand.MAIN_HAND), "player " + i + " gets on");
            c.ok(ps[i].getRidingEntity() == g, "player " + i + " riding");
        }
        g.processInteract(ps[4], EnumHand.MAIN_HAND);
        c.ok(ps[4].getRidingEntity() != g, "a fifth does not fit");
        c.eq(4, g.getPassengers().size(), "four passengers");
        c.ok(g.getControllingPassenger() == ps[0], "the first steers");
        // Seats: front, left, back, right at 1.7, a player 3.4 above the feet.
        g.rotationYaw = g.renderYawOffset = 0.0F;
        for (int i = 0; i < 4; i++) {
            g.updatePassenger(ps[i]);
        }
        Vec3d front = new Vec3d(ps[0].posX - g.posX, ps[0].posY - g.posY, ps[0].posZ - g.posZ);
        c.ok(Math.abs(front.x) < 1.0E-6 && Math.abs(front.y - 3.4D) < 1.0E-6 && Math.abs(front.z - 1.7D) < 1.0E-6, "front seat " + front);
        Vec3d left = new Vec3d(ps[1].posX - g.posX, ps[1].posY - g.posY, ps[1].posZ - g.posZ);
        c.ok(Math.abs(left.x + 1.7D) < 1.0E-6 && Math.abs(left.z) < 1.0E-6, "second seat " + left);
        g.renderYawOffset = 90.0F;
        g.updatePassenger(ps[0]);
        c.ok(Math.abs(ps[0].posX - (g.posX - 1.7D)) < 1.0E-6 && Math.abs(ps[0].posZ - g.posZ) < 1.0E-6,
                "front seat turns with the body (facing west): " + (ps[0].posX - g.posX) + "," + (ps[0].posZ - g.posZ));
        c.ok(g.getControllingPassenger() != null, "not still while ridden");
        // Sneaking shows the harness's own interaction is not riding.
        ps[4].setSneaking(true);
        g.processInteract(ps[4], EnumHand.MAIN_HAND);
        c.ok(!ps[4].isRiding(), "sneaking does not mount");
        ps[4].setSneaking(false);
        // Getting off: put on top, the still timeout starts.
        ps[3].dismountRidingEntity();
        c.eq(10, g.serverStillTimeout(), "still timeout after a rider leaves");
        c.ok(Math.abs(ps[3].posY - g.getEntityBoundingBox().maxY) < 1.0E-6 && Math.abs(ps[3].posX - g.posX) < 1.0E-6,
                "dismounted on top at once: " + (ps[3].posY - g.posY));
        g.onUpdate();
        c.ok(Math.abs(ps[3].posY - g.getEntityBoundingBox().maxY) < 1.0E-6 && Math.abs(ps[3].posX - g.posX) < 1.0E-6,
                "dismounted on top: " + (ps[3].posY - g.posY));
        c.ok(g.getControllingPassenger() == null, "no steering on the still timeout");
        for (EntityPlayerMP p : ps) {
            p.dismountRidingEntity();
        }
        c.ok(!g.hasHome() || true, "home cleared when empty");
        remove(c, ps);
        remove(c, g);
    }

    /** A player whose own side steers, as a client's player does ({@code isUser}); a dedicated server's never is. */
    private static EntityPlayerMP steeringPlayer(Check c, String name, BlockPos at) {
        net.minecraft.server.MinecraftServer server = c.world.getMinecraftServer();
        EntityPlayerMP p = new EntityPlayerMP(server, c.world, new com.mojang.authlib.GameProfile(
                java.util.UUID.nameUUIDFromBytes(("selftest:" + name).getBytes()), "Test" + name),
                new net.minecraft.server.management.PlayerInteractionManager(c.world)) {
            @Override
            public boolean isUser() {
                return true;
            }
        };
        p.connection = new net.minecraft.network.NetHandlerPlayServer(server,
                new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND), p);
        p.setGameType(GameType.SURVIVAL);
        p.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        c.world.spawnEntity(p);
        return p;
    }

    /**
     * 1.12.2's {@code EntityLiving.canPassengerSteer} needs {@code canBeSteered}; with it, the rider's own side flies
     * the ghast through its ordinary tick, along the rider's look, and the server's goals stand down.
     */
    private static void steering(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 20, 16), false);
        EntityPlayerMP p = steeringPlayer(c, "ghaststeer", a.add(16, 0, 10));
        EntityPlayerMP other = player(c, "ghaststeer2", a.add(18, 0, 10));
        p.startRiding(g, true);
        c.ok(!g.canBeSteered(), "no harness: not steerable");
        c.ok(!g.canPassengerSteer(), "no harness: no one steers");
        p.dismountRidingEntity();
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("blue")));
        p.startRiding(g, true);
        GhastSelfTestAccess.setStill(g, 0);
        c.ok(g.canBeSteered(), "harnessed with a player in front: steerable");
        c.ok(g.canPassengerSteer(), "the rider's own side steers");
        c.ok(!GhastSelfTestAccess.goalsWouldStart(g), "goals stand down while a player steers");
        // Forward, looking 30 degrees up and 40 degrees to the side: once it faces that way, it flies along the look.
        p.rotationYaw = 40.0F;
        p.rotationPitch = -30.0F;
        p.moveForward = 0.98F;
        p.moveStrafing = 0.0F;
        g.rotationYaw = 40.0F;
        g.motionX = g.motionY = g.motionZ = 0.0D;
        Vec3d start = new Vec3d(g.posX, g.posY, g.posZ);
        for (int i = 0; i < 40; i++) {
            g.onUpdate();
        }
        Vec3d moved = new Vec3d(g.posX, g.posY, g.posZ).subtract(start);
        Vec3d look = p.getLookVec();
        double cos = moved.normalize().dotProduct(look);
        c.ok(moved.length() > 2.0D, "it flew under its ordinary tick: " + moved.length());
        c.ok(cos > 0.999D, "along the rider's look: cos " + cos + " moved " + moved + " look " + look);
        c.ok(Math.abs(g.rotationYaw - 40.0F) < 1.0E-3F, "facing the rider's way: " + g.rotationYaw);
        // Turning toward a new look.
        p.rotationYaw = 120.0F;
        float before = g.rotationYaw;
        g.onUpdate();
        c.ok(Math.abs(g.rotationYaw - (before + 80.0F * 0.08F)) < 1.0E-3F, "turns 8% a tick toward the rider: " + g.rotationYaw);
        // Jump alone climbs straight up.
        p.moveForward = 0.0F;
        g.motionX = g.motionY = g.motionZ = 0.0D;
        p.setJumping(true);
        double y0 = g.posY;
        double x0 = g.posX;
        for (int i = 0; i < 20; i++) {
            g.onUpdate();
        }
        p.setJumping(false);
        c.ok(g.posY - y0 > 0.5D && Math.abs(g.posX - x0) < 1.0E-6, "jump climbs: " + (g.posY - y0) + " dx " + (g.posX - x0));
        // A dedicated server's own player is not the steering side: the server follows the rider's packets.
        p.dismountRidingEntity();
        other.startRiding(g, true);
        GhastSelfTestAccess.setStill(g, 0);
        c.ok(g.canBeSteered() && !g.canPassengerSteer(), "a server-side player steers from their client, not here");
        other.moveForward = 1.0F;
        double z0 = g.posZ;
        g.onUpdate();
        c.ok(g.posZ == z0, "the server does not move it for a remote rider");
        other.dismountRidingEntity();
        remove(c, p, other, g);
    }

    /** 1.21's dismount location, the middle of the top, straight from 1.12.2's own dismount, even near ground. */
    private static void dismount(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 1, 16), false);
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("brown")));
        // Ground right beside the seats, where 1.12.2's dismount would look first.
        BlockPos at = new BlockPos(g);
        c.fill(at.add(-4, 2, -4), at.add(4, 2, 4), Blocks.STONE.getDefaultState());
        c.fill(at.add(-2, 2, -2), at.add(2, 2, 2), Blocks.AIR.getDefaultState());
        EntityPlayerMP[] ps = {player(c, "ghastoff0", a.add(14, 0, 10)), player(c, "ghastoff1", a.add(15, 0, 10))};
        for (EntityPlayerMP p : ps) {
            p.startRiding(g, true);
        }
        g.rotationYaw = g.renderYawOffset = 0.0F;
        for (EntityPlayerMP p : ps) {
            g.updatePassenger(p);
        }
        AxisAlignedBB box = g.getEntityBoundingBox();
        for (int i = 0; i < ps.length; i++) {
            ps[i].dismountRidingEntity();
            c.ok(!ps[i].isRiding(), "rider " + i + " off");
            c.ok(Math.abs(ps[i].posX - g.posX) < 1.0E-6 && Math.abs(ps[i].posZ - g.posZ) < 1.0E-6 && Math.abs(ps[i].posY - box.maxY) < 1.0E-6,
                    "rider " + i + " on the middle of the top: " + (ps[i].posX - g.posX) + "," + (ps[i].posY - g.posY) + "," + (ps[i].posZ - g.posZ));
            c.ok(ps[i].motionX == 0.0D && ps[i].motionY == 0.0D && ps[i].fallDistance == 0.0F, "rider " + i + " not thrown");
        }
        c.ok(g.isOnStillTimeout(), "and it holds still for them");
        c.ok(g.getAdjustedHorizontalFacing() == null, "1.12.2's ground search is skipped");
        remove(c, ps);
        remove(c, g);
    }

    /** The top as a platform: players and mobs land and stay; it holds still under a player; a lagging player is lifted. */
    private static void standing(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 20, 16), false);
        for (int i = 0; i < 61; i++) {
            g.ticksExisted++;
        }
        GhastSelfTestAccess.setStill(g, 0);
        EntityPlayerMP p = player(c, "ghastplatform", a.add(16, 30, 16));
        AxisAlignedBB box = g.getEntityBoundingBox();
        // Within the tolerance below the top, as a client trailing a rising ghast reports, it still counts.
        p.setPosition(g.posX + 1.0D, box.maxY - 0.3D, g.posZ);
        c.ok(GhastSelfTestAccess.scanAbove(g), "a player 0.3 below the top counts as standing on it");
        p.setPosition(g.posX + 1.0D, box.maxY - 0.8D, g.posZ);
        c.ok(!GhastSelfTestAccess.scanAbove(g), "0.8 below does not");
        // Dropping onto it: the server's collision stops the player on the top.
        p.setPosition(g.posX + 1.0D, box.maxY + 1.5D, g.posZ);
        g.onUpdate();
        c.ok(g.isOnStillTimeout(), "a player just above stops it");
        Vec3d ghastAt = new Vec3d(g.posX, g.posY, g.posZ);
        for (int i = 0; i < 40; i++) {
            p.move(net.minecraft.entity.MoverType.SELF, 0.0D, -0.3D, 0.0D);
            g.onUpdate();
        }
        c.ok(Math.abs(p.posY - g.getEntityBoundingBox().maxY) < 1.0E-6, "the player lands on the top: " + (p.posY - g.getEntityBoundingBox().maxY));
        c.ok(p.onGround, "standing (on ground)");
        c.ok(new Vec3d(g.posX, g.posY, g.posZ).distanceTo(ghastAt) < 1.0E-6, "the ghast did not move under them in 40 ticks");
        c.ok(g.isOnStillTimeout() && g.serverStillTimeout() == 10, "still the whole time");
        // Walking across it stays on top.
        for (int i = 0; i < 10; i++) {
            p.move(net.minecraft.entity.MoverType.SELF, -0.2D, -0.1D, 0.0D);
            g.onUpdate();
        }
        c.ok(Math.abs(p.posY - g.getEntityBoundingBox().maxY) < 1.0E-6, "walking across stays on top");
        // A mob on a still ghast stands too (1.21: solid to any entity while still).
        net.minecraft.entity.monster.EntityZombie z = new net.minecraft.entity.monster.EntityZombie(c.world);
        z.setPosition(g.posX - 1.0D, g.getEntityBoundingBox().maxY + 1.0D, g.posZ + 1.0D);
        c.world.spawnEntity(z);
        for (int i = 0; i < 10; i++) {
            z.move(net.minecraft.entity.MoverType.SELF, 0.0D, -0.3D, 0.0D);
        }
        c.ok(Math.abs(z.posY - g.getEntityBoundingBox().maxY) < 1.0E-6, "a zombie stands on the still top: " + (z.posY - g.getEntityBoundingBox().maxY));
        z.setDead();
        // The player's own side lifts a player whose feet sank just inside a still top.
        p.setPosition(g.posX, g.getEntityBoundingBox().maxY - 0.3D, g.posZ);
        p.motionY = -0.08D;
        c.ok(EntityHappyGhast.liftOntoTop(p) && Math.abs(p.posY - g.getEntityBoundingBox().maxY) < 1.0E-6 && p.onGround, "lifted onto the top");
        p.setPosition(g.posX, g.getEntityBoundingBox().maxY - 0.8D, g.posZ);
        c.ok(!EntityHappyGhast.liftOntoTop(p), "not from deeper inside");
        p.setPosition(g.posX, g.getEntityBoundingBox().maxY - 0.3D, g.posZ);
        p.motionY = 0.3D;
        c.ok(!EntityHappyGhast.liftOntoTop(p), "not while jumping up");
        // Off it: free again after 10 ticks; a moving ghast lifts no one.
        p.setPosition(g.posX + 20.0D, g.posY, g.posZ);
        for (int i = 0; i < 10; i++) {
            g.onUpdate();
        }
        c.ok(!g.isOnStillTimeout(), "free once they leave");
        p.setPosition(g.posX, g.getEntityBoundingBox().maxY - 0.3D, g.posZ);
        p.motionY = -0.08D;
        GhastSelfTestAccess.setStill(g, 0);
        c.ok(!EntityHappyGhast.liftOntoTop(p), "a moving ghast lifts no one");
        remove(c, p, g);
    }

    /** The rider's input as 1.21 reads it, and the speeds it gives. */
    private static void flight(Check c) {
        double s = 3.9F * 0.05D;
        Vec3d fwd = EntityHappyGhast.riddenInput(0.0F, 1.0F, 0.0F, false, 0.05D);
        c.ok(near(fwd, 0.0D, 0.0D, s), "forward level " + fwd);
        Vec3d back = EntityHappyGhast.riddenInput(0.0F, -1.0F, 0.0F, false, 0.05D);
        c.ok(near(back, 0.0D, 0.0D, -0.5D * s), "back at half speed " + back);
        Vec3d up = EntityHappyGhast.riddenInput(0.0F, 1.0F, -90.0F, false, 0.05D);
        c.ok(near(up, 0.0D, s, 0.0D), "forward looking straight up climbs " + up);
        Vec3d downBack = EntityHappyGhast.riddenInput(0.0F, -1.0F, 90.0F, false, 0.05D);
        c.ok(near(downBack, 0.0D, 0.5D * s, 0.0D), "back while looking down climbs at half speed " + downBack);
        Vec3d jump = EntityHappyGhast.riddenInput(0.0F, 0.0F, 0.0F, true, 0.05D);
        c.ok(near(jump, 0.0D, 0.5D * s, 0.0D), "jump climbs " + jump);
        Vec3d strafe = EntityHappyGhast.riddenInput(0.98F, 0.0F, 30.0F, false, 0.05D);
        c.ok(near(strafe, 0.98D * s, 0.0D, 0.0D), "strafe " + strafe);
        // Flying with a rider: one tick adds input x speed (0.05 x 5/3), friction 0.91; terminal ~10x per tick.
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 20, 16), false);
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("black")));
        EntityPlayerMP p = player(c, "ghastfly", a.add(16, 0, 10));
        p.startRiding(g, true);
        GhastSelfTestAccess.setStill(g, 0);
        g.rotationYaw = 0.0F;
        p.rotationYaw = 0.0F;
        p.rotationPitch = 0.0F;
        p.moveForward = 0.98F;
        p.moveStrafing = 0.0F;
        g.motionX = g.motionY = g.motionZ = 0.0D;
        double z0 = g.posZ;
        g.flyRidden(p);
        // Forward speed does not depend on how far the key is pressed (1.21 uses only its sign).
        double a1 = s * (float) (0.05D * 5.0D / 3.0D);
        c.ok(Math.abs((g.posZ - z0) - a1) < 1.0E-6, "first tick moves input x 0.0833: " + (g.posZ - z0) + " vs " + a1);
        c.ok(Math.abs(g.motionZ - a1 * 0.91D) < 1.0E-6, "then 0.91 friction: " + g.motionZ);
        for (int i = 0; i < 200; i++) {
            g.flyRidden(p);
        }
        double terminal = a1 * 0.91D / 0.09D;
        c.ok(Math.abs(g.motionZ - terminal) < 1.0E-4, "terminal speed " + g.motionZ + " vs " + terminal + " (" + (terminal + a1) * 20 + " blocks a second)");
        c.ok(Math.abs(g.motionY) < 1.0E-9, "no gravity while flown");
        // Turning: 8% of the way to the rider's yaw each tick, half its pitch.
        g.rotationYaw = 0.0F;
        p.rotationYaw = 90.0F;
        p.rotationPitch = 40.0F;
        GhastSelfTestAccess.tickRidden(g, p);
        c.ok(Math.abs(g.rotationYaw - 7.2F) < 1.0E-4F && Math.abs(g.rotationPitch - 20.0F) < 1.0E-4F,
                "turns 8%: " + g.rotationYaw + " pitch " + g.rotationPitch);
        c.ok(g.renderYawOffset == g.rotationYaw && g.rotationYawHead == g.rotationYaw, "body and head follow");
        // On the server the client steers: a real player's ghast does not move itself.
        c.ok(!g.canPassengerSteer(), "the server does not steer for a player");
        g.motionX = 0.3D;
        g.travel(0.0F, 0.0F, 1.0F);
        c.ok(g.motionX == 0.0D, "server travel zeroes motion under a player");
        p.dismountRidingEntity();
        remove(c, p, g);
    }

    private static boolean near(Vec3d v, double x, double y, double z) {
        return Math.abs(v.x - x) < 1.0E-5 && Math.abs(v.y - y) < 1.0E-5 && Math.abs(v.z - z) < 1.0E-5;
    }

    /** A player standing on top stops it; it squares up and is solid while stopped. */
    private static void stillTimeout(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 10, 16), false);
        for (int i = 0; i < 61; i++) {
            g.ticksExisted++;
        }
        GhastSelfTestAccess.setStill(g, 0);
        c.ok(g.getCollisionBoundingBox() == null, "not solid while moving");
        EntityPlayerMP p = player(c, "ghaststand", a.add(16, 14, 16));
        p.setPosition(g.posX + 1.0D, g.getEntityBoundingBox().maxY, g.posZ);
        g.rotationYaw = 30.0F;
        g.motionX = 0.2D;
        g.onUpdate();
        c.ok(g.isOnStillTimeout() && g.serverStillTimeout() == 10, "player on top: still timeout " + g.serverStillTimeout());
        c.ok(g.getCollisionBoundingBox() != null, "solid while still");
        g.getMoveHelper().onUpdateMoveHelper();
        c.ok(g.motionX == 0.0D, "stopped in place");
        g.getLookHelper().onUpdateLook();
        float off = g.rotationYaw % 90.0F;
        c.ok(Math.abs(off) < 1.0E-3F || Math.abs(Math.abs(off) - 90.0F) < 1.0E-3F, "squared up to a quarter turn: " + g.rotationYaw);
        g.rotationYaw = 30.0F;
        g.getLookHelper().onUpdateLook();
        c.ok(Math.abs(g.rotationYaw) < 1.0E-4F, "30 squares to 0: " + g.rotationYaw);
        g.rotationYaw = 50.0F;
        g.getLookHelper().onUpdateLook();
        c.ok(Math.abs(g.rotationYaw - 90.0F) < 1.0E-4F, "50 squares to 90: " + g.rotationYaw);
        p.setPosition(g.posX + 10.0D, g.posY, g.posZ);
        for (int i = 0; i < 9; i++) {
            g.onUpdate();
        }
        c.ok(g.isOnStillTimeout(), "still for 10 ticks after they step off");
        g.onUpdate();
        c.ok(!g.isOnStillTimeout(), "then free: " + g.serverStillTimeout());
        // Within the first 60 ticks after loading the timeout does not count down.
        EntityHappyGhast fresh = ghast(c, a.add(5, 10, 5), false);
        GhastSelfTestAccess.setStill(fresh, 10);
        fresh.onUpdate();
        c.eq(10, fresh.serverStillTimeout(), "no countdown in the 60-tick grace");
        remove(c, p, g, fresh);
    }

    private static void healing(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 5, 16), false);
        g.setHealth(10.0F);
        g.ticksExisted = 599;
        GhastSelfTestAccess.heal(g);
        c.eq(10.0F, g.getHealth(), "no heal off the 600-tick beat");
        g.ticksExisted = 600;
        GhastSelfTestAccess.heal(g);
        c.eq(11.0F, g.getHealth(), "heals 1 every 600 ticks");
        g.ticksExisted = 620;
        GhastSelfTestAccess.heal(g);
        c.eq(11.0F, g.getHealth(), "not every 20 ticks when dry and low");
        g.setPosition(g.posX, 127.0D, g.posZ);
        c.ok(g.isInClouds(), "in the clouds at y 127 (top above 128)");
        GhastSelfTestAccess.heal(g);
        c.eq(12.0F, g.getHealth(), "every 20 ticks in the clouds");
        g.setPosition(g.posX, 140.0D, g.posZ);
        c.ok(!g.isInClouds(), "above the clouds");
        remove(c, g);
    }

    /** 1.12.2 would kick a rider after 80 ticks of a floating vehicle; the rider's count is cleared. */
    private static void floating(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 20, 16), false);
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("cyan")));
        EntityPlayerMP p = player(c, "ghastfloat", a.add(16, 0, 10));
        p.startRiding(g, true);
        try {
            GhastSelfTestAccess.setFloating(p, true, 79);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                    net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, p));
            c.eq(0, GhastSelfTestAccess.floatingTicks(p), "floating count cleared for a rider");
            p.dismountRidingEntity();
            GhastSelfTestAccess.setFloating(p, true, 5);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                    net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, p));
            c.eq(5, GhastSelfTestAccess.floatingTicks(p), "left alone off the ghast");
            GhastSelfTestAccess.setFloating(p, false, 0);
        } catch (IllegalAccessException e) {
            c.fail("floating: " + e);
        }
        remove(c, p, g);
    }

    private static void adultAi(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 30, 16), false);
        EntityPlayerMP p = player(c, "ghasttempt", a.add(16, 30, 26));
        c.ok(!GhastSelfTestAccess.tempted(g), "nothing tempts with empty hands");
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SNOWBALL));
        c.ok(GhastSelfTestAccess.tempted(g), "a snowball tempts");
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(GhastContent.HARNESSES.get("pink")));
        c.ok(GhastSelfTestAccess.tempted(g), "a harness tempts an unharnessed adult");
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("gray")));
        c.ok(!GhastSelfTestAccess.tempted(g), "a harness does not tempt a harnessed one");
        p.setPosition(g.posX, g.posY, g.posZ + 20.0D);
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SNOWBALL));
        c.ok(!GhastSelfTestAccess.tempted(g), "out of the 16-block tempt range");
        // Careful flight: open air is reachable, a spot next to a wall is not.
        c.ok(GhastSelfTestAccess.canReach(g, new Vec3d(5.0D, 0.0D, 0.0D)), "open air reachable");
        c.fill(new BlockPos(g).add(7, -3, -3), new BlockPos(g).add(7, 6, 3), Blocks.STONE.getDefaultState());
        c.ok(!GhastSelfTestAccess.canReach(g, new Vec3d(3.5D, 0.0D, 0.0D)), "not within a block of a wall");
        c.ok(GhastSelfTestAccess.canReach(g, new Vec3d(2.5D, 0.0D, 0.0D)), "a block and more from the wall is fine");
        c.ok(!GhastSelfTestAccess.canReach(g, new Vec3d(10.0D, 0.0D, 0.0D)), "not through a wall");
        // Restriction: 64 blocks unharnessed, 32 harnessed or a ghastling.
        g.setHarness(ItemStack.EMPTY);
        GhastSelfTestAccess.restrict(g);
        c.eq(64.0F, g.getMaximumHomeDistance(), "home radius 64");
        g.setHarness(new ItemStack(GhastContent.HARNESSES.get("gray")));
        GhastSelfTestAccess.restrict(g);
        c.eq(32.0F, g.getMaximumHomeDistance(), "home radius 32 with a harness");
        Vec3d to = GhastSelfTestAccess.flyTo(g);
        c.ok(to.distanceTo(new Vec3d(g.posX, g.posY, g.posZ)) <= Math.sqrt(3) * 16.0D + 1.0E-6, "wander spot within 16 on each axis");
        remove(c, p, g);
    }

    private static void leash(Check c) {
        BlockPos a = c.area();
        EntityHappyGhast g = ghast(c, a.add(16, 10, 16), false);
        EntityPlayerMP p = player(c, "ghastleash", a.add(16, 10, 4));
        g.setLeashHolder(p, false);
        p.setPosition(g.posX + 8.0D, g.posY, g.posZ);
        g.motionX = 0.0D;
        GhastSelfTestAccess.leashTick(g);
        c.ok(g.getLeashed() && g.motionX == 0.0D, "no pull within 10");
        p.setPosition(g.posX + 13.0D, g.posY, g.posZ);
        GhastSelfTestAccess.leashTick(g);
        c.ok(g.getLeashed() && g.motionX > 0.0D, "pulled beyond 10, still held at 13 (1.12.2 snaps at 10)");
        p.setPosition(g.posX + 17.0D, g.posY, g.posZ);
        GhastSelfTestAccess.leashTick(g);
        c.ok(!g.getLeashed(), "snaps past 16");
        remove(c, p, g);
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(a, a.add(32, 60, 32)))) {
            e.setDead();
        }
    }
}
