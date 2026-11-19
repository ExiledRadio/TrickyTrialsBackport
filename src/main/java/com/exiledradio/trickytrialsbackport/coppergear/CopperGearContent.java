package com.exiledradio.trickytrialsbackport.coppergear;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 20: 1.21.9's copper tools and items.
 *
 * <ul>
 *   <li><b>Copper nugget</b> (9 from an ingot and back).</li>
 *   <li><b>Copper tools</b>: sword, pickaxe, axe, shovel, hoe of 1.21.9's {@code ToolMaterial.COPPER}: 190 uses,
 *       speed 5, +1 damage (stone's damage and attack speeds), enchantability 13, stone's mining level.</li>
 *   <li><b>Copper armor</b>: 1.21.9's {@code ArmorMaterials.COPPER}: durability 11 (121/176/165/143), defense
 *       2/4/3/1, no toughness, enchantability 8, {@code item.armor.equip_copper}.</li>
 *   <li><b>Copper horse armor</b>: protection 4; found, not crafted (chests that hold iron horse armor).</li>
 *   <li><b>Copper torch</b> (light 14, green flame), <b>copper lantern</b> (light 15), <b>copper bars</b> and
 *       <b>copper chain</b>; bars, chains and lanterns in four oxidation stages and waxed.</li>
 *   <li>Every copper tool, armor piece and the horse armor smelt to one nugget (0.1 experience).</li>
 *   <li>Mobs wearing copper: {@link CopperGearHooks}.</li>
 * </ul>
 *
 * <p>The copper ingot is Caves &amp; Cliffs' (Rule 1): its item when it is installed, else the copper stream's copy
 * ({@code trickytrialsbackport:copper_ingot}). With neither (Caves &amp; Cliffs absent and the copper stream switched
 * off) the items exist but nothing made from ingots can be crafted; the log says so.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperGearContent {

    /** The three shapes that oxidize, by their unaffected name. */
    public static final String[] SHAPES = {"copper_bars", "copper_chain", "copper_lantern"};
    public static final String[] TOOLS = {"copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe"};
    public static final String[] ARMOR = {"copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots"};

    /** Every block of this stream, by name. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<String, Block>();
    /** Every item of this stream (block items included), by name. */
    public static final Map<String, Item> ITEMS = new LinkedHashMap<String, Item>();

    public static Item.ToolMaterial toolMaterial;
    public static ItemArmor.ArmorMaterial armorMaterial;
    public static HorseArmorType horseArmorType;
    public static Item nugget;
    public static Item horseArmor;
    public static Block torch;

    private CopperGearContent() {
    }

    public static boolean enabled() {
        return ModConfig.copperGear.enabled;
    }

    /** A shape's name at a stage, waxed or not: {@code waxed_weathered_copper_chain}. */
    public static String name(String shape, int stage, boolean waxed) {
        return (waxed ? "waxed_" : "") + Owners.OXIDATION[stage] + shape;
    }

    /** The copper ingot: Caves &amp; Cliffs' if installed, else the copper stream's copy; null if neither. */
    @Nullable
    public static Item ingot() {
        return Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType copper = Content.soundType("copper", 1.0F, 1.0F);
        SoundType chain = Content.soundType("chain", 1.0F, 1.0F);
        SoundType lantern = Content.soundType("lantern", 1.0F, 1.0F);
        Content.sound("item.armor.equip_copper");

        torch = add(r, "copper_torch", new CopperGearBlocks.Torch(), null);
        for (boolean waxed : new boolean[] {false, true}) {
            for (int stage = 0; stage < 4; stage++) {
                boolean ages = !waxed && stage < 3;
                add(r, name("copper_bars", stage, waxed), new CopperGearBlocks.Bars(copper, ages), "pickaxe");
                Content.multipartFile(BLOCKS.get(name("copper_bars", stage, waxed)));
                add(r, name("copper_chain", stage, waxed), new CopperGearBlocks.Chain(chain, ages), "pickaxe");
                add(r, name("copper_lantern", stage, waxed), new CopperGearBlocks.Lantern(lantern, ages), "pickaxe");
            }
        }
        for (String shape : SHAPES) {
            Block[] chainOf = new Block[4];
            for (int stage = 0; stage < 4; stage++) {
                chainOf[stage] = BLOCKS.get(name(shape, stage, false));
                Oxidation.waxed(chainOf[stage], BLOCKS.get(name(shape, stage, true)));
            }
            Oxidation.chain(chainOf);
        }
        BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, BLOCKS.keySet().toArray(new String[0]));
    }

    private static <B extends Block> B add(IForgeRegistry<Block> r, String name, B block, @Nullable String tool) {
        Content.block(r, name, block, tool);
        BLOCKS.put(name, block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        // ToolMaterial.COPPER (1.21.9): incorrect_for_copper_tool is stone's, 190 uses, speed 5, +1 damage, 13.
        toolMaterial = EnumHelper.addToolMaterial("TRICKYTRIALSBACKPORT_COPPER", 1, 190, 5.0F, 1.0F, 13);
        // ArmorMaterials.COPPER: durability 11, defense boots 1, leggings 3, chestplate 4, helmet 2, enchantability 8.
        armorMaterial = EnumHelper.addArmorMaterial("TRICKYTRIALSBACKPORT_COPPER", TrickyTrialsBackport.MOD_ID + ":copper", 11,
                new int[] {1, 3, 4, 2}, 8, Content.sound("item.armor.equip_copper"), 0.0F);
        horseArmorType = EnumHelper.addHorseArmor("TRICKYTRIALSBACKPORT_COPPER",
                TrickyTrialsBackport.MOD_ID + ":textures/entity/horse/armor/horse_armor_copper.png", 4);

        for (Block b : BLOCKS.values()) {
            ITEMS.put(b.getRegistryName().getPath(), Content.registerBlockItem(r, b));
        }
        nugget = item(r, "copper_nugget", new Item());
        item(r, "copper_sword", new ItemSword(toolMaterial));
        item(r, "copper_pickaxe", new ItemPickaxe(toolMaterial) {
        });
        // ItemAxe's own table has no row for a new material: 1.21.9's 6 + 3 = 9 damage, 0.8 speed (as stone's).
        item(r, "copper_axe", new ItemAxe(toolMaterial, 8.0F, -3.2F) {
        });
        item(r, "copper_shovel", new ItemSpade(toolMaterial));
        item(r, "copper_hoe", new ItemHoe(toolMaterial));
        EntityEquipmentSlot[] slots = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
        for (int i = 0; i < ARMOR.length; i++) {
            item(r, ARMOR[i], new ItemArmor(armorMaterial, 0, slots[i]));
        }
        horseArmor = item(r, "copper_horse_armor", new ItemCopperHorseArmor());
        OreDictionary.registerOre("nuggetCopper", nugget);
        TrickyTrialsBackport.LOGGER.info("Registered {} copper gear blocks and {} items.", BLOCKS.size(), ITEMS.size());
    }

    private static <I extends Item> I item(IForgeRegistry<Item> r, String name, I item) {
        Content.item(r, name, item);
        ITEMS.put(name, item);
        return item;
    }

    /** Copper horse armor: protection 4, one to a stack. */
    public static final class ItemCopperHorseArmor extends Item {
        public ItemCopperHorseArmor() {
            setMaxStackSize(1);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC);
        }

        @Override
        public HorseArmorType getHorseArmorType(ItemStack stack) {
            return horseArmorType;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Recipes

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> r = event.getRegistry();
        Item ingot = ingot();
        if (ingot != null) {
            ItemStack repair = new ItemStack(ingot);
            toolMaterial.setRepairItem(repair);
            armorMaterial.setRepairItem(repair);
            shapeless(r, "copper_nugget", new ItemStack(nugget, 9), ingot);
            shaped(r, "copper_ingot_from_nuggets", new ItemStack(ingot), "###", "###", "###", '#', nugget);
            Item stick = Items.STICK;
            shaped(r, "copper_sword", out("copper_sword"), "X", "X", "#", 'X', ingot, '#', stick);
            shaped(r, "copper_pickaxe", out("copper_pickaxe"), "XXX", " # ", " # ", 'X', ingot, '#', stick);
            shaped(r, "copper_axe", out("copper_axe"), "XX", "X#", " #", 'X', ingot, '#', stick);
            shaped(r, "copper_shovel", out("copper_shovel"), "X", "#", "#", 'X', ingot, '#', stick);
            shaped(r, "copper_hoe", out("copper_hoe"), "XX", " #", " #", 'X', ingot, '#', stick);
            shaped(r, "copper_helmet", out("copper_helmet"), "XXX", "X X", 'X', ingot);
            shaped(r, "copper_chestplate", out("copper_chestplate"), "X X", "XXX", "XXX", 'X', ingot);
            shaped(r, "copper_leggings", out("copper_leggings"), "XXX", "X X", "X X", 'X', ingot);
            shaped(r, "copper_boots", out("copper_boots"), "X X", "X X", 'X', ingot);
            shaped(r, "copper_bars", new ItemStack(ITEMS.get("copper_bars"), 16), "###", "###", '#', ingot);
            shaped(r, "copper_chain", out("copper_chain"), "N", "I", "N", 'I', ingot, 'N', nugget);
        } else {
            TrickyTrialsBackport.LOGGER.warn("No copper ingot (Caves & Cliffs Backport absent and the copper stream off): "
                    + "copper tools, armor, bars and chains cannot be crafted.");
        }
        shaped(r, "copper_torch", new ItemStack(torch, 4), "C", "X", "#", 'C', nugget,
                'X', new ItemStack(Items.COAL, 1, OreDictionary.WILDCARD_VALUE), '#', Items.STICK);
        shaped(r, "copper_lantern", out("copper_lantern"), "XXX", "X#X", "XXX", '#', torch, 'X', nugget);
        if (!ModConfig.copper.enabled) {
            // The copper stream gives the wax items their ore name; without it, this stream does.
            for (String wax : ModConfig.copper.waxItems) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(wax));
                if (item != null && item != Items.AIR) {
                    OreDictionary.registerOre("honeycomb", item);
                }
            }
        }
        for (String shape : SHAPES) {
            for (int stage = 0; stage < 4; stage++) {
                String waxed = name(shape, stage, true);
                ShapelessOreRecipe wax = new ShapelessOreRecipe(Content.id(name(shape, 0, true)), out(waxed),
                        out(name(shape, stage, false)), "honeycomb");
                wax.setRegistryName(Content.id(waxed + "_from_honeycomb"));
                r.register(wax);
            }
        }
        // copper_nugget_from_smelting: every copper tool, armor piece and the horse armor, 0.1 experience.
        for (String name : TOOLS) {
            GameRegistry.addSmelting(ITEMS.get(name), new ItemStack(nugget), 0.1F);
        }
        for (String name : ARMOR) {
            GameRegistry.addSmelting(ITEMS.get(name), new ItemStack(nugget), 0.1F);
        }
        GameRegistry.addSmelting(horseArmor, new ItemStack(nugget), 0.1F);
    }

    private static ItemStack out(String name) {
        return new ItemStack(ITEMS.get(name));
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe sr = new ShapedOreRecipe(Content.id(name), out, recipe);
        sr.setRegistryName(Content.id(name));
        r.register(sr);
    }

    private static void shapeless(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapelessOreRecipe sr = new ShapelessOreRecipe(Content.id(name), out, recipe);
        sr.setRegistryName(Content.id(name));
        r.register(sr);
    }

    // ---------------------------------------------------------------------------------------------
    // Loot

    /** 1.21.9's chests with copper horse armor beside iron horse armor, with its weight there. */
    static final Map<String, Integer> HORSE_ARMOR_CHESTS = new LinkedHashMap<String, Integer>();

    static {
        HORSE_ARMOR_CHESTS.put("minecraft:chests/simple_dungeon", 15);
        HORSE_ARMOR_CHESTS.put("minecraft:chests/desert_pyramid", 15);
        HORSE_ARMOR_CHESTS.put("minecraft:chests/nether_bridge", 5);
        HORSE_ARMOR_CHESTS.put("minecraft:chests/jungle_temple", 1);
        HORSE_ARMOR_CHESTS.put("minecraft:chests/stronghold_corridor", 1);
        HORSE_ARMOR_CHESTS.put("minecraft:chests/end_city_treasure", 1);
        // 1.21.11's village_weaponsmith; 1.12.2's one village smith chest is the blacksmith's.
        HORSE_ARMOR_CHESTS.put("minecraft:chests/village_blacksmith", 1);
    }

    /** @return the loot tables copper horse armor was added to, for the self-test */
    public static final List<String> LOOT_ADDED = new ArrayList<String>();

    @SubscribeEvent
    public static void lootTables(LootTableLoadEvent event) {
        if (!enabled() || !ModConfig.copperGear.horseArmorLoot) {
            return;
        }
        Integer weight = HORSE_ARMOR_CHESTS.get(event.getName().toString());
        if (weight == null) {
            return;
        }
        for (String poolName : new String[] {"main", "pool0", "pool1", "pool2", "pool3"}) {
            LootPool pool = event.getTable().getPool(poolName);
            if (pool == null || pool.getEntry("minecraft:iron_horse_armor") == null) {
                continue;
            }
            String id = TrickyTrialsBackport.MOD_ID + ":copper_horse_armor";
            if (pool.getEntry(id) == null) {
                LootEntry entry = new LootEntryItem(horseArmor, weight, 0, new LootFunction[0], new LootCondition[0], id);
                pool.addEntry(entry);
                LOOT_ADDED.add(event.getName().toString());
            }
            return;
        }
    }
}
