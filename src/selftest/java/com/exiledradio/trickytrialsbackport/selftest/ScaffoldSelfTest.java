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
        check.eq(Owners.CAVES_AND_CLIFFS, BlockOwnership.owner("tuff"), "tuff's owner");
        check.eq(Owners.CAVES_AND_CLIFFS, BlockOwnership.owner("waxed_weathered_cut_copper_slab"), "waxed cut copper slab's owner");
        boolean cc = Owners.present(Owners.CAVES_AND_CLIFFS);
        check.eq(!cc, Owners.copy("tuff"), "copy tuff only without Caves & Cliffs");
        check.eq(!cc, Owners.copy("copper_block"), "copy copper only without Caves & Cliffs");
        check.eq(true, Owners.copy("polished_tuff"), "an unclaimed block is always ours to register");
    }
}
