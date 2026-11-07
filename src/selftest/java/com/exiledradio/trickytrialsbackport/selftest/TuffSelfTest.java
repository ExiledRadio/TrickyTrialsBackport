package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.building.BlockShapedSlab;
import com.exiledradio.trickytrialsbackport.tuff.TuffContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Stream 1: the tuff blocks, their shapes, recipes and the copied tuff's generation. */
public final class TuffSelfTest implements SelfTest {

    private static final String[] FULL = {"tuff", "chiseled_tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff_bricks"};
    private static final String[] PREFIXES = {"tuff", "polished_tuff", "tuff_brick"};

    @Override
    public String name() {
        return "tuff";
    }

    @Override
    public void run(Check c) {
        registration(c);
        numbers(c);
        recipes(c);
        placing(c);
        generation(c);
    }

    /** Ours, except tuff itself, which is the owner's when Caves & Cliffs is installed. */
    private static Block block(String name) {
        if ("tuff".equals(name)) {
            return com.exiledradio.trickytrialsbackport.common.Owners.block("tuff");
        }
        return ForgeRegistries.BLOCKS.getValue(Content.id(name));
    }

    /** The owner provides tuff (installed with that stream on). */
    private static boolean owner() {
        return !com.exiledradio.trickytrialsbackport.common.Owners.copy("tuff");
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(Content.id(name));
    }

    private void registration(Check c) {
        if (owner()) {
            c.ok(TuffContent.tuff == null && block("tuff") != null
                    && "cavesandcliffsbackport".equals(block("tuff").getRegistryName().getNamespace()), "with its owner, the owner's tuff");
        } else {
            c.ok(TuffContent.tuff != null && TuffContent.tuff == block("tuff"), "tuff copy registered without its owner");
        }
        List<String> all = new ArrayList<String>();
        for (String f : FULL) {
            if (!"tuff".equals(f) || !owner()) {
                all.add(f);
            }
        }
        for (String p : PREFIXES) {
            all.add(p + "_stairs");
            all.add(p + "_slab");
            all.add(p + "_wall");
        }
        for (String name : all) {
            Block b = block(name);
            c.ok(b != null && Content.id(name).equals(b.getRegistryName()), name + " registered");
            Item i = item(name);
            c.ok(i != null && Content.id(name).equals(i.getRegistryName()), name + " has an item");
            for (IBlockState state : b.getBlockState().getValidStates()) {
                IBlockState back = b.getStateFromMeta(b.getMetaFromState(state));
                if (b instanceof BlockStairs || b instanceof BlockWall) {
                    // Stairs' shape and walls' sides are actual-state properties, never stored.
                    c.eq(b.getMetaFromState(state), b.getMetaFromState(back), name + " metadata round trip");
                } else {
                    c.eq(state, back, name + " metadata round trip");
                }
            }
        }
        for (String p : PREFIXES) {
            Block full = block(p + "_double_slab");
            c.ok(full instanceof BlockShapedSlab && ((BlockSlab) full).isDouble(), p + "_double_slab registered as a double");
            Item none = item(p + "_double_slab");
            c.ok(none == null || !Content.id(p + "_double_slab").equals(none.getRegistryName()), p + "_double_slab has no item");
            c.ok(item(p + "_slab") instanceof ItemSlab, p + "_slab item joins slabs");
            c.ok(block(p + "_stairs") instanceof BlockStairs && block(p + "_wall") instanceof BlockWall, p + " shapes are vanilla kinds");
        }
    }

