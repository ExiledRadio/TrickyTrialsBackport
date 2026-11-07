package com.exiledradio.trickytrialsbackport.mace;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;

/**
 * Density, Breach and Wind Burst: mace-only enchantments with 1.21.11's levels, weights (as rarities),
 * enchanting costs and exclusive set. Their effects are in {@link MaceEvents} and {@link ItemMace}.
 */
public final class EnchantmentMace extends Enchantment {

    private final int maxLevel;
    private final int minBase;
    private final int minPerLevel;
    private final int maxBase;
    private final int maxPerLevel;
    private final boolean treasure;
    /** In 1.21's {@code exclusive_set/damage}: not with Sharpness, Smite, Bane, Impaling or each other. */
    private final boolean damageSet;

    EnchantmentMace(String name, Rarity rarity, EnumEnchantmentType type, int maxLevel, int minBase, int minPerLevel, int maxBase,
                    int maxPerLevel, boolean treasure, boolean damageSet) {
        super(rarity, type, new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        setRegistryName(Content.id(name));
        setName(Content.MOD_ID + "." + name);
        this.maxLevel = maxLevel;
        this.minBase = minBase;
        this.minPerLevel = minPerLevel;
        this.maxBase = maxBase;
        this.maxPerLevel = maxPerLevel;
        this.treasure = treasure;
        this.damageSet = damageSet;
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    public int getMinEnchantability(int level) {
        return this.minBase + (level - 1) * this.minPerLevel;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return this.maxBase + (level - 1) * this.maxPerLevel;
    }

    @Override
    public boolean isTreasureEnchantment() {
        return this.treasure;
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        if (this.damageSet && inDamageSet(other)) {
            return false;
        }
        return super.canApplyTogether(other);
    }

    /** 1.21's damage set: Sharpness, Smite, Bane of Arthropods, Impaling (any mod's, by name), Density, Breach. */
    static boolean inDamageSet(Enchantment e) {
        if (e == Enchantments.SHARPNESS || e == Enchantments.SMITE || e == Enchantments.BANE_OF_ARTHROPODS) {
            return true;
        }
        if (e instanceof EnchantmentMace) {
            return ((EnchantmentMace) e).damageSet;
        }
        ResourceLocation id = e.getRegistryName();
        return id != null && "impaling".equals(id.getPath());
    }
}
