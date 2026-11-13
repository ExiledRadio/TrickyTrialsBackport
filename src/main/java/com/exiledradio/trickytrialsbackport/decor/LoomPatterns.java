package com.exiledradio.trickytrialsbackport.decor;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The four banner pattern items, and Village &amp; Pillage's loom.
 *
 * <h2>With Village &amp; Pillage</h2>
 * Its loom's pattern slot takes only its own {@code ItemBannerPattern}, so our four items are made from that class,
 * by name ({@link #item}); its loom then reads the pattern from the item as it does for the creeper charge. Its loom
 * lists every enum pattern as freely chosen except the ones it knows need an item, which would put Flow and Guster
 * on the free list, and 1.21.2 moved bricks and curly border behind their items too. {@link #arrangeLoom} moves those
 * four from the free part of its list to the part that needs an item ({@code docs/CORE_REQUESTS.md} Decor-3 asks for a
 * call to do this instead). Every step is checked; when the loom's code is not what this expects, nothing is changed
 * and the patterns stay free there.
 *
 * <h2>Without it</h2>
 * Our own {@link ItemPattern}: the four go into 1.12.2's crafting-grid banner recipe as a pattern's item, and stay in
 * the grid, as 1.14 made patterns reusable ({@link #gridItems}). Bricks and curly border then need their 1.21.2 items
 * there instead of a brick block and vines.
 */
final class LoomPatterns {

    static final String VP_ITEM = "com.exiledradio.villagerbackport.item.ItemBannerPattern";
    static final String VP_ORDER = "com.exiledradio.villagerbackport.block.BannerPatterns";

    private LoomPatterns() {
    }

    static boolean villageAndPillage() {
        return Owners.present(Owners.VILLAGE_AND_PILLAGE);
    }

    /**
     * @param description the design's name in Village &amp; Pillage's English fallback, or null for an item with no
     *                    description line (1.21.2's two)
     */
    static Item item(@Nullable BannerPattern pattern, @Nullable String description, EnumRarity rarity) {
        if (pattern != null && villageAndPillage()) {
            try {
                Constructor<?> make = Class.forName(VP_ITEM).getConstructor(BannerPattern.class, String.class);
                return (Item) make.newInstance(pattern, description == null ? "" : description);
            } catch (Exception | LinkageError e) {
                TrickyTrialsBackport.LOGGER.warn("Village & Pillage's banner pattern item is not as expected; "
                        + "our patterns go into the crafting grid instead.", e);
            }
        }
        return new ItemPattern(pattern, description != null, rarity);
    }

    /** Without Village &amp; Pillage's items, ours become the grid recipe's pattern items. */
    static void gridItems() {
        grid(DecorContent.flow, DecorContent.flowBannerPattern);
        grid(DecorContent.guster, DecorContent.gusterBannerPattern);
        grid(BannerPattern.BRICKS, DecorContent.fieldMasonedBannerPattern);
        grid(BannerPattern.CURLY_BORDER, DecorContent.bordureIndentedBannerPattern);
    }

    private static void grid(@Nullable BannerPattern pattern, Item item) {
        if (pattern != null && item instanceof ItemPattern) {
            Patterns.setGridItem(pattern, new ItemStack(item));
        }
    }

    /** @return the patterns that need an item in 1.21's loom and are ours to move */
    static List<BannerPattern> needItems() {
        List<BannerPattern> out = new ArrayList<BannerPattern>();
        for (BannerPattern p : new BannerPattern[] {BannerPattern.BRICKS, BannerPattern.CURLY_BORDER, DecorContent.flow, DecorContent.guster}) {
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Moves {@link #needItems} out of Village &amp; Pillage's free patterns. Post-init, before any loom is used. */
    static void arrangeLoom() {
        if (!villageAndPillage() || !(DecorContent.flowBannerPattern != null
                && DecorContent.flowBannerPattern.getClass().getName().equals(VP_ITEM))) {
            return;
        }
        try {
            Class<?> order = Class.forName(VP_ORDER, true, LoomPatterns.class.getClassLoader());
            Field listField = order.getDeclaredField("ORDER");
            Field freeField = order.getDeclaredField("FREE_COUNT");
            listField.setAccessible(true);
            freeField.setAccessible(true);
            BannerPattern[] list = (BannerPattern[]) listField.get(null);
            int free = freeField.getInt(null);
            List<BannerPattern> move = new ArrayList<BannerPattern>();
            for (BannerPattern p : needItems()) {
                int at = Arrays.asList(list).indexOf(p);
                if (at > 0 && at <= free) {
                    move.add(p);
                }
            }
            if (move.isEmpty()) {
                return;
            }
            List<BannerPattern> arranged = new ArrayList<BannerPattern>();
            for (int i = 0; i <= free; i++) {
                if (!move.contains(list[i])) {
                    arranged.add(list[i]);
                }
            }
            arranged.addAll(move);
            for (int i = free + 1; i < list.length; i++) {
                arranged.add(list[i]);
            }
            int newFree = free - move.size();
            // The count first: if it cannot be set, the list is left as it was.
            if (!setStaticInt(freeField, newFree)) {
                TrickyTrialsBackport.LOGGER.warn("Could not rearrange Village & Pillage's loom; Flow, Guster, bricks "
                        + "and curly border stay freely chosen there.");
                return;
            }
            for (int i = 0; i < list.length; i++) {
                list[i] = arranged.get(i);
            }
            TrickyTrialsBackport.LOGGER.info("Village & Pillage's loom: {} now need their pattern items.", move);
        } catch (Exception | LinkageError e) {
            TrickyTrialsBackport.LOGGER.warn("Village & Pillage's loom is not as expected; Flow, Guster, bricks and "
                    + "curly border stay freely chosen there.", e);
        }
    }

    /** Writes a static final int (its class has just been initialized, so no code has read it yet). */
    private static boolean setStaticInt(Field field, int value) {
        try {
            Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            Class<?> u = unsafe.getClass();
            Object base = u.getMethod("staticFieldBase", Field.class).invoke(unsafe, field);
            long offset = (Long) u.getMethod("staticFieldOffset", Field.class).invoke(unsafe, field);
            u.getMethod("putIntVolatile", Object.class, long.class, int.class).invoke(unsafe, base, offset, value);
            return field.getInt(null) == value;
        } catch (Exception | LinkageError e) {
            TrickyTrialsBackport.LOGGER.debug("Unsafe unavailable", e);
            return false;
        }
    }
}
