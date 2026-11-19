package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.WindBurst;
import com.exiledradio.trickytrialsbackport.copper.CopperBlocks;
import com.exiledradio.trickytrialsbackport.copper.CopperContent;
import com.exiledradio.trickytrialsbackport.copper.CopperStage;
import com.exiledradio.trickytrialsbackport.copper.Weathering;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Random;

/**
 * Stream 2: 1.21's copper blocks, and the copies of Caves &amp; Cliffs' copper. Runs both ways: alone (copies
 * registered) and with Caves &amp; Cliffs in {@code eclipse/mods} (none registered, its blocks used).
 */
public final class CopperSelfTest implements SelfTest {

    @Override
    public String name() {
        return "copper";
    }

    private static Block ours(String name) {
        Block b = ForgeRegistries.BLOCKS.getValue(Content.id(name));
        return b != null && Content.id(name).equals(b.getRegistryName()) ? b : null;
    }

    @Override
    public void run(Check c) {
        // Honeycomb for the test: a slimeball takes the ore name (no bee mod in the test run).
        OreDictionary.registerOre("honeycomb", Items.SLIME_BALL);
        // The owner provides copper: installed with its copper switched on (switched off, we copy).
        boolean owner = !Owners.copy("copper_block");
        ownership(c, owner);
        blocks(c);
        recipes(c, owner);
        BlockPos area = c.area();
        aging(c, area);
        interactions(c, area);
        bulbs(c, area);
        doors(c, area);
        doorsInWater(c);
        lightning(c, area);
        if (!owner) {
            ore(c);
        }
    }

