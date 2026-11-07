package com.exiledradio.trickytrialsbackport.copper;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.WindBurst;
import com.exiledradio.trickytrialsbackport.common.building.BlockShapedSlab;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 2: 1.21's copper blocks, and copies of Caves &amp; Cliffs' 1.17 copper when it is absent.
 *
 * <h2>1.21's blocks</h2>
 * Chiseled copper, copper grates, copper bulbs, copper doors and copper trapdoors, each in four stages and
 * waxed: 40 blocks, aging, waxing and scraping as all copper does.
 *
 * <h2>The copies (Rule 1)</h2>
 * Caves &amp; Cliffs owns copper. Without it, this registers its copper ore, raw copper and its block, the
 * copper ingot and its 32 copper blocks plus 8 double slabs, under the same names, states and metadata
 * (checked against its published table, {@code caves-and-cliffs-copper-states.md}), with its ore
 * generation (stone only: deepslate is its) and smelting. With it, none of these exist here and every
 * recipe takes its blocks. All or nothing: the copies are one set.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperContent {

    /** Caves &amp; Cliffs' four shapes; the full block's name comes from {@link CopperStage#blockName()}. */
    public static final String[] OWNED_SHAPES = {"block", "cut_copper", "cut_copper_stairs", "cut_copper_slab"};
    /** 1.21's five. */
    public static final String[] SHAPES = {"chiseled_copper", "copper_grate", "copper_bulb", "copper_door", "copper_trapdoor"};

    /** Every block this stream registers, by name. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<String, Block>();
    private static final Map<String, Block> DOUBLES = new LinkedHashMap<String, Block>();
    private static final List<Block> ALL = new ArrayList<Block>();

    /** True when the copies are registered (Caves &amp; Cliffs absent). */
    public static boolean copies;
    @Nullable public static Item copperIngot;
    @Nullable public static Item rawCopper;

    private CopperContent() {
    }

    public static boolean enabled() {
        return ModConfig.copper.enabled;
    }

    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Caves &amp; Cliffs' name of one of its shapes at a stage. */
    public static String ownedName(String shape, CopperStage stage, boolean waxed) {
        String base = "block".equals(shape) ? stage.blockName() : stage.prefix + shape;
        return waxed ? "waxed_" + base : base;
    }

    /** A 1.21 shape's name at a stage: {@code waxed_weathered_copper_bulb}. */
    public static String name(String shape, CopperStage stage, boolean waxed) {
        return (waxed ? "waxed_" : "") + stage.name(shape);
    }

    private static String doubleOf(String slab) {
        return slab.substring(0, slab.length() - "_slab".length()) + "_double_slab";
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType copper = Content.soundType("copper", 1.0F, 1.0F);
        SoundType bulb = Content.soundType("copper_bulb", 1.0F, 1.0F);
        SoundType grate = Content.soundType("copper_grate", 1.0F, 1.0F);
        for (String s : new String[] {"item.honeycomb.wax_on", "item.axe.wax_off", "item.axe.scrape", "block.copper_bulb.turn_on",
                "block.copper_bulb.turn_off", "block.copper_door.open", "block.copper_door.close", "block.copper_trapdoor.open",
                "block.copper_trapdoor.close"}) {
            Content.sound(s);
        }

        copies = Owners.copy("copper_block");
        if (copies) {
            add(r, "copper_ore", new BlockCopperOre(() -> rawCopper));
            Block raw = new Block(Material.ROCK, MapColor.ADOBE) {
                {
                    setSoundType(SoundType.STONE);
                }
            };
            raw.setHardness(5.0F);
            raw.setResistance(10.0F);
            raw.setHarvestLevel("pickaxe", 1);
            add(r, "raw_copper_block", raw);
            for (boolean waxed : new boolean[] {false, true}) {
                for (CopperStage stage : CopperStage.values()) {
                    boolean ages = !waxed && stage != CopperStage.OXIDIZED;
                    add(r, ownedName("block", stage, waxed), new CopperBlocks.Full(stage, copper, ages));
                    Block cut = add(r, ownedName("cut_copper", stage, waxed), new CopperBlocks.Full(stage, copper, ages));
                    add(r, ownedName("cut_copper_stairs", stage, waxed), new CopperBlocks.Stairs(cut.getDefaultState(), ages));
                    String slab = ownedName("cut_copper_slab", stage, waxed);
                    CopperBlocks.Slab half = add(r, slab, new CopperBlocks.Slab(cut, ages));
                    Block dbl = add(r, doubleOf(slab), new CopperBlocks.DoubleSlab(cut, half, ages));
                    dbl.setCreativeTab(null);
                    DOUBLES.put(slab, dbl);
                    Content.ignoreProperties(half, BlockShapedSlab.VARIANT);
                    Content.ignoreProperties(dbl, BlockShapedSlab.VARIANT);
                }
            }
            for (String shape : OWNED_SHAPES) {
                chain(shape, true);
                if ("cut_copper_slab".equals(shape)) {
                    Block[] chain = new Block[4];
                    for (CopperStage stage : CopperStage.values()) {
                        chain[stage.ordinal()] = DOUBLES.get(ownedName(shape, stage, false));
                        Weathering.waxed(chain[stage.ordinal()], DOUBLES.get(ownedName(shape, stage, true)));
                    }
                    Weathering.chain(chain);
                }
            }
        }

        for (boolean waxed : new boolean[] {false, true}) {
            for (CopperStage stage : CopperStage.values()) {
                boolean ages = !waxed && stage != CopperStage.OXIDIZED;
                add(r, name("chiseled_copper", stage, waxed), new CopperBlocks.Full(stage, copper, ages));
                add(r, name("copper_grate", stage, waxed), new CopperBlocks.Grate(stage, grate, ages));
                add(r, name("copper_bulb", stage, waxed), new CopperBlocks.Bulb(stage, bulb, ages));
                CopperBlocks.Door door = add(r, name("copper_door", stage, waxed), new CopperBlocks.Door(stage, copper, ages));
                door.setCreativeTab(null);
                Content.ignoreProperties(door, BlockDoor.POWERED);
                WindBurst.openable(door);
                WindBurst.openable(add(r, name("copper_trapdoor", stage, waxed), new CopperBlocks.Trapdoor(stage, copper, ages)));
            }
        }
        for (String shape : SHAPES) {
            chain(shape, false);
        }
        List<String> ours = new ArrayList<String>();
        for (String shape : SHAPES) {
            for (CopperStage stage : CopperStage.values()) {
                ours.add(name(shape, stage, false));
                ours.add(name(shape, stage, true));
            }
        }
        BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, ours.toArray(new String[0]));
        TrickyTrialsBackport.LOGGER.info("Registered {} copper blocks ({}).", ALL.size(), copies ? "with copies of the 1.17 set" : "1.21's only");
    }

    /** One shape's four stages and their waxed forms. */
    private static void chain(String shape, boolean owned) {
        Block[] chain = new Block[4];
        for (CopperStage stage : CopperStage.values()) {
            String plain = owned ? ownedName(shape, stage, false) : name(shape, stage, false);
            String waxed = owned ? ownedName(shape, stage, true) : name(shape, stage, true);
            chain[stage.ordinal()] = BLOCKS.get(plain);
            Weathering.waxed(BLOCKS.get(plain), BLOCKS.get(waxed));
        }
        Weathering.chain(chain);
    }

    private static <B extends Block> B add(IForgeRegistry<Block> r, String name, B block) {
        Content.block(r, name, block, "pickaxe");
        if (block.getHarvestLevel(block.getDefaultState()) < 1 && !(block instanceof CopperBlocks.Door)) {
            block.setHarvestLevel("pickaxe", 1);
        }
        BLOCKS.put(name, block);
        ALL.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        for (Map.Entry<String, Block> e : BLOCKS.entrySet()) {
            Block b = e.getValue();
            if (b instanceof CopperBlocks.DoubleSlab) {
                continue;
            }
            if (b instanceof CopperBlocks.Slab) {
                Content.blockItem(r, b, new ItemSlab(b, (BlockShapedSlab) b, (BlockShapedSlab) DOUBLES.get(e.getKey())));
            } else if (b instanceof CopperBlocks.Door) {
                ItemDoor item = new ItemDoor(b);
                item.setTranslationKey(TrickyTrialsBackport.MOD_ID + "." + e.getKey());
                item.setCreativeTab(com.exiledradio.trickytrialsbackport.common.ModCreativeTab.TAB);
                Content.blockItem(r, b, item);
                ((CopperBlocks.Door) b).item = item;
            } else {
                Content.registerBlockItem(r, b);
            }
        }
        if (copies) {
            copperIngot = Content.item(r, "copper_ingot", new Item());
            rawCopper = Content.item(r, "raw_copper", new Item());
            OreDictionary.registerOre("oreCopper", BLOCKS.get("copper_ore"));
            OreDictionary.registerOre("ingotCopper", copperIngot);
            OreDictionary.registerOre("rawCopper", rawCopper);
            OreDictionary.registerOre("blockCopper", BLOCKS.get("copper_block"));
            OreDictionary.registerOre("blockRawCopper", BLOCKS.get("raw_copper_block"));
        }
        // Every mod's blocks are registered now: enter Caves & Cliffs' copper in the aging table.
        Weathering.addOwnersBlocks();
    }

    /** The owner's block or our copy, by Caves &amp; Cliffs' name. */
    @Nullable
    static Block owned(String name) {
        return Owners.block(name);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Weathering.registerWaxOres();
        IForgeRegistry<IRecipe> r = event.getRegistry();
        if (copies) {
            copyRecipes(r);
        }
        Item ingot = Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
        for (boolean waxed : new boolean[] {false, true}) {
            for (CopperStage stage : CopperStage.values()) {
                Block full = owned(ownedName("block", stage, waxed));
                Block slab = owned(ownedName("cut_copper_slab", stage, waxed));
                if (full != null) {
                    shaped(r, name("copper_bulb", stage, waxed), new ItemStack(BLOCKS.get(name("copper_bulb", stage, waxed)), 4),
                            " C ", "CBC", " R ", 'C', full, 'B', Items.BLAZE_ROD, 'R', Items.REDSTONE);
                    shaped(r, name("copper_grate", stage, waxed), new ItemStack(BLOCKS.get(name("copper_grate", stage, waxed)), 4),
                            " M ", "M M", " M ", 'M', full);
                }
                if (slab != null) {
                    shaped(r, name("chiseled_copper", stage, waxed), new ItemStack(BLOCKS.get(name("chiseled_copper", stage, waxed))),
                            "#", "#", '#', slab);
                }
            }
        }
        if (ingot != null) {
            shaped(r, "copper_door", new ItemStack(((CopperBlocks.Door) BLOCKS.get("copper_door")).item, 3), "##", "##", "##", '#', ingot);
            shaped(r, "copper_trapdoor", new ItemStack(BLOCKS.get("copper_trapdoor")), "##", "##", '#', ingot);
        }
        // Waxing in the crafting grid: a 1.21 copper block and honeycomb.
        for (String shape : SHAPES) {
            for (CopperStage stage : CopperStage.values()) {
                Block plain = BLOCKS.get(name(shape, stage, false));
                Block waxed = BLOCKS.get(name(shape, stage, true));
                ItemStack in = plain instanceof CopperBlocks.Door ? new ItemStack(((CopperBlocks.Door) plain).item) : new ItemStack(plain);
                ItemStack out = waxed instanceof CopperBlocks.Door ? new ItemStack(((CopperBlocks.Door) waxed).item) : new ItemStack(waxed);
                shapeless(r, name(shape, stage, true) + "_from_honeycomb", out, in, "honeycomb");
            }
        }
    }

    /** Caves &amp; Cliffs' recipes for its copper, for our copies. */
    private static void copyRecipes(IForgeRegistry<IRecipe> r) {
        ItemStack ingot = new ItemStack(copperIngot);
        shaped(r, "copper_block", new ItemStack(BLOCKS.get("copper_block")), "XXX", "XXX", "XXX", 'X', ingot);
        shaped(r, "copper_ingot", new ItemStack(copperIngot, 9), "X", 'X', BLOCKS.get("copper_block"));
        shaped(r, "copper_ingot_from_waxed_copper_block", new ItemStack(copperIngot, 9), "X", 'X', BLOCKS.get("waxed_copper_block"));
        shaped(r, "raw_copper_block", new ItemStack(BLOCKS.get("raw_copper_block")), "XXX", "XXX", "XXX", 'X', rawCopper);
        shaped(r, "raw_copper", new ItemStack(rawCopper, 9), "X", 'X', BLOCKS.get("raw_copper_block"));
        for (boolean waxed : new boolean[] {false, true}) {
            for (CopperStage stage : CopperStage.values()) {
                Block full = BLOCKS.get(ownedName("block", stage, waxed));
                Block cut = BLOCKS.get(ownedName("cut_copper", stage, waxed));
                shaped(r, ownedName("cut_copper", stage, waxed), new ItemStack(cut, 4), "XX", "XX", 'X', full);
                shaped(r, ownedName("cut_copper_stairs", stage, waxed), new ItemStack(BLOCKS.get(ownedName("cut_copper_stairs", stage, waxed)), 4),
                        "X  ", "XX ", "XXX", 'X', cut);
                shaped(r, ownedName("cut_copper_slab", stage, waxed), new ItemStack(BLOCKS.get(ownedName("cut_copper_slab", stage, waxed)), 6),
                        "XXX", 'X', cut);
            }
        }
        for (String shape : OWNED_SHAPES) {
            for (CopperStage stage : CopperStage.values()) {
                String waxed = ownedName(shape, stage, true);
                shapeless(r, waxed + "_from_honeycomb", new ItemStack(BLOCKS.get(waxed)), new ItemStack(BLOCKS.get(ownedName(shape, stage, false))),
                        "honeycomb");
            }
        }
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe sr = new ShapedOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, name), out, recipe);
        sr.setRegistryName(Content.id(name));
        r.register(sr);
    }

    private static void shapeless(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapelessOreRecipe sr = new ShapelessOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "waxed_copper"), out, recipe);
        sr.setRegistryName(Content.id(name));
        r.register(sr);
    }

    /** Smelting and the copies' ore generation, from the mod's init. */
    public static void init() {
        if (!enabled() || !copies) {
            return;
        }
        FurnaceRecipes f = FurnaceRecipes.instance();
        f.addSmeltingRecipe(new ItemStack(BLOCKS.get("copper_ore")), new ItemStack(copperIngot), 0.7F);
        f.addSmelting(rawCopper, new ItemStack(copperIngot), 0.7F);
        GameRegistry.registerWorldGenerator(new CopperOreGenerator(), 0);
    }
}
