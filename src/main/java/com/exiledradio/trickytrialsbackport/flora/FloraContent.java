package com.exiledradio.trickytrialsbackport.flora;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stream 15: 1.21.5's spring plants.
 *
 * <ul>
 *   <li>Bush and firefly bush ({@link BlockBushPlant}), short and tall dry grass ({@link BlockDryGrass}), the cactus
 *       flower ({@link BlockCactusFlower}), wildflowers and leaf litter ({@link BlockSegmented}, added to in place by
 *       {@link ItemSegmented}).</li>
 *   <li>Where they grow: {@link FloraFeatures} (every placed feature, for any terrain to call) and {@link FloraGen}
 *       (1.12.2's decoration: new chunks only), fallen trees ({@link FallenTrees}).</li>
 *   <li>1.21.5's changes to vanilla blocks, by bytecode patches ({@code asm}, {@link FloraHooks}): cacti grow
 *       flowers; dead bushes stand on farmland and every {@code #dry_vegetation_may_place_on} block; sand and dead
 *       bushes make their desert sounds.</li>
 * </ul>
 *
 * <p>Map colors are 1.21.5's by 1.12.2's names: {@code PLANT} is {@code FOLIAGE}, {@code COLOR_YELLOW},
 * {@code COLOR_PINK} and {@code COLOR_BROWN} are {@code YELLOW}, {@code PINK} and {@code BROWN}.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class FloraContent {

    public static final SoundEvent EMPTY = Content.sound("intentionally_empty");
    public static final SoundType PINK_PETALS = Content.soundType("pink_petals", 1.0F, 1.0F);
    public static final SoundType LEAF_LITTER = Content.soundType("leaf_litter", 1.0F, 1.0F);
    public static final SoundType CACTUS_FLOWER = new SoundType(1.0F, 1.0F, Content.sound("block.cactus_flower.break"), EMPTY,
            Content.sound("block.cactus_flower.place"), EMPTY, EMPTY);
    public static final SoundType SWEET_BERRY_BUSH = new SoundType(1.0F, 1.0F, Content.sound("block.sweet_berry_bush.break"),
            SoundEvents.BLOCK_GRASS_STEP, Content.sound("block.sweet_berry_bush.place"), SoundEvents.BLOCK_GRASS_HIT,
            SoundEvents.BLOCK_GRASS_FALL);
    public static final SoundEvent FIREFLY_BUSH_IDLE = Content.sound("block.firefly_bush.idle");
    public static final SoundEvent DEAD_BUSH_IDLE = Content.sound("block.deadbush.idle");
    public static final SoundEvent DRY_GRASS = Content.sound("block.dry_grass.ambient");
    public static final SoundEvent SAND_IDLE = Content.sound("block.sand.idle");

    /** Null when the stream is off. */
    public static BlockBushPlant bush;
    public static BlockBushPlant fireflyBush;
    public static BlockCactusFlower cactusFlower;
    public static BlockDryGrass shortDryGrass;
    public static BlockDryGrass tallDryGrass;
    public static BlockSegmented leafLitter;
    public static BlockSegmented wildflowers;

    private static final Map<Item, Float> COMPOST = new LinkedHashMap<Item, Float>();

    private FloraContent() {
    }

    public static boolean enabled() {
        return ModConfig.flora.enabled;
    }

    /** Pre-init: this mod ships these first (1.21.5), so it owns them in Backport Core's table. */
    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, "bush", "firefly_bush", "cactus_flower", "short_dry_grass",
                    "tall_dry_grass", "leaf_litter", "wildflowers");
        }
    }

    /**
     * Post-init: leaf litter smelts from any leaves ({@code #leaves}: every {@code treeLeaves} ore, so other mods'
     * leaves too, registered by now), and compost chances go to Village &amp; Pillage's composter.
     */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        for (ItemStack leaves : OreDictionary.getOres("treeLeaves")) {
            if (FurnaceRecipes.instance().getSmeltingResult(leaves).isEmpty()) {
                FurnaceRecipes.instance().addSmeltingRecipe(leaves, new ItemStack(leafLitter), 0.1F);
            }
        }
        compost();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        bush = Content.block(event.getRegistry(), "bush", new BlockBushPlant(false), null);
        fireflyBush = Content.block(event.getRegistry(), "firefly_bush", new BlockBushPlant(true), null);
        cactusFlower = Content.block(event.getRegistry(), "cactus_flower", new BlockCactusFlower(), null);
        shortDryGrass = Content.block(event.getRegistry(), "short_dry_grass", new BlockDryGrass(false), null);
        tallDryGrass = Content.block(event.getRegistry(), "tall_dry_grass", new BlockDryGrass(true), null);
        leafLitter = Content.block(event.getRegistry(), "leaf_litter", new BlockSegmented(false), null);
        wildflowers = Content.block(event.getRegistry(), "wildflowers", new BlockSegmented(true), null);
        Content.multipart(leafLitter);
        Content.multipart(wildflowers);
        // 1.21.5's FireBlock: every one of them 60/100.
        for (Block b : new Block[] {bush, fireflyBush, cactusFlower, shortDryGrass, tallDryGrass, leafLitter, wildflowers}) {
            Blocks.FIRE.setFireInfo(b, 60, 100);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        Item bushItem = Content.registerBlockItem(event.getRegistry(), bush);
        Item firefly = Content.registerBlockItem(event.getRegistry(), fireflyBush);
        Item flower = Content.registerBlockItem(event.getRegistry(), cactusFlower);
        // 1.21.5's FuelValues: dry grass and leaf litter burn half a plank's time.
        Item shortGrass = Content.blockItem(event.getRegistry(), shortDryGrass, fuel(shortDryGrass));
        Item tallGrass = Content.blockItem(event.getRegistry(), tallDryGrass, fuel(tallDryGrass));
        ItemSegmented litter = Content.blockItem(event.getRegistry(), leafLitter, new ItemSegmented(leafLitter) {
            @Override
            public int getItemBurnTime(ItemStack stack) {
                return 100;
            }
        });
        ItemSegmented flowers = Content.blockItem(event.getRegistry(), wildflowers, new ItemSegmented(wildflowers));
        leafLitter.setItem(litter);
        wildflowers.setItem(flowers);
        // 1.21.5's ComposterBlock: every one of them 30%.
        for (Item i : new Item[] {flowers, litter, firefly, bushItem, flower, shortGrass, tallGrass}) {
            COMPOST.put(i, 0.3F);
        }
    }

    private static ItemBlock fuel(Block block) {
        return new ItemBlock(block) {
            @Override
            public int getItemBurnTime(ItemStack stack) {
                return 100;
            }
        };
    }

    // Normal priority, as the family's other recipes.
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        shapeless(event, "pink_dye_from_cactus_flower", new ItemStack(Items.DYE, 1, EnumDyeColor.PINK.getDyeDamage()), cactusFlower);
        shapeless(event, "yellow_dye_from_wildflowers", new ItemStack(Items.DYE, 1, EnumDyeColor.YELLOW.getDyeDamage()), wildflowers);
    }

    private static void shapeless(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        event.getRegistry().register(new ShapelessOreRecipe(id, out, recipe).setRegistryName(id));
    }

    /**
     * The composter is Village &amp; Pillage's 1.14 block (Rule 2): with it installed, every plant here composts at
     * 30%; without it, nothing composts.
     */
    private static void compost() {
        if (!Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) || COMPOST.isEmpty()) {
            return;
        }
        try {
            Method register = Class.forName("com.exiledradio.villagerbackport.block.Compostables")
                    .getMethod("register", float.class, Item.class);
            for (Map.Entry<Item, Float> e : COMPOST.entrySet()) {
                register.invoke(null, e.getValue(), e.getKey());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("Could not add the spring plants to the composter; they stay uncompostable.", e);
        }
    }
}
