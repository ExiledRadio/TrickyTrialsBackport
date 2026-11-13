package com.exiledradio.trickytrialsbackport.palegarden.boat;

import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The woods of this mod that float. Pale oak now; a later wood (26.3's poplar) adds itself here. The order
 * is in synced data only; saves keep the name.
 */
public enum BoatWood {
    PALE_OAK("pale_oak", "pale_oak_planks");

    public final String name;
    private final String planks;

    BoatWood(String name, String planks) {
        this.name = name;
        this.planks = planks;
    }

    /** "pale_oak_boat" */
    public String itemName() {
        return this.name + "_boat";
    }

    @Nullable
    public ItemStack planks(int count) {
        Block b = Owners.ourBlock(this.planks);
        return b == null ? null : new ItemStack(b, count);
    }

    public static BoatWood byIndex(int i) {
        BoatWood[] all = values();
        return i >= 0 && i < all.length ? all[i] : PALE_OAK;
    }

    @Nullable
    public static BoatWood byName(String name) {
        for (BoatWood w : values()) {
            if (w.name.equals(name)) {
                return w;
            }
        }
        return null;
    }
}
