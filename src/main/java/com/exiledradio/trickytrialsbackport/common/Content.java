package com.exiledradio.trickytrialsbackport.common;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one way every stream names and registers its blocks and items.
 *
 * <p>Going through here means a block always gets its translation key, creative tab and harvest tool
 * the same way, and the client always gets a model request for every item: {@code client.ClientSetup}
 * registers every entry in {@link #itemModels()} and nothing else, so an item can never end up
 * without a model because a stream forgot its client class. It holds names only, never client types,
 * so the server loads it safely.
 *
 * <p>A stream keeps the blocks it registers in its own fields; this class only records what the client
 * and the self-test need.
 */
public final class Content {

    /** item -> (metadata -> model name under this mod's {@code models/item/}), in registration order */
    private static final Map<Item, Map<Integer, String>> ITEM_MODELS = new LinkedHashMap<Item, Map<Integer, String>>();

    /** Every block registered through here, for the blockstate check. */
    private static final List<Block> BLOCKS = new ArrayList<Block>();

    public static final String MOD_ID = TrickyTrialsBackport.MOD_ID;

    private Content() {
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(TrickyTrialsBackport.MOD_ID, name);
    }

    /**
     * Names and registers a block. Its item, if it has one, is registered separately with
     * {@link #registerBlockItem} in the item event.
     *
     * @param tool the harvest tool ("axe", "pickaxe", "shovel"), or null for none
     */
    public static <B extends Block> B block(IForgeRegistry<Block> registry, String name, B block, @Nullable String tool) {
        block.setRegistryName(id(name));
        block.setTranslationKey(TrickyTrialsBackport.MOD_ID + "." + name);
        block.setCreativeTab(ModCreativeTab.TAB);
        if (tool != null) {
            block.setHarvestLevel(tool, 0);
        }
        registry.register(block);
        BLOCKS.add(block);
        return block;
    }

    /** Registers a plain {@link ItemBlock} for a block, with its model at metadata 0. */
    public static ItemBlock registerBlockItem(IForgeRegistry<Item> registry, Block block) {
        ItemBlock item = new ItemBlock(block);
        return blockItem(registry, block, item);
    }

    /** Registers a given item for a block, under the block's name, with its model at metadata 0. */
    public static <I extends Item> I blockItem(IForgeRegistry<Item> registry, Block block, I item) {
        item.setRegistryName(block.getRegistryName());
        registry.register(item);
        model(item, 0, block.getRegistryName().getPath());
        return item;
    }

    /** Names and registers an item, with its model at metadata 0. */
    public static <I extends Item> I item(IForgeRegistry<Item> registry, String name, I item) {
        item.setRegistryName(id(name));
        item.setTranslationKey(TrickyTrialsBackport.MOD_ID + "." + name);
        item.setCreativeTab(ModCreativeTab.TAB);
        registry.register(item);
        model(item, 0, name);
        return item;
    }

    /** Asks the client for one more model: {@code models/item/<model>.json} for this metadata. */
    public static void model(Item item, int meta, String model) {
        Map<Integer, String> metas = ITEM_MODELS.get(item);
        if (metas == null) {
            metas = new LinkedHashMap<Integer, String>();
            ITEM_MODELS.put(item, metas);
        }
        metas.put(meta, model);
    }

    /** @return every item model request, for the client and the self-test */
    public static Map<Item, Map<Integer, String>> itemModels() {
        return Collections.unmodifiableMap(ITEM_MODELS);
    }

    /** @return every block registered through here */
    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    /** Blocks drawn from a multipart-only blockstate: every state maps to the one model name "multipart". */
    private static final List<Block> MULTIPART = new ArrayList<Block>();

    /**
     * Declares that a block's blockstate file is multipart-only.
     *
     * <p>The client then maps every state to one model name instead of one per state - the parts are
     * picked from the state when drawn, so nothing is lost, and a block whose states multiply (water's
     * LEVEL alone is 16) never asks the model loader for thousands of identical models.
     */
    public static void multipart(Block block) {
        MULTIPART.add(block);
        MULTIPART_FILES.add(block);
    }

    /** Blocks whose blockstate file is multipart, however the client maps their states. */
    private static final List<Block> MULTIPART_FILES = new ArrayList<Block>();

    /**
     * Declares that a block's blockstate file is multipart while its states map to variant strings as
     * usual (walls: the parts are picked by {@code up}, {@code north}, ...), for the asset check.
     */
    public static void multipartFile(Block block) {
        MULTIPART_FILES.add(block);
    }

    public static List<Block> multipartFiles() {
        return Collections.unmodifiableList(MULTIPART_FILES);
    }

    public static List<Block> multipartBlocks() {
        return Collections.unmodifiableList(MULTIPART);
    }

    /** Properties the client's state mapper leaves out, per block (slabs' and walls' pinned variants). */
    private static final Map<Block, IProperty<?>[]> IGNORED = new LinkedHashMap<Block, IProperty<?>[]>();

    /** Declares properties a block's blockstate file does not mention; the client and the self-test skip them. */
    public static void ignoreProperties(Block block, IProperty<?>... properties) {
        IGNORED.put(block, properties);
    }

    public static Map<Block, IProperty<?>[]> ignoredProperties() {
        return Collections.unmodifiableMap(IGNORED);
    }

    // ------------------------------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------------------------------

    /** Every sound event made through {@link #sound}, registered in the sound registry event. */
    private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<String, SoundEvent>();

    /**
     * @return our sound event {@code <modid>:<name>} (sounds.json's key, such as {@code block.tuff.break}),
     * made on first use. Blocks register before sounds, so a block may hold the event before it is
     * registered; it is registered with the rest in the sound event.
     */
    public static synchronized SoundEvent sound(String name) {
        SoundEvent event = SOUNDS.get(name);
        if (event == null) {
            event = new SoundEvent(id(name));
            event.setRegistryName(id(name));
            SOUNDS.put(name, event);
        }
        return event;
    }

    /** @return a sound type from our five events {@code block.<name>.break/step/place/hit/fall} */
    public static SoundType soundType(String name, float volume, float pitch) {
        return new SoundType(volume, pitch, sound("block." + name + ".break"), sound("block." + name + ".step"),
                sound("block." + name + ".place"), sound("block." + name + ".hit"), sound("block." + name + ".fall"));
    }

    @Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
            synchronized (Content.class) {
                for (SoundEvent sound : SOUNDS.values()) {
                    event.getRegistry().register(sound);
                }
            }
        }
    }
}