    private void ownership(Check c, boolean owner) {
        c.eq(!owner, CopperContent.copies, "copies only when Caves & Cliffs does not provide copper");
        c.eq(owner ? 40 : 82, CopperContent.all().size(), "block count");
        c.eq(Content.MOD_ID, BlockOwnership.owner("copper_bulb"), "we own the 1.21 shapes");
        c.ok(String.valueOf(BlockOwnership.owner("waxed_weathered_cut_copper_double_slab")).startsWith(Owners.CAVES_AND_CLIFFS),
                "Caves & Cliffs owns 1.17's");
        for (CopperStage stage : CopperStage.values()) {
            for (boolean waxed : new boolean[] {false, true}) {
                for (String shape : CopperContent.OWNED_SHAPES) {
                    String name = CopperContent.ownedName(shape, stage, waxed);
                    c.eq(!owner, ours(name) != null, name + (owner ? " not copied" : " copied"));
                    if (owner) {
                        Block theirs = Owners.block(name);
                        c.ok(theirs != null && Owners.CAVES_AND_CLIFFS.equals(theirs.getRegistryName().getNamespace()), name + " is theirs");
                    }
                }
                for (String shape : CopperContent.SHAPES) {
                    c.ok(ours(CopperContent.name(shape, stage, waxed)) != null, CopperContent.name(shape, stage, waxed) + " registered");
                }
            }
        }
        if (!owner) {
            c.ok(ours("copper_ore") != null && ours("raw_copper_block") != null && ours("cut_copper_double_slab") != null
                    && ours("waxed_oxidized_cut_copper_double_slab") != null, "ores and double slabs copied");
            c.ok(Owners.ours("copper_ingot") != null && Owners.ours("raw_copper") != null, "ingot and raw copper copied");
            Block slab = ours("exposed_cut_copper_slab");
            IBlockState top = slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);
            c.eq(8, slab.getMetaFromState(top), "slab top is metadata 8, as the owner's");
            Item none = ForgeRegistries.ITEMS.getValue(Content.id("cut_copper_double_slab"));
            c.ok(none == null || !Content.id("cut_copper_double_slab").equals(none.getRegistryName()), "double slabs have no item");
        }
    }

    private void blocks(Check c) {
        for (Block b : CopperContent.all()) {
            IBlockState s = b.getDefaultState();
            String n = b.getRegistryName().getPath();
            if (n.equals("copper_ore") || n.equals("raw_copper_block")) {
                continue;
            }
            c.eq(Material.IRON, s.getMaterial(), n + " is metal");
            c.eq(3.0F, s.getBlockHardness(null, null), n + " hardness 3");
            c.ok(Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-3, n + " resistance 6");
            boolean shouldAge = !n.startsWith("waxed_") && !n.startsWith("oxidized_") && !n.contains("oxidized_");
            c.eq(shouldAge, b.getTickRandomly(), n + (shouldAge ? " ages" : " never ages"));
        }
        Block bulb = ours("copper_bulb");
        c.eq(Content.id("block.copper_bulb.place"), bulb.getSoundType().getPlaceSound().getRegistryName(), "bulb sound");
        c.eq(Content.id("block.copper_grate.step"), ours("exposed_copper_grate").getSoundType().getStepSound().getRegistryName(), "grate sound");
        Block grate = ours("copper_grate");
        c.ok(!grate.getDefaultState().isOpaqueCube() && grate.getDefaultState().getLightOpacity() == 0, "grates are see-through");
        c.ok(ours("copper_door").canHarvestBlock(null, BlockPos.ORIGIN, null), "doors drop without a tool");
        c.eq(1, ours("chiseled_copper").getHarvestLevel(ours("chiseled_copper").getDefaultState()), "a stone pickaxe for the rest");
    }

    private void recipes(Check c, boolean owner) {
        ItemStack ingot = new ItemStack(Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot"));
        ItemStack e = ItemStack.EMPTY;
        ItemStack exposed = new ItemStack(Owners.block("exposed_copper"));
        ItemStack rod = new ItemStack(Items.BLAZE_ROD);
        ItemStack red = new ItemStack(Items.REDSTONE);
        Crafting.expect(c, Crafting.grid(3, 3, e, exposed, e, exposed, rod, exposed, e, red, e),
                new ItemStack(ours("exposed_copper_bulb"), 4), "exposed copper makes exposed bulbs");
        ItemStack waxedBlock = new ItemStack(Owners.block("waxed_copper_block"));
        Crafting.expect(c, Crafting.grid(3, 3, e, waxedBlock, e, waxedBlock, rod, waxedBlock, e, red, e),
                new ItemStack(ours("waxed_copper_bulb"), 4), "waxed copper makes waxed bulbs");
        Crafting.expect(c, Crafting.grid(3, 3, e, exposed, e, exposed, e, exposed, e, exposed, e),
                new ItemStack(ours("exposed_copper_grate"), 4), "four exposed copper make four grates");
        ItemStack slab = new ItemStack(Owners.block("weathered_cut_copper_slab"));
        Crafting.expect(c, Crafting.grid(1, 2, slab, slab), new ItemStack(ours("weathered_chiseled_copper")), "two slabs make chiseled copper");
        Crafting.expect(c, Crafting.grid(2, 3, ingot, ingot, ingot, ingot, ingot, ingot),
                new ItemStack(Item.getItemFromBlock(Blocks.AIR) == null ? Items.AIR : ForgeRegistries.ITEMS.getValue(Content.id("copper_door")), 3),
                "six ingots make three doors");
        Crafting.expect(c, Crafting.grid(2, 2, ingot, ingot, ingot, ingot), new ItemStack(ours("copper_trapdoor")), "four ingots make a trapdoor");
        Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(ours("oxidized_copper_bulb")), new ItemStack(Items.SLIME_BALL)),
                new ItemStack(ours("waxed_oxidized_copper_bulb")), "honeycomb waxes a bulb in the grid");
        if (!owner) {
            Crafting.expect(c, Crafting.grid(3, 3, ingot, ingot, ingot, ingot, ingot, ingot, ingot, ingot, ingot),
                    new ItemStack(ours("copper_block")), "nine ingots make a copy's block");
            ItemStack cut = new ItemStack(ours("cut_copper"));
            Crafting.expect(c, Crafting.grid(3, 3, cut, e, e, cut, cut, e, cut, cut, cut), new ItemStack(ours("cut_copper_stairs"), 4), "cut stairs");
            ItemStack raw = new ItemStack(Owners.ours("raw_copper"));
            ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(raw);
            c.ok(smelted.getItem() == Owners.ours("copper_ingot"), "raw copper smelts to an ingot");
        }
    }

    private void aging(Check c, BlockPos area) {
        Random rand = new Random(1);
        BlockPos pos = area.add(2, 0, 2);
        Block bulb = ours("copper_bulb");
        c.world.setBlockState(pos, bulb.getDefaultState().withProperty(CopperBlocks.Bulb.LIT, true), 2);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(pos);
            s.getBlock().randomTick(c.world, pos, s, rand);
        }
        IBlockState end = c.world.getBlockState(pos);
        c.eq(ours("oxidized_copper_bulb"), end.getBlock(), "a lone bulb ages to oxidized");
        c.ok(end.getValue(CopperBlocks.Bulb.LIT), "and stays lit");
        c.eq(4, end.getLightValue(c.world, pos), "an oxidized lit bulb gives light 4");

        BlockPos w = area.add(4, 0, 2);
        c.world.setBlockState(w, ours("waxed_copper_grate").getDefaultState(), 2);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(w);
            s.getBlock().randomTick(c.world, w, s, rand);
        }
        c.eq(ours("waxed_copper_grate"), c.world.getBlockState(w).getBlock(), "waxed copper never ages");

        BlockPos young = area.add(8, 0, 2);
        c.world.setBlockState(young, ours("chiseled_copper").getDefaultState(), 2);
        c.world.setBlockState(young.east(), ours("exposed_copper_grate").getDefaultState(), 2);
        c.eq(0.0F, Weathering.ageChance(c.world, young.east(), 1), "copper waits while less oxidized copper is near");
        c.ok(Weathering.ageChance(c.world, young, 0) > 0.0F, "the younger one may age");
        c.world.setBlockState(young.up(), Owners.block("weathered_cut_copper").getDefaultState(), 2);
        c.eq(1, Weathering.stage(ours("exposed_copper_grate")), "stage of an exposed grate");
        c.eq(2, Weathering.stage(Owners.block("weathered_cut_copper")), "the owner's (or copied) copper counts too");
    }

    private void interactions(Check c, BlockPos area) {
        FakePlayer player = c.player("copper", area.up());
        BlockPos pos = area.add(12, 0, 2);
        c.world.setBlockState(pos, ours("exposed_chiseled_copper").getDefaultState(), 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        click(player, pos);
        c.eq(ours("chiseled_copper"), c.world.getBlockState(pos).getBlock(), "an axe scrapes a stage off");
        c.eq(1, player.getHeldItemMainhand().getItemDamage(), "and wears the axe");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL, 2));
        click(player, pos);
        c.eq(ours("waxed_chiseled_copper"), c.world.getBlockState(pos).getBlock(), "honeycomb waxes it");
        c.eq(1, player.getHeldItemMainhand().getCount(), "using one");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        click(player, pos);
        c.eq(ours("chiseled_copper"), c.world.getBlockState(pos).getBlock(), "an axe takes the wax off first");
        click(player, pos);
        c.eq(ours("chiseled_copper"), c.world.getBlockState(pos).getBlock(), "nothing to scrape from unaffected");
    }

    private static void click(FakePlayer player, BlockPos pos) {
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.UP, new Vec3d(pos)));
    }

    private void bulbs(Check c, BlockPos area) {
        BlockPos pos = area.add(16, 0, 2);
        c.world.setBlockState(pos, ours("copper_bulb").getDefaultState());
        c.world.setBlockState(pos.east(), Blocks.REDSTONE_BLOCK.getDefaultState());
        IBlockState s = c.world.getBlockState(pos);
        c.ok(s.getValue(CopperBlocks.Bulb.LIT) && s.getValue(CopperBlocks.Bulb.POWERED), "a pulse lights it, in the same tick");
        c.eq(15, s.getLightValue(c.world, pos), "light 15");
        c.eq(15, s.getComparatorInputOverride(c.world, pos), "comparator 15 when lit");
        c.world.setBlockToAir(pos.east());
        s = c.world.getBlockState(pos);
        c.ok(s.getValue(CopperBlocks.Bulb.LIT) && !s.getValue(CopperBlocks.Bulb.POWERED), "stays lit when the power goes");
        c.world.setBlockState(pos.east(), Blocks.REDSTONE_BLOCK.getDefaultState());
        s = c.world.getBlockState(pos);
        c.ok(!s.getValue(CopperBlocks.Bulb.LIT), "the next pulse turns it off");
        c.eq(0, s.getComparatorInputOverride(c.world, pos), "comparator 0 when off");
        c.eq(12, ours("exposed_copper_bulb").getDefaultState().withProperty(CopperBlocks.Bulb.LIT, true).getLightValue(), "exposed lit: 12");
        c.eq(8, ours("waxed_weathered_copper_bulb").getDefaultState().withProperty(CopperBlocks.Bulb.LIT, true).getLightValue(),
                "weathered lit: 8");
        c.world.setBlockToAir(pos.east());
    }

    private void doors(Check c, BlockPos area) {
        BlockPos pos = area.add(2, 0, 8);
        Block door = ours("copper_door");
        ItemDoor.placeDoor(c.world, pos, EnumFacing.EAST, door, false);
        FakePlayer player = c.player("doors", pos.west());
        door.onBlockActivated(c.world, pos.up(), c.world.getBlockState(pos.up()), player, EnumHand.MAIN_HAND, EnumFacing.WEST, 0.5F, 0.5F, 0.5F);
        c.ok(BlockDoor.isOpen(c.world, pos), "a copper door opens by hand (unlike iron)");

        // Aging changes both halves; the upper half never acts alone.
        Random rand = new Random(2);
        for (int i = 0; i < 400 && c.world.getBlockState(pos).getBlock() == door; i++) {
            IBlockState up = c.world.getBlockState(pos.up());
            up.getBlock().randomTick(c.world, pos.up(), up, rand);
        }
        c.eq(door, c.world.getBlockState(pos).getBlock(), "the upper half's ticks do nothing");
        for (int i = 0; i < 400 && c.world.getBlockState(pos).getBlock() == door; i++) {
            IBlockState low = c.world.getBlockState(pos);
            low.getBlock().randomTick(c.world, pos, low, rand);
        }
        Block aged = c.world.getBlockState(pos).getBlock();
        c.eq(ours("exposed_copper_door"), aged, "the door ages");
        c.eq(aged, c.world.getBlockState(pos.up()).getBlock(), "both halves together");
        c.ok(BlockDoor.isOpen(c.world, pos), "still open");

        // Waxing through the upper half waxes both.
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL));
        click(player, pos.up());
        c.ok(c.world.getBlockState(pos).getBlock() == ours("waxed_exposed_copper_door")
                && c.world.getBlockState(pos.up()).getBlock() == ours("waxed_exposed_copper_door"), "waxing the top waxes both halves");
        c.ok(ours("waxed_exposed_copper_door").getItemDropped(c.world.getBlockState(pos), rand, 0) == ForgeRegistries.ITEMS
                .getValue(Content.id("waxed_exposed_copper_door")), "it drops its own door item");

        // Wind opens copper trapdoors and doors.
        BlockPos trap = area.add(8, 0, 8);
        c.world.setBlockState(trap, ours("copper_trapdoor").getDefaultState());
        WindBurst.burst(c.world, null, trap.getX() - 0.25, trap.getY() + 0.5, trap.getZ() + 0.5,
                new WindBurst.Settings(1.2F, 1.22F, false, Content.sound("entity.wind_charge.wind_burst"), true, null));
        c.ok(c.world.getBlockState(trap).getValue(BlockTrapDoor.OPEN), "a wind charge opens a copper trapdoor");
        ours("copper_trapdoor").onBlockActivated(c.world, trap, c.world.getBlockState(trap), player, EnumHand.MAIN_HAND, EnumFacing.UP,
                0.5F, 0.5F, 0.5F);
        c.ok(!c.world.getBlockState(trap).getValue(BlockTrapDoor.OPEN), "and a hand closes it");
    }

    /**
     * 1.21.11: a door placed into water (still or flowing) displaces it, holds none, and water never flows into
     * or breaks it. Vanilla's oak and iron doors and the pale oak door are checked alongside ours.
     */
    private void doorsInWater(Check c) {
        Item[] doors = {Items.OAK_DOOR, Items.IRON_DOOR, ForgeRegistries.ITEMS.getValue(Content.id("pale_oak_door")),
                ForgeRegistries.ITEMS.getValue(Content.id("copper_door")), ForgeRegistries.ITEMS.getValue(Content.id("oxidized_copper_door")),
                ForgeRegistries.ITEMS.getValue(Content.id("waxed_copper_door"))};
        c.ok(com.exiledradio.trickytrialsbackport.asm.DoorWaterTransformer.applied, "P25 applied (Fluidlogged API's flow)");
        BlockPos floor = Pools.pool(c, 2);
        FakePlayer player = c.player("water doors", floor.up());
        for (int i = 0; i < doors.length; i++) {
            if (doors[i] == null) {
                continue;
            }
            // Still water, two deep: both halves go into water.
            BlockPos at = floor.add(1 + 2 * i, 0, 2);
            place(c, player, doors[i], at);
            Pools.settle(c.world);
            dryDoor(c, at.up(), doors[i] + " in still water");
        }
        // Flowing water: a door placed into a flowing block beside a source.
        BlockPos area = c.area();
        for (int i = 0; i < doors.length; i++) {
            if (doors[i] == null) {
                continue;
            }
            BlockPos source = area.add(2 + 5 * i, 0, 4);
            BlockPos at = source.add(0, -1, 1);
            c.world.setBlockState(source, Blocks.WATER.getDefaultState(), 2);
            c.world.setBlockState(at.up(), Blocks.FLOWING_WATER.getDefaultState().withProperty(net.minecraft.block.BlockLiquid.LEVEL, 1), 2);
            c.world.scheduleUpdate(at.up(), Blocks.FLOWING_WATER, 1);
            c.world.scheduleUpdate(source, Blocks.WATER, 1);
            IBlockState flowing = c.world.getBlockState(at.up());
            c.ok(flowing.getMaterial() == Material.WATER && flowing.getValue(net.minecraft.block.BlockLiquid.LEVEL) > 0,
                    doors[i] + ": flowing water at the spot (got " + flowing + ")");
            place(c, player, doors[i], at);
            Pools.settle(c.world);
            dryDoor(c, at.up(), doors[i] + " in flowing water");
            // And a door on a dry floor next to a new source: the water flows around it, not into it.
            BlockPos dry = area.add(2 + 5 * i, -1, 14);
            place(c, player, doors[i], dry);
            c.world.setBlockState(dry.up().west(), Blocks.FLOWING_WATER.getDefaultState());
            Pools.settle(c.world);
            dryDoor(c, dry.up(), doors[i] + " beside a source");
        }
    }

    private static void place(Check c, FakePlayer player, Item door, BlockPos floor) {
        player.rotationYaw = 0;
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(door));
        door.onItemUse(player, c.world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
    }

    private static void dryDoor(Check c, BlockPos lower, String what) {
        IBlockState low = c.world.getBlockState(lower);
        IBlockState up = c.world.getBlockState(lower.up());
        boolean both = low.getBlock() instanceof BlockDoor && up.getBlock() == low.getBlock()
                && low.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER && up.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER;
        c.ok(both, what + ": both halves stay (got " + low + " / " + up + ")");
        // Fluidlogged API waterlogs vanilla's doors on purpose (its built-in list); ours stay dry, as in 1.21.
        if (!low.getBlock().getClass().getName().startsWith("com.exiledradio.")) {
            return;
        }
        c.ok(git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils.getFluidState(c.world, lower).isEmpty()
                && git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils.getFluidState(c.world, lower.up()).isEmpty(),
                what + ": holds no water");
    }

    private void lightning(Check c, BlockPos area) {
        BlockPos pos = area.add(14, 0, 8);
        c.world.setBlockState(pos, ours("oxidized_chiseled_copper").getDefaultState(), 2);
        c.world.setBlockState(pos.east(), ours("oxidized_copper_grate").getDefaultState(), 2);
        c.world.addWeatherEffect(new EntityLightningBolt(c.world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, false));
        c.eq(ours("chiseled_copper"), c.world.getBlockState(pos).getBlock(), "lightning cleans the struck block");
        c.ok(Weathering.stage(c.world.getBlockState(pos.east()).getBlock()) < 3, "and scrapes its neighbor");
    }

    private void ore(Check c) {
        BlockPos spawn = c.world.getSpawnPoint();
        Block ore = ours("copper_ore");
        int found = 0;
        for (int dx = -48; dx < 48; dx++) {
            for (int dz = -48; dz < 48; dz++) {
                for (int y = 1; y < 100; y++) {
                    BlockPos p = new BlockPos((spawn.getX() & ~15) + dx, y, (spawn.getZ() & ~15) + dz);
                    if (c.world.isBlockLoaded(p) && c.world.getBlockState(p).getBlock() == ore) {
                        found++;
                    }
                }
            }
        }
        c.ok(found > 200, "copper ore generated around spawn: " + found);
        c.eq(new ResourceLocation(Content.MOD_ID, "raw_copper"), ore.getItemDropped(ore.getDefaultState(), new Random(), 0).getRegistryName(),
                "copper ore drops raw copper");
    }
}
