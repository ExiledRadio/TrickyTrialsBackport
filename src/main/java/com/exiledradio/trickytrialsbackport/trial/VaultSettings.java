package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 1.21's {@code VaultConfig}: the loot table a vault gives, the key it takes, and the ranges at which it
 * lights up (4) and goes out (4.5) for players nearby. Stored in the vault's NBT as {@code config}, with
 * vanilla's field names; {@code key_item} may be written in 1.21's form ({@code {id, count}}) with a 1.21
 * item name.
 */
public final class VaultSettings {

    public static final String REWARD = "minecraft:chests/trial_chambers/reward";
    public static final String REWARD_OMINOUS = "minecraft:chests/trial_chambers/reward_ominous";

    final String lootTable;
    final double activationRange;
    final double deactivationRange;
    final ItemStack keyItem;
    @Nullable
    final String overrideLootTableToDisplay;

    public VaultSettings(String lootTable, double activationRange, double deactivationRange, ItemStack keyItem,
                         @Nullable String overrideLootTableToDisplay) {
        this.lootTable = lootTable;
        this.activationRange = activationRange;
        // 1.21 rejects an activation range above the deactivation range.
        this.deactivationRange = Math.max(activationRange, deactivationRange);
        this.keyItem = keyItem;
        this.overrideLootTableToDisplay = overrideLootTableToDisplay;
    }

    /** A vault as players place it, and the normal vault of trial chambers: a trial key for the reward table. */
    public static VaultSettings normal() {
        return new VaultSettings(REWARD, 4.0, 4.5, key(TrialContent.trialKey), null);
    }

    /** The ominous vault of trial chambers: an ominous trial key for the ominous reward table. */
    public static VaultSettings ominous() {
        return new VaultSettings(REWARD_OMINOUS, 4.0, 4.5, key(TrialContent.ominousTrialKey), null);
    }

    /** @return true for the ominous vault's settings: the ominous trial key and the ominous reward table */
    boolean isOminousReward() {
        return REWARD_OMINOUS.equals(this.lootTable) && !this.keyItem.isEmpty() && this.keyItem.getItem() == TrialContent.ominousTrialKey;
    }

    private static ItemStack key(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("loot_table", this.lootTable);
        tag.setDouble("activation_range", this.activationRange);
        tag.setDouble("deactivation_range", this.deactivationRange);
        if (!this.keyItem.isEmpty()) {
            tag.setTag("key_item", this.keyItem.writeToNBT(new NBTTagCompound()));
        }
        if (this.overrideLootTableToDisplay != null) {
            tag.setString("override_loot_table_to_display", this.overrideLootTableToDisplay);
        }
        return tag;
    }

    static VaultSettings read(NBTTagCompound tag) {
        VaultSettings d = normal();
        return new VaultSettings(tag.hasKey("loot_table", 8) ? tag.getString("loot_table") : d.lootTable,
                tag.hasKey("activation_range") ? tag.getDouble("activation_range") : d.activationRange,
                tag.hasKey("deactivation_range") ? tag.getDouble("deactivation_range") : d.deactivationRange,
                tag.hasKey("key_item", 10) ? stack(tag.getCompoundTag("key_item")) : d.keyItem,
                tag.hasKey("override_loot_table_to_display", 8) ? tag.getString("override_loot_table_to_display") : null);
    }

    /** A stack in 1.12.2's form ({@code Count}), or 1.21's ({@code count}, a 1.21 name). */
    static ItemStack stack(NBTTagCompound tag) {
        if (tag.hasKey("Count")) {
            ItemStack stack = new ItemStack(tag);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        ItemStack stack = ModernIds.stack(new ResourceLocation(tag.getString("id")).toString());
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        stack.setCount(tag.hasKey("count") ? Math.max(1, tag.getInteger("count")) : 1);
        return stack;
    }
}
