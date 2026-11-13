package com.exiledradio.trickytrialsbackport.effects;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.init.Blocks;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream 8: the four effects of 1.21 and their potions.
 *
 * <ul>
 *   <li><b>Effects</b> {@code wind_charged}, {@code weaving}, {@code oozing}, {@code infested}: harmful,
 *       1.21's colors. What they do is in {@link EffectEvents}.</li>
 *   <li><b>Potions</b> of the same names, 3:00 (3600 ticks) each. 1.12.2 makes their splash and lingering
 *       forms and tipped arrows from every potion type by itself, with vanilla's own scaling (lingering
 *       clouds give a quarter, 0:45; arrows an eighth, 0:22).</li>
 *   <li><b>Brewing</b>, 1.21's {@code addStartMix}: an awkward potion with a breeze rod, a slime block, stone
 *       or a cobweb gives each potion; a water bottle with any of them gives a mundane potion. The breeze rod
 *       is the breeze stream's, by name: with that stream off, no Wind Charging potion is brewed.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class EffectsContent {

    /** 1.21's potion duration for all four: 3:00. */
    public static final int DURATION = 3600;

    /** The cobweb placing sound of 1.21's level event 3018. */
    static final String COBWEB_PLACE = "block.cobweb.place";
    /** Wind Charged's burst sound (1.21's {@code BREEZE_WIND_CHARGE_BURST}); its files ship with the breeze stream. */
    static final String BURST_SOUND = "entity.breeze.wind_burst";

    public static EffectPotion windCharged;
    public static EffectPotion weaving;
    public static EffectPotion oozing;
    public static EffectPotion infested;

    public static PotionType windChargedPotion;
    public static PotionType weavingPotion;
    public static PotionType oozingPotion;
    public static PotionType infestedPotion;

    private EffectsContent() {
    }

    public static boolean enabled() {
        return ModConfig.effects.enabled;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<Item> event) {
        // Sounds are made before the sound registry event (items come first) so Content registers them.
        if (enabled()) {
            Content.sound(COBWEB_PLACE);
            Content.sound(BURST_SOUND);
        }
    }

    @SubscribeEvent
    public static void registerEffects(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        // 1.21's MobEffects colors.
        windCharged = effect(event, "wind_charged", 12438015);
        weaving = effect(event, "weaving", 7891290);
        oozing = effect(event, "oozing", 10092451);
        infested = effect(event, "infested", 9214860);
    }

    private static EffectPotion effect(RegistryEvent.Register<Potion> event, String name, int color) {
        EffectPotion potion = new EffectPotion(name, color);
        potion.setRegistryName(Content.id(name));
        event.getRegistry().register(potion);
        return potion;
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<PotionType> event) {
        if (!enabled()) {
            return;
        }
        windChargedPotion = potion(event, "wind_charged", windCharged);
        weavingPotion = potion(event, "weaving", weaving);
        oozingPotion = potion(event, "oozing", oozing);
        infestedPotion = potion(event, "infested", infested);
    }

    private static PotionType potion(RegistryEvent.Register<PotionType> event, String name, Potion effect) {
        // The base name gives potion.effect.<mod>.<name>, splash_potion.effect..., lingering_potion.effect...
        // and tipped_arrow.effect... for the four item names.
        PotionType type = new PotionType(TrickyTrialsBackport.MOD_ID + "." + name, new PotionEffect(effect, DURATION));
        type.setRegistryName(Content.id(name));
        event.getRegistry().register(type);
        return type;
    }

    /** Brewing recipes; from init, once every stream's items exist. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        Item rod = Owners.ours("breeze_rod");
        if (rod != null) {
            startMix(Ingredient.fromItem(rod), windChargedPotion);
        }
        startMix(Ingredient.fromItem(Item.getItemFromBlock(Blocks.SLIME_BLOCK)), oozingPotion);
        // Plain stone only: 1.12.2's stone item also carries granite, diorite and andesite.
        startMix(Ingredient.fromStacks(new ItemStack(Blocks.STONE, 1, 0)), infestedPotion);
        startMix(Ingredient.fromItem(Item.getItemFromBlock(Blocks.WEB)), weavingPotion);
    }

    /** 1.21's {@code PotionBrewing.Builder.addStartMix}: water to mundane, awkward to the potion. */
    private static void startMix(Ingredient ingredient, PotionType potion) {
        PotionHelper.addMix(PotionTypes.WATER, ingredient, PotionTypes.MUNDANE);
        PotionHelper.addMix(PotionTypes.AWKWARD, ingredient, potion);
    }

    /** @return our sound event of this name (registered above) */
    static SoundEvent sound(String name) {
        return Content.sound(name);
    }
}
