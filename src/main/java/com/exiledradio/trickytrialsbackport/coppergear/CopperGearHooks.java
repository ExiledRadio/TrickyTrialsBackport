package com.exiledradio.trickytrialsbackport.coppergear;

import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Patch P16 ({@code docs/PATCHES.md}): the armor a mob spawns with, as 1.21.9's {@code Mob.populateDefaultEquipmentSlots}.
 *
 * <p>Called at the head of {@code EntityLiving.setEquipmentBasedOnDifficulty}, which zombies, husks, zombie villagers,
 * skeletons, strays and bogged call first (their weapons come after, unchanged). 1.21.9:
 * <pre>
 * if (random &lt; 0.15 * clampedAdditionalDifficulty) {
 *     tier = nextInt(3)                       // leather, copper, gold (1.12.2: nextInt(2), leather or gold)
 *     three times: if (random &lt; 0.1087) tier++  // 1.12.2: 0.095
 *     stop = Hard ? 0.1 : 0.25
 *     for head, chest, legs, feet:            // 1.12.2: feet first
 *         if (not the first piece and random &lt; stop) break
 *         if the slot is empty: wear tier's piece
 * }
 * </pre>
 * Tiers 0-5: leather, copper, gold, chainmail, iron, diamond (23.60%, 32.23%, 33.29%, 9.73%, 1.10%, 0.04%).
 */
public final class CopperGearHooks {

    /** 1.21.9's {@code EQUIPMENT_POPULATION_ORDER}. */
    private static final EntityEquipmentSlot[] ORDER = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};

    private CopperGearHooks() {
    }

    /** @return true when this did the armor (and 1.12.2's own roll is skipped) */
    public static boolean equipArmor(EntityLiving mob, DifficultyInstance difficulty) {
        if (!ModConfig.copperGear.enabled || !ModConfig.copperGear.mobArmor || CopperGearContent.armorMaterial == null) {
            return false;
        }
        Random rand = mob.getRNG();
        if (rand.nextFloat() < 0.15F * difficulty.getClampedAdditionalDifficulty()) {
            int tier = rand.nextInt(3);
            for (int i = 0; i < 3; i++) {
                if (rand.nextFloat() < 0.1087F) {
                    tier++;
                }
            }
            float stop = mob.world.getDifficulty() == EnumDifficulty.HARD ? 0.1F : 0.25F;
            boolean first = true;
            for (EntityEquipmentSlot slot : ORDER) {
                if (!first && rand.nextFloat() < stop) {
                    break;
                }
                first = false;
                if (mob.getItemStackFromSlot(slot).isEmpty()) {
                    Item item = armor(slot, tier);
                    if (item != null) {
                        mob.setItemStackToSlot(slot, new ItemStack(item));
                    }
                }
            }
        }
        return true;
    }

    /** {@code Mob.getEquipmentForSlot}: the piece for a slot at a tier (0 leather ... 5 diamond). */
    @Nullable
    public static Item armor(EntityEquipmentSlot slot, int tier) {
        int s = slot == EntityEquipmentSlot.HEAD ? 0 : slot == EntityEquipmentSlot.CHEST ? 1 : slot == EntityEquipmentSlot.LEGS ? 2 : 3;
        switch (tier) {
            case 0:
                return new Item[] {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS}[s];
            case 1:
                return CopperGearContent.ITEMS.get(CopperGearContent.ARMOR[s]);
            case 2:
                return new Item[] {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS}[s];
            case 3:
                return new Item[] {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS}[s];
            case 4:
                return new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}[s];
            case 5:
                return new Item[] {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}[s];
            default:
                return null;
        }
    }
}
