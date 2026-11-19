package com.exiledradio.trickytrialsbackport.mounts;

import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * The calls patch P24a adds to {@code EntityZombieHorse} (docs/PATCHES.md): 1.21.11's zombie horse wears horse armor,
 * despawns while untamed, and is tamed, fed and equipped as {@code ZombieHorse.mobInteract} does. While
 * {@code mounts.zombieHorses} (or the stream) is off every hook answers 1.12.2's way.
 */
public final class MountsHooks {

    /** {@link #zombieHorseInteract}: run 1.12.2's own {@code processInteract}. */
    public static final int VANILLA = 0;
    /** {@link #zombieHorseInteract}: return true. */
    public static final int CONSUMED = 1;
    /** {@link #zombieHorseInteract}: return false. */
    public static final int PASS = 2;

    private static final Method EATING = ReflectionHelper.findMethod(AbstractHorse.class, "eatingHorse", "func_110266_cB");

    private MountsHooks() {
    }

    public static boolean zombieHorses() {
        return MountsContent.enabled() && ModConfig.mounts.zombieHorses;
    }

    /** {@code wearsArmor()}: 1.21.11's zombie horse has a body slot ({@code canUseSlot} is true for every slot). */
    public static boolean zombieHorseWearsArmor() {
        return zombieHorses();
    }

    /** {@code isArmor(stack)}: any horse armor, as a horse's. */
    public static boolean zombieHorseIsArmor(ItemStack stack) {
        return zombieHorses() && HorseArmorType.isHorseArmor(stack);
    }

    /** {@code canDespawn()}: 1.21.11's {@code removeWhenFarAway} is true, and a tamed horse is persistent. */
    public static boolean zombieHorseCanDespawn(EntityZombieHorse horse) {
        return zombieHorses() && !horse.isTame() && !horse.getLeashed();
    }

    /** @return true if this item is 1.21.11's {@code #zombie_horse_food} (a red mushroom) */
    public static boolean isZombieHorseFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ZombieHorses.RED_MUSHROOM.get();
    }

    /**
     * 1.21.11's {@code ZombieHorse.mobInteract} (with {@code Entity.interact} making it persistent, and
     * {@code AbstractHorse.mobInteract} after it), in 1.12.2's terms.
     *
     * @return {@link #VANILLA}, {@link #CONSUMED} or {@link #PASS}
     */
    public static int zombieHorseInteract(EntityZombieHorse horse, EntityPlayer player, EnumHand hand) {
        if (!zombieHorses()) {
            return VANILLA;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() == Items.SPAWN_EGG) {
            return VANILLA;
        }
        horse.enablePersistence();
        boolean server = !horse.world.isRemote;
        boolean sneakOpens = !horse.isChild() && horse.isTame() && player.isSneaking();
        if (!horse.isBeingRidden() && !sneakOpens && !stack.isEmpty()) {
            if (isZombieHorseFood(stack)) {
                if (!server) {
                    return CONSUMED;
                }
                if (eat(horse)) {
                    if (!player.capabilities.isCreativeMode) {
                        stack.shrink(1);
                    }
                    return CONSUMED;
                }
                return PASS;
            }
            if (!horse.isTame()) {
                horse.makeMad();
                return CONSUMED;
            }
        }
        // AbstractHorse.mobInteract
        if (horse.isBeingRidden() || horse.isChild()) {
            return PASS;
        }
        if (horse.isTame() && player.isSneaking()) {
            horse.openGUI(player);
            return CONSUMED;
        }
        if (!stack.isEmpty()) {
            if (stack.interactWithEntity(player, horse, hand)) {
                return CONSUMED;
            }
            ContainerHorseChest chest = ZombieHorses.chest(horse);
            if (horse.isTame() && chest != null) {
                if (stack.getItem() == Items.SADDLE && !horse.isHorseSaddled()) {
                    if (server) {
                        equip(horse, chest, 0, stack, player);
                        horse.world.playSound(null, horse.posX, horse.posY, horse.posZ, SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.NEUTRAL, 0.5F, 1.0F);
                    }
                    return CONSUMED;
                }
                if (HorseArmorType.isHorseArmor(stack) && chest.getStackInSlot(1).isEmpty()) {
                    if (server) {
                        equip(horse, chest, 1, stack, player);
                        horse.world.playSound(null, horse.posX, horse.posY, horse.posZ, SoundEvents.ENTITY_HORSE_ARMOR, SoundCategory.NEUTRAL, 0.5F, 1.0F);
                    }
                    return CONSUMED;
                }
            }
        }
        if (server) {
            player.rotationYaw = horse.rotationYaw;
            player.rotationPitch = horse.rotationPitch;
            horse.setEatingHaystack(false);
            horse.setRearing(false);
            player.startRiding(horse);
        }
        return CONSUMED;
    }

    private static void equip(EntityZombieHorse horse, ContainerHorseChest chest, int slot, ItemStack stack, EntityPlayer player) {
        ItemStack one = stack.copy();
        one.setCount(1);
        chest.setInventorySlotContents(slot, one);
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        ZombieHorses.syncArmor(horse);
    }

    /** 1.21.11's {@code AbstractHorse.handleEating} for a red mushroom: 3 health, no growth, 3 temper. Server only. */
    static boolean eat(AbstractHorse horse) {
        boolean ate = false;
        if (horse.getHealth() < horse.getMaxHealth()) {
            horse.heal(3.0F);
            ate = true;
        }
        if ((ate || !horse.isTame()) && horse.getTemper() < horse.getMaxTemper()) {
            horse.increaseTemper(3);
            ate = true;
        }
        if (ate) {
            try {
                EATING.invoke(horse);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return ate;
    }
}
