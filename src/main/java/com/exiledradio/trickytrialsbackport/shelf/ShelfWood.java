package com.exiledradio.trickytrialsbackport.shelf;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

/**
 * The twelve woods 1.21.9 makes shelves of, in vanilla's order, and who provides each wood here.
 *
 * <p>The six 1.12.2 woods are always there. Pale oak is stream 12's (this mod's own {@code pale_oak_planks}); cherry
 * and bamboo are Trails &amp; Tales', mangrove Wild Update's, crimson and warped Nether Update's. A shelf of one of
 * those woods is registered only when its wood's planks are, so a shelf never exists without its wood.
 */
public enum ShelfWood {
    ACACIA("acacia", null, "acacia_planks", "stripped_acacia_log", MapColor.ADOBE, true),
    BAMBOO("bamboo", Owners.TRAILS_AND_TALES, "bamboo_planks", "stripped_bamboo_block", MapColor.YELLOW, true),
    BIRCH("birch", null, "birch_planks", "stripped_birch_log", MapColor.SAND, true),
    CHERRY("cherry", Owners.TRAILS_AND_TALES, "cherry_planks", "stripped_cherry_log", MapColor.WHITE_STAINED_HARDENED_CLAY, true),
    CRIMSON("crimson", Owners.NETHER_UPDATE, "crimson_planks", "stripped_crimson_stem", MapColor.MAGENTA_STAINED_HARDENED_CLAY, false),
    DARK_OAK("dark_oak", null, "dark_oak_planks", "stripped_dark_oak_log", MapColor.BROWN, true),
    JUNGLE("jungle", null, "jungle_planks", "stripped_jungle_log", MapColor.DIRT, true),
    MANGROVE("mangrove", Owners.WILD_UPDATE, "mangrove_planks", "stripped_mangrove_log", MapColor.RED, true),
    OAK("oak", null, "oak_planks", "stripped_oak_log", MapColor.WOOD, true),
    PALE_OAK("pale_oak", TrickyTrialsBackport.MOD_ID, "pale_oak_planks", "stripped_pale_oak_log", MapColor.QUARTZ, true),
    /** 26.3's poplar, Game Drops Backport's wood (its request 2): light gray, flammable. */
    POPLAR("poplar", Owners.GAME_DROPS, "poplar_planks", "stripped_poplar_log", MapColor.SILVER, true),
    SPRUCE("spruce", null, "spruce_planks", "stripped_spruce_log", MapColor.OBSIDIAN, true),
    WARPED("warped", Owners.NETHER_UPDATE, "warped_planks", "stripped_warped_stem", MapColor.CYAN, false);

    /** The family mods that may register a copy of a stripped log when its owner is absent, in the order asked. */
    private static final String[] STRIPPED_LOG_COPIES = {
            "villagerbackport", Owners.UPDATE_AQUATIC, Owners.TRAILS_AND_TALES, Owners.WILD_UPDATE,
            Owners.NETHER_UPDATE, Owners.CAVES_AND_CLIFFS, Owners.BUZZY_BEES, TrickyTrialsBackport.MOD_ID};

    public final String name;
    /** The mod whose planks this wood needs, or null for 1.12.2's own woods */
    @Nullable
    public final String owner;
    final String planks;
    final String strippedLog;
    /** 1.21.9's map color (its planks', or its log's top for spruce, dark oak, jungle, mangrove), as 1.12.2 names it */
    final MapColor mapColor;
    /** Overworld woods burn and are fuel; crimson and warped are not ({@code non_flammable_wood}). */
    public final boolean flammable;

    ShelfWood(String name, @Nullable String owner, String planks, String strippedLog, MapColor mapColor, boolean flammable) {
        this.name = name;
        this.owner = owner;
        this.planks = planks;
        this.strippedLog = strippedLog;
        this.mapColor = mapColor;
        this.flammable = flammable;
    }

    public String shelfName() {
        return this.name + "_shelf";
    }

    /** @return true if this wood exists in this game: always for 1.12.2's, else when its owner registered its planks */
    public boolean exists() {
        if (this.owner == null) {
            return true;
        }
        if (!TrickyTrialsBackport.MOD_ID.equals(this.owner) && !Owners.present(this.owner)) {
            return false;
        }
        return exact(new ResourceLocation(this.owner, this.planks)) != null;
    }

    /**
     * The recipe's ingredient: 1.21.9's six stripped logs. Stripped logs are 1.13's: their owner's (Core's
     * ownership table), else any family mod's copy. A 1.12.2 wood with no stripped log in the game takes its plain
     * log instead, so the shelf can still be made.
     *
     * @return the ingredient, or null when there is none
     */
    @Nullable
    public ItemStack ingredient() {
        Block stripped = findStrippedLog();
        if (stripped != null) {
            return new ItemStack(stripped, 1, OreDictionary.WILDCARD_VALUE);
        }
        switch (this) {
            case OAK: return new ItemStack(Blocks.LOG, 1, 0);
            case SPRUCE: return new ItemStack(Blocks.LOG, 1, 1);
            case BIRCH: return new ItemStack(Blocks.LOG, 1, 2);
            case JUNGLE: return new ItemStack(Blocks.LOG, 1, 3);
            case ACACIA: return new ItemStack(Blocks.LOG2, 1, 0);
            case DARK_OAK: return new ItemStack(Blocks.LOG2, 1, 1);
            default: return null;
        }
    }

    /** @return the stripped log the recipe uses, or null when no mod registers one */
    @Nullable
    public Block findStrippedLog() {
        String tableOwner = BlockOwnership.owner(this.strippedLog);
        if (tableOwner != null && !tableOwner.contains(":")) {
            Block block = exact(new ResourceLocation(tableOwner, this.strippedLog));
            if (block != null) {
                return block;
            }
        }
        if (this.owner != null) {
            Block block = exact(new ResourceLocation(this.owner, this.strippedLog));
            if (block != null) {
                return block;
            }
        }
        for (String mod : STRIPPED_LOG_COPIES) {
            Block block = exact(new ResourceLocation(mod, this.strippedLog));
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    @Nullable
    private static Block exact(ResourceLocation id) {
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null || block == Blocks.AIR || !id.equals(block.getRegistryName()) ? null : block;
    }
}
