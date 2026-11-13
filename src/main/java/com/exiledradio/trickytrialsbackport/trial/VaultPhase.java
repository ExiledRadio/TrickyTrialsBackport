package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.util.IStringSerializable;

/** 1.21's {@code VaultState}: the block state property {@code vault_state}, with its light. */
public enum VaultPhase implements IStringSerializable {
    INACTIVE("inactive", 6),
    ACTIVE("active", 12),
    UNLOCKING("unlocking", 12),
    EJECTING("ejecting", 12);

    private final String name;
    public final int light;

    VaultPhase(String name, int light) {
        this.name = name;
        this.light = light;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
