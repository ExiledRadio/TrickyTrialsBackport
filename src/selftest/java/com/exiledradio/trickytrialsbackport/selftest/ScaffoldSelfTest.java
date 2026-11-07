package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.common.Owners;

import net.minecraft.util.math.BlockPos;

/** The shared code every stream builds on. */
public final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) {
        BlockPos area = check.area();
        check.ok(check.world.isAirBlock(area.up()), "test area is clear");

        // The ownership shim: Caves & Cliffs owns 1.17's copper and tuff, and without it we copy them.
        // Caves & Cliffs, or the pseudo-owner it names for a stream it has switched off.
        check.ok(String.valueOf(BlockOwnership.owner("tuff")).startsWith(Owners.CAVES_AND_CLIFFS), "tuff's owner");
        check.ok(String.valueOf(BlockOwnership.owner("waxed_weathered_cut_copper_slab")).startsWith(Owners.CAVES_AND_CLIFFS),
                "waxed cut copper slab's owner");
        boolean cc = Owners.present(Owners.CAVES_AND_CLIFFS);
        check.ok(cc || Owners.copy("tuff"), "copy tuff without Caves & Cliffs");
        check.ok(cc || Owners.copy("copper_block"), "copy copper without Caves & Cliffs");
        check.eq(true, Owners.copy("polished_tuff"), "an unclaimed block is always ours to register");
    }
}
