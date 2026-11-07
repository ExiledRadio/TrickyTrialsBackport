package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The oxidation chain every copper block belongs to: what it oxidizes into, what scraping returns it to,
 * and its waxed and unwaxed forms. The same table and the same rule as Caves &amp; Cliffs'
 * {@code copper/Weathering} (whose author proposes it for Backport Core), so copper ages alike whichever
 * mod a block comes from.
 *
 * <h2>Oxidation (current Java)</h2>
 * On each random tick an unwaxed copper block that is not fully oxidized tries to age with chance
 * 64/1125. It looks at every other unwaxed copper block within a Manhattan distance of 4: if any is less
 * oxidized it stops; otherwise, with {@code a} as many at its own stage and {@code b} more oxidized, it
 * ages with chance {@code ((b + 1) / (a + b + 1))^2}, times 0.75 while unaffected.
 *
 * <h2>Two mods' copper</h2>
 * With Caves &amp; Cliffs installed its blocks are entered here by name too ({@link #addOwnersBlocks}), so
 * our bulbs and doors wait for its copper and age with it. Its own table does not know ours yet; one table
 * in Backport Core ends that ({@code docs/CORE_REQUESTS.md} #5).
 *
 * <p>States carry across by property (stairs keep facing, half and shape; bulbs stay lit; a door's two
 * halves change together).
 */
public final class Weathering {

    /** 64/1125: the chance a random tick tries to age a block at all. */
    public static final float TRY_CHANCE = 0.05688889F;

    private static final Map<Block, Block> NEXT = new HashMap<Block, Block>();
    private static final Map<Block, Block> PREVIOUS = new HashMap<Block, Block>();
    private static final Map<Block, Block> WAXED = new HashMap<Block, Block>();
    private static final Map<Block, Block> UNWAXED = new HashMap<Block, Block>();
    /** Oxidation stage of every unwaxed copper block, 0-3. */
    private static final Map<Block, Integer> STAGE = new HashMap<Block, Integer>();

    private Weathering() {
    }

    /** Declares one shape's four stages, unaffected first. */
    public static void chain(Block... stages) {
        for (int i = 0; i < stages.length; i++) {
            STAGE.put(stages[i], i);
            if (i + 1 < stages.length) {
                NEXT.put(stages[i], stages[i + 1]);
                PREVIOUS.put(stages[i + 1], stages[i]);
            }
        }
    }

    /** Declares a block and its waxed form. */
    public static void waxed(Block plain, Block waxed) {
        WAXED.put(plain, waxed);
        UNWAXED.put(waxed, plain);
    }

    @Nullable
    public static Block next(Block b) {
        return NEXT.get(b);
    }

    @Nullable
    public static Block previous(Block b) {
        return PREVIOUS.get(b);
    }

    @Nullable
    public static Block waxedOf(Block b) {
        return WAXED.get(b);
    }

    @Nullable
    public static Block unwaxed(Block b) {
        return UNWAXED.get(b);
    }

    /** @return the unaffected form of an unwaxed copper block (itself if unaffected) */
    public static Block first(Block b) {
        Block p;
        while ((p = PREVIOUS.get(b)) != null) {
            b = p;
        }
        return b;
    }

    /** @return 0-3 for unwaxed copper blocks, -1 for anything else (waxed copper included) */
    public static int stage(Block b) {
        Integer s = STAGE.get(b);
        return s == null ? -1 : s;
    }

    public static boolean isCopper(Block b) {
        return STAGE.containsKey(b) || UNWAXED.containsKey(b);
    }

    /** The same state on another block of the chain: every property both blocks have is copied. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static IBlockState copy(IBlockState from, Block to) {
        IBlockState out = to.getDefaultState();
        for (IProperty p : from.getPropertyKeys()) {
            if (out.getPropertyKeys().contains(p)) {
                out = out.withProperty(p, from.getValue(p));
            }
        }
        return out;
    }

    /**
     * Changes a copper block to another block of its chain, keeping its state; for a door, both halves.
     */
    public static void change(World world, BlockPos pos, IBlockState state, Block to) {
        if (state.getBlock() instanceof BlockDoor) {
            boolean lower = state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER;
            BlockPos other = lower ? pos.up() : pos.down();
            IBlockState otherState = world.getBlockState(other);
            // The upper half first, so the lower half never sees a mismatched partner and breaks.
            if (otherState.getBlock() == state.getBlock()) {
                world.setBlockState(lower ? other : pos, copy(lower ? otherState : state, to), 2);
                world.setBlockState(lower ? pos : other, copy(lower ? state : otherState, to), 3);
                return;
            }
        }
        world.setBlockState(pos, copy(state, to), 3);
    }

    /** A random tick on an unwaxed copper block. */
    public static void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!ModConfig.copper.oxidation) {
            return;
        }
        if (state.getBlock() instanceof BlockDoor && state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER) {
            // A door ages as one block: its lower half decides.
            return;
        }
        Block next = NEXT.get(state.getBlock());
        if (next == null || rand.nextFloat() >= TRY_CHANCE) {
            return;
        }
        if (rand.nextFloat() < ageChance(world, pos, stage(state.getBlock()))) {
            change(world, pos, state, next);
        }
    }

    /** @return the chance this block ages, given the copper around it; 0 if a less oxidized one is near */
    public static float ageChance(World world, BlockPos pos, int age) {
        int same = 0;
        int older = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            int rx = 4 - Math.abs(dx);
            for (int dy = -rx; dy <= rx; dy++) {
                int rz = rx - Math.abs(dy);
                for (int dz = -rz; dz <= rz; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    p.setPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!world.isBlockLoaded(p)) {
                        continue;
                    }
                    int other = stage(world.getBlockState(p).getBlock());
                    if (other < 0) {
                        continue;
                    }
                    if (other < age) {
                        return 0;
                    }
                    if (other > age) {
                        older++;
                    } else {
                        same++;
                    }
                }
            }
        }
        float f = (older + 1) / (float) (older + same + 1);
        return f * f * (age == 0 ? 0.75F : 1.0F);
    }

    /**
     * Enters Caves &amp; Cliffs' copper blocks by name, for the nearby-copper count, when it provides them.
     */
    public static void addOwnersBlocks() {
        if (!Owners.present(Owners.CAVES_AND_CLIFFS)) {
            return;
        }
        java.util.List<String> shapes = new java.util.ArrayList<String>(Arrays.asList(CopperContent.OWNED_SHAPES));
        shapes.add("cut_copper_double_slab");
        for (String shape : shapes) {
            Block[] chain = new Block[4];
            boolean all = true;
            for (CopperStage stage : CopperStage.values()) {
                Block plain = theirs(CopperContent.ownedName(shape, stage, false));
                Block waxed = theirs(CopperContent.ownedName(shape, stage, true));
                chain[stage.ordinal()] = plain;
                all &= plain != null;
                if (plain != null && waxed != null) {
                    waxed(plain, waxed);
                }
            }
            if (all) {
                chain(chain);
            }
        }
    }

    @Nullable
    private static Block theirs(String name) {
        ResourceLocation id = new ResourceLocation(Owners.CAVES_AND_CLIFFS, name);
        Block b = ForgeRegistries.BLOCKS.getValue(id);
        return b != null && id.equals(b.getRegistryName()) ? b : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Wax

    /** @return whether this stack waxes copper: an item in {@code copper.waxItems}, or one with the ore name honeycomb */
    public static boolean isWax(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id != null && Arrays.asList(ModConfig.copper.waxItems).contains(id.toString())) {
            return true;
        }
        for (int ore : OreDictionary.getOreIDs(stack)) {
            if ("honeycomb".equals(OreDictionary.getOreName(ore))) {
                return true;
            }
        }
        return false;
    }

    /** Gives every installed item of {@code copper.waxItems} the ore name honeycomb, so recipes take them. */
    public static void registerWaxOres() {
        for (String name : ModConfig.copper.waxItems) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
            if (item != null && item != net.minecraft.init.Items.AIR) {
                OreDictionary.registerOre("honeycomb", item);
            }
        }
    }
}
