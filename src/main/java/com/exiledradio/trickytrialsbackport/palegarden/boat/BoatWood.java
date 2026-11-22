package com.exiledradio.trickytrialsbackport.palegarden.boat;

import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The woods of this mod that float: pale oak. Their boats are Backport Core's
 * ({@link com.exiledradio.backportcore.boat.BoatWood}), each wood registered there as {@code trickytrialsbackport:<name>}.
 */
public enum BoatWood {
    PALE_OAK("pale_oak", "pale_oak_planks");

    public final String name;
    private final String planks;

    BoatWood(String name, String planks) {
        this.name = name;
        this.planks = planks;
    }

    /** "pale_oak_boat"; with a chest: "pale_oak_chest_boat" */
    public String itemName(boolean chest) {
        return this.name + (chest ? "_chest_boat" : "_boat");
    }

    /** @return Backport Core's wood for this one, or null when its boats are not in this game */
    @Nullable
    public com.exiledradio.backportcore.boat.BoatWood core() {
        return com.exiledradio.backportcore.boat.BoatWood.find(com.exiledradio.trickytrialsbackport.TrickyTrialsBackport.MOD_ID + ":" + this.name);
    }

    @Nullable
    public ItemStack planks(int count) {
        Block b = Owners.ourBlock(this.planks);
        return b == null ? null : new ItemStack(b, count);
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