    private void numbers(Check c) {
        for (String name : new String[] {"tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff", "tuff_stairs",
                "polished_tuff_slab", "tuff_brick_wall", "tuff_brick_double_slab"}) {
            Block b = block(name);
            IBlockState s = b.getDefaultState();
            c.eq(1.5F, b.getBlockHardness(s, c.world, BlockPos.ORIGIN), name + " hardness");
            c.ok(Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-4, name + " blast resistance 6, got " + b.getExplosionResistance(null));
            c.eq("pickaxe", b.getHarvestTool(s), name + " harvest tool");
        }
        c.eq(owner() ? new net.minecraft.util.ResourceLocation("cavesandcliffsbackport", "block.tuff.break") : Content.id("block.tuff.break"),
                block("tuff").getSoundType().getBreakSound().getRegistryName(), "tuff sound");
        c.eq(Content.id("block.tuff.step"), block("tuff_stairs").getSoundType().getStepSound().getRegistryName(), "tuff stairs sound");
        c.eq(Content.id("block.polished_tuff.place"), block("polished_tuff_wall").getSoundType().getPlaceSound().getRegistryName(),
                "polished tuff wall sound");
        c.eq(Content.id("block.tuff_bricks.hit"), block("chiseled_tuff_bricks").getSoundType().getHitSound().getRegistryName(),
                "chiseled tuff bricks sound");
        c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("block.tuff_bricks.fall")), "sound events registered");

        // The double slab drops two halves.
        NonNullList<ItemStack> drops = NonNullList.create();
        Block full = block("tuff_double_slab");
        full.getDrops(drops, c.world, BlockPos.ORIGIN, full.getDefaultState(), 0);
        int slabs = 0;
        for (ItemStack d : drops) {
            slabs += d.getItem() == item("tuff_slab") ? d.getCount() : 100;
        }
        c.eq(2, slabs, "double slab drops two slabs: " + drops);
    }

    private void recipes(Check c) {
        ItemStack tuff = new ItemStack(block("tuff"));
        ItemStack polished = new ItemStack(block("polished_tuff"));
        ItemStack bricks = new ItemStack(block("tuff_bricks"));
        ItemStack e = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(2, 2, tuff, tuff, tuff, tuff), new ItemStack(block("polished_tuff"), 4), "4 tuff to polished");
        Crafting.expect(c, Crafting.grid(2, 2, polished, polished, polished, polished), new ItemStack(block("tuff_bricks"), 4),
                "4 polished to bricks");
        Crafting.expect(c, Crafting.grid(1, 2, new ItemStack(block("tuff_slab")), new ItemStack(block("tuff_slab"))),
                new ItemStack(block("chiseled_tuff")), "2 slabs to chiseled tuff");
        Crafting.expect(c, Crafting.grid(1, 2, new ItemStack(block("tuff_brick_slab")), new ItemStack(block("tuff_brick_slab"))),
                new ItemStack(block("chiseled_tuff_bricks")), "2 brick slabs to chiseled bricks");
        String[] prefixes = {"tuff", "polished_tuff", "tuff_brick"};
        ItemStack[] bases = {tuff, polished, bricks};
        for (int i = 0; i < 3; i++) {
            ItemStack b = bases[i];
            Crafting.expect(c, Crafting.grid(3, 3, b, e, e, b, b, e, b, b, b), new ItemStack(block(prefixes[i] + "_stairs"), 4),
                    prefixes[i] + " stairs");
            Crafting.expect(c, Crafting.grid(3, 1, b, b, b), new ItemStack(block(prefixes[i] + "_slab"), 6), prefixes[i] + " slab");
            Crafting.expect(c, Crafting.grid(3, 2, b, b, b, b, b, b), new ItemStack(block(prefixes[i] + "_wall"), 6),
                    prefixes[i] + " wall");
        }
        // Vanilla's cobblestone recipes are untouched by ours.
        ItemStack cobble = new ItemStack(Blocks.COBBLESTONE);
        Crafting.expect(c, Crafting.grid(3, 2, cobble, cobble, cobble, cobble, cobble, cobble), new ItemStack(Blocks.COBBLESTONE_WALL, 6),
                "cobblestone wall still vanilla");
    }

    private void placing(Check c) {
        BlockPos area = c.area();
        FakePlayer player = c.player("tuff", area.up());
        BlockPos floor = area.add(4, 0, 4);
        c.world.setBlockState(floor, Blocks.STONE.getDefaultState());

        // A slab on the floor, then a second on top of it, makes the double slab.
        Block slab = block("tuff_slab");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(slab, 2));
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState placed = c.world.getBlockState(floor.up());
        c.ok(placed.getBlock() == slab && placed.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM, "slab placed bottom: " + placed);
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, floor.up(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.eq(block("tuff_double_slab"), c.world.getBlockState(floor.up()).getBlock(), "second slab makes the double");
        c.ok(player.getHeldItem(EnumHand.MAIN_HAND).isEmpty(), "both slabs used");

        // A tuff wall joins a cobblestone wall and a polished tuff wall beside it.
        BlockPos w = area.add(10, 0, 10);
        c.world.setBlockState(w, block("tuff_wall").getDefaultState());
        c.world.setBlockState(w.north(), Blocks.COBBLESTONE_WALL.getDefaultState());
        c.world.setBlockState(w.east(), block("polished_tuff_wall").getDefaultState());
        IBlockState actual = c.world.getBlockState(w).getActualState(c.world, w);
        c.ok(actual.getValue(BlockWall.NORTH) && actual.getValue(BlockWall.EAST) && !actual.getValue(BlockWall.SOUTH),
                "wall joins walls: " + actual);
        c.ok(c.world.getBlockState(w.north()).getActualState(c.world, w.north()).getValue(BlockWall.SOUTH),
                "cobblestone wall joins the tuff wall");

        // Stairs placed by a player face them.
        Block stairs = block("tuff_brick_stairs");
        player.rotationYaw = 0.0F; // facing south
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(stairs));
        BlockPos sf = area.add(14, 0, 4);
        c.world.setBlockState(sf, Blocks.STONE.getDefaultState());
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, sf, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState st = c.world.getBlockState(sf.up());
        c.ok(st.getBlock() == stairs && st.getValue(BlockStairs.FACING) == EnumFacing.SOUTH, "stairs placed facing the player's way: " + st);

        // Mining by hand drops nothing; a pickaxe drops the block.
        c.world.setBlockState(sf, block("polished_tuff").getDefaultState());
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.ok(!block("polished_tuff").canHarvestBlock(c.world, sf, player), "no drop without a pickaxe");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        c.ok(block("polished_tuff").canHarvestBlock(c.world, sf, player), "a wooden pickaxe mines polished tuff");
    }

    private void generation(Check c) {
        if (owner()) {
            // The owner generates its own tuff, in its deepslate.
            return;
        }
        // The spawn chunks were generated with our copy's generator: count tuff below y 17 around spawn.
        BlockPos spawn = c.world.getSpawnPoint();
        Block tuff = block("tuff");
        int found = 0;
        int above = 0;
        int chunks = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                int x0 = (spawn.getX() >> 4) * 16 + cx * 16;
                int z0 = (spawn.getZ() >> 4) * 16 + cz * 16;
                if (!c.world.isBlockLoaded(new BlockPos(x0, 0, z0))) {
                    continue;
                }
                chunks++;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 40; y++) {
                            if (c.world.getBlockState(new BlockPos(x0 + x, y, z0 + z)).getBlock() == tuff) {
                                if (y <= 16 + 4) {
                                    found++;
                                } else {
                                    above++;
                                }
                            }
                        }
                    }
                }
            }
        }
        c.ok(chunks >= 25, "spawn chunks loaded: " + chunks);
        c.ok(found > chunks * 10, "tuff generated deep down: " + found + " blocks in " + chunks + " chunks");
        c.eq(0, above, "no tuff above y 20");
    }
}
