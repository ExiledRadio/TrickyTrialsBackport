package com.exiledradio.trickytrialsbackport.spear;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.common.ValueTrigger;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Stream 22: 1.21.11's spears and Lunge.
 *
 * <ul>
 *   <li>Seven spears ({@link ItemSpear}, {@link SpearTier}): wooden, stone, copper, iron, golden, diamond and
 *       netherite. Crafted from two sticks and one material on a diagonal; the netherite spear by Backport Core's
 *       smithing upgrade from a diamond spear, when a netherite mod is installed. A dropped netherite spear does not
 *       burn ({@link EntityFireproofItem}, entity id 55).</li>
 *   <li>The jab and the charge ({@link SpearAttacks}); the client's half in {@code spear.client}.</li>
 *   <li>Lunge I-III ({@link EnchantmentLunge}).</li>
 *   <li>Zombies (iron) and zombie pigmen (golden) spawning with spears, and charging with them ({@link SpearEvents},
 *       {@link EntityAISpearUse}, patch P20).</li>
 *   <li>Advancement <i>Mob Kabob</i>: five mobs in one charge.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class SpearContent {

    /** Spears only. */
    public static final EnumEnchantmentType SPEAR = EnumHelper.addEnchantmentType("TRICKYTRIALSBACKPORT_SPEAR",
            item -> item instanceof ItemSpear);

    /** {@code trickytrialsbackport:spear_mobs}: living things hit in one charge, for <i>Mob Kabob</i>. */
    public static final ValueTrigger MOB_KABOB = new ValueTrigger(Content.id("spear_mobs"), "count");

    static final String[] SOUNDS = {"item.spear.attack", "item.spear.hit", "item.spear.use", "item.spear.lunge_1",
            "item.spear.lunge_2", "item.spear.lunge_3", "item.spear_wood.attack", "item.spear_wood.hit", "item.spear_wood.use"};

    /** 1.21.11's {@code enchantable/melee_weapon} enchantments, which 1.12.2 gives swords only. */
    private static final Set<Enchantment> MELEE = new HashSet<Enchantment>(Arrays.asList(Enchantments.SHARPNESS, Enchantments.SMITE,
            Enchantments.BANE_OF_ARTHROPODS, Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING));

    public static final Map<SpearTier, ItemSpear> SPEARS = new EnumMap<SpearTier, ItemSpear>(SpearTier.class);
    public static EnchantmentLunge lunge;

    private SpearContent() {
    }

    public static boolean enabled() {
        return ModConfig.spears.enabled;
    }

    /** @return the spear of a tier, or null while the stream is off */
    public static ItemSpear spear(SpearTier tier) {
        return SPEARS.get(tier);
    }

    public static Map<SpearTier, ItemSpear> spears() {
        return Collections.unmodifiableMap(SPEARS);
    }

    static boolean fireproofItem() {
        return enabled();
    }

    /** At the enchanting table: the melee set, Lunge, and whatever 1.12.2's types already let on a spear (Unbreaking). */
    static boolean atTable(Enchantment e) {
        return MELEE.contains(e) || e.type != null && e.type.canEnchantItem(SPEARS.get(SpearTier.IRON));
    }

    /** At the anvil: the same, Mending and Curse of Vanishing coming in through their own types. */
    public static boolean takes(Enchantment e) {
        return atTable(e);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (String sound : SOUNDS) {
            Content.sound(sound);
        }
        add(event, SpearTier.WOOD, () -> "plankWood");
        add(event, SpearTier.STONE, () -> "cobblestone");
        add(event, SpearTier.COPPER, () -> Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot"));
        add(event, SpearTier.IRON, () -> "ingotIron");
        add(event, SpearTier.GOLD, () -> "ingotGold");
        add(event, SpearTier.DIAMOND, () -> "gemDiamond");
        add(event, SpearTier.NETHERITE, () -> SmithingUpgrades.INGOT_ORE);
        MOB_KABOB.register();
    }

    private static void add(RegistryEvent.Register<Item> event, SpearTier tier, Supplier<Object> repair) {
        SPEARS.put(tier, Content.item(event.getRegistry(), tier.itemName, new ItemSpear(tier, repair)));
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!enabled()) {
            return;
        }
        lunge = new EnchantmentLunge(SPEAR);
        event.getRegistry().register(lunge);
        // Lunge books in the Combat tab, next to the other weapon books.
        CreativePlacement.addEnchantmentType(CreativeTabs.COMBAT, SPEAR);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityFireproofItem.class)
                .id(Content.id("fireproof_item"), 55).name(Content.MOD_ID + ".fireproof_item").tracker(64, 20, true).build());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        recipe(event, SpearTier.WOOD, "plankWood");
        recipe(event, SpearTier.STONE, "cobblestone");
        Item copper = Owners.item(Owners.CAVES_AND_CLIFFS, "copper_ingot");
        if (copper != null) {
            recipe(event, SpearTier.COPPER, copper);
        } else {
            TrickyTrialsBackport.LOGGER.info("No copper ingot (Caves & Cliffs absent and the copper stream off): the copper spear has no recipe.");
        }
        recipe(event, SpearTier.IRON, "ingotIron");
        recipe(event, SpearTier.GOLD, "ingotGold");
        recipe(event, SpearTier.DIAMOND, "gemDiamond");
        // 1.21.11's netherite_spear_smithing; Core's table finds it when a netherite mod and a smithing table exist.
        SmithingUpgrades.add(SPEARS.get(SpearTier.DIAMOND), null, new ItemStack(SPEARS.get(SpearTier.NETHERITE)), TrickyTrialsBackport.MOD_ID);
    }

    /** 1.21.11's spear recipe: the material top right, two sticks down to the bottom left. */
    private static void recipe(RegistryEvent.Register<IRecipe> event, SpearTier tier, Object material) {
        ShapedOreRecipe r = new ShapedOreRecipe(null, new ItemStack(SPEARS.get(tier)), "  X", " # ", "#  ", 'X', material, '#', Items.STICK);
        r.setRegistryName(Content.id(tier.itemName));
        event.getRegistry().register(r);
    }

    /** @return true if an entity holds a spear in its main hand */
    public static boolean holdsSpear(Entity e) {
        return e instanceof net.minecraft.entity.EntityLivingBase
                && ((net.minecraft.entity.EntityLivingBase) e).getHeldItemMainhand().getItem() instanceof ItemSpear;
    }
}
