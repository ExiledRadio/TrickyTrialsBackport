package com.exiledradio.trickytrialsbackport.decor;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/**
 * New banner patterns in 1.12.2's {@link BannerPattern} enum.
 *
 * <p>A constant added with {@link EnumHelper} is a pattern like any other: a banner stores it by its hash, and the
 * client draws {@code minecraft:textures/entity/banner/<file>.png} and {@code .../shield/<file>.png}, which this
 * mod ships under the {@code minecraft} domain. Nothing in 1.12.2 switches on this enum, so adding to it during
 * registration is safe (unlike the creature types, see the memory note on switch tables).
 *
 * <p>Modern banners name patterns by id ({@code minecraft:flow}); 1.12.2 needs a hash, and 1.21's patterns never
 * had one, so ours are {@code flw} and {@code gus}. A pattern another mod already added with the same file name is
 * reused rather than doubled.
 */
final class Patterns {

    private Patterns() {
    }

    @Nullable
    static BannerPattern add(String constant, String file, String hash) {
        for (BannerPattern pattern : BannerPattern.values()) {
            if (constant.equals(pattern.name()) || file.equals(fileName(pattern))) {
                return pattern;
            }
        }
        try {
            return EnumHelper.addEnum(BannerPattern.class, constant, new Class<?>[] {String.class, String.class}, file, hash);
        } catch (RuntimeException e) {
            TrickyTrialsBackport.LOGGER.error("Could not add the {} banner pattern; it will be missing.", file, e);
            return null;
        }
    }

    /** {@code getFileName} is client-only; the field is not. */
    static String fileName(BannerPattern pattern) {
        return ObfuscationReflectionHelper.getPrivateValue(BannerPattern.class, pattern, "field_191014_N");
    }

    /** Sets the item a crafting-grid banner recipe needs for this pattern (1.12.2's creeper head, say). */
    static void setGridItem(BannerPattern pattern, ItemStack item) {
        ObfuscationReflectionHelper.setPrivateValue(BannerPattern.class, pattern, item, "field_191017_Q");
    }
}
