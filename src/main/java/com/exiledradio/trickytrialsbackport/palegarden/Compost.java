package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The composter is Village &amp; Pillage's 1.14 block (Rule 2). With it installed, the pale garden's items
 * compost at 1.21.4's chances ({@code ComposterBlock}): leaves, sapling, carpet and hanging moss 30%,
 * eyeblossoms and the pale moss block 65%. Reached by name at init; without it, nothing composts.
 */
public final class Compost {

    private static final Map<Item, Float> CHANCES = new LinkedHashMap<Item, Float>();

    private Compost() {
    }

    static void add(float chance, Item... items) {
        for (Item item : items) {
            if (item != null) {
                CHANCES.put(item, chance);
            }
        }
    }

    /** Init: hands the chances to Village &amp; Pillage's composter, if it is installed. */
    public static void init() {
        if (!Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) || CHANCES.isEmpty()) {
            return;
        }
        try {
            Method register = Class.forName("com.exiledradio.villagerbackport.block.Compostables")
                    .getMethod("register", float.class, Item.class);
            for (Map.Entry<Item, Float> e : CHANCES.entrySet()) {
                register.invoke(null, e.getValue(), e.getKey());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("Could not add the pale garden's items to the composter; they stay uncompostable.", e);
        }
    }
}
