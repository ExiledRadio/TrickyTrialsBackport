package com.exiledradio.trickytrialsbackport.mounts;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.biome.Biome;

/** The self-test's way into the stream's package-private parts. Never in a release jar. */
public final class MountsTestAccess {

    public static final String RIDER = ZombieHorses.RIDER;

    private MountsTestAccess() {
    }

    public static void burn(EntityZombieHorse horse) {
        ZombieHorses.burn(horse);
    }

    public static EntityZombie spawnRider(EntityZombieHorse horse) {
        return ZombieHorses.spawnRider(horse);
    }

    public static ContainerHorseChest chest(AbstractHorse horse) {
        return ZombieHorses.chest(horse);
    }

    public static boolean monsterRules(EntityLiving mob) {
        return ZombieHorses.monsterRules(mob);
    }

    public static boolean isRunAroundUnlessMobRides(Object goal) {
        return goal instanceof ZombieHorses.RunAroundUnlessMobRides;
    }

    public static boolean isTempt(Object goal) {
        return goal instanceof ZombieHorses.Tempt;
    }

    public static boolean inDaylightCheck(EntityParched parched) {
        return parched.inDaylightCheck();
    }

    public static double waterAboveFeet(Entity e) {
        return Riding.waterAboveFeet(e);
    }

    public static AxisAlignedBB attackBox(Entity e) {
        return Riding.attackBox(e);
    }

    public static Biome[] deserts() {
        return MountsContent.deserts();
    }

    public static Biome[] plains() {
        return ZombieHorses.biomes();
    }
}
