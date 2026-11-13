package com.exiledradio.trickytrialsbackport.decor;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.BannerPattern;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 9: the 1.21 and later paintings, banner patterns, music discs, pottery sherds and armor trim
 * templates.
 *
 * <ul>
 *   <li>21 paintings ({@link Paintings}) on our own painting entity ({@link EntityModernPainting}, id 25), placed
 *       by vanilla's painting item as 1.21 places them ({@link DecorEvents#placePainting}).</li>
 *   <li>Banner patterns Flow and Guster (1.21) added to 1.12.2's pattern enum; Field Masoned and Bordure Indented
 *       (1.21.2) are the items 1.21.2 now needs for the bricks and curly border patterns. With Village &amp;
 *       Pillage the four are its loom's pattern items ({@link LoomPatterns}); without it they apply in the
 *       crafting grid and are not used up.</li>
 *   <li>Discs Creator, Creator (Music Box), Precipice (1.21), Tears (1.21.6, a ghast killed by a fireball a
 *       player sent back) and Lava Chicken (1.21.7, a baby zombie riding a chicken).</li>
 *   <li>Sherds Flow, Guster and Scrape, in the {@code potterySherd} ore name Trails &amp; Tales' decorated pot
 *       reads, with their pot faces; templates Flow and Bolt, which Trails &amp; Tales' smithing reads
 *       ({@code docs/CORE_REQUESTS.md} Decor-2).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class DecorContent {

    public static final int PAINTING_ID = 25;

    /** The ore name Trails &amp; Tales' decorated pot recipe takes sherds by. */
    public static final String SHERD_ORE = "potterySherd";
    /** The ore name our trim templates carry, for Trails &amp; Tales' smithing table. */
    public static final String TRIM_TEMPLATE_ORE = "armorTrimTemplate";

    public static final String[] SHERDS = {"flow", "guster", "scrape"};
    public static final String[] TRIMS = {"flow", "bolt"};

    /** Disc name, comparator signal, rarity: 1.21.11's jukebox songs and items. */
    static final Object[][] DISCS = {
            {"creator", 12, EnumRarity.RARE},
            {"creator_music_box", 11, EnumRarity.UNCOMMON},
            {"precipice", 13, EnumRarity.UNCOMMON},
            {"tears", 10, EnumRarity.UNCOMMON},
            {"lava_chicken", 9, EnumRarity.RARE},
    };

    public static final Map<String, ItemDecorDisc> DISC_ITEMS = new LinkedHashMap<String, ItemDecorDisc>();
    public static final Map<String, Item> SHERD_ITEMS = new LinkedHashMap<String, Item>();
    public static final Map<String, ItemTrimTemplate> TRIM_ITEMS = new LinkedHashMap<String, ItemTrimTemplate>();

    @Nullable
    public static BannerPattern flow;
    @Nullable
    public static BannerPattern guster;

    public static Item flowBannerPattern;
    public static Item gusterBannerPattern;
    public static Item fieldMasonedBannerPattern;
    public static Item bordureIndentedBannerPattern;

    private DecorContent() {
    }

    public static boolean enabled() {
        return ModConfig.decor.enabled;
    }

    /** The first registry event: the two new patterns join the enum before anything reads it. */
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        flow = Patterns.add("FLOW", "flow", "flw");
        guster = Patterns.add("GUSTER", "guster", "gus");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (Object[] disc : DISCS) {
            String name = (String) disc[0];
            ItemDecorDisc item = new ItemDecorDisc(name, Content.sound("music_disc." + name), (Integer) disc[1], (EnumRarity) disc[2]);
            DISC_ITEMS.put(name, Content.item(event.getRegistry(), "music_disc_" + name, item));
        }
        for (final String sherd : SHERDS) {
            Item item = Content.item(event.getRegistry(), sherd + "_pottery_sherd", new Item() {
                @Override
                public EnumRarity getRarity(ItemStack stack) {
                    return EnumRarity.UNCOMMON;
                }
            });
            OreDictionary.registerOre(SHERD_ORE, item);
            SHERD_ITEMS.put(sherd, item);
        }
        for (String trim : TRIMS) {
            ItemTrimTemplate item = Content.item(event.getRegistry(), trim + "_armor_trim_smithing_template", new ItemTrimTemplate(trim));
            OreDictionary.registerOre(TRIM_TEMPLATE_ORE, item);
            TRIM_ITEMS.put(trim, item);
        }
        flowBannerPattern = Content.item(event.getRegistry(), "flow_banner_pattern",
                LoomPatterns.item(flow, "Flow", EnumRarity.RARE));
        gusterBannerPattern = Content.item(event.getRegistry(), "guster_banner_pattern",
                LoomPatterns.item(guster, "Guster", EnumRarity.RARE));
        fieldMasonedBannerPattern = Content.item(event.getRegistry(), "field_masoned_banner_pattern",
                LoomPatterns.item(BannerPattern.BRICKS, null, EnumRarity.COMMON));
        bordureIndentedBannerPattern = Content.item(event.getRegistry(), "bordure_indented_banner_pattern",
                LoomPatterns.item(BannerPattern.CURLY_BORDER, null, EnumRarity.COMMON));
        LoomPatterns.gridItems();
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        // Vanilla's painting: tracked to 160 blocks, never sent position updates (it does not move).
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityModernPainting.class)
                .id(Content.id("painting"), PAINTING_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".painting")
                .tracker(160, Integer.MAX_VALUE, false)
                .build());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        // 1.21.2's shapeless pattern recipes.
        shapeless(event, "field_masoned_banner_pattern", new ItemStack(fieldMasonedBannerPattern), Items.PAPER, Blocks.BRICK_BLOCK);
        shapeless(event, "bordure_indented_banner_pattern", new ItemStack(bordureIndentedBannerPattern), Items.PAPER, Blocks.VINE);

        // Template duplication: 7 diamonds, the template and its block, two templates out.
        Item rod = Owners.ours("breeze_rod");
        if (rod != null) {
            duplicate(event, "flow", Ingredient.fromItem(rod));
        } else {
            TrickyTrialsBackport.LOGGER.info("The breeze is switched off: the flow template cannot be duplicated.");
        }
        List<ItemStack> copper = new ArrayList<ItemStack>();
        for (String name : new String[] {"copper_block", "waxed_copper_block"}) {
            Block block = Owners.block(name);
            if (block != null && block != Blocks.AIR) {
                copper.add(new ItemStack(block));
            }
        }
        if (!copper.isEmpty()) {
            duplicate(event, "bolt", Ingredient.fromStacks(copper.toArray(new ItemStack[0])));
        } else {
            TrickyTrialsBackport.LOGGER.info("No copper block is registered: the bolt template cannot be duplicated.");
        }
    }

    private static void shapeless(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... in) {
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(null, out, in);
        recipe.setRegistryName(Content.id(name));
        event.getRegistry().register(recipe);
    }

    private static void duplicate(RegistryEvent.Register<IRecipe> event, String trim, Ingredient block) {
        Item template = TRIM_ITEMS.get(trim);
        ShapedOreRecipe recipe = new ShapedOreRecipe(null, new ItemStack(template, 2), "#S#", "#C#", "###",
                '#', Items.DIAMOND, 'S', template, 'C', block);
        recipe.setRegistryName(Content.id(trim + "_armor_trim_smithing_template"));
        event.getRegistry().register(recipe);
    }

    /** Post-init: the new patterns move behind their items in Village &amp; Pillage's loom. */
    public static void postInit() {
        if (enabled()) {
            LoomPatterns.arrangeLoom();
        }
    }
}
