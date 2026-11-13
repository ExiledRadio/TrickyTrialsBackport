package com.exiledradio.trickytrialsbackport.bundle;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 11: bundles (1.21.2).
 *
 * <ul>
 *   <li>{@code bundle} and {@code <color>_bundle} for the sixteen colors ({@link ItemBundle}); string over
 *       leather, and any bundle with a dye ({@link RecipeBundleDye}).</li>
 *   <li>Screens: {@link BundleClicks}, packets 55 ({@link BundleClickMessage}) and 56
 *       ({@link BundleSelectMessage}); the client side is {@code bundle.client}.</li>
 *   <li>A bundle destroyed as an item (fire, lava, cactus, an explosion) spills what it holds, as vanilla's
 *       {@code onDestroyed}.</li>
 *   <li>1.21.5's village chests: one bundle in a third of cartographer, tannery, weaponsmith and house
 *       chests (Village &amp; Pillage's village loot tables; without it, the blacksmith's chest, 1.14's
 *       weaponsmith).</li>
 *   <li>Dispensers drop a bundle whole, as vanilla's do: 1.12.2's default.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class BundleContent {

    public static SoundEvent insert;
    public static SoundEvent insertFail;
    public static SoundEvent removeOne;
    public static SoundEvent dropContents;

    /** The plain bundle first, then the colors in vanilla's item order (white ... black). */
    public static final Map<String, ItemBundle> BUNDLES = new LinkedHashMap<String, ItemBundle>();

    /** The village chests 1.21.5 adds a bundle to (Village &amp; Pillage's names). */
    static final List<String> VILLAGE_CHESTS = Arrays.asList("village_cartographer", "village_desert_house",
            "village_plains_house", "village_savanna_house", "village_snowy_house", "village_taiga_house",
            "village_tannery", "village_weaponsmith");
    static final String VILLAGE_OWNER = "villagerbackport";

    private BundleContent() {
    }

    public static boolean enabled() {
        return ModConfig.bundles.enabled;
    }

    /** 1.21's color name: 1.12.2's "silver" is light gray. */
    public static String colorName(EnumDyeColor color) {
        return color == EnumDyeColor.SILVER ? "light_gray" : color.getName();
    }

    /** The ore-dictionary name of a color's dye, as Forge registers it ({@code dyeLightGray}). */
    static String dyeOre(EnumDyeColor color) {
        String[] parts = colorName(color).split("_");
        StringBuilder out = new StringBuilder("dye");
        for (String part : parts) {
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        insert = Content.sound("item.bundle.insert");
        insertFail = Content.sound("item.bundle.insert_fail");
        removeOne = Content.sound("item.bundle.remove_one");
        dropContents = Content.sound("item.bundle.drop_contents");
        BUNDLES.put("bundle", Content.item(event.getRegistry(), "bundle", new ItemBundle(null)));
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = colorName(color) + "_bundle";
            BUNDLES.put(name, Content.item(event.getRegistry(), name, new ItemBundle(color)));
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        ItemBundle plain = BUNDLES.get("bundle");
        ShapedOreRecipe bundle = new ShapedOreRecipe(null, new ItemStack(plain), "-", "#", '-', "string", '#', "leather");
        bundle.setRegistryName(Content.id("bundle"));
        event.getRegistry().register(bundle);
        Item[] all = BUNDLES.values().toArray(new Item[0]);
        ResourceLocation group = Content.id("bundle_dye");
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = colorName(color) + "_bundle";
            RecipeBundleDye dye = new RecipeBundleDye(group, BUNDLES.get(name), all, dyeOre(color));
            dye.setRegistryName(Content.id(name));
            event.getRegistry().register(dye);
        }
    }

    @SubscribeEvent
    public static void lootTables(LootTableLoadEvent event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation name = event.getName();
        boolean ownerChest = VILLAGE_OWNER.equals(name.getNamespace()) && name.getPath().startsWith("chests/village/")
                && VILLAGE_CHESTS.contains(name.getPath().substring("chests/village/".length()));
        boolean blacksmith = !Loader.isModLoaded(VILLAGE_OWNER) && "minecraft".equals(name.getNamespace())
                && "chests/village_blacksmith".equals(name.getPath());
        if (ownerChest || blacksmith) {
            event.getTable().addPool(villageBundlePool());
        }
    }

    /** 1.21.11's pool: one roll, a bundle (weight 1) or nothing (weight 2). */
    static LootPool villageBundlePool() {
        LootEntry bundle = new LootEntryItem(BUNDLES.get("bundle"), 1, 0, new LootFunction[0], new LootCondition[0],
                TrickyTrialsBackport.MOD_ID + ":bundle");
        LootEntry empty = new LootEntryEmpty(2, 0, new LootCondition[0], "empty");
        return new LootPool(new LootEntry[]{bundle, empty}, new LootCondition[0], new RandomValueRange(1.0F),
                new RandomValueRange(0.0F), TrickyTrialsBackport.MOD_ID + ":bundle");
    }

    /** Clears every pick in a closing screen: the client clears its own, but its packet may come too late. */
    @SubscribeEvent
    public static void containerClosed(PlayerContainerEvent.Close event) {
        if (!enabled()) {
            return;
        }
        clearPicks(event.getContainer());
    }

    public static void clearPicks(Container container) {
        for (Slot slot : container.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (BundleContents.isBundle(stack) && BundleContents.selectedItem(stack) >= 0) {
                BundleContents.toggleSelectedItem(stack, -1);
            }
        }
    }

    @SubscribeEvent
    public static void worldLoad(WorldEvent.Load event) {
        if (enabled() && !event.getWorld().isRemote) {
            event.getWorld().addEventListener(new DestroyedBundles());
        }
    }

    // ------------------------------------------------------------------------------------------
    // A destroyed bundle spills
    // ------------------------------------------------------------------------------------------

    private static Field itemHealth;

    /** @return an item entity's health (1.12.2 keeps it private), or 1 if it cannot be read */
    public static int health(EntityItem item) {
        try {
            if (itemHealth == null) {
                itemHealth = ReflectionHelper.findField(EntityItem.class, "health", "field_70291_e");
            }
            return itemHealth.getInt(item);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Vanilla's {@code BundleItem.onDestroyed} and {@code ItemUtils.onContainerDestroyed}: when a bundle item
     * dies of damage (its health at 0, unlike one picked up, merged, despawned or fallen out of the world),
     * every stack it held drops where it was.
     */
    public static void spill(World world, EntityItem entity) {
        ItemStack bundle = entity.getItem();
        if (!BundleContents.isBundle(bundle) || health(entity) > 0) {
            return;
        }
        List<ItemStack> items = BundleContents.items(bundle);
        if (items.isEmpty()) {
            return;
        }
        bundle.getTagCompound().removeTag(BundleContents.ITEMS);
        bundle.getTagCompound().removeTag(BundleContents.SELECTED);
        for (ItemStack stack : items) {
            world.spawnEntity(new EntityItem(world, entity.posX, entity.posY, entity.posZ, stack));
        }
    }

    public static final class DestroyedBundles extends WorldListener {
        @Override
        public void onEntityRemoved(Entity entity) {
            if (entity instanceof EntityItem && entity.isDead) {
                spill(entity.world, (EntityItem) entity);
            }
        }
    }
}
