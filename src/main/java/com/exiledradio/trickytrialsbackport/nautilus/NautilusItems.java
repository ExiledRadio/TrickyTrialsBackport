package com.exiledradio.trickytrialsbackport.nautilus;

import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;

/**
 * 1.21.11's item tags for the nautilus, on 1.12.2's items. The fish are 1.12.2's own ({@code fish} 0-3 and
 * {@code cooked_fish} 0-1: cod, salmon, pufferfish, tropical fish); the fish buckets are Update Aquatic's, looked up by
 * name, so without it there are none.
 */
public final class NautilusItems {

    /** 1.21.11's {@code #nautilus_bucket_food}. */
    public static final String[] BUCKETS = {"pufferfish_bucket", "cod_bucket", "salmon_bucket", "tropical_fish_bucket"};

    /**
     * Update Aquatic's item by name, or null without it. The self-test swaps in stand-ins to try the paths that need
     * Update Aquatic.
     */
    public static java.util.function.Function<String, Item> uaItem = NautilusItems::updateAquatic;

    private NautilusItems() {
    }

    @javax.annotation.Nullable
    public static Item updateAquatic(String name) {
        return Owners.present(Owners.UPDATE_AQUATIC) ? Owners.item(Owners.UPDATE_AQUATIC, name) : null;
    }

    /** 1.21.11's {@code #nautilus_taming_items}: a pufferfish or a bucket of pufferfish. */
    public static boolean isTamingItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == Items.FISH && stack.getMetadata() == ItemFishFood.FishType.PUFFERFISH.getMetadata()) {
            return true;
        }
        Item bucket = uaItem.apply("pufferfish_bucket");
        return bucket != null && stack.getItem() == bucket;
    }

    /** 1.21.11's {@code #nautilus_food}: {@code #fishes} and the fish buckets. */
    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == Items.FISH || stack.getItem() == Items.COOKED_FISH || isBucketFood(stack));
    }

    public static boolean isBucketFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (String name : BUCKETS) {
            Item bucket = uaItem.apply(name);
            if (bucket != null && stack.getItem() == bucket) {
                return true;
            }
        }
        return false;
    }
}
