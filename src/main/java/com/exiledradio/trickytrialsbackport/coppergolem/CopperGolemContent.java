package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;

/**
 * Stream 19: the copper golem (1.21.9). The copper golem ({@code copper_golem}, entity id 50; 51-54 unused), the eight
 * copper chests and the eight copper golem statues (four stages, each waxed), their recipes, sounds, oxidation and
 * waxing. No packets (95-99 unused): everything the client draws is synced entity data or block state.
 *
 * <p>Behavior is 1.21.11's ({@code CopperGolem}, {@code CopperGolemAi}, {@code TransportItemsBetweenContainers},
 * {@code CopperChestBlock}, {@code WeatheringCopperChestBlock}, {@code CopperGolemStatueBlock},
 * {@code WeatheringCopperGolemStatueBlock}, {@code CarvedPumpkinBlock}, {@code OfferFlowerGoal}), decompiled from the
 * 1.21.11 client jar. Copper blocks and ingots are Caves &amp; Cliffs' when it is installed, the copper stream's copies
 * otherwise, looked up by name.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class CopperGolemContent {

    public static final int COPPER_GOLEM_ID = 50;
    /** Spawn egg colors: the two commonest of 1.21.9's egg texture (its light and dark copper). */
    public static final int EGG_PRIMARY = 0xE3826C;
    public static final int EGG_SECONDARY = 0x783219;

    /** The weathering state names (1.21's {@code WeatheringCopper.WeatherState}), in stage order. */
    public static final String[] STATE_NAMES = {"unaffected", "exposed", "weathered", "oxidized"};

    // Sounds: the golem's by oxidation level (exposed shares the unaffected ones), the chest's and the statue's.
    public static final SoundEvent[] SPIN = stageSounds("spin");
    public static final SoundEvent[] HURT = stageSounds("hurt");
    public static final SoundEvent[] DEATH = stageSounds("death");
    public static final SoundEvent[] STEP = stageSounds("step");
    public static final SoundEvent SPAWN = Content.sound("entity.copper_golem.spawn");
    public static final SoundEvent ITEM_DROP = Content.sound("entity.copper_golem.item_drop");
    public static final SoundEvent ITEM_NO_DROP = Content.sound("entity.copper_golem.item_no_drop");
    public static final SoundEvent ITEM_GET = Content.sound("entity.copper_golem.no_item_get");
    public static final SoundEvent ITEM_NO_GET = Content.sound("entity.copper_golem.no_item_no_get");
    public static final SoundEvent SHEAR = Content.sound("entity.copper_golem.shear");
    public static final SoundEvent BECOME_STATUE = Content.sound("entity.copper_golem_become_statue");
    public static final SoundEvent WAX_ON = Content.sound("item.honeycomb.wax_on");
    public static final SoundEvent AXE_SCRAPE = Content.sound("item.axe.scrape");
    public static final SoundEvent AXE_WAX_OFF = Content.sound("item.axe.wax_off");
    /** Chest open and close sounds by stage: unaffected and exposed share the plain ones (1.21.9). */
    public static final SoundEvent[] CHEST_OPEN = {Content.sound("block.copper_chest.open"), Content.sound("block.copper_chest.open"),
            Content.sound("block.copper_chest_weathered.open"), Content.sound("block.copper_chest_oxidized.open")};
    public static final SoundEvent[] CHEST_CLOSE = {Content.sound("block.copper_chest.close"), Content.sound("block.copper_chest.close"),
            Content.sound("block.copper_chest_weathered.close"), Content.sound("block.copper_chest_oxidized.close")};

    /** 1.21's {@code SoundType.COPPER}, for the chests (the copper stream's events). */
    static final SoundType COPPER_SOUND = Content.soundType("copper", 1.0F, 1.0F);
    /** 1.21.9's {@code SoundType.COPPER_GOLEM_STATUE}. */
    static final SoundType STATUE_SOUND = Content.soundType("copper_golem_statue", 1.0F, 1.0F);

    /** Map colors by stage: the copper stream's (1.12.2 has no warped colors). */
    static final MapColor[] MAP_COLORS = {MapColor.ADOBE, MapColor.SILVER_STAINED_HARDENED_CLAY, MapColor.CYAN, MapColor.CYAN};

    /** The chest type every copper chest shares, so 1.12.2's chest code joins copper chests only with each other. */
    static BlockChest.Type copperType;

    /** [waxed ? 1 : 0][stage] */
    public static final BlockCopperChest[][] CHESTS = new BlockCopperChest[2][4];
    public static final BlockCopperGolemStatue[][] STATUES = new BlockCopperGolemStatue[2][4];

    /** 1.21's {@code #copper} block tag, by name: the owner's copper blocks or the copper stream's copies. */
    static final String[] COPPER_BLOCKS = {"copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"};

    private CopperGolemContent() {
    }

    public static boolean enabled() {
        return ModConfig.copperGolem.enabled;
    }

    private static SoundEvent[] stageSounds(String what) {
        SoundEvent plain = Content.sound("entity.copper_golem." + what);
        return new SoundEvent[] {plain, plain, Content.sound("entity.copper_golem_weathered." + what),
                Content.sound("entity.copper_golem_oxidized." + what)};
    }

    public static String prefix(int stage) {
        return Owners.OXIDATION[stage];
    }

    public static String chestName(int stage, boolean waxed) {
        return (waxed ? "waxed_" : "") + prefix(stage) + "copper_chest";
    }

    public static String statueName(int stage, boolean waxed) {
        return (waxed ? "waxed_" : "") + prefix(stage) + "copper_golem_statue";
    }

    /** @return the weathering state's saved name, such as "exposed" */
    public static String stateName(int stage) {
        return STATE_NAMES[Math.max(0, Math.min(3, stage))];
    }

    /** @return the stage of a saved weathering state name; 0 for anything unknown (1.21's codec default) */
    public static int stageOf(String name) {
        int i = Arrays.asList(STATE_NAMES).indexOf(name == null ? "" : name.toLowerCase(Locale.ROOT));
        return i < 0 ? 0 : i;
    }

    /**
     * 1.21's {@code #copper} block tag: the eight full copper blocks.
     *
     * @return the stage 0-3 of the copper block, or -1 if this block is not one
     */
    public static int copperBlockStage(Block block) {
        for (int stage = 0; stage < 4; stage++) {
            if (block == Owners.block(COPPER_BLOCKS[stage]) || block == Owners.block("waxed_" + COPPER_BLOCKS[stage])) {
                return stage;
            }
        }
        return -1;
    }

    /** @return the copper ingot: Caves &amp; Cliffs' when installed, else the copper stream's copy (null if neither) */
    @Nullable
    public static Item ingot() {
        return Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        copperType = EnumHelper.addEnum(BlockChest.Type.class, "TRICKYTRIALSBACKPORT_COPPER", new Class<?>[0]);
        for (int w = 0; w < 2; w++) {
            for (int stage = 0; stage < 4; stage++) {
                CHESTS[w][stage] = Content.block(event.getRegistry(), chestName(stage, w == 1),
                        new BlockCopperChest(copperType, stage, w == 1), "pickaxe");
                STATUES[w][stage] = Content.block(event.getRegistry(), statueName(stage, w == 1),
                        new BlockCopperGolemStatue(stage, w == 1), "pickaxe");
            }
        }
        // 1.21.9's names, ours (Core's ownership table, so no other mod registers a copy).
        String[] names = new String[16];
        for (int w = 0; w < 2; w++) {
            for (int stage = 0; stage < 4; stage++) {
                names[w * 8 + stage * 2] = chestName(stage, w == 1);
                names[w * 8 + stage * 2 + 1] = statueName(stage, w == 1);
            }
        }
        BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, names);
        // Backport Core's oxidation table: each shape's four stages and their waxed forms.
        Oxidation.chain(CHESTS[0]);
        Oxidation.chain(STATUES[0]);
        for (int stage = 0; stage < 4; stage++) {
            Oxidation.waxed(CHESTS[0][stage], CHESTS[1][stage]);
            Oxidation.waxed(STATUES[0][stage], STATUES[1][stage]);
        }
        GameRegistry.registerTileEntity(TileEntityCopperChest.class, Content.id("copper_chest"));
        GameRegistry.registerTileEntity(TileEntityCopperGolemStatue.class, Content.id("copper_golem_statue"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (int w = 0; w < 2; w++) {
            for (int stage = 0; stage < 4; stage++) {
                Content.blockItem(event.getRegistry(), CHESTS[w][stage], new ItemCopperChest(CHESTS[w][stage]));
                ItemCopperGolemStatue statue = Content.blockItem(event.getRegistry(), STATUES[w][stage],
                        new ItemCopperGolemStatue(STATUES[w][stage]));
                for (BlockCopperGolemStatue.Pose pose : BlockCopperGolemStatue.Pose.values()) {
                    if (pose != BlockCopperGolemStatue.Pose.STANDING) {
                        Content.model(statue, pose.ordinal(), STATUES[w][stage].getRegistryName().getPath() + "_" + pose.getName());
                    }
                }
            }
        }
    }

    /** After the items: the owner's copper ingot and every honeycomb are registered by then. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Item ingot = ingot();
        if (ingot == null) {
            TrickyTrialsBackport.LOGGER.warn("Copper golem: no copper ingot (Caves & Cliffs absent and copper.enabled=false); "
                    + "copper chests cannot be crafted");
        } else {
            ShapedOreRecipe chest = new ShapedOreRecipe(null, new ItemStack(CHESTS[0][0]), "###", "#X#", "###",
                    '#', ingot, 'X', Blocks.CHEST);
            chest.setRegistryName(Content.id("copper_chest"));
            event.getRegistry().register(chest);
        }
        registerWax();
        for (int stage = 0; stage < 4; stage++) {
            for (Block[] shape : new Block[][] {CHESTS[0], STATUES[0]}) {
                Block plain = shape[stage];
                Block waxed = Oxidation.waxedOf(plain);
                String name = waxed.getRegistryName().getPath();
                ShapelessOreRecipe wax = new ShapelessOreRecipe(Content.id(name), new ItemStack(waxed), new ItemStack(plain),
                        new OreIngredient("honeycomb"));
                wax.setRegistryName(Content.id(name + "_from_honeycomb"));
                event.getRegistry().register(wax);
            }
        }
    }

    /** Every item in {@code copper.waxItems} under the ore name honeycomb, as the copper streams do (once each). */
    private static void registerWax() {
        for (String wax : ModConfig.copper.waxItems) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(wax));
            if (item == null || !new ResourceLocation(wax).equals(item.getRegistryName())) {
                continue;
            }
            if (!OreDictionary.containsMatch(false, OreDictionary.getOres("honeycomb", false), new ItemStack(item))) {
                OreDictionary.registerOre("honeycomb", item);
            }
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityCopperGolem.class)
                .id(Content.id("copper_golem"), COPPER_GOLEM_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".copper_golem")
                .tracker(160, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
    }
}
