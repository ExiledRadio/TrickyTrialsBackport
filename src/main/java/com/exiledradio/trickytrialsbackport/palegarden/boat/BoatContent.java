package com.exiledradio.trickytrialsbackport.palegarden.boat;

import com.exiledradio.backportcore.boat.Boats;
import com.exiledradio.backportcore.boat.ItemWoodBoat;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The pale oak boat and, with Wild Update (the owner of chest boats, Rule 2), the pale oak chest boat.
 *
 * <p>The boats are Backport Core's ({@code backportcore:boat} and {@code chest_boat}, Core's {@code boat/}, our
 * request #9): this registers pale oak with Core, the items under our names, and the recipes, 1.21.4's: five planks in
 * a U make a boat; a boat and a chest, shapeless, make the chest boat. Worlds saved before (our own
 * {@code trickytrialsbackport:boat}, id 30) load as Core's, wood kept ({@link #preInit}).
 */
@Mod.EventBusSubscriber(modid = com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.MOD_ID)
public final class BoatContent {

    private static final Map<String, Item> ITEMS = new HashMap<String, Item>();

    private BoatContent() {
    }

    /** Our boats saved before Core 1.4.4 load as Core's, with or without the pale garden on. */
    public static void preInit() {
        Boats.renamed(Content.id("boat"), false);
    }

    /** @return true if chest boats exist in this game: Wild Update owns them (1.19) */
    public static boolean chestBoats() {
        return Owners.present(Owners.WILD_UPDATE);
    }

    /** @return the item of a wood's boat, or null if that boat is not in this game */
    @Nullable
    public static Item item(BoatWood wood) {
        return item(wood, false);
    }

    /** @return the item of a wood's boat or chest boat, or null if it is not in this game */
    @Nullable
    public static Item item(BoatWood wood, boolean chest) {
        return ITEMS.get(wood.itemName(chest));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        for (final BoatWood w : BoatWood.values()) {
            com.exiledradio.backportcore.boat.BoatWood wood = com.exiledradio.backportcore.boat.BoatWood.register(Content.id(w.name),
                    () -> w.planks(1), false)
                    .setTextures(Content.id("textures/entity/boat/" + w.name + ".png"), Content.id("textures/entity/chest_boat/" + w.name + ".png"));
            for (boolean chest : chestBoats() ? new boolean[] {false, true} : new boolean[] {false}) {
                String name = w.itemName(chest);
                Item item = Content.item(event.getRegistry(), name, new ItemWoodBoat(wood, chest));
                ITEMS.put(name, item);
                if (chest) {
                    wood.setChestBoatItem(item);
                } else {
                    wood.setBoatItem(item);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            Item boat = item(w, false);
            Item chestBoat = item(w, true);
            ItemStack planks = w.planks(1);
            if (boat == null || planks == null) {
                continue;
            }
            ResourceLocation id = Content.id(w.itemName(false));
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(boat), "# #", "###", '#', planks).setRegistryName(id));
            if (chestBoat == null) {
                continue;
            }
            ResourceLocation cid = Content.id(w.itemName(true));
            event.getRegistry().register(new ShapelessOreRecipe(cid, new ItemStack(chestBoat), Blocks.CHEST, boat).setRegistryName(cid));
        }
    }
}
