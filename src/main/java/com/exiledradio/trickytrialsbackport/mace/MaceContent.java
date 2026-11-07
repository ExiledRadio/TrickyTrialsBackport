package com.exiledradio.trickytrialsbackport.mace;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Stream 5: the mace, the heavy core, and Density, Breach and Wind Burst.
 *
 * <ul>
 *   <li>{@link ItemMace} ({@code mace}), crafted from a heavy core over a breeze rod (the breeze stream's,
 *       by name; without it the mace has no recipe).</li>
 *   <li>{@link BlockHeavyCore} ({@code heavy_core}), which ominous vaults give out (stream 7).</li>
 *   <li>Density I-V (uncommon, at the table), Breach I-IV (rare, at the table), Wind Burst I-III (rare,
 *       treasure: vaults only), for maces only.</li>
 *   <li>Advancement <i>Over-Overkill</i>.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class MaceContent {

    /** Maces only. */
    public static final EnumEnchantmentType MACE = EnumHelper.addEnchantmentType("TRICKYTRIALSBACKPORT_MACE",
            item -> item instanceof ItemMace);

    static final String[] SOUNDS = {"block.heavy_core.break", "block.heavy_core.fall", "block.heavy_core.hit", "block.heavy_core.place",
            "block.heavy_core.step", "item.mace.smash_air", "item.mace.smash_ground", "item.mace.smash_ground_heavy",
            "entity.wind_charge.wind_burst"};

    public static ItemMace mace;
    public static Block heavyCore;
    public static EnchantmentMace density;
    public static EnchantmentMace breach;
    public static EnchantmentMace windBurst;

    private MaceContent() {
    }

    public static boolean enabled() {
        return ModConfig.mace.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        heavyCore = Content.block(event.getRegistry(), "heavy_core", new BlockHeavyCore(Content.soundType("heavy_core", 1.0F, 1.0F)), null);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (String sound : SOUNDS) {
            Content.sound(sound);
        }
        mace = Content.item(event.getRegistry(), "mace", new ItemMace());
        Content.blockItem(event.getRegistry(), heavyCore, new ItemBlock(heavyCore) {
            @Override
            public EnumRarity getRarity(ItemStack stack) {
                return EnumRarity.EPIC;
            }
        });
        MaceEvents.MACE_SMASH.register();
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!enabled()) {
            return;
        }
        // 1.21.11's numbers; weight 5 is uncommon, 2 is rare.
        density = new EnchantmentMace("density", Enchantment.Rarity.UNCOMMON, MACE, 5, 5, 8, 25, 8, false, true);
        breach = new EnchantmentMace("breach", Enchantment.Rarity.RARE, MACE, 4, 15, 9, 65, 9, false, true);
        windBurst = new EnchantmentMace("wind_burst", Enchantment.Rarity.RARE, MACE, 3, 15, 9, 65, 9, true, false);
        event.getRegistry().registerAll(density, breach, windBurst);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Item rod = Owners.ours("breeze_rod");
        if (rod == null) {
            TrickyTrialsBackport.LOGGER.info("The breeze is switched off: the mace has no recipe.");
            return;
        }
        ShapedOreRecipe r = new ShapedOreRecipe(null, new ItemStack(mace), "#", "I", '#', heavyCore, 'I', rod);
        r.setRegistryName(Content.id("mace"));
        event.getRegistry().register(r);
    }
}
