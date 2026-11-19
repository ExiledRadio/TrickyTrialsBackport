package com.exiledradio.trickytrialsbackport.nautilus;

/**
 * The five nautilus armors: 1.21.11's {@code ArmorMaterials} body defense, toughness and knockback resistance, and the
 * worn texture's name ({@code textures/entity/equipment/nautilus_body/<asset>.png}).
 */
public enum NautilusArmorTier {
    COPPER("copper_nautilus_armor", "copper", 4, 0.0F, 0.0F),
    IRON("iron_nautilus_armor", "iron", 5, 0.0F, 0.0F),
    GOLDEN("golden_nautilus_armor", "gold", 7, 0.0F, 0.0F),
    DIAMOND("diamond_nautilus_armor", "diamond", 11, 2.0F, 0.0F),
    NETHERITE("netherite_nautilus_armor", "netherite", 19, 3.0F, 0.1F);

    public final String itemName;
    public final String asset;
    public final int protection;
    public final float toughness;
    public final float knockbackResistance;

    NautilusArmorTier(String itemName, String asset, int protection, float toughness, float knockbackResistance) {
        this.itemName = itemName;
        this.asset = asset;
        this.protection = protection;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }
}
