package com.exiledradio.trickytrialsbackport.effects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** The self-test's way into the stream's package-private steps. Compiled only into test builds. */
public final class EffectsTestAccess {

    private EffectsTestAccess() {
    }

    public static Set<BlockPos> weave(World world, Random random, BlockPos pos) {
        return EffectEvents.weave(world, random, pos);
    }

    public static int slimesToSpawn(int cramming, int nearby, int count) {
        return EffectEvents.slimesToSpawn(cramming, nearby, count);
    }

    public static List<EntitySilverfish> infest(EntityLivingBase entity) {
        return EffectEvents.infest(entity);
    }

    public static void setInWeb(Entity entity, boolean value) {
        EffectEvents.setInWeb(entity, value);
    }

    public static int particleState(EntityLivingBase entity) {
        return EffectEvents.particleState(entity);
    }

    public static int swirlColor(EntityLivingBase entity) {
        return EffectEvents.swirlColor(entity);
    }

    public static int sentState(EntityLivingBase entity) {
        return EffectEvents.sentState(entity);
    }

    @SuppressWarnings("unchecked")
    public static int dataColor(EntityLivingBase entity) {
        try {
            Object key = ReflectionHelper.findField(EntityLivingBase.class, "POTION_EFFECTS", "field_184633_f").get(null);
            return entity.getDataManager().get((net.minecraft.network.datasync.DataParameter<Integer>) key);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
