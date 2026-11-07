package com.exiledradio.trickytrialsbackport.tuff;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.building.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * Stream 1: 1.21's tuff blocks, and a copy of 1.17's tuff when its owner (Caves &amp; Cliffs) is absent.
 *
 * <ul>
 *   <li>Tuff stairs, slab and wall; polished tuff and its three shapes; chiseled tuff; tuff bricks and
 *       their three shapes; chiseled tuff bricks.</li>
 *   <li>Three sound types, as 1.21: tuff (tuff, its shapes, chiseled tuff), polished tuff (and its
 *       shapes), tuff bricks (and theirs, and chiseled tuff bricks).</li>
 *   <li>Crafting as 1.21.11; a 1.14 stonecutter derives its cuts from these recipes by itself.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class TuffContent {

    /** Our tuff, when we register the copy; null when Caves &amp; Cliffs provides it or the stream is off. */
    @Nullable public static Block tuff;
    /** The tuff every shape is cut from: ours, or an unregistered twin standing in for the owner's. */
    private static Block tuffModel;

    public static Block polishedTuff;
    public static Block chiseledTuff;
    public static Block tuffBricks;
    public static Block chiseledTuffBricks;
    public static Shapes tuffShapes;
    public static Shapes polishedShapes;
    public static Shapes brickShapes;

    private TuffContent() {
    }

    public static boolean enabled() {
        return ModConfig.tuff.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();
        SoundType tuffSound = Content.soundType("tuff", 1.0F, 1.0F);
        SoundType polishedSound = Content.soundType("polished_tuff", 1.0F, 1.0F);
        SoundType brickSound = Content.soundType("tuff_bricks", 1.0F, 1.0F);

        if (Owners.copy("tuff")) {
            tuff = Content.block(registry, "tuff", new BlockTuff(tuffSound), "pickaxe");
            tuffModel = tuff;
        } else {
            tuff = null;
            // Same numbers as the owner's tuff, which its shapes copy; never registered.
            tuffModel = new BlockTuff(tuffSound);
        }
        tuffShapes = Shapes.register(registry, "tuff", tuffModel, true, true, true);
        chiseledTuff = Content.block(registry, "chiseled_tuff", new BlockTuff(tuffSound), "pickaxe");

        polishedTuff = Content.block(registry, "polished_tuff", new BlockTuff(polishedSound), "pickaxe");
        polishedShapes = Shapes.register(registry, "polished_tuff", polishedTuff, true, true, true);

        tuffBricks = Content.block(registry, "tuff_bricks", new BlockTuff(brickSound), "pickaxe");
        brickShapes = Shapes.register(registry, "tuff_brick", tuffBricks, true, true, true);
        chiseledTuffBricks = Content.block(registry, "chiseled_tuff_bricks", new BlockTuff(brickSound), "pickaxe");

        if (tuff != null && ModConfig.tuff.generate) {
            GameRegistry.registerWorldGenerator(new TuffGenerator(tuff.getDefaultState()), 0);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        if (tuff != null) {
            Content.registerBlockItem(registry, tuff);
        }
        tuffShapes.registerItems(registry);
        Content.registerBlockItem(registry, chiseledTuff);
        Content.registerBlockItem(registry, polishedTuff);
        polishedShapes.registerItems(registry);
        Content.registerBlockItem(registry, tuffBricks);
        brickShapes.registerItems(registry);
        Content.registerBlockItem(registry, chiseledTuffBricks);

        // Forge's names for "any block of this stone", as vanilla's granite and andesite have.
        if (tuff != null) {
            OreDictionary.registerOre("stoneTuff", tuff);
        }
        OreDictionary.registerOre("stoneTuffPolished", polishedTuff);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Block base = Owners.block("tuff");
        if (base == null) {
            TrickyTrialsBackport.LOGGER.warn("No tuff block is registered; tuff recipes that start from tuff are skipped.");
        }
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        if (base != null) {
            shapes(registry, base, tuffShapes);
            shaped(registry, "polished_tuff", new ItemStack(polishedTuff, 4), "##", "##", '#', base);
        }
        shaped(registry, "chiseled_tuff", new ItemStack(chiseledTuff), "#", "#", '#', tuffShapes.slab);
        shapes(registry, polishedTuff, polishedShapes);
        shaped(registry, "tuff_bricks", new ItemStack(tuffBricks, 4), "##", "##", '#', polishedTuff);
        shapes(registry, tuffBricks, brickShapes);
        shaped(registry, "chiseled_tuff_bricks", new ItemStack(chiseledTuffBricks), "#", "#", '#', brickShapes.slab);
    }

    /** Vanilla's three shape recipes: 6 to 4 stairs, 3 to 6 slabs, 6 to 6 walls. */
    private static void shapes(IForgeRegistry<IRecipe> registry, Block base, Shapes s) {
        shaped(registry, s.prefix + "_stairs", new ItemStack(s.stairs, 4), "#  ", "## ", "###", '#', base);
        shaped(registry, s.prefix + "_slab", new ItemStack(s.slab, 6), "###", '#', base);
        shaped(registry, s.prefix + "_wall", new ItemStack(s.wall, 6), "###", "###", '#', base);
    }

    private static void shaped(IForgeRegistry<IRecipe> registry, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe r = new ShapedOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "tuff"), out, recipe);
        r.setRegistryName(Content.id(name));
        registry.register(r);
    }
}
