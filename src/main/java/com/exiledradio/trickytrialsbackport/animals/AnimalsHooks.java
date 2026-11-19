package com.exiledradio.trickytrialsbackport.animals;

import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;

/**
 * The calls patches P16-P19 (docs/PATCHES.md) put into vanilla's classes. Each does nothing while its switch is off,
 * so 1.12.2's behavior stays.
 */
public final class AnimalsHooks {

    private AnimalsHooks() {
    }

    /** P16, before {@code EntityLiving.onInitialSpawn} returns. */
    public static void initialSpawn(EntityLiving entity) {
        if (ModConfig.animals.enabled && FarmAnimals.carries(entity)) {
            FarmAnimals.initialSpawn(entity);
        }
    }

    /** P17, before {@code EntitySheep.onInitialSpawn} returns (after 1.12.2 has picked its color). */
    public static void sheepSpawn(EntitySheep sheep) {
        FarmAnimals.sheepSpawn(sheep);
    }

    /** P18, at the head of {@code EntityAnimal.readEntityFromNBT}. */
    public static void read(EntityAnimal animal, NBTTagCompound tag) {
        if (ModConfig.animals.enabled && FarmAnimals.carries(animal)) {
            FarmAnimals.read(animal, tag);
        }
    }

    /** P18, at the head of {@code EntityAnimal.writeEntityToNBT}. */
    public static void write(EntityAnimal animal, NBTTagCompound tag) {
        if (ModConfig.animals.enabled && FarmAnimals.carries(animal)) {
            FarmAnimals.write(animal, tag);
        }
    }

    /** P19, on what {@code EntityWolf.getAmbientSound}, {@code getHurtSound} and {@code getDeathSound} return. */
    public static SoundEvent wolfSound(SoundEvent sound, EntityWolf wolf) {
        if (!ModConfig.animals.enabled || !ModConfig.animals.wolfSoundVariants) {
            return sound;
        }
        return FarmAnimals.voice(wolf).swap(sound);
    }
}
