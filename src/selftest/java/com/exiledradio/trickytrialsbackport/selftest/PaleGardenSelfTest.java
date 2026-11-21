package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.palegarden.BlockEyeblossom;
import com.exiledradio.trickytrialsbackport.palegarden.BlockPaleMossCarpet;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenFeatures;
import com.exiledradio.trickytrialsbackport.palegarden.TileCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.wood.AxeStripping;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/** Stream 12: the pale garden's blocks, the tree, moss, eyeblossoms, the creaking heart and the biome. */
public final class PaleGardenSelfTest implements SelfTest {

    private static final String[] BLOCKS = {"pale_oak_log", "pale_oak_wood", "stripped_pale_oak_log", "stripped_pale_oak_wood",
            "pale_oak_planks", "pale_oak_stairs", "pale_oak_slab", "pale_oak_double_slab", "pale_oak_fence",
            "pale_oak_fence_gate", "pale_oak_door", "pale_oak_trapdoor", "pale_oak_pressure_plate", "pale_oak_button",
            "pale_oak_sign", "pale_oak_wall_sign", "pale_oak_leaves", "pale_oak_sapling", "potted_pale_oak_sapling",
            "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss", "open_eyeblossom", "closed_eyeblossom",
            "potted_open_eyeblossom", "potted_closed_eyeblossom", "creaking_heart"};
    private static final String[] ITEMS = {"pale_oak_log", "pale_oak_wood", "stripped_pale_oak_log", "stripped_pale_oak_wood",
            "pale_oak_planks", "pale_oak_stairs", "pale_oak_slab", "pale_oak_fence", "pale_oak_fence_gate", "pale_oak_door",
            "pale_oak_trapdoor", "pale_oak_pressure_plate", "pale_oak_button", "pale_oak_sign", "pale_oak_leaves",
            "pale_oak_sapling", "pale_oak_boat", "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss", "open_eyeblossom",
            "closed_eyeblossom", "creaking_heart"};

    @Override
    public String name() {
        return "palegarden";
    }

    @Override
    public void run(Check c) {
        registration(c);
        jigsawFeature(c);
        numbers(c);
        recipes(c);
        tree(c);
        moss(c);
        carpet(c);
        hangingMoss(c);
        eyeblossoms(c);
        heart(c);
        biome(c);
        c.world.setWorldTime(1000L);
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(Content.id(name));
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(Content.id(name));
    }

