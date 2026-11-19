package com.exiledradio.trickytrialsbackport.shelf;

import net.minecraft.util.IStringSerializable;

/**
 * 1.21.9's {@code SideChainPart}: where a powered shelf sits in its row. A shelf connected to its right is the
 * row's {@link #LEFT} end, one connected to its left the {@link #RIGHT} end, one connected both ways the
 * {@link #CENTER}.
 */
public enum SideChainPart implements IStringSerializable {
    UNCONNECTED("unconnected"),
    RIGHT("right"),
    CENTER("center"),
    LEFT("left");

    private final String name;

    SideChainPart(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isConnected() {
        return this != UNCONNECTED;
    }

    public boolean isConnectionTowards(SideChainPart side) {
        return this == CENTER || this == side;
    }

    public boolean isChainEnd() {
        return this != CENTER;
    }

    public SideChainPart whenConnectedToTheRight() {
        return this == UNCONNECTED || this == LEFT ? LEFT : CENTER;
    }

    public SideChainPart whenConnectedToTheLeft() {
        return this == UNCONNECTED || this == RIGHT ? RIGHT : CENTER;
    }

    public SideChainPart whenDisconnectedFromTheRight() {
        return this == UNCONNECTED || this == LEFT ? UNCONNECTED : RIGHT;
    }

    public SideChainPart whenDisconnectedFromTheLeft() {
        return this == UNCONNECTED || this == RIGHT ? UNCONNECTED : LEFT;
    }

    /** @return the part with this name, or {@link #UNCONNECTED} */
    public static SideChainPart byName(String name) {
        for (SideChainPart part : values()) {
            if (part.name.equals(name)) {
                return part;
            }
        }
        return UNCONNECTED;
    }
}
