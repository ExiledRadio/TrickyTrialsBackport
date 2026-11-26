package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.asm.CactusFlowerTransformer;
import com.exiledradio.trickytrialsbackport.asm.DeadBushSoilTransformer;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.flora.BlockSegmented;
import com.exiledradio.trickytrialsbackport.flora.FallingLeaves;
import com.exiledradio.trickytrialsbackport.flora.FallenTrees;
import com.exiledradio.trickytrialsbackport.flora.FloraBiomes;
import com.exiledradio.trickytrialsbackport.flora.FloraContent;
import com.exiledradio.trickytrialsbackport.flora.FloraFeatures;
import com.exiledradio.trickytrialsbackport.flora.FloraGen;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSand;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Random;

/** Stream 15: the spring plants, their placement, the patches to vanilla blocks and their generation. */
public final class FloraSelfTest implements SelfTest {

    /**
     * One random for every draw-dependent check: Java's first draw from consecutive small seeds is nearly the same
     * number (about 0.73 from {@code new Random(0..n)}), so a one-in-four roll would never hit.
     */
    private static final Random R = new Random(0x5EED15L);

    private static final String[] NAMES = {"bush", "firefly_bush", "cactus_flower", "short_dry_grass", "tall_dry_grass",
            "leaf_litter", "wildflowers"};

    @Override
    public String name() {
        return "flora";
    }

    @Override
    public void run(Check c) {
        registration(c);
        numbers(c);
        recipes(c);
        ground(c);
        items(c);
        drops(c);
        boneMeal(c);
        cactus(c);
        deadBush(c);
        fallen(c);
        litterUnderTrees(c);
        generation(c);
        fallingLeaves(c);
    }

