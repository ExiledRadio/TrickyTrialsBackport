package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
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
 * The table is Backport Core's ({@code copper.Oxidation}, CORE_REQUESTS #5), shared by every mod's copper. Until
 * Caves &amp; Cliffs enters its own blocks there, they are entered by name ({@link #addOwnersBlocks}), so our
 * bulbs and doors wait for its copper and age with it.
 *
 * <p>States carry across by property (stairs keep facing, half and shape; bulbs stay lit; a door's two
 * halves change together).
 */
public final class Weathering {

    /** 64/1125: the chance a random tick tries to age a block at all. */
    public static final float TRY_CHANCE = Oxidation.TRY_CHANCE;

    private Weathering() {
    }

    /** Declares one shape's four stages, unaffected first, in Backport Core's table. */
    public static void chain(Block... stages) {
        Oxidation.chain(stages);
    }

    /** Declares a block and its waxed form, in Backport Core's table. */
    public static void waxed(Block plain, Block waxed) {
        Oxidation.waxed(plain, waxed);
    }

    @Nullable
    public static Block next(Block b) {
        return Oxidation.next(b);
    }

    @Nullable
    public static Block previous(Block b) {
        return Oxidation.previous(b);
    }

    @Nullable
    public static Block waxedOf(Block b) {
        return Oxidation.waxedOf(b);
    }

    @Nullable
    public static Block unwaxed(Block b) {
        return Oxidation.unwaxed(b);
    }

    /** @return the unaffected form of an unwaxed copper block (itself if unaffected) */
    public static Block first(Block b) {
        return Oxidation.first(b);
    }

    /** @return 0-3 for unwaxed copper blocks, -1 for anything else (waxed copper included) */
    public static int stage(Block b) {
        return Oxidation.stage(b);
    }

    public static boolean isCopper(Block b) {
        return Oxidation.isCopper(b);
    }

    /** The same state on another block of the chain: every property both blocks have is copied. */
    public static IBlockState copy(IBlockState from, Block to) {
        return Oxidation.copy(from, to);
    }

    /** Changes a copper block to another block of its chain, keeping its state; for a door, both halves. */
    public static void change(World world, BlockPos pos, IBlockState state, Block to) {
        Oxidation.change(world, pos, state, to);
    }

    /** A random tick on an unwaxed copper block: Backport Core's rule, while {@code copper.oxidation} is on. */
    public static void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (ModConfig.copper.oxidation) {
            Oxidation.randomTick(world, pos, state, rand);
        }
    }

    /** @return the chance this block ages, given every mod's copper around it */
    public static float ageChance(World world, BlockPos pos, int age) {
        return Oxidation.ageChance(world, pos, age);
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
