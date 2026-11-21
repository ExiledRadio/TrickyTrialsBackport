package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.palegarden.wood.AxeStripping;
import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodKind;
import com.exiledradio.trickytrialsbackport.palegarden.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Stream 12: the pale garden of 1.21.4.
 *
 * <ul>
 *   <li>The pale oak wood set ({@link WoodSet}, vanilla's wooden sounds as 1.21.4's {@code BlockSetType.PALE_OAK}
 *       has), pale oak leaves and sapling (2x2 only, {@link PaleOakTree}), the potted sapling and the boat
 *       ({@code boat.BoatContent}).</li>
 *   <li>Pale moss block (bone meal spreads a patch), pale moss carpet (side walls), pale hanging moss.</li>
 *   <li>Open and closed eyeblossoms and their pots, switching at dusk and dawn.</li>
 *   <li>The creaking heart. The creaking itself is stream 13; the heart reaches it by name only
 *       ({@link TileCreakingHeart}).</li>
 *   <li>The pale garden biome ({@link BiomePaleGarden}, placed by {@link PaleGardenLayer}); its trees and
 *       ground cover are all in {@link PaleGardenFeatures}, for other terrain to call.</li>
 * </ul>
 *
 * <p>Map colors are 1.21.4's, by 1.12.2's names: planks and stripped wood {@code QUARTZ}, bark {@code STONE},
 * leaves and sapling {@code METAL} (1.12.2's {@link MapColor#IRON}), pale moss {@code COLOR_LIGHT_GRAY}
 * ({@link MapColor#SILVER}), the creaking heart and open eyeblossom {@code COLOR_ORANGE}
 * ({@link MapColor#ADOBE}).
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class PaleGardenContent {

    public static final WoodKind PALE_OAK = new WoodKind("pale_oak", MapColor.QUARTZ, MapColor.STONE);

    public static final SoundType MOSS = Content.soundType("moss", 1.0F, 1.0F);
    public static final SoundType MOSS_CARPET = Content.soundType("moss_carpet", 1.0F, 1.0F);
    public static final SoundType CREAKING_HEART = Content.soundType("creaking_heart", 1.0F, 1.0F);
    public static final SoundEvent HEART_IDLE = Content.sound("block.creaking_heart.idle");
    public static final SoundEvent HANGING_MOSS_IDLE = Content.sound("block.pale_hanging_moss.idle");
    public static final SoundEvent EYEBLOSSOM_IDLE = Content.sound("block.eyeblossom.idle");
    public static final SoundEvent EYEBLOSSOM_OPEN = Content.sound("block.eyeblossom.open");
    public static final SoundEvent EYEBLOSSOM_OPEN_LONG = Content.sound("block.eyeblossom.open_long");
    public static final SoundEvent EYEBLOSSOM_CLOSE = Content.sound("block.eyeblossom.close");
    public static final SoundEvent EYEBLOSSOM_CLOSE_LONG = Content.sound("block.eyeblossom.close_long");

    /** Null when the stream is off. */
    public static WoodSet set;
    public static BlockPaleOakLeaves leaves;
    public static BlockPaleOakSapling sapling;
    public static BlockPotted pottedSapling;
    public static BlockPaleMoss mossBlock;
    public static BlockPaleMossCarpet mossCarpet;
    public static BlockPaleHangingMoss hangingMoss;
    public static BlockEyeblossom openEyeblossom;
    public static BlockEyeblossom closedEyeblossom;
    public static BlockEyeblossom.Potted pottedOpenEyeblossom;
    public static BlockEyeblossom.Potted pottedClosedEyeblossom;
    public static BlockCreakingHeart creakingHeart;
    /** Null when the stream is off or the biome is switched off. */
    public static BiomePaleGarden biome;

    private PaleGardenContent() {
    }

    public static boolean enabled() {
        return ModConfig.paleGarden.enabled;
    }

    /** Init: compost chances to Village &amp; Pillage's composter. */
    public static void init() {
        if (enabled()) {
            Compost.init();
            // 26.3's pale_oak_checked, which Game Drops Backport's camps grow in their jigsaw pools: the worldgen pale oak
            // (with pale moss), where a pale oak sapling would survive. Registered at init, after the camps' preInit
            // fallback (which grows a sapling), so this one is used.
            JigsawPools.feature("minecraft:pale_oak_checked", (world, pos, random, feature) -> {
                if (sapling != null && sapling.canPlaceBlockAt(world, pos)) {
                    PaleGardenFeatures.tree(world, random, pos, PaleGardenFeatures.TreeKind.PALE_OAK);
                }
            });
        }
    }

    /** Pre-init: this mod ships these first (1.21.4), so it owns them in Backport Core's table. */
    public static void preInit() {
        if (enabled()) {
            PaleGardenLayer.register();
            BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, "pale_oak_log", "pale_oak_wood", "stripped_pale_oak_log",
                    "stripped_pale_oak_wood", "pale_oak_planks", "pale_oak_stairs", "pale_oak_slab", "pale_oak_double_slab",
                    "pale_oak_fence", "pale_oak_fence_gate", "pale_oak_door", "pale_oak_trapdoor", "pale_oak_pressure_plate",
                    "pale_oak_button", "pale_oak_sign", "pale_oak_wall_sign", "pale_oak_leaves", "pale_oak_sapling",
                    "potted_pale_oak_sapling", "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss",
                    "open_eyeblossom", "closed_eyeblossom", "potted_open_eyeblossom", "potted_closed_eyeblossom",
                    "creaking_heart");
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        set = WoodSet.tree(PALE_OAK);
        set.registerBlocks(event.getRegistry());
        leaves = Content.block(event.getRegistry(), "pale_oak_leaves", new BlockPaleOakLeaves(), "hoe");
        Content.ignoreProperties(leaves, BlockPaleOakLeaves.CHECK_DECAY, BlockPaleOakLeaves.DECAYABLE);
        sapling = Content.block(event.getRegistry(), "pale_oak_sapling", new BlockPaleOakSapling(), null);
        Content.ignoreProperties(sapling, BlockPaleOakSapling.STAGE);
        pottedSapling = Content.block(event.getRegistry(), "potted_pale_oak_sapling", new BlockPotted(), null);
        pottedSapling.setCreativeTab(null);

        mossBlock = Content.block(event.getRegistry(), "pale_moss_block", new BlockPaleMoss(), "hoe");
        mossCarpet = Content.block(event.getRegistry(), "pale_moss_carpet", new BlockPaleMossCarpet(), "hoe");
        Content.multipart(mossCarpet);
        hangingMoss = Content.block(event.getRegistry(), "pale_hanging_moss", new BlockPaleHangingMoss(), "hoe");

        openEyeblossom = Content.block(event.getRegistry(), "open_eyeblossom", new BlockEyeblossom(true), null);
        closedEyeblossom = Content.block(event.getRegistry(), "closed_eyeblossom", new BlockEyeblossom(false), null);
        pottedOpenEyeblossom = Content.block(event.getRegistry(), "potted_open_eyeblossom", new BlockEyeblossom.Potted(true), null);
        pottedClosedEyeblossom = Content.block(event.getRegistry(), "potted_closed_eyeblossom", new BlockEyeblossom.Potted(false), null);
        pottedOpenEyeblossom.setCreativeTab(null);
        pottedClosedEyeblossom.setCreativeTab(null);

        creakingHeart = Content.block(event.getRegistry(), "creaking_heart", new BlockCreakingHeart(), null);
        GameRegistry.registerTileEntity(TileCreakingHeart.class, Content.id("creaking_heart"));

        // 1.21.4: leaves 30/60 as all leaves; moss blocks, carpet and hanging moss burn as vanilla's moss
        // (Blocks' FireBlock table: moss 5/100, moss carpet 5/100, hanging moss 5/100); eyeblossoms 60/100.
        Blocks.FIRE.setFireInfo(leaves, 30, 60);
        Blocks.FIRE.setFireInfo(mossBlock, 5, 100);
        Blocks.FIRE.setFireInfo(mossCarpet, 5, 100);
        Blocks.FIRE.setFireInfo(hangingMoss, 5, 100);
        Blocks.FIRE.setFireInfo(openEyeblossom, 60, 100);
        Blocks.FIRE.setFireInfo(closedEyeblossom, 60, 100);
        AxeStripping.add(set.stripping());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        set.registerItems(event.getRegistry());
        Item leavesItem = Content.registerBlockItem(event.getRegistry(), leaves);
        Item saplingItem = Content.blockItem(event.getRegistry(), sapling, new ItemBlock(sapling) {
            @Override
            public int getItemBurnTime(ItemStack stack) {
                return 100;
            }
        });
        leaves.setSapling(saplingItem);
        pottedSapling.setPlant(saplingItem);
        Item moss = Content.registerBlockItem(event.getRegistry(), mossBlock);
        // Not fuel: 1.21's fuel table burns wool carpets only.
        Item carpet = Content.registerBlockItem(event.getRegistry(), mossCarpet);
        Item hanging = Content.registerBlockItem(event.getRegistry(), hangingMoss);
        Item open = Content.registerBlockItem(event.getRegistry(), openEyeblossom);
        Item closed = Content.registerBlockItem(event.getRegistry(), closedEyeblossom);
        pottedOpenEyeblossom.setPlant(open);
        pottedClosedEyeblossom.setPlant(closed);
        Content.registerBlockItem(event.getRegistry(), creakingHeart);

        OreDictionary.registerOre("treeLeaves", leaves);
        OreDictionary.registerOre("treeSapling", sapling);
        Compost.add(0.3F, leavesItem, saplingItem, carpet, hanging);
        Compost.add(0.65F, moss, open, closed);
    }

    // Normal priority, as the family's other wood sets: at LOW, Quark's any-planks trapdoor recipe (registered
    // earlier) matched the pale oak trapdoor's grid first.
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        set.registerRecipes(event.getRegistry());
        // 1.21.4: two pale moss blocks make three carpets; an open eyeblossom one orange dye, a closed one gray.
        shaped(event, "pale_moss_carpet", new ItemStack(mossCarpet, 3), "##", '#', mossBlock);
        shapeless(event, "orange_dye_from_open_eyeblossom", new ItemStack(Items.DYE, 1, EnumDyeColor.ORANGE.getDyeDamage()), openEyeblossom);
        shapeless(event, "gray_dye_from_closed_eyeblossom", new ItemStack(Items.DYE, 1, EnumDyeColor.GRAY.getDyeDamage()), closedEyeblossom);
        // The creaking heart: a block of resin between two pale oak logs. Resin is stream 14's, found by name.
        Item resinBlock = Owners.ours("resin_block");
        if (resinBlock != null) {
            shaped(event, "creaking_heart", new ItemStack(creakingHeart), " L ", " R ", " L ", 'L', set.log, 'R', resinBlock);
        }
    }

    private static void shaped(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        event.getRegistry().register(new ShapedOreRecipe(id, out, recipe).setRegistryName(id));
    }

    private static void shapeless(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        event.getRegistry().register(new ShapelessOreRecipe(id, out, recipe).setRegistryName(id));
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled() || !ModConfig.paleGarden.generateBiome) {
            return;
        }
        biome = new BiomePaleGarden();
        biome.setRegistryName(Content.id("pale_garden"));
        event.getRegistry().register(biome);
        BiomePaleGarden.addTypes(biome);
    }
}
