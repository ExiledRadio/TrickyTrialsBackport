package com.exiledradio.trickytrialsbackport.spear;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * Lunge I-III (1.21.11): spears only; weight 5 (uncommon), anvil cost 2, enchanting cost 5 + 8 a level above the
 * first to 25 + 8, at the table and on books, with no exclusive set. Its effect is {@link SpearAttacks#lunge}.
 */
public final class EnchantmentLunge extends Enchantment {

    EnchantmentLunge(EnumEnchantmentType type) {
        super(Rarity.UNCOMMON, type, new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        setRegistryName(Content.id("lunge"));
        setName(Content.MOD_ID + ".lunge");
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 25 + (level - 1) * 8;
    }
}