    /** 1.21.5's falling leaves (FallingLeaves, drawn through P3): which leaves get them, and 26.3's numbers. */
    private void fallingLeaves(Check c) {
        c.eq(0.01F, FallingLeaves.CHANCE, "falling leaves: one display tick in a hundred");
        c.eq(-9399763 & 0xFFFFFF, FallingLeaves.AZALEA, "falling leaves: the azaleas' fixed color");
        c.ok(com.exiledradio.trickytrialsbackport.ModConfig.flora.fallingLeaves, "falling leaves: on by default");
        for (BlockPlanks.EnumType t : new BlockPlanks.EnumType[] {BlockPlanks.EnumType.OAK, BlockPlanks.EnumType.BIRCH,
                BlockPlanks.EnumType.JUNGLE}) {
            IBlockState s = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, t);
            c.ok(FallingLeaves.source(s) == s, "falling leaves: " + t.getName() + " leaves drop them");
            c.ok(FallingLeaves.fixedColor(s) == null, "falling leaves: " + t.getName() + " in its block color");
        }
        for (BlockPlanks.EnumType t : new BlockPlanks.EnumType[] {BlockPlanks.EnumType.ACACIA, BlockPlanks.EnumType.DARK_OAK}) {
            IBlockState s = Blocks.LEAVES2.getDefaultState().withProperty(BlockNewLeaf.VARIANT, t);
            c.ok(FallingLeaves.source(s) == s, "falling leaves: " + t.getName() + " leaves drop them");
        }
        IBlockState decaying = Blocks.LEAVES.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, true)
                .withProperty(BlockLeaves.DECAYABLE, false);
        c.ok(FallingLeaves.source(decaying) == decaying, "falling leaves: whatever the decay flags");
        IBlockState spruce = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.SPRUCE);
        c.ok(FallingLeaves.source(spruce) == null, "falling leaves: none from spruce (26.3)");
        if (PaleGardenContent.leaves != null) {
            c.ok(FallingLeaves.source(PaleGardenContent.leaves.getDefaultState()) == null,
                    "falling leaves: pale oak keeps its own, not the tinted one too");
        }
        c.ok(FallingLeaves.source(Blocks.STONE.getDefaultState()) == null, "falling leaves: not from stone");
        c.ok(FallingLeaves.source(Blocks.GRASS.getDefaultState()) == null, "falling leaves: not from grass");
        // Every other mod's leaves in this game: the ones with their own particles are left out, the rest drop them.
        int others = 0;
        for (Block b : ForgeRegistries.BLOCKS) {
            if (!(b instanceof BlockLeaves) || b.getRegistryName() == null) {
                continue;
            }
            String id = b.getRegistryName().toString();
            boolean own = id.equals("trailsandtalesbackport:cherry_leaves") || id.endsWith("_poplar_leaves")
                    || b instanceof com.exiledradio.trickytrialsbackport.palegarden.BlockPaleOakLeaves;
            if (b != Blocks.LEAVES && b != Blocks.LEAVES2) {
                others++;
                c.eq(!own, FallingLeaves.source(b.getDefaultState()) != null, "falling leaves: " + id + (own ? " keeps its own" : " drops them"));
            }
        }
        c.ok(others >= 1, "falling leaves: other leaves blocks checked: " + others);
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(Content.id(name));
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(Content.id(name));
    }

    private void registration(Check c) {
        for (String name : NAMES) {
            Block b = block(name);
            c.ok(b != null && Content.id(name).equals(b.getRegistryName()), name + " registered");
            c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(name), name + " owned by this mod");
            Item i = item(name);
            c.ok(i != null && i != Items.AIR, name + " item registered");
        }
    }

    private void numbers(Check c) {
        BlockPos o = BlockPos.ORIGIN;
        for (String name : NAMES) {
            Block b = block(name);
            c.eq(0.0F, b.getBlockHardness(b.getDefaultState(), c.world, o), name + " breaks instantly");
            c.eq(60, Blocks.FIRE.getEncouragement(b), name + " fire encouragement");
            c.eq(100, Blocks.FIRE.getFlammability(b), name + " flammability");
            c.ok(b.getCollisionBoundingBox(b.getDefaultState(), c.world, o) == Block.NULL_AABB, name + " has no collision");
        }
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(item("short_dry_grass"))), "short dry grass burns 100");
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(item("tall_dry_grass"))), "tall dry grass burns 100");
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(item("leaf_litter"))), "leaf litter burns 100");
        c.eq(0, TileEntityFurnace.getItemBurnTime(new ItemStack(item("wildflowers"))), "wildflowers do not burn");
        c.eq(2, block("firefly_bush").getDefaultState().getLightValue(c.world, o), "firefly bush gives light 2");
        c.eq(0, block("bush").getDefaultState().getLightValue(c.world, o), "bush gives no light");
        for (String name : new String[] {"bush", "short_dry_grass", "tall_dry_grass", "leaf_litter"}) {
            c.ok(block(name).getDefaultState().getMaterial().isReplaceable(), name + " is replaceable");
        }
        for (String name : new String[] {"firefly_bush", "cactus_flower", "wildflowers"}) {
            c.ok(!block(name).getDefaultState().getMaterial().isReplaceable(), name + " is not replaceable");
        }
        c.eq(Block.EnumOffsetType.XYZ, block("short_dry_grass").getOffsetType(), "dry grass is offset in x, y and z");
        c.eq(Block.EnumOffsetType.NONE, block("bush").getOffsetType(), "the bush is not offset");
        c.eq(SoundType.PLANT, block("bush").getSoundType(), "bush sounds like grass");
        c.eq(FloraContent.PINK_PETALS, block("wildflowers").getSoundType(), "wildflowers sound like pink petals");
        c.eq(Content.id("block.leaf_litter.step"), block("leaf_litter").getSoundType().getStepSound().getRegistryName(), "leaf litter steps");
        for (String name : new String[] {"leaf_litter", "wildflowers"}) {
            Block b = block(name);
            boolean round = true;
            for (int meta = 0; meta < 16; meta++) {
                IBlockState s = b.getStateFromMeta(meta);
                round &= b.getStateFromMeta(b.getMetaFromState(s)) == s && b.getMetaFromState(s) == meta;
            }
            c.ok(round, name + " metadata round trip");
        }
        // Outlines: the facing's quarter, then counterclockwise; wildflowers 3 high, leaf litter 1.
        BlockSegmented flowers = FloraContent.wildflowers;
        IBlockState north1 = flowers.getDefaultState();
        c.eq(new AxisAlignedBB(0, 0, 0, 0.5, 3 / 16.0, 0.5), flowers.getBoundingBox(north1, c.world, o), "one wildflower: north-west");
        c.eq(new AxisAlignedBB(0, 0, 0, 0.5, 3 / 16.0, 1), flowers.getBoundingBox(north1.withProperty(flowers.amount(), 2), c.world, o),
                "two wildflowers: then south-west");
        c.eq(new AxisAlignedBB(0.5, 0, 0, 1, 3 / 16.0, 0.5),
                flowers.getBoundingBox(north1.withProperty(BlockSegmented.FACING, EnumFacing.EAST), c.world, o), "east: north-east");
        c.eq(new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1), FloraContent.leafLitter.getBoundingBox(
                FloraContent.leafLitter.getDefaultState().withProperty(FloraContent.leafLitter.amount(), 4), c.world, o), "four leaf litter");
        c.eq(new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 12 / 16.0, 15 / 16.0),
                block("cactus_flower").getBoundingBox(block("cactus_flower").getDefaultState(), c.world, o), "cactus flower outline");
        c.eq(Block.FULL_BLOCK_AABB, block("firefly_bush").getBoundingBox(block("firefly_bush").getDefaultState(), c.world, o),
                "firefly bush outline is a full block");
    }

    private void recipes(Check c) {
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(item("cactus_flower"))),
                new ItemStack(Items.DYE, 1, EnumDyeColor.PINK.getDyeDamage()), "pink dye from a cactus flower");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(item("wildflowers"))),
                new ItemStack(Items.DYE, 1, EnumDyeColor.YELLOW.getDyeDamage()), "yellow dye from wildflowers");
        c.ok(ItemStack.areItemsEqual(new ItemStack(item("leaf_litter")),
                FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.LEAVES, 1, 2))), "birch leaves smelt to leaf litter");
        c.ok(ItemStack.areItemsEqual(new ItemStack(item("leaf_litter")),
                FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.LEAVES2, 1, 1))), "dark oak leaves smelt to leaf litter");
        c.eq(0.1F, FurnaceRecipes.instance().getSmeltingExperience(new ItemStack(item("leaf_litter"))), "leaf litter smelting experience");
    }

    private static boolean stays(Check c, BlockPos at, IBlockState ground, Block plant) {
        c.world.setBlockState(at.down(), ground, 2 | 16);
        c.world.setBlockState(at, Blocks.AIR.getDefaultState(), 2 | 16);
        return plant.canPlaceBlockAt(c.world, at);
    }

    private void ground(Check c) {
        BlockPos at = c.area().add(4, 1, 4);
        IBlockState grass = Blocks.GRASS.getDefaultState();
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState farmland = Blocks.FARMLAND.getDefaultState();
        IBlockState sand = Blocks.SAND.getDefaultState();
        IBlockState redSand = sand.withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND);
        IBlockState terracotta = Blocks.HARDENED_CLAY.getDefaultState();
        for (String name : new String[] {"bush", "firefly_bush", "wildflowers"}) {
            Block b = block(name);
            c.ok(stays(c, at, grass, b), name + " on grass");
            c.ok(stays(c, at, farmland, b), name + " on farmland");
            c.ok(stays(c, at, Blocks.MYCELIUM.getDefaultState(), b), name + " on mycelium");
            c.ok(!stays(c, at, sand, b), name + " not on sand");
            c.ok(!stays(c, at, stone, b), name + " not on stone");
        }
        for (String name : new String[] {"short_dry_grass", "tall_dry_grass"}) {
            Block b = block(name);
            for (IBlockState g : new IBlockState[] {sand, redSand, terracotta, Blocks.STAINED_HARDENED_CLAY.getDefaultState(), grass,
                    Blocks.DIRT.getDefaultState(), farmland}) {
                c.ok(stays(c, at, g, b), name + " on " + g);
            }
            c.ok(!stays(c, at, stone, b), name + " not on stone");
        }
        Block flower = block("cactus_flower");
        c.ok(stays(c, at, Blocks.CACTUS.getDefaultState(), flower), "cactus flower on a cactus");
        c.ok(stays(c, at, stone, flower), "cactus flower on stone");
        c.ok(stays(c, at, Blocks.OAK_FENCE.getDefaultState(), flower), "cactus flower on a fence post");
        c.ok(stays(c, at, farmland, flower), "cactus flower on farmland");
        c.ok(!stays(c, at, Blocks.AIR.getDefaultState(), flower), "cactus flower not on air");
        c.ok(!stays(c, at, Blocks.STONE_SLAB.getDefaultState(), flower), "cactus flower not on a bottom slab");
        Block litter = block("leaf_litter");
        c.ok(stays(c, at, stone, litter), "leaf litter on stone");
        c.ok(stays(c, at, grass, litter), "leaf litter on grass");
        c.ok(!stays(c, at, Blocks.AIR.getDefaultState(), litter), "leaf litter not on air");
        c.ok(!stays(c, at, Blocks.STONE_SLAB.getDefaultState(), litter), "leaf litter not on a bottom slab");
        // Ground taken away: the plant breaks.
        c.world.setBlockState(at.down(), grass);
        c.world.setBlockState(at, block("bush").getDefaultState());
        c.world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
        c.eq(Blocks.AIR, c.world.getBlockState(at).getBlock(), "a bush without ground breaks");
    }

    private void items(Check c) {
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(15, -1, 15), Blocks.GRASS.getDefaultState());
        for (String name : new String[] {"wildflowers", "leaf_litter"}) {
            BlockSegmented b = (BlockSegmented) block(name);
            BlockPos at = corner.add(name.equals("wildflowers") ? 3 : 9, 0, 3);
            FakePlayer player = c.player("flora-" + name, at.south(3));
            player.rotationYaw = 180.0F; // facing north
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(item(name), 64));
            ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
            held.onItemUse(player, c.world, at.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            IBlockState s = c.world.getBlockState(at);
            c.eq(b, s.getBlock(), name + " placed on grass");
            c.eq(EnumFacing.SOUTH, s.getValue(BlockSegmented.FACING), name + " faces away from the player");
            for (int n = 2; n <= 4; n++) {
                held.onItemUse(player, c.world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
                c.eq(n, c.world.getBlockState(at).getValue(b.amount()), name + " added in place to " + n);
            }
            held.onItemUse(player, c.world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
            c.eq(4, c.world.getBlockState(at).getValue(b.amount()), name + " stops at four");
            c.eq(Blocks.AIR, c.world.getBlockState(at.up()).getBlock(), name + ": a full block is not replaced or stacked on");
            // Clicking the grass beside: the item adds to the block the click would place into.
            BlockPos next = at.east();
            held.onItemUse(player, c.world, next.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            held.onItemUse(player, c.world, next.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            c.eq(2, c.world.getBlockState(next).getValue(b.amount()), name + " added through the block below");
            player.setSneaking(true);
            held.onItemUse(player, c.world, next, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
            c.eq(2, c.world.getBlockState(next).getValue(b.amount()), name + ": sneaking adds nothing");
            player.setSneaking(false);
            c.eq(64 - 6, held.getCount(), name + ": each piece used one item");
        }
        // A grass block placed on leaf litter replaces it; wildflowers are not replaced.
        c.ok(FloraContent.leafLitter.isReplaceable(c.world, corner.add(9, 0, 3)), "leaf litter can be built over");
        c.ok(!FloraContent.wildflowers.isReplaceable(c.world, corner.add(3, 0, 3)), "wildflowers cannot");
    }

    private void drops(Check c) {
        NonNullList<ItemStack> list = NonNullList.create();
        BlockSegmented flowers = FloraContent.wildflowers;
        flowers.getDrops(list, c.world, BlockPos.ORIGIN, flowers.getDefaultState().withProperty(flowers.amount(), 3), 0);
        c.ok(list.size() == 1 && list.get(0).getItem() == item("wildflowers") && list.get(0).getCount() == 3, "three wildflowers drop three");
        list.clear();
        FloraContent.leafLitter.getDrops(list, c.world, BlockPos.ORIGIN, FloraContent.leafLitter.getDefaultState(), 0);
        c.ok(list.size() == 1 && list.get(0).getCount() == 1, "one leaf litter drops one");
        Random r = new Random(1);
        c.eq(item("firefly_bush"), block("firefly_bush").getItemDropped(block("firefly_bush").getDefaultState(), r, 0), "firefly bush drops itself");
        c.eq(item("cactus_flower"), block("cactus_flower").getItemDropped(block("cactus_flower").getDefaultState(), r, 0), "cactus flower drops itself");
        BlockPos corner = c.area();
        for (String name : new String[] {"bush", "short_dry_grass", "tall_dry_grass"}) {
            Block b = block(name);
            c.eq(Items.AIR, b.getItemDropped(b.getDefaultState(), r, 0), name + " drops nothing by hand");
            for (ItemStack tool : new ItemStack[] {new ItemStack(Items.SHEARS), ItemStack.EMPTY}) {
                BlockPos at = corner.add(2, 0, 2);
                c.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new AxisAlignedBB(at).grow(3))
                        .forEach(net.minecraft.entity.Entity::setDead);
                FakePlayer p = c.player("flora-drops", at.east(2));
                b.harvestBlock(c.world, p, at, b.getDefaultState(), null, tool);
                int n = Drops.count(c.world, at, item(name));
                c.eq(tool.isEmpty() ? 0 : 1, n, name + (tool.isEmpty() ? " by hand drops nothing" : " sheared drops itself"));
            }
        }
    }

    private void boneMeal(Check c) {
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(15, -1, 15), Blocks.GRASS.getDefaultState());
        Random r = new Random(7);
        BlockPos at = corner.add(4, 0, 4);
        for (String name : new String[] {"bush", "firefly_bush"}) {
            Block b = block(name);
            c.fill(corner, corner.add(15, 2, 15), Blocks.AIR.getDefaultState());
            c.world.setBlockState(at, b.getDefaultState());
            IGrowable g = (IGrowable) b;
            c.ok(g.canGrow(c.world, at, b.getDefaultState(), false), name + " takes bone meal");
            g.grow(c.world, r, at, b.getDefaultState());
            int around = 0;
            for (EnumFacing f : EnumFacing.HORIZONTALS) {
                around += c.world.getBlockState(at.offset(f)).getBlock() == b ? 1 : 0;
            }
            c.eq(1, around, name + " spreads one beside it");
            for (EnumFacing f : EnumFacing.HORIZONTALS) {
                c.world.setBlockState(at.offset(f), Blocks.STONE.getDefaultState());
            }
            c.ok(!g.canGrow(c.world, at, b.getDefaultState(), false), name + " walled in takes no bone meal");
        }
        c.fill(corner, corner.add(15, 2, 15), Blocks.AIR.getDefaultState());
        Block shortGrass = block("short_dry_grass");
        Block tallGrass = block("tall_dry_grass");
        c.world.setBlockState(at, shortGrass.getDefaultState());
        ((IGrowable) shortGrass).grow(c.world, r, at, shortGrass.getDefaultState());
        c.eq(tallGrass, c.world.getBlockState(at).getBlock(), "short dry grass grows tall");
        ((IGrowable) tallGrass).grow(c.world, r, at, tallGrass.getDefaultState());
        int shorts = 0;
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            shorts += c.world.getBlockState(at.offset(f)).getBlock() == shortGrass ? 1 : 0;
        }
        c.eq(1, shorts, "tall dry grass spreads short dry grass");
        BlockSegmented flowers = FloraContent.wildflowers;
        BlockPos f = corner.add(10, 0, 10);
        c.world.setBlockState(f, flowers.getDefaultState());
        for (int n = 2; n <= 4; n++) {
            flowers.grow(c.world, r, f, c.world.getBlockState(f));
            c.eq(n, c.world.getBlockState(f).getValue(flowers.amount()), "bone meal adds a wildflower: " + n);
        }
        flowers.grow(c.world, r, f, c.world.getBlockState(f));
        c.eq(1, Drops.count(c.world, f, item("wildflowers")), "bone meal on four wildflowers drops one");
        c.ok(!FloraContent.leafLitter.canGrow(c.world, f, FloraContent.leafLitter.getDefaultState(), false), "leaf litter takes no bone meal");
    }

    private void cactus(Check c) {
        c.ok(CactusFlowerTransformer.applied, "P1 applied to BlockCactus");
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
        // 1.21.5: a three-tall cactus still ages (1.12.2 left its top alone).
        BlockPos base = corner.add(4, 0, 4);
        c.world.setBlockState(base, Blocks.CACTUS.getDefaultState(), 2);
        c.world.setBlockState(base.up(), Blocks.CACTUS.getDefaultState(), 2);
        c.world.setBlockState(base.up(2), Blocks.CACTUS.getDefaultState(), 2);
        BlockPos top = base.up(2);
        Blocks.CACTUS.updateTick(c.world, top, c.world.getBlockState(top), new Random(1));
        c.eq(1, c.world.getBlockState(top).getValue(BlockCactus.AGE), "the top of a three-tall cactus ages");
        // At age 8: a flower, one time in four on a three-tall cactus, one in ten on a shorter one.
        int tall = 0;
        int small = 0;
        BlockPos lone = corner.add(12, 0, 12);
        for (int i = 0; i < 400; i++) {
            c.world.setBlockState(top.up(), Blocks.AIR.getDefaultState(), 2);
            c.world.setBlockState(top, Blocks.CACTUS.getDefaultState().withProperty(BlockCactus.AGE, 8), 2);
            Blocks.CACTUS.updateTick(c.world, top, c.world.getBlockState(top), R);
            tall += c.world.getBlockState(top.up()).getBlock() == FloraContent.cactusFlower ? 1 : 0;
            c.world.setBlockState(lone.up(), Blocks.AIR.getDefaultState(), 2);
            c.world.setBlockState(lone, Blocks.CACTUS.getDefaultState().withProperty(BlockCactus.AGE, 8), 2);
            Blocks.CACTUS.updateTick(c.world, lone, c.world.getBlockState(lone), R);
            small += c.world.getBlockState(lone.up()).getBlock() == FloraContent.cactusFlower ? 1 : 0;
        }
        c.ok(tall > 70 && tall < 130, "flowers on three-tall cacti about 1 in 4 (" + tall + "/400)");
        c.ok(small > 20 && small < 65, "flowers on one-tall cacti about 1 in 10 (" + small + "/400)");
        c.eq(9, c.world.getBlockState(lone).getValue(BlockCactus.AGE), "the cactus ages past 8 either way");
        // A flowered cactus no longer grows (air above it is gone); a bare one at 15 grows.
        c.world.setBlockState(lone.up(), Blocks.AIR.getDefaultState(), 2);
        c.world.setBlockState(lone, Blocks.CACTUS.getDefaultState().withProperty(BlockCactus.AGE, 15), 2);
        Blocks.CACTUS.updateTick(c.world, lone, c.world.getBlockState(lone), new Random(3));
        c.eq(Blocks.CACTUS, c.world.getBlockState(lone.up()).getBlock(), "a cactus at 15 grows one taller");
        c.eq(0, c.world.getBlockState(lone).getValue(BlockCactus.AGE), "and starts again at 0");
        // No flower where a cactus could not grow (a block beside the spot above).
        c.world.setBlockState(lone.up(), Blocks.AIR.getDefaultState(), 2);
        c.world.setBlockState(lone.up().east(), Blocks.STONE.getDefaultState(), 2);
        int walled = 0;
        for (int i = 0; i < 100; i++) {
            c.world.setBlockState(lone, Blocks.CACTUS.getDefaultState().withProperty(BlockCactus.AGE, 8), 2);
            Blocks.CACTUS.updateTick(c.world, lone, c.world.getBlockState(lone), R);
            walled += c.world.getBlockState(lone.up()).getBlock() == FloraContent.cactusFlower ? 1 : 0;
            c.world.setBlockState(lone.up(), Blocks.AIR.getDefaultState(), 2);
        }
        c.eq(0, walled, "no flower where a cactus could not stand");
    }

    private void deadBush(Check c) {
        c.ok(DeadBushSoilTransformer.applied, "P2 applied to BlockDeadBush");
        BlockPos at = c.area().add(4, 1, 4);
        c.ok(stays(c, at, Blocks.FARMLAND.getDefaultState(), Blocks.DEADBUSH), "dead bush on farmland");
        c.ok(stays(c, at, Blocks.GRASS.getDefaultState(), Blocks.DEADBUSH), "dead bush on grass");
        c.ok(stays(c, at, Blocks.MYCELIUM.getDefaultState(), Blocks.DEADBUSH), "dead bush on mycelium");
        c.ok(stays(c, at, Blocks.SAND.getDefaultState(), Blocks.DEADBUSH), "dead bush still on sand");
        c.ok(!stays(c, at, Blocks.STONE.getDefaultState(), Blocks.DEADBUSH), "no dead bush on stone");
    }

    private void fallen(Check c) {
        int placed = 0;
        int logs = 0;
        int sideways = 0;
        int mushrooms = 0;
        int vines = 0;
        for (int seed = 0; seed < 12; seed++) {
            BlockPos corner = c.area();
            c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.GRASS.getDefaultState());
            BlockPos at = corner.add(16, 0, 16);
            FallenTrees.Fallen kind = FallenTrees.Fallen.values()[seed % FallenTrees.Fallen.values().length];
            FallenTrees.place(c.world, R, at, kind);
            IBlockState stump = c.world.getBlockState(at);
            if (stump.getBlock() instanceof BlockLog && stump.getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.Y) {
                placed++;
            }
            for (BlockPos p : BlockPos.getAllInBox(corner.add(2, 0, 2), corner.add(29, 1, 29))) {
                IBlockState s = c.world.getBlockState(p);
                if (s.getBlock() instanceof BlockLog && !p.equals(at)) {
                    logs++;
                    if (s.getValue(BlockLog.LOG_AXIS) != BlockLog.EnumAxis.Y) {
                        sideways++;
                    }
                }
                if (s.getBlock() == Blocks.RED_MUSHROOM || s.getBlock() == Blocks.BROWN_MUSHROOM) {
                    mushrooms++;
                }
                if (s.getBlock() == Blocks.VINE) {
                    vines++;
                }
            }
        }
        c.eq(12, placed, "every fallen tree leaves an upright stump");
        c.ok(logs >= 12 * 2 && logs <= 12 * 13, "fallen logs lie beside the stumps (" + logs + ")");
        c.eq(logs, sideways, "every fallen log lies on its side");
        c.ok(vines > 0, "oak and jungle stumps carry vines (" + vines + ")");
        c.ok(mushrooms >= 0, "mushrooms on some logs (" + mushrooms + ")");
        // On a ledge with nowhere to lie: only the stump.
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.AIR.getDefaultState());
        c.world.setBlockState(corner.add(16, -1, 16), Blocks.GRASS.getDefaultState());
        FallenTrees.place(c.world, new Random(5), corner.add(16, 0, 16), FallenTrees.Fallen.SPRUCE);
        int rest = 0;
        for (BlockPos p : BlockPos.getAllInBox(corner.add(2, -8, 2), corner.add(29, 2, 29))) {
            rest += c.world.getBlockState(p).getBlock() instanceof BlockLog ? 1 : 0;
        }
        c.eq(1, rest, "over a drop, only the stump");
    }

    /** A plain 1.12.2-like oak: a 5-high trunk under a leaf blob. */
    private static void oak(Check c, BlockPos at) {
        for (int y = 0; y < 5; y++) {
            c.world.setBlockState(at.up(y), Blocks.LOG.getDefaultState(), 2);
        }
        for (BlockPos p : BlockPos.getAllInBox(at.add(-2, 3, -2), at.add(2, 6, 2))) {
            if (c.world.isAirBlock(p)) {
                c.world.setBlockState(p, Blocks.LEAVES.getDefaultState(), 2);
            }
        }
    }

    private static void biome(Check c, BlockPos corner, Biome biome) {
        for (int x = corner.getX() - 16; x < corner.getX() + 48; x += 16) {
            for (int z = corner.getZ() - 16; z < corner.getZ() + 48; z += 16) {
                Chunk chunk = c.world.getChunk(new BlockPos(x, 0, z));
                Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(biome));
            }
        }
    }

    private static int count(Check c, BlockPos from, BlockPos to, Block b) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            n += c.world.getBlockState(p).getBlock() == b ? 1 : 0;
        }
        return n;
    }

    private void litterUnderTrees(Check c) {
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.GRASS.getDefaultState());
        biome(c, corner, Biomes.FOREST);
        oak(c, corner.add(15, 0, 15));
        FallenTrees.litterUnderTrees(c.world, new Random(3), corner.add(8, 0, 8));
        int litter = count(c, corner.add(9, 0, 9), corner.add(21, 0, 21), block("leaf_litter"));
        c.ok(litter > 20, "a forest tree drops leaf litter around its foot (" + litter + ")");
        c.eq(0, count(c, corner.add(0, 0, 0), corner.add(9, 0, 31), block("leaf_litter")), "none beyond four blocks out");
        boolean onGround = true;
        for (BlockPos p : BlockPos.getAllInBox(corner, corner.add(31, 3, 31))) {
            if (c.world.getBlockState(p).getBlock() == block("leaf_litter")) {
                onGround &= p.getY() == corner.getY() && c.world.getBlockState(p.down()).getBlock() == Blocks.GRASS;
            }
        }
        c.ok(onGround, "leaf litter lies on the ground, never on leaves");
        // A plains tree drops none.
        BlockPos plain = c.area();
        c.fill(plain.add(0, -1, 0), plain.add(31, -1, 31), Blocks.GRASS.getDefaultState());
        biome(c, plain, Biomes.PLAINS);
        oak(c, plain.add(15, 0, 15));
        FallenTrees.litterUnderTrees(c.world, new Random(3), plain.add(8, 0, 8));
        c.eq(0, count(c, plain, plain.add(31, 0, 31), block("leaf_litter")), "a plains tree drops no leaf litter");
    }

    private void generation(Check c) {
        c.eq("forest", FloraBiomes.modernName(Biomes.FOREST), "1.12.2's forest is the forest");
        c.eq("old_growth_birch_forest", FloraBiomes.modernName(Biomes.MUTATED_BIRCH_FOREST), "tall birch forest is old growth birch");
        c.eq("wooded_badlands", FloraBiomes.modernName(Biomes.MESA_ROCK), "mesa plateau F is wooded badlands");
        c.eq(null, FloraBiomes.modernName(Biomes.HELL), "the nether has none");
        c.ok(FloraBiomes.features("plains").contains("patch_bush"), "plains have bushes");

        // Plains with water channels: bushes on grass, firefly bushes beside water.
        int bushes = 0;
        int fireflies = 0;
        boolean fireflyByWater = true;
        boolean bushOnGrass = true;
        String fireflyOn = null;
        for (int seed = 0; seed < 16; seed++) {
            BlockPos corner = c.area();
            c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.GRASS.getDefaultState());
            for (int x = 0; x < 32; x += 4) {
                c.fill(corner.add(x, -1, 0), corner.add(x, -1, 31), Blocks.WATER.getDefaultState());
            }
            biome(c, corner, Biomes.PLAINS);
            FloraGen.decorate(c.world, R, corner, "plains");
            for (BlockPos p : BlockPos.getAllInBox(corner.add(0, -2, 0), corner.add(31, 4, 31))) {
                Block b = c.world.getBlockState(p).getBlock();
                if (b == block("bush")) {
                    bushes++;
                    bushOnGrass &= c.world.getBlockState(p.down()).getBlock() != Blocks.WATER
                            && FloraContent.bush.canBlockStay(c.world, p, c.world.getBlockState(p));
                } else if (b == block("firefly_bush")) {
                    fireflies++;
                    // On soil it grows on: the laid grass, or a neighbor chunk's own soil decorated into the area
                    // later (Caves & Cliffs' moss, a 1.21 #dirt block), never on the water channels.
                    Block under = c.world.getBlockState(p.down()).getBlock();
                    boolean soil = under != Blocks.WATER && FloraContent.fireflyBush.canBlockStay(c.world, p, c.world.getBlockState(p));
                    if (!soil && fireflyOn == null) {
                        fireflyOn = c.world.getBlockState(p.down()) + " at " + p.subtract(corner);
                    }
                    fireflyByWater &= soil;
                }
            }
        }
        c.ok(bushes > 10, "plains grow bushes (" + bushes + " in 16 chunks)");
        c.ok(bushOnGrass, "every bush stands on soil it grows on");
        c.ok(fireflies > 10, "firefly bushes grow near water (" + fireflies + ")");
        c.ok(fireflyByWater, "every firefly bush stands on soil it grows on" + (fireflyOn == null ? "" : ": one on " + fireflyOn));

        // Desert: dry grass, and flowers on 1.12.2's cacti.
        int dry = 0;
        int flowers = 0;
        int cacti = 0;
        for (int seed = 0; seed < 12; seed++) {
            BlockPos corner = c.area();
            c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
            biome(c, corner, Biomes.DESERT);
            for (int i = 0; i < 16; i++) {
                BlockPos p = corner.add(9 + (i % 4) * 4, 0, 9 + (i / 4) * 4);
                c.world.setBlockState(p, Blocks.CACTUS.getDefaultState(), 2);
                cacti++;
            }
            FloraGen.decorate(c.world, R, corner, "desert");
            dry += count(c, corner, corner.add(31, 0, 31), block("short_dry_grass"));
            dry += count(c, corner, corner.add(31, 0, 31), block("tall_dry_grass"));
            flowers += count(c, corner.add(0, 1, 0), corner.add(31, 1, 31), block("cactus_flower"));
        }
        c.ok(dry > 10, "deserts grow dry grass (" + dry + " in 12 chunks)");
        c.ok(flowers > cacti / 8 && flowers < cacti / 2, "about one cactus in four flowers (" + flowers + "/" + cacti + ")");

        // Birch forest: wildflowers. Dark forest: leaf litter patches on grass.
        int wild = 0;
        int litter = 0;
        for (int seed = 0; seed < 12; seed++) {
            BlockPos corner = c.area();
            c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.GRASS.getDefaultState());
            biome(c, corner, seed % 2 == 0 ? Biomes.BIRCH_FOREST : Biomes.ROOFED_FOREST);
            FloraGen.decorate(c.world, R, corner, seed % 2 == 0 ? "birch_forest" : "dark_forest");
            wild += count(c, corner, corner.add(31, 0, 31), block("wildflowers"));
            litter += count(c, corner, corner.add(31, 0, 31), block("leaf_litter"));
        }
        c.ok(wild > 20, "birch forests grow wildflowers (" + wild + ")");
        c.ok(litter > 20, "dark forests lay leaf litter (" + litter + ")");

        // The whole cactus patch (for other terrain): columns of 1-3 cacti, some with a flower.
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
        Random r = new Random(11);
        for (int i = 0; i < 20; i++) {
            FloraFeatures.patch(c.world, r, corner.add(16, 0, 16), 10, 7, 3, FloraFeatures.Kind.CACTUS);
        }
        int cactusBlocks = count(c, corner, corner.add(31, 4, 31), Blocks.CACTUS);
        int topFlowers = count(c, corner, corner.add(31, 4, 31), block("cactus_flower"));
        c.ok(cactusBlocks > 10 && topFlowers > 0, "cactus patches place cacti and flowers (" + cactusBlocks + ", " + topFlowers + ")");
        boolean flowerOnCactus = true;
        for (BlockPos p : BlockPos.getAllInBox(corner, corner.add(31, 4, 31))) {
            if (c.world.getBlockState(p).getBlock() == block("cactus_flower")) {
                flowerOnCactus &= c.world.getBlockState(p.down()).getBlock() == Blocks.CACTUS;
            }
        }
        c.ok(flowerOnCactus, "every patch flower tops a cactus");
        c.ok(!FloraFeatures.place(c.world, r, corner, "no_such_feature"), "an unknown feature is refused");
        naturalChunks(c);
    }

    /** The spawn chunks this world generated itself went through 1.12.2's decoration hook. */
    private void naturalChunks(Check c) {
        java.util.Map<Block, Integer> found = new java.util.LinkedHashMap<Block, Integer>();
        int chunks = 0;
        int horizontalLogs = 0;
        for (Chunk chunk : c.world.getChunkProvider().getLoadedChunks()) {
            if (!chunk.isTerrainPopulated() || Math.abs(chunk.x) > 64 || Math.abs(chunk.z) > 64) {
                continue;
            }
            chunks++;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int top = chunk.getHeightValue(x, z) + 2;
                    for (int y = 40; y < Math.min(top + 16, 255); y++) {
                        IBlockState s = chunk.getBlockState(x, y, z);
                        Block b = s.getBlock();
                        if (b.getRegistryName() != null && TrickyTrialsBackport.MOD_ID.equals(b.getRegistryName().getNamespace())
                                && Arrays.asList(NAMES).contains(b.getRegistryName().getPath())) {
                            found.merge(b, 1, Integer::sum);
                        }
                        if (b instanceof BlockLog && s.getValue(BlockLog.LOG_AXIS) != BlockLog.EnumAxis.Y
                                && s.getValue(BlockLog.LOG_AXIS) != BlockLog.EnumAxis.NONE) {
                            horizontalLogs++;
                        }
                    }
                }
            }
        }
        java.util.Map<String, Integer> biomes = new java.util.TreeMap<String, Integer>();
        for (Chunk chunk : c.world.getChunkProvider().getLoadedChunks()) {
            if (chunk.isTerrainPopulated() && Math.abs(chunk.x) <= 64 && Math.abs(chunk.z) <= 64) {
                String n = String.valueOf(FloraBiomes.modernName(c.world.getBiome(new BlockPos(chunk.x * 16 + 16, 64, chunk.z * 16 + 16))));
                biomes.merge(n, 1, Integer::sum);
            }
        }
        int total = 0;
        StringBuilder b = new StringBuilder();
        for (java.util.Map.Entry<Block, Integer> e : found.entrySet()) {
            total += e.getValue();
            b.append(e.getKey().getRegistryName().getPath()).append('=').append(e.getValue()).append(' ');
        }
        TrickyTrialsBackport.LOGGER.info("SELFTEST flora natural chunks: {} populated, biomes {}, found {}, sideways logs {}",
                chunks, biomes, b, horizontalLogs);
        c.ok(chunks > 50 && total > 0, "the world's own chunks grew spring plants (" + chunks + " chunks: " + b + ")");
    }
}
