package com.exiledradio.trickytrialsbackport.palegarden.boat;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The pale oak boat: one entity ({@code trickytrialsbackport:boat}, id 30 of the pale garden's block 30-34)
 * whose wood is in its data, so later woods share it. Recipe as 1.21.4: five planks in a U.
 *
 * <p>The pale oak chest boat is Wild Update's chest boat system; it is left to that integration.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class BoatContent {

    public static final int BOAT_ID = 30;

    private static final Map<BoatWood, Item> ITEMS = new EnumMap<BoatWood, Item>(BoatWood.class);

    private BoatContent() {
    }

    /** @return the item of a wood's boat, or null if that boat is not in this game */
    @Nullable
    public static Item item(BoatWood wood) {
        return ITEMS.get(wood);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityModBoat.class).id(Content.id("boat"), BOAT_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".boat").tracker(80, 3, true).build());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            ITEMS.put(w, Content.item(event.getRegistry(), w.itemName(), new ItemModBoat(w)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!PaleGardenContent.enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            Item boat = item(w);
            ItemStack planks = w.planks(1);
            if (boat == null || planks == null) {
                continue;
            }
            ResourceLocation id = Content.id(w.itemName());
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(boat), "# #", "###", '#', planks).setRegistryName(id));
        }
    }
}
