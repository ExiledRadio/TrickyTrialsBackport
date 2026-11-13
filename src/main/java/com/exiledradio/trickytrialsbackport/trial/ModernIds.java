package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

/**
 * 1.21's names for items, potions, enchantments and entities, as this game knows them.
 *
 * <p>Vanilla's data files (spawner configs, loot tables) name everything {@code minecraft:<name>}. A thing
 * 1.12.2 has under that name is used as it is; anything newer is looked for under this mod's id (our
 * other streams), then under each family mod that owns older content, in Rule 1's order. A thing found
 * nowhere is {@code null}, and the loot entry or spawn that needs it is left out.
 */
public final class ModernIds {

    /** Where a 1.21 name is looked for after {@code minecraft}, in order. */
    private static final String[] NAMESPACES = {Content.MOD_ID, Owners.VILLAGE_AND_PILLAGE, Owners.UPDATE_AQUATIC,
            Owners.BUZZY_BEES, Owners.NETHER_UPDATE, Owners.CAVES_AND_CLIFFS, Owners.WILD_UPDATE, Owners.TRAILS_AND_TALES};

    private ModernIds() {
    }

    /** @return a stack of one of the item a 1.21 id names, or null if this game has none */
    @Nullable
    public static ItemStack stack(String id) {
        ResourceLocation rl = new ResourceLocation(id);
        if ("minecraft".equals(rl.getNamespace())) {
            // Renamed by 1.13's flattening.
            if ("enchanted_golden_apple".equals(rl.getPath())) {
                return new ItemStack(Items.GOLDEN_APPLE, 1, 1);
            }
            if ("ominous_bottle".equals(rl.getPath())) {
                Item bottle = Owners.item(Owners.VILLAGE_AND_PILLAGE, "ominous_bottle");
                return bottle == null ? null : new ItemStack(bottle);
            }
        }
        Item item = find(ForgeRegistries.ITEMS, rl);
        return item == null ? null : new ItemStack(item);
    }

    @Nullable
    public static Enchantment enchantment(String id) {
        return find(ForgeRegistries.ENCHANTMENTS, new ResourceLocation(id));
    }

    @Nullable
    public static PotionType potion(String id) {
        return find(ForgeRegistries.POTION_TYPES, new ResourceLocation(id));
    }

    /** @return the registry name this game spawns a 1.21 entity id under, or null */
    @Nullable
    public static ResourceLocation entity(String id) {
        ResourceLocation rl = new ResourceLocation(id);
        if (EntityList.isRegistered(rl)) {
            return rl;
        }
        if ("minecraft".equals(rl.getNamespace())) {
            for (String ns : NAMESPACES) {
                ResourceLocation other = new ResourceLocation(ns, rl.getPath());
                if (EntityList.isRegistered(other)) {
                    return other;
                }
            }
        }
        return null;
    }

    @Nullable
    private static <T extends IForgeRegistryEntry<T>> T find(IForgeRegistry<T> registry, ResourceLocation rl) {
        T found = exact(registry, rl);
        if (found != null || !"minecraft".equals(rl.getNamespace())) {
            return found;
        }
        for (String ns : NAMESPACES) {
            found = exact(registry, new ResourceLocation(ns, rl.getPath()));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Nullable
    private static <T extends IForgeRegistryEntry<T>> T exact(IForgeRegistry<T> registry, ResourceLocation rl) {
        if (!registry.containsKey(rl)) {
            return null;
        }
        T value = registry.getValue(rl);
        return value != null && rl.equals(value.getRegistryName()) ? value : null;
    }
}
