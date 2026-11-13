package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/** A mob's equipment drop chances, which 1.12.2 keeps private: hands and armor, by slot index. */
final class DropChances {

    private DropChances() {
    }

    @Nullable
    static float[] hands(EntityLiving mob) {
        return read(mob, "field_82174_bp");
    }

    @Nullable
    static float[] armor(EntityLiving mob) {
        return read(mob, "field_184655_bs");
    }

    @Nullable
    private static float[] read(EntityLiving mob, String srg) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(EntityLiving.class, mob, srg);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
