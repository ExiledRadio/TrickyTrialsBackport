package com.exiledradio.trickytrialsbackport.crafter;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

/**
 * 1.21's {@code orientation}: where the crafter's front faces and which way its top points, named
 * {@code <front>_<top>} as vanilla's blockstate file names them. Twelve values, the block's metadata.
 */
public enum CrafterOrientation implements IStringSerializable {
    DOWN_EAST(EnumFacing.DOWN, EnumFacing.EAST),
    DOWN_NORTH(EnumFacing.DOWN, EnumFacing.NORTH),
    DOWN_SOUTH(EnumFacing.DOWN, EnumFacing.SOUTH),
    DOWN_WEST(EnumFacing.DOWN, EnumFacing.WEST),
    UP_EAST(EnumFacing.UP, EnumFacing.EAST),
    UP_NORTH(EnumFacing.UP, EnumFacing.NORTH),
    UP_SOUTH(EnumFacing.UP, EnumFacing.SOUTH),
    UP_WEST(EnumFacing.UP, EnumFacing.WEST),
    WEST_UP(EnumFacing.WEST, EnumFacing.UP),
    EAST_UP(EnumFacing.EAST, EnumFacing.UP),
    NORTH_UP(EnumFacing.NORTH, EnumFacing.UP),
    SOUTH_UP(EnumFacing.SOUTH, EnumFacing.UP);

    public final EnumFacing front;
    public final EnumFacing top;

    CrafterOrientation(EnumFacing front, EnumFacing top) {
        this.front = front;
        this.top = top;
    }

    @Override
    public String getName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static CrafterOrientation of(EnumFacing front, EnumFacing top) {
        for (CrafterOrientation o : values()) {
            if (o.front == front && o.top == top) {
                return o;
            }
        }
        return NORTH_UP;
    }

    /**
     * 1.21's placement: the front faces the player; looking straight up or down, the top points the way
     * they face (up) or toward them (down).
     */
    public static CrafterOrientation forPlacement(EnumFacing towardPlayer, EnumFacing playerFacing) {
        if (towardPlayer == EnumFacing.DOWN) {
            return of(EnumFacing.DOWN, playerFacing.getOpposite());
        }
        if (towardPlayer == EnumFacing.UP) {
            return of(EnumFacing.UP, playerFacing);
        }
        return of(towardPlayer, EnumFacing.UP);
    }
}