    private void registration(Check c) {
        for (String name : BLOCKS) {
            Block b = block(name);
            c.ok(b != null && Content.id(name).equals(b.getRegistryName()), name + " registered");
            c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(name), name + " owned by this mod");
        }
        for (String name : ITEMS) {
            Item i = item(name);
            c.ok(i != null && Content.id(name).equals(i.getRegistryName()), name + " item registered");
        }
        c.ok(item("pale_oak_wall_sign") == null || item("pale_oak_wall_sign") == Items.AIR, "no wall sign item");
        c.ok(EntityList.getClass(Content.id("boat")) != null, "boat entity registered");
        c.eq(30, net.minecraftforge.fml.common.registry.EntityRegistry.instance().lookupModSpawn(
                EntityList.getClass(Content.id("boat")), false).getModEntityId(), "boat entity id 30");
        c.ok(PaleGardenContent.biome != null && ForgeRegistries.BIOMES.getValue(Content.id("pale_garden")) == PaleGardenContent.biome,
                "pale garden biome registered");
    }

    private void numbers(Check c) {
        BlockPos o = BlockPos.ORIGIN;
        c.eq(2.0F, block("pale_oak_planks").getBlockHardness(block("pale_oak_planks").getDefaultState(), c.world, o), "planks hardness");
        c.eq(2.0F, block("pale_oak_log").getBlockHardness(block("pale_oak_log").getDefaultState(), c.world, o), "log hardness");
        c.eq(0.2F, block("pale_oak_leaves").getBlockHardness(block("pale_oak_leaves").getDefaultState(), c.world, o), "leaves hardness");
        c.eq(0.1F, block("pale_moss_block").getBlockHardness(block("pale_moss_block").getDefaultState(), c.world, o), "pale moss hardness");
        c.eq(0.1F, block("pale_moss_carpet").getBlockHardness(block("pale_moss_carpet").getDefaultState(), c.world, o), "carpet hardness");
        c.eq(10.0F, block("creaking_heart").getBlockHardness(block("creaking_heart").getDefaultState(), c.world, o), "heart hardness");
        c.eq(5, Blocks.FIRE.getEncouragement(block("pale_oak_planks")), "planks fire encouragement");
        c.eq(20, Blocks.FIRE.getFlammability(block("pale_oak_planks")), "planks flammability");
        c.eq(30, Blocks.FIRE.getEncouragement(block("pale_oak_leaves")), "leaves encouragement");
        c.eq(100, Blocks.FIRE.getFlammability(block("pale_moss_block")), "pale moss flammability");
        c.eq(60, Blocks.FIRE.getEncouragement(block("open_eyeblossom")), "eyeblossom encouragement");
        c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(item("pale_oak_planks"))), "planks burn");
        c.eq(150, TileEntityFurnace.getItemBurnTime(new ItemStack(item("pale_oak_slab"))), "slab burn");
        c.eq(1200, TileEntityFurnace.getItemBurnTime(new ItemStack(item("pale_oak_boat"))), "boat burn");
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(item("pale_oak_sapling"))), "sapling burn");
        c.eq(0, TileEntityFurnace.getItemBurnTime(new ItemStack(item("pale_moss_carpet"))), "carpet does not burn");
        c.eq(net.minecraft.block.SoundType.WOOD, block("pale_oak_planks").getSoundType(), "planks sound is vanilla wood");
        // Every stored state comes back from its metadata.
        for (String name : new String[] {"creaking_heart", "pale_moss_carpet", "pale_oak_sapling", "pale_oak_leaves"}) {
            Block b = block(name);
            boolean round = true;
            for (int meta = 0; meta < 16; meta++) {
                IBlockState s = b.getStateFromMeta(meta);
                if (b.getStateFromMeta(b.getMetaFromState(s)) != s) {
                    round = false;
                }
            }
            c.ok(round, name + " metadata round trip");
        }
        c.ok(AxeStripping.stripped(block("pale_oak_log").getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X))
                == block("stripped_pale_oak_log").getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X),
                "axe strips the log keeping its axis");
        c.ok(AxeStripping.stripped(block("pale_oak_wood").getDefaultState()).getBlock() == block("stripped_pale_oak_wood"),
                "axe strips the wood");
    }

    private void recipes(Check c) {
        expect(c, Crafting.grid(1, 1, new ItemStack(item("pale_oak_log"))), new ItemStack(item("pale_oak_planks"), 4), "planks from log");
        expect(c, Crafting.grid(1, 1, new ItemStack(item("stripped_pale_oak_wood"))), new ItemStack(item("pale_oak_planks"), 4), "planks from stripped wood");
        ItemStack p = new ItemStack(item("pale_oak_planks"));
        expect(c, Crafting.grid(3, 2, p, ItemStack.EMPTY, p, p, p, p), new ItemStack(item("pale_oak_boat")), "boat");
        ItemStack m = new ItemStack(item("pale_moss_block"));
        expect(c, Crafting.grid(2, 1, m, m), new ItemStack(item("pale_moss_carpet"), 3), "pale moss carpet");
        expect(c, Crafting.grid(1, 1, new ItemStack(item("open_eyeblossom"))),
                new ItemStack(Items.DYE, 1, EnumDyeColor.ORANGE.getDyeDamage()), "orange dye");
        expect(c, Crafting.grid(1, 1, new ItemStack(item("closed_eyeblossom"))),
                new ItemStack(Items.DYE, 1, EnumDyeColor.GRAY.getDyeDamage()), "gray dye");
        expect(c, Crafting.grid(3, 3, p, p, p, p, p, p, ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY),
                new ItemStack(item("pale_oak_sign"), 3), "sign");
        expect(c, Crafting.grid(2, 1, p, p), new ItemStack(item("pale_oak_pressure_plate")), "pressure plate");
        expect(c, Crafting.grid(1, 2, p, p), new ItemStack(Items.STICK, 4), "pale oak planks make sticks");
        if (com.exiledradio.trickytrialsbackport.common.Owners.ours("resin_block") == null) {
            c.ok(CraftingManager.getRecipe(Content.id("creaking_heart")) == null, "no heart recipe without resin");
        }
    }

    private static void expect(Check c, InventoryCrafting grid, ItemStack want, String what) {
        Crafting.expect(c, grid, want, what);
    }

    /** A grass floor at y=99 over dirt, for growing. */
    private static BlockPos grassArea(Check c) {
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.GRASS.getDefaultState());
        c.fill(corner.add(0, -2, 0), corner.add(31, -2, 31), Blocks.DIRT.getDefaultState());
        return corner;
    }

    private static int count(Check c, BlockPos from, BlockPos to, Block b) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            if (c.world.getBlockState(p).getBlock() == b) {
                n++;
            }
        }
        return n;
    }

    private void tree(Check c) {
        BlockPos corner = grassArea(c);
        Block log = block("pale_oak_log");
        Block leaves = block("pale_oak_leaves");
        Block sapling = block("pale_oak_sapling");
        Random rand = new Random(12345L);

        // One sapling alone never grows; a 2x2 does.
        BlockPos lone = corner.add(4, 0, 4);
        c.world.setBlockState(lone, sapling.getDefaultState());
        for (int i = 0; i < 10; i++) {
            ((IGrowable) sapling).grow(c.world, rand, lone, c.world.getBlockState(lone));
        }
        c.eq(sapling, c.world.getBlockState(lone).getBlock(), "a lone sapling stays a sapling");
        BlockPos sq = corner.add(14, 0, 14);
        for (BlockPos p : new BlockPos[] {sq, sq.east(), sq.south(), sq.south().east()}) {
            c.world.setBlockState(p, sapling.getDefaultState());
        }
        for (int i = 0; i < 4 && c.world.getBlockState(sq.east()).getBlock() == sapling; i++) {
            ((IGrowable) sapling).grow(c.world, rand, sq.east(), c.world.getBlockState(sq.east()));
        }
        c.eq(log, c.world.getBlockState(sq).getBlock(), "2x2 saplings grow a tree from its corner");
        c.eq(log, c.world.getBlockState(sq.south().east()).getBlock(), "the trunk is 2x2");
        c.eq(Blocks.DIRT, c.world.getBlockState(sq.down()).getBlock(), "grass under the trunk turns to dirt");
        int logs = count(c, corner.add(10, 0, 10), corner.add(20, 14, 20), log);
        int leafs = count(c, corner.add(8, 0, 8), corner.add(22, 16, 22), leaves);
        c.ok(logs >= 24 && logs <= 80, "grown tree has 24-80 logs (" + logs + ")");
        c.ok(leafs >= 40, "grown tree has leaves (" + leafs + ")");
        c.eq(0, count(c, corner.add(8, 0, 8), corner.add(22, 16, 22), block("pale_hanging_moss")), "a grown tree has no hanging moss");
        boolean decayable = true;
        for (BlockPos p : BlockPos.getAllInBox(corner.add(8, 0, 8), corner.add(22, 16, 22))) {
            IBlockState s = c.world.getBlockState(p);
            if (s.getBlock() == leaves && !s.getValue(BlockLeaves.DECAYABLE)) {
                decayable = false;
            }
        }
        c.ok(decayable, "a tree's leaves can decay");

        // Worldgen trees, many seeds: pale moss decorations, hearts in creaking trees.
        int hangers = 0;
        int moss = 0;
        int hearts = 0;
        int natural = 0;
        int enclosed = 0;
        int trees = 0;
        for (int seed = 0; seed < 12; seed++) {
            BlockPos area = grassArea(c);
            BlockPos at = area.add(14, 0, 14);
            PaleGardenFeatures.TreeKind kind = seed % 2 == 0 ? PaleGardenFeatures.TreeKind.PALE_OAK : PaleGardenFeatures.TreeKind.CREAKING;
            if (PaleGardenFeatures.tree(c.world, new Random(seed), at, kind)) {
                trees++;
            }
            hangers += count(c, area.add(6, 0, 6), area.add(24, 16, 24), block("pale_hanging_moss"));
            moss += count(c, area.add(6, -1, 6), area.add(24, -1, 24), block("pale_moss_block"));
            for (BlockPos p : BlockPos.getAllInBox(area.add(10, 0, 10), area.add(20, 14, 20))) {
                if (c.world.getBlockState(p).getBlock() == block("creaking_heart")) {
                    hearts++;
                    TileEntity t = c.world.getTileEntity(p);
                    if (t instanceof TileCreakingHeart && ((TileCreakingHeart) t).isNatural()) {
                        natural++;
                    }
                    boolean all = true;
                    for (EnumFacing f : EnumFacing.values()) {
                        all &= c.world.getBlockState(p.offset(f)).getBlock() == log;
                    }
                    if (all) {
                        enclosed++;
                    }
                    c.eq(BlockCreakingHeart.State.DORMANT, c.world.getBlockState(p).getValue(BlockCreakingHeart.STATE),
                            "a grown heart starts dormant");
                }
            }
        }
        c.eq(12, trees, "twelve worldgen trees in open space");
        c.ok(hangers > 10, "worldgen trees carry pale hanging moss (" + hangers + ")");
        c.ok(moss > 10, "worldgen trees lay pale moss at their foot (" + moss + ")");
        // A heart needs a trunk log with logs on all six sides: two branches beside it. Many trees, then.
        for (int seed = 100; seed < 160; seed++) {
            BlockPos area = grassArea(c);
            PaleGardenFeatures.tree(c.world, new Random(seed), area.add(14, 0, 14), PaleGardenFeatures.TreeKind.CREAKING);
            for (BlockPos p : BlockPos.getAllInBox(area.add(10, 0, 10), area.add(20, 14, 20))) {
                if (c.world.getBlockState(p).getBlock() == block("creaking_heart")) {
                    hearts++;
                    TileEntity t = c.world.getTileEntity(p);
                    if (t instanceof TileCreakingHeart && ((TileCreakingHeart) t).isNatural()) {
                        natural++;
                    }
                    boolean all = true;
                    for (EnumFacing f : EnumFacing.values()) {
                        all &= c.world.getBlockState(p.offset(f)).getBlock() == log;
                    }
                    if (all) {
                        enclosed++;
                    }
                }
            }
        }
        TrickyTrialsBackport.LOGGER.info("SELFTEST palegarden: {} hearts in 66 creaking trees", hearts);
        c.ok(hearts >= 3 && hearts <= 66, "some creaking trees hold a heart (" + hearts + " of 66)");
        c.eq(hearts, natural, "grown hearts are natural");
        c.eq(hearts, enclosed, "a heart sits in logs on all six sides");

        // No room: a stone roof three above stops a tree.
        BlockPos roofed = grassArea(c).add(14, 0, 14);
        c.fill(roofed.add(-3, 3, -3), roofed.add(4, 3, 4), Blocks.STONE.getDefaultState());
        c.ok(!PaleGardenFeatures.tree(c.world, new Random(1), roofed, PaleGardenFeatures.TreeKind.PALE_OAK), "no tree under a low roof");
        c.eq(Blocks.GRASS, c.world.getBlockState(roofed.down()).getBlock(), "and the ground is untouched");
    }

    private void moss(Check c) {
        BlockPos corner = grassArea(c);
        Block moss = block("pale_moss_block");
        BlockPos at = corner.add(16, -1, 16);
        c.world.setBlockState(at, moss.getDefaultState());
        ((IGrowable) moss).grow(c.world, new Random(7), at, moss.getDefaultState());
        int spread = count(c, corner.add(10, -1, 10), corner.add(22, -1, 22), moss);
        c.ok(spread >= 5 && spread <= 46, "bone meal spreads a pale moss patch (" + spread + ")");
        c.world.setBlockState(at.up(), Blocks.STONE.getDefaultState());
        c.ok(!((IGrowable) moss).canGrow(c.world, at, moss.getDefaultState(), false), "no bone meal with a block above");
        c.world.setBlockState(corner.add(25, -1, 25), moss.getDefaultState());
        c.ok(block("pale_oak_sapling").canPlaceBlockAt(c.world, corner.add(25, 0, 25)), "a sapling goes on pale moss");
        c.ok(Blocks.TALLGRASS.canBlockStay(c.world, corner.add(25, 0, 25),
                Blocks.TALLGRASS.getDefaultState()), "grass grows on pale moss");
    }

    private void carpet(Check c) {
        BlockPos corner = c.area();
        BlockPaleMossCarpet carpet = PaleGardenContent.mossCarpet;
        BlockPos at = corner.add(5, 0, 5);
        c.world.setBlockState(at.north(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(at.north().up(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(at, carpet.getDefaultState());
        IBlockState actual = c.world.getBlockState(at).getActualState(c.world, at);
        c.ok(actual.getValue(BlockPaleMossCarpet.BOTTOM), "carpet on the ground");
        c.eq(BlockPaleMossCarpet.Side.LOW, actual.getValue(BlockPaleMossCarpet.NORTH), "a side grows up the wall");
        c.eq(BlockPaleMossCarpet.Side.NONE, actual.getValue(BlockPaleMossCarpet.SOUTH), "no side without a wall");
        // Bone meal grows a topper with every side it can have; the carpet's side then reads tall.
        c.ok(carpet.canGrow(c.world, at, c.world.getBlockState(at), false), "bone meal can grow a topper");
        carpet.grow(c.world, c.world.rand, at, c.world.getBlockState(at));
        IBlockState top = c.world.getBlockState(at.up());
        c.ok(top.getBlock() == carpet && !top.getValue(BlockPaleMossCarpet.BOTTOM), "topper above");
        c.eq(BlockPaleMossCarpet.Side.TALL, c.world.getBlockState(at).getActualState(c.world, at).getValue(BlockPaleMossCarpet.NORTH),
                "the carpet's side is tall under a topper");
        c.ok(!carpet.canGrow(c.world, at, c.world.getBlockState(at), false), "no second topper");
        // Only the carpet drops; breaking it takes the topper down.
        NonNullList<ItemStack> drops = NonNullList.create();
        carpet.getDrops(drops, c.world, at.up(), top, 0);
        c.ok(drops.isEmpty() || drops.get(0).isEmpty(), "a topper drops nothing");
        c.world.destroyBlock(at, true);
        c.eq(Blocks.AIR, c.world.getBlockState(at.up()).getBlock(), "a topper goes with its carpet");
        c.eq(1, Drops.count(c.world, at, item("pale_moss_carpet")), "one carpet dropped");
        // A carpet without walls grows no topper; one on air is not placeable.
        BlockPos open = corner.add(15, 0, 15);
        c.world.setBlockState(open, carpet.getDefaultState());
        c.ok(!carpet.canGrow(c.world, open, c.world.getBlockState(open), false), "no topper without a wall");
        c.ok(!carpet.canPlaceBlockAt(c.world, corner.add(20, 5, 20)), "not placeable on air");
        c.ok(c.world.getBlockState(open).getCollisionBoundingBox(c.world, open) != null, "a carpet has collision");
    }

    private void hangingMoss(Check c) {
        BlockPos corner = c.area();
        Block hanging = block("pale_hanging_moss");
        BlockPos leaf = corner.add(8, 10, 8);
        c.world.setBlockState(leaf, block("pale_oak_leaves").getDefaultState());
        c.ok(hanging.canPlaceBlockAt(c.world, leaf.down()), "hangs under leaves");
        c.ok(!hanging.canPlaceBlockAt(c.world, corner.add(20, 10, 20)), "not in mid air");
        c.world.setBlockState(leaf.down(), hanging.getDefaultState());
        ((IGrowable) hanging).grow(c.world, c.world.rand, leaf.down(), hanging.getDefaultState());
        c.eq(hanging, c.world.getBlockState(leaf.down(2)).getBlock(), "bone meal adds one below the tip");
        c.ok(!c.world.getBlockState(leaf.down()).getActualState(c.world, leaf.down()).getValue(
                com.exiledradio.trickytrialsbackport.palegarden.BlockPaleHangingMoss.TIP), "upper block is not the tip");
        c.ok(c.world.getBlockState(leaf.down(2)).getActualState(c.world, leaf.down(2)).getValue(
                com.exiledradio.trickytrialsbackport.palegarden.BlockPaleHangingMoss.TIP), "lowest block is the tip");
        NonNullList<ItemStack> drops = NonNullList.create();
        hanging.getDrops(drops, c.world, leaf.down(), hanging.getDefaultState(), 0);
        c.ok(drops.isEmpty(), "drops nothing by hand");
        c.ok(((net.minecraftforge.common.IShearable) hanging).isShearable(new ItemStack(Items.SHEARS), c.world, leaf.down()), "shearable");
        c.world.setBlockToAir(leaf);
        hanging.updateTick(c.world, leaf.down(), c.world.getBlockState(leaf.down()), c.world.rand);
        hanging.updateTick(c.world, leaf.down(2), c.world.getBlockState(leaf.down(2)), c.world.rand);
        c.eq(Blocks.AIR, c.world.getBlockState(leaf.down()).getBlock(), "falls without support");
        c.eq(Blocks.AIR, c.world.getBlockState(leaf.down(2)).getBlock(), "the strand comes down");
    }

    private void eyeblossoms(Check c) {
        BlockPos corner = grassArea(c);
        BlockEyeblossom open = PaleGardenContent.openEyeblossom;
        BlockEyeblossom closed = PaleGardenContent.closedEyeblossom;
        BlockPos at = corner.add(10, 0, 10);
        c.world.setBlockState(at, closed.getDefaultState());
        c.world.setBlockState(at.east(), closed.getDefaultState());
        c.world.setWorldTime(6000L);
        closed.randomTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(closed, c.world.getBlockState(at).getBlock(), "closed by day");
        c.world.setWorldTime(18000L);
        closed.randomTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(open, c.world.getBlockState(at).getBlock(), "opens at night");
        c.ok(c.world.isUpdateScheduled(at.east(), closed), "a neighbor is scheduled to follow");
        closed.updateTick(c.world, at.east(), c.world.getBlockState(at.east()), c.world.rand);
        c.eq(open, c.world.getBlockState(at.east()).getBlock(), "the neighbor opens on its tick");
        c.world.setWorldTime(24000L + 1000L);
        open.randomTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(closed, c.world.getBlockState(at).getBlock(), "closes the next morning");
        c.world.setWorldTime(23400L);
        c.ok(BlockEyeblossom.openNow(c.world, false), "still open at 23400");
        c.world.setWorldTime(23401L);
        c.ok(!BlockEyeblossom.openNow(c.world, true), "closed at 23401");
        c.world.setWorldTime(12600L);
        c.ok(BlockEyeblossom.openNow(c.world, false), "open from 12600");
        // Potted.
        BlockPos pot = corner.add(20, 0, 20);
        // World.checkLight works only with every chunk 17 blocks round loaded.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.world.getChunkProvider().provideChunk((pot.getX() >> 4) + dx, (pot.getZ() >> 4) + dz);
            }
        }
        c.world.setBlockState(pot, PaleGardenContent.pottedClosedEyeblossom.getDefaultState());
        PaleGardenContent.pottedClosedEyeblossom.randomTick(c.world, pot, c.world.getBlockState(pot), c.world.rand);
        c.eq(PaleGardenContent.pottedOpenEyeblossom, c.world.getBlockState(pot).getBlock(), "a potted eyeblossom opens at night");
        c.eq(BlockEyeblossom.LIGHT, c.world.getLightFor(EnumSkyBlock.BLOCK, pot), "light: a potted open eyeblossom glows");
        // Light (not vanilla, paleGarden.eyeblossomLight): open ones glow, closed ones not, rechecked on the switch.
        BlockPos lone = corner.add(30, 0, 30);
        c.world.setBlockState(lone, closed.getDefaultState());
        c.eq(0, c.world.getLightFor(EnumSkyBlock.BLOCK, lone), "light: a closed eyeblossom gives none");
        closed.randomTick(c.world, lone, c.world.getBlockState(lone), c.world.rand);
        c.eq(open, c.world.getBlockState(lone).getBlock(), "light: the lone eyeblossom opened");
        c.eq(BlockEyeblossom.LIGHT, c.world.getLightFor(EnumSkyBlock.BLOCK, lone), "light: it glows once open");
        c.eq(BlockEyeblossom.LIGHT - 1, c.world.getLightFor(EnumSkyBlock.BLOCK, lone.north()), "light: it spreads to the next block");
        c.world.setWorldTime(1000L);
        open.randomTick(c.world, lone, c.world.getBlockState(lone), c.world.rand);
        c.eq(closed, c.world.getBlockState(lone).getBlock(), "light: the lone eyeblossom closed");
        c.eq(0, c.world.getLightFor(EnumSkyBlock.BLOCK, lone), "light: none once closed again");
        ModConfig.paleGarden.eyeblossomLight = false;
        try {
            c.world.setBlockState(lone, open.getDefaultState());
            c.eq(0, c.world.getLightFor(EnumSkyBlock.BLOCK, lone), "light: setting off, an open eyeblossom gives none");
        } finally {
            ModConfig.paleGarden.eyeblossomLight = true;
        }
        c.world.setBlockToAir(lone);
        // Day and night with the daylight cycle off (a showcase world): /time set alone flips them on random ticks.
        c.ok(open.getTickRandomly() && closed.getTickRandomly(), "both eyeblossoms tick randomly");
        c.ok(PaleGardenContent.pottedOpenEyeblossom.getTickRandomly() && PaleGardenContent.pottedClosedEyeblossom.getTickRandomly(),
                "both potted eyeblossoms tick randomly");
        String cycle = c.world.getGameRules().getString("doDaylightCycle");
        c.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        try {
            BlockPos frozen = corner.add(30, 0, 25);
            c.world.setBlockState(frozen, closed.getDefaultState());
            c.world.setWorldTime(18000L);
            closed.randomTick(c.world, frozen, c.world.getBlockState(frozen), c.world.rand);
            c.eq(open, c.world.getBlockState(frozen).getBlock(), "daylight cycle off: opens at night on a random tick");
            c.world.setWorldTime(1000L);
            open.randomTick(c.world, frozen, c.world.getBlockState(frozen), c.world.rand);
            c.eq(closed, c.world.getBlockState(frozen).getBlock(), "daylight cycle off: closes by day on a random tick");
            c.world.setBlockToAir(frozen);
        } finally {
            c.world.getGameRules().setOrCreateGameRule("doDaylightCycle", cycle);
        }
        c.world.setWorldTime(1000L);
    }

    private void heart(Check c) {
        BlockPos corner = c.area();
        Block heart = block("creaking_heart");
        Block log = block("pale_oak_log");
        BlockPos at = corner.add(10, 5, 10);
        c.world.setWorldTime(1000L);
        c.world.setBlockState(at, heart.getDefaultState());
        c.eq(BlockCreakingHeart.State.UPROOTED, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "uprooted alone");
        c.world.setBlockState(at.up(), log.getDefaultState());
        c.world.setBlockState(at.down(), log.getDefaultState());
        heart.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(BlockCreakingHeart.State.DORMANT, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "dormant by day between logs");
        c.world.setWorldTime(14000L);
        TileEntity t = c.world.getTileEntity(at);
        c.ok(t instanceof TileCreakingHeart, "heart has its tile");
        for (int i = 0; i < 30; i++) {
            ((TileCreakingHeart) c.world.getTileEntity(at)).update();
        }
        c.eq(BlockCreakingHeart.State.AWAKE, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "awake at night");
        c.eq(0, c.world.getBlockState(at).getComparatorInputOverride(c.world, at), "no signal without a creaking");
        // Wrong axis: a log lying sideways does not count.
        c.world.setBlockState(at.up(), log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X));
        for (int i = 0; i < 30; i++) {
            TileEntity tile = c.world.getTileEntity(at);
            if (tile instanceof TileCreakingHeart) {
                ((TileCreakingHeart) tile).update();
            }
        }
        c.eq(BlockCreakingHeart.State.UPROOTED, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "uprooted with a log the wrong way");
        // Stripped wood counts.
        c.world.setBlockState(at.up(), block("stripped_pale_oak_wood").getDefaultState());
        heart.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(BlockCreakingHeart.State.AWAKE, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "stripped pale oak wood counts");
        // Breaking: a natural heart gives 20-24 experience; drops no resin without resin.
        ((TileCreakingHeart) c.world.getTileEntity(at)).setNatural(true);
        net.minecraft.entity.player.EntityPlayerMP player = Players.real(c.world, "heart");
        player.setPosition(at.getX() + 0.5D, at.getY() + 1, at.getZ() + 2.5D);
        player.interactionManager.tryHarvestBlock(at);
        c.eq(Blocks.AIR, c.world.getBlockState(at).getBlock(), "heart broken");
        int xp = 0;
        for (EntityXPOrb orb : c.world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(at).grow(3))) {
            xp += orb.getXpValue();
            orb.setDead();
        }
        c.ok(xp >= 20 && xp <= 24, "a natural heart gives 20-24 experience (" + xp + ")");
        if (com.exiledradio.trickytrialsbackport.common.Owners.ours("resin_clump") == null) {
            c.eq(0, Drops.count(c.world, at, Item.getItemFromBlock(heart)), "no heart dropped without Silk Touch");
        }
        c.world.setWorldTime(1000L);
    }

    private void biome(Check c) {
        Biome pale = PaleGardenContent.biome;
        c.eq(0.7F, pale.getDefaultTemperature(), "temperature");
        c.eq(0.8F, pale.getRainfall(), "downfall");
        c.ok(pale.getSpawnableList(net.minecraft.entity.EnumCreatureType.CREATURE).isEmpty(), "no animals");
        c.ok(!pale.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER).isEmpty(), "monsters");
        c.eq(Biomes.ROOFED_FOREST.getBaseHeight(), pale.getBaseHeight(), "the roofed forest's height");

        // The layer: over a map of nothing but roofed forest, about one patch in three is pale, in patches.
        GenLayer all = new GenLayer(0L) {
            @Override
            public int[] getInts(int x, int z, int w, int d) {
                int[] out = IntCache.getIntCache(w * d);
                java.util.Arrays.fill(out, Biome.getIdForBiome(Biomes.ROOFED_FOREST));
                return out;
            }
        };
        GenLayer plains = new GenLayer(0L) {
            @Override
            public int[] getInts(int x, int z, int w, int d) {
                int[] out = IntCache.getIntCache(w * d);
                java.util.Arrays.fill(out, Biome.getIdForBiome(Biomes.PLAINS));
                return out;
            }
        };
        net.minecraftforge.event.terraingen.WorldTypeEvent.InitBiomeGens ev =
                new net.minecraftforge.event.terraingen.WorldTypeEvent.InitBiomeGens(net.minecraft.world.WorldType.DEFAULT, 42L,
                        new GenLayer[] {all, all, all});
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.post(ev);
        GenLayer[] out = ev.getNewBiomeGens();
        IntCache.resetIntCache();
        int[] map = out[0].getInts(0, 0, 256, 256).clone();
        int paleId = Biome.getIdForBiome(pale);
        int n = 0;
        int edges = 0;
        for (int i = 0; i < map.length; i++) {
            if (map[i] == paleId) {
                n++;
            }
            if (i % 256 > 0 && (map[i] == paleId) != (map[i - 1] == paleId)) {
                edges++;
            }
        }
        double share = n / (double) map.length;
        c.ok(share > 0.15D && share < 0.55D, "about a third of a roofed forest is pale (" + share + ")");
        c.ok(edges < map.length / 20, "pale in patches, not noise (" + edges + " edges)");
        // The block-scale map agrees with the 1:4 one.
        IntCache.resetIntCache();
        int[] fine = out[1].getInts(0, 0, 64, 64).clone();
        IntCache.resetIntCache();
        int[] coarse = out[0].getInts(-1, -1, 18, 18).clone();
        int agree = 0;
        for (int z = 0; z < 64; z++) {
            for (int x = 0; x < 64; x++) {
                boolean f = fine[x + z * 64] == paleId;
                // Voronoi picks one of the nearby 1:4 cells; agree when one around matches.
                boolean any = false;
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int cx = (x >> 2) + 1 + dx;
                        int cz = (z >> 2) + 1 + dz;
                        if ((coarse[cx + cz * 18] == paleId) == f) {
                            any = true;
                        }
                    }
                }
                if (any) {
                    agree++;
                }
            }
        }
        c.ok(agree == 64 * 64, "the block map follows the biome map (" + agree + "/4096)");
        net.minecraftforge.event.terraingen.WorldTypeEvent.InitBiomeGens ev2 =
                new net.minecraftforge.event.terraingen.WorldTypeEvent.InitBiomeGens(net.minecraft.world.WorldType.DEFAULT, 42L,
                        new GenLayer[] {plains, plains, plains});
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.post(ev2);
        IntCache.resetIntCache();
        int[] none = ev2.getNewBiomeGens()[0].getInts(0, 0, 128, 128);
        boolean noPale = true;
        for (int v : none) {
            noPale &= v != paleId;
        }
        c.ok(noPale, "no pale garden outside roofed forests");
        IntCache.resetIntCache();

        // Decoration: chunks set to the pale garden on a flat grass floor, one decorated each round.
        c.world.setWorldTime(1000L);
        int logs = 0;
        int hearts = 0;
        int mossGround = 0;
        int blossoms = 0;
        for (int round = 0; round < 6; round++) {
            BlockPos cc = new BlockPos(40000 + 64 * round, 0, 40000);
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    Chunk chunk = c.world.getChunk((cc.getX() >> 4) + dx, (cc.getZ() >> 4) + dz);
                    java.util.Arrays.fill(chunk.getBiomeArray(), (byte) paleId);
                }
            }
            for (int x = -8; x < 40; x++) {
                for (int z = -8; z < 40; z++) {
                    for (int y = 60; y < 160; y++) {
                        BlockPos p = cc.add(x, y, z);
                        IBlockState want = y == 99 ? Blocks.GRASS.getDefaultState()
                                : y < 99 ? Blocks.DIRT.getDefaultState() : Blocks.AIR.getDefaultState();
                        if (c.world.getBlockState(p) != want) {
                            c.world.setBlockState(p, want, 2 | 16);
                        }
                    }
                }
            }
            PaleGardenFeatures.vegetation(c.world, new Random(1000L + round), cc, pale);
            BlockPos from = cc.add(0, 99, 0);
            BlockPos to = cc.add(31, 125, 31);
            logs += count(c, from, to, block("pale_oak_log"));
            hearts += count(c, from, to, block("creaking_heart"));
            mossGround += count(c, cc.add(0, 99, 0), cc.add(31, 99, 31), block("pale_moss_block"));
            blossoms += count(c, from, to, block("closed_eyeblossom")) + count(c, from, to, block("open_eyeblossom"));
        }
        c.ok(logs > 6 * 60, "a pale garden chunk grows several trees (" + logs / 6 + " logs a chunk)");
        c.ok(mossGround > 0, "pale moss on the ground (" + mossGround + ")");
        c.ok(hearts >= 0, "creaking hearts: " + hearts + " in 6 chunks");
        TrickyTrialsBackport.LOGGER.info("SELFTEST palegarden decoration: {} logs, {} hearts, {} moss, {} eyeblossoms in 6 chunks",
                logs, hearts, mossGround, blossoms);
    }

    /** Game Drops Backport's camps: pale_oak_checked grows this mod's worldgen pale oak through Core's JigsawPools. */
    private static void jigsawFeature(Check c) {
        String id = "minecraft:pale_oak_checked";
        Object placer;
        try {
            java.lang.reflect.Method m = com.exiledradio.backportcore.structure.JigsawPools.class.getDeclaredMethod("featurePlacer",
                    com.google.gson.JsonElement.class);
            m.setAccessible(true);
            placer = m.invoke(null, new com.google.gson.JsonPrimitive(id));
        } catch (ReflectiveOperationException e) {
            c.ok(false, id + ": JigsawPools.featurePlacer reachable: " + e);
            return;
        }
        if (!c.ok(placer != null, id + " has a placer") || PaleGardenContent.set == null) {
            return;
        }
        BlockPos ground = new BlockPos(2300, 80, 2300);
        for (BlockPos p : BlockPos.getAllInBox(ground.add(-8, 0, -8), ground.add(8, 24, 8))) {
            c.world.setBlockState(p, p.getY() == 80 ? Blocks.GRASS.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        }
        com.exiledradio.backportcore.structure.JigsawPools.FeaturePlacer f = (com.exiledradio.backportcore.structure.JigsawPools.FeaturePlacer) placer;
        f.place(c.world, ground.up(), new java.util.Random(3L), new com.google.gson.JsonPrimitive(id));
        c.eq(PaleGardenContent.set.log, c.world.getBlockState(ground.up()).getBlock(), id + " grows a pale oak log on grass");
        for (BlockPos p : BlockPos.getAllInBox(ground.add(-8, 1, -8), ground.add(8, 24, 8))) {
            c.world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
        c.world.setBlockState(ground, Blocks.STONE.getDefaultState(), 2);
        f.place(c.world, ground.up(), new java.util.Random(3L), new com.google.gson.JsonPrimitive(id));
        c.ok(c.world.isAirBlock(ground.up()), id + " places nothing where a sapling could not stand (stone)");
    }
}
