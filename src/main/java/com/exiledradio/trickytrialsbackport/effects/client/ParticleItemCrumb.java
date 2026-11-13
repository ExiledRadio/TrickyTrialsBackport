package com.exiledradio.trickytrialsbackport.effects.client;

import net.minecraft.client.particle.ParticleBreaking;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/** 1.21's {@code BreakingItemParticle} without a set speed ({@code item_cobweb}, {@code item_slime}). */
final class ParticleItemCrumb extends ParticleBreaking {

    ParticleItemCrumb(World world, double x, double y, double z, Item item) {
        super(world, x, y, z, item);
    }
}
