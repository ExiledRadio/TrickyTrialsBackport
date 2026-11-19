package com.exiledradio.trickytrialsbackport.shelf;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stream 21: 1.21.9's shelves.
 *
 * <ul>
 *   <li>A shelf ({@link BlockShelf}) for every wood in the game ({@link ShelfWood}): 1.12.2's six always, pale oak
 *       with stream 12, cherry and bamboo with Trails &amp; Tales, mangrove with Wild Update, crimson and warped with
 *       Nether Update.</li>
 *   <li>Recipes as 1.21.9 (six stripped logs make six shelves, group {@code shelf}); their sounds.</li>
 * </ul>
 * The blocks register at the lowest priority, after every mod's normal-priority blocks, so the woods other mods (and
 * stream 12) provide are already in the registry when this stream asks for them.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class ShelfContent {

    private static final Map<ShelfWood, BlockShelf> SHELVES = new EnumMap<ShelfWood, BlockShelf>(ShelfWood.class);

    public static SoundEvent activate;
    public static SoundEvent deactivate;
    public static SoundEvent multiSwap;
    public static SoundEvent placeItem;
    public static SoundEvent singleSwap;
    public static SoundEvent takeItem;

    private ShelfContent() {
    }

    public static boolean enabled() {
        return ModConfig.shelves.enabled;
    }

    /** @return the shelves registered in this game, by wood */
    public static Map<ShelfWood, BlockShelf> shelves() {
        return Collections.unmodifiableMap(SHELVES);
    }

    /** Pre-init: shelves first shipped in 1.21.9, so this mod owns every one in Backport Core's table. */
    public static void preInit() {
        if (!enabled()) {
            return;
        }
        String[] names = new String[ShelfWood.values().length];
        for (ShelfWood wood : ShelfWood.values()) {
            names[wood.ordinal()] = wood.shelfName();
        }
        BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, names);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();
        activate = Content.sound("block.shelf.activate");
        deactivate = Content.sound("block.shelf.deactivate");
        multiSwap = Content.sound("block.shelf.multi_swap");
        placeItem = Content.sound("block.shelf.place_item");
        singleSwap = Content.sound("block.shelf.single_swap");
        takeItem = Content.sound("block.shelf.take_item");
        SoundType sound = Content.soundType("shelf", 1.0F, 1.0F);
        for (ShelfWood wood : ShelfWood.values()) {
            if (wood.exists()) {
                BlockShelf shelf = Content.block(registry, wood.shelfName(), new BlockShelf(wood, sound), "axe");
                // 1.21.9's blockstate is multipart only (facing, power, place in the row).
                Content.multipart(shelf);
                SHELVES.put(wood, shelf);
            }
        }
        GameRegistry.registerTileEntity(TileEntityShelf.class, Content.id("shelf"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        for (BlockShelf shelf : SHELVES.values()) {
            Content.blockItem(registry, shelf, new ItemShelf(shelf));
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        for (BlockShelf shelf : SHELVES.values()) {
            ItemStack log = shelf.wood.ingredient();
            if (log == null) {
                TrickyTrialsBackport.LOGGER.warn("No stripped log for {}: it has no recipe", shelf.getRegistryName());
                continue;
            }
            ShapedOreRecipe recipe = new ShapedOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "shelf"),
                    new ItemStack(shelf, 6), "###", "   ", "###", '#', log);
            recipe.setRegistryName(Content.id(shelf.wood.shelfName()));
            event.getRegistry().register(recipe);
        }
    }

    /** The shelf's item: fuel as any wooden block (300 ticks), except crimson's and warped's, which do not burn. */
    public static final class ItemShelf extends ItemBlock {
        public ItemShelf(BlockShelf block) {
            super(block);
        }

        @Override
        public int getItemBurnTime(ItemStack stack) {
            return ((BlockShelf) this.block).wood.flammable ? -1 : 0;
        }
    }
}
