package com.exiledradio.trickytrialsbackport.common;

import com.exiledradio.backportcore.ownership.BlockOwnership;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Who owns the things from older updates this mod needs (family Rule 1), and the lookups for them.
 *
 * <h2>The rule</h2>
 * The mod for the version line in which a thing first shipped owns it. When the owner is installed this
 * mod registers nothing for it and uses the owner's; when it is not, this mod registers its own copy
 * under its own id, with the same states and metadata, so the owner can later rename it in place.
 * Blocks go through Backport Core's {@link BlockOwnership}; items and effects, which Core's table does
 * not cover yet, are looked up here by registry name.
 *
 * <h2>Shim</h2>
 * Core 1.0.0's table only lists Village &amp; Pillage's 1.13 blocks. Until Caves &amp; Cliffs declares its
 * own (or Core lists them, {@code docs/CORE_REQUESTS.md} #1), {@link #declareOlderOwners} claims them for
 * it, so {@code BlockOwnership.shouldRegister} answers correctly whether or not it is installed.
 */
public final class Owners {

    public static final String UPDATE_AQUATIC = "updateaquaticbackport";
    /** Bad Omen, raids, Raid Omen and the ominous bottle; the loom, smithing table, lantern, composter. */
    public static final String VILLAGE_AND_PILLAGE = "villagerbackport";
    /** Honeycomb (copper waxing), bees. */
    public static final String BUZZY_BEES = "buzzybeesbackport";
    /** Netherite, chains, crimson and warped wood. */
    public static final String NETHER_UPDATE = "netherupdatebackport";
    /** Copper, tuff, the lightning rod, moss. */
    public static final String CAVES_AND_CLIFFS = "cavesandcliffsbackport";
    /** Mangrove wood and swamps, chest boats. */
    public static final String WILD_UPDATE = "wildupdatebackport";
    /** Decorated pots and sherds, armor trims, hanging signs, cherry and bamboo wood, camels. */
    public static final String TRAILS_AND_TALES = "trailsandtalesbackport";

    /** The four oxidation stages, in order, as vanilla names their blocks' prefixes. */
    public static final String[] OXIDATION = {"", "exposed_", "weathered_", "oxidized_"};

    private Owners() {
    }

    /** Claims Caves &amp; Cliffs' 1.17 blocks this mod copies, in Backport Core's table, for their owner. */
    public static void declareOlderOwners() {
        BlockOwnership.declare(CAVES_AND_CLIFFS, "tuff");
        for (String stage : OXIDATION) {
            String block = stage.isEmpty() ? "copper_block" : stage + "copper";
            for (String waxed : new String[] {"", "waxed_"}) {
                BlockOwnership.declare(CAVES_AND_CLIFFS, waxed + block, waxed + stage + "cut_copper",
                        waxed + stage + "cut_copper_stairs", waxed + stage + "cut_copper_slab",
                        waxed + stage + "cut_copper_double_slab");
            }
        }
        BlockOwnership.declare(CAVES_AND_CLIFFS, "copper_ore", "raw_copper_block");
    }

    /** @return true if the owning mod is installed */
    public static boolean present(String owner) {
        return Loader.isModLoaded(owner);
    }

    /** @return true if this mod should register its own copy of an owned block */
    public static boolean copy(String path) {
        return BlockOwnership.shouldRegister(Content.MOD_ID, path);
    }

    /** @return the owner's block if it provides it, else ours; null if neither is registered */
    @Nullable
    public static Block block(String path) {
        return BlockOwnership.resolve(path, Content.MOD_ID);
    }

    /**
     * @return the item {@code owner:name} if the owner is installed and has it, else ours under this mod's
     * id; null if neither exists
     */
    @Nullable
    public static Item item(String owner, String name) {
        if (present(owner)) {
            Item theirs = exact(ForgeRegistries.ITEMS.getValue(new ResourceLocation(owner, name)), new ResourceLocation(owner, name));
            if (theirs != null) {
                return theirs;
            }
        }
        return ours(name);
    }

    /** As {@link #item}, for effects. */
    @Nullable
    public static Potion effect(String owner, String name) {
        if (present(owner)) {
            ResourceLocation id = new ResourceLocation(owner, name);
            Potion theirs = ForgeRegistries.POTIONS.getValue(id);
            if (theirs != null && id.equals(theirs.getRegistryName())) {
                return theirs;
            }
        }
        Potion ours = ForgeRegistries.POTIONS.getValue(Content.id(name));
        return ours != null && Content.id(name).equals(ours.getRegistryName()) ? ours : null;
    }

    /**
     * Looks up another stream's item by name, so streams never link against each other's classes.
     *
     * @return this mod's item {@code name}, or null when its stream is switched off
     */
    @Nullable
    public static Item ours(String name) {
        return exact(ForgeRegistries.ITEMS.getValue(Content.id(name)), Content.id(name));
    }

    /** As {@link #ours(String)}, for blocks. */
    @Nullable
    public static Block ourBlock(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(Content.id(name));
        return block == null || !Content.id(name).equals(block.getRegistryName()) ? null : block;
    }

    @Nullable
    private static Item exact(@Nullable Item item, ResourceLocation id) {
        return item == null || !id.equals(item.getRegistryName()) ? null : item;
    }
}
