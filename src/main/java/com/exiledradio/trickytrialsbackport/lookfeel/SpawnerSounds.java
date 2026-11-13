package com.exiledradio.trickytrialsbackport.lookfeel;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.21.2: the monster spawner breaks, steps, is placed, hit and fallen on with its own sounds
 * ({@code block.spawner.*}, 1.21.11's {@code SoundType.SPAWNER}: volume 1, pitch 1) instead of stone's.
 *
 * <p>1.12.2's {@code Block.setSoundType} is protected, so the field is set by its SRG name, on both sides
 * (the server picks the sound it sends for breaking and placing).
 */
public final class SpawnerSounds {

    /** 1.21.11's {@code SoundType.SPAWNER}. */
    public static final SoundType SPAWNER = Content.soundType("spawner", 1.0F, 1.0F);

    private SpawnerSounds() {
    }

    static void apply() {
        try {
            ObfuscationReflectionHelper.setPrivateValue(net.minecraft.block.Block.class, Blocks.MOB_SPAWNER, SPAWNER, "field_149762_H");
        } catch (RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("Look and feel: could not give the spawner its 1.21.2 sounds.", e);
        }
    }
}
