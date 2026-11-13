package com.exiledradio.trickytrialsbackport.trial;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Reads 1.21's own loot table files (as 1.21.11 ships them) and rolls them, for what trial spawners eject,
 * what vaults give and what ominous trial mobs wear.
 *
 * <p>1.12.2's loot tables can't name items that do not exist when they load, and the trial tables name
 * items from several of our streams and other family mods. Reading vanilla's files ourselves lets every
 * name be looked up when the table is rolled, through {@link ModernIds}: an entry whose item, potion or
 * enchantment this game doesn't have is left out of its pool, so the other entries keep their relative
 * weights.
 *
 * <h2>What is supported</h2>
 * Exactly what the trial tables use: entries {@code item}, {@code loot_table} (by name or inline) and
 * {@code empty}; the {@code random_chance} condition; the functions {@code set_count},
 * {@code set_damage}, {@code set_potion}, {@code set_enchantments}, {@code enchant_randomly},
 * {@code enchant_with_levels} and {@code set_ominous_bottle_amplifier}; numbers as constants,
 * {@code uniform} and {@code binomial}. {@code set_components} (only armor trims here) is skipped: 1.12.2's
 * armor has no trims. A table name no 1.21 file has is rolled as 1.12.2's own loot table of that name, so a
 * config may point at any loot table of the game.
 */
public final class ModernLoot {

    private ModernLoot() {
    }

    /** @return the items of a roll, split into stacks no bigger than each item allows */
    public static List<ItemStack> roll(WorldServer world, String table, Random random, float luck) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        JsonElement json = TrialData.read("loot_table", table);
        if (json == null) {
            LootTable old = world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(table));
            if (old != LootTable.EMPTY_LOOT_TABLE) {
                out.addAll(old.generateLootForPools(random, new LootContext.Builder(world).withLuck(luck).build()));
            } else {
                TrickyTrialsBackport.LOGGER.warn("No loot table {}", table);
            }
        } else {
            rollTable(json.getAsJsonObject(), random, luck, out, 0);
        }
        List<ItemStack> split = new ArrayList<ItemStack>();
        for (ItemStack stack : out) {
            while (stack.getCount() > stack.getMaxStackSize()) {
                split.add(stack.splitStack(stack.getMaxStackSize()));
            }
            if (!stack.isEmpty()) {
                split.add(stack);
            }
        }
        return split;
    }

    private static void rollTable(JsonObject table, Random random, float luck, List<ItemStack> out, int depth) {
        if (depth > 16 || !table.has("pools")) {
            return;
        }
        for (JsonElement p : table.getAsJsonArray("pools")) {
            JsonObject pool = p.getAsJsonObject();
            if (!conditions(pool, random)) {
                continue;
            }
            int rolls = intOf(pool.get("rolls"), random, 1)
                    + MathHelper.floor(floatOf(pool.get("bonus_rolls"), random, 0.0F) * luck);
            List<JsonObject> entries = new ArrayList<JsonObject>();
            List<Integer> weights = new ArrayList<Integer>();
            for (JsonElement e : pool.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                if (!available(entry)) {
                    continue;
                }
                int weight = Math.max(MathHelper.floor(intOf(entry.get("weight"), random, 1) + intOf(entry.get("quality"), random, 0) * luck), 0);
                entries.add(entry);
                weights.add(weight);
            }
            for (int r = 0; r < rolls; r++) {
                List<JsonObject> live = new ArrayList<JsonObject>();
                List<Integer> liveWeights = new ArrayList<Integer>();
                int liveTotal = 0;
                for (int i = 0; i < entries.size(); i++) {
                    if (conditions(entries.get(i), random)) {
                        live.add(entries.get(i));
                        liveWeights.add(weights.get(i));
                        liveTotal += weights.get(i);
                    }
                }
                if (live.isEmpty() || liveTotal <= 0 && live.size() > 1) {
                    continue;
                }
                JsonObject chosen = live.get(0);
                if (live.size() > 1) {
                    int pick = random.nextInt(liveTotal);
                    for (int i = 0; i < live.size(); i++) {
                        pick -= liveWeights.get(i);
                        if (pick < 0) {
                            chosen = live.get(i);
                            break;
                        }
                    }
                }
                expand(chosen, random, luck, out, depth);
            }
        }
    }

    /** An item entry whose item (or potion, or enchantment) this game lacks takes no part in its pool. */
    private static boolean available(JsonObject entry) {
        String type = string(entry, "type", "minecraft:item");
        if (!type.endsWith(":item") && !"item".equals(type)) {
            return true;
        }
        if (ModernIds.stack(string(entry, "name", "minecraft:air")) == null) {
            return false;
        }
        if (entry.has("functions")) {
            for (JsonElement f : entry.getAsJsonArray("functions")) {
                JsonObject fn = f.getAsJsonObject();
                String name = string(fn, "function", "");
                if (name.endsWith("set_potion") && potionOf(string(fn, "id", "")) == null) {
                    return false;
                }
                if (name.endsWith("set_enchantments") && fn.has("enchantments")) {
                    for (Map.Entry<String, JsonElement> e : fn.getAsJsonObject("enchantments").entrySet()) {
                        if (ModernIds.enchantment(e.getKey()) == null) {
                            return false;
                        }
                    }
                }
                if (name.endsWith("enchant_randomly") && fn.has("options") && fn.get("options").isJsonArray()) {
                    boolean any = false;
                    for (JsonElement o : fn.getAsJsonArray("options")) {
                        any |= ModernIds.enchantment(o.getAsString()) != null;
                    }
                    if (!any) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void expand(JsonObject entry, Random random, float luck, List<ItemStack> out, int depth) {
        String type = string(entry, "type", "minecraft:item");
        if (type.endsWith("loot_table")) {
            JsonElement value = entry.has("value") ? entry.get("value") : entry.get("name");
            List<ItemStack> sub = new ArrayList<ItemStack>();
            if (value.isJsonObject()) {
                rollTable(value.getAsJsonObject(), random, luck, sub, depth + 1);
            } else {
                JsonElement json = TrialData.read("loot_table", value.getAsString());
                if (json != null) {
                    rollTable(json.getAsJsonObject(), random, luck, sub, depth + 1);
                }
            }
            for (ItemStack s : sub) {
                ItemStack done = functions(entry, s, random);
                if (done != null && !done.isEmpty()) {
                    out.add(done);
                }
            }
        } else if (type.endsWith(":item") || "item".equals(type)) {
            ItemStack stack = ModernIds.stack(string(entry, "name", "minecraft:air"));
            if (stack != null) {
                stack = functions(entry, stack, random);
                if (stack != null && !stack.isEmpty()) {
                    out.add(stack);
                }
            }
        }
    }

    @Nullable
    private static ItemStack functions(JsonObject holder, ItemStack stack, Random random) {
        if (!holder.has("functions")) {
            return stack;
        }
        for (JsonElement f : holder.getAsJsonArray("functions")) {
            JsonObject fn = f.getAsJsonObject();
            if (!conditions(fn, random)) {
                continue;
            }
            stack = function(fn, stack, random);
            if (stack == null) {
                return null;
            }
        }
        return stack;
    }

    @Nullable
    private static ItemStack function(JsonObject fn, ItemStack stack, Random random) {
        String name = string(fn, "function", "");
        name = name.substring(name.indexOf(':') + 1);
        if ("set_count".equals(name)) {
            int count = intOf(fn.get("count"), random, 1);
            stack.setCount(fn.has("add") && fn.get("add").getAsBoolean() ? stack.getCount() + count : count);
        } else if ("set_damage".equals(name)) {
            if (stack.isItemStackDamageable()) {
                float fraction = MathHelper.clamp(floatOf(fn.get("damage"), random, 1.0F), 0.0F, 1.0F);
                stack.setItemDamage(MathHelper.floor((1.0F - fraction) * stack.getMaxDamage()));
            }
        } else if ("set_potion".equals(name)) {
            return withPotion(stack, string(fn, "id", ""));
        } else if ("set_ominous_bottle_amplifier".equals(name)) {
            stack.setItemDamage(MathHelper.clamp(intOf(fn.get("amplifier"), random, 0), 0, 4));
        } else if ("set_enchantments".equals(name)) {
            List<EnchantmentData> list = new ArrayList<EnchantmentData>();
            for (Map.Entry<String, JsonElement> e : fn.getAsJsonObject("enchantments").entrySet()) {
                Enchantment ench = ModernIds.enchantment(e.getKey());
                int level = intOf(e.getValue(), random, 1);
                if (ench != null && level > 0) {
                    list.add(new EnchantmentData(ench, level));
                }
            }
            return enchant(stack, list);
        } else if ("enchant_randomly".equals(name)) {
            List<Enchantment> options = new ArrayList<Enchantment>();
            if (fn.has("options") && fn.get("options").isJsonArray()) {
                for (JsonElement o : fn.getAsJsonArray("options")) {
                    Enchantment e = ModernIds.enchantment(o.getAsString());
                    if (e != null && (stack.getItem() == Items.BOOK || e.canApply(stack))) {
                        options.add(e);
                    }
                }
            } else {
                for (Enchantment e : Enchantment.REGISTRY) {
                    if (!e.isTreasureEnchantment() && (stack.getItem() == Items.BOOK || e.canApply(stack))) {
                        options.add(e);
                    }
                }
            }
            if (!options.isEmpty()) {
                Enchantment e = options.get(random.nextInt(options.size()));
                int level = MathHelper.getInt(random, e.getMinLevel(), e.getMaxLevel());
                return enchant(stack, Collections.singletonList(new EnchantmentData(e, level)));
            }
        } else if ("enchant_with_levels".equals(name)) {
            // 1.21's #on_random_loot: every enchantment an enchanting table can give, as 1.12.2 picks them.
            return EnchantmentHelper.addRandomEnchantment(random, stack, intOf(fn.get("levels"), random, 1), false);
        }
        return stack;
    }

    private static ItemStack enchant(ItemStack stack, List<EnchantmentData> list) {
        if (stack.getItem() == Items.BOOK || stack.getItem() == Items.ENCHANTED_BOOK) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, stack.getCount());
            for (EnchantmentData d : list) {
                ItemEnchantedBook.addEnchantment(book, d);
            }
            return book;
        }
        for (EnchantmentData d : list) {
            stack.addEnchantment(d.enchantment, d.enchantmentLevel);
        }
        return stack;
    }

    // ------------------------------------------------------------------------------------------
    // Potions
    // ------------------------------------------------------------------------------------------

    /** A potion 1.21 has and 1.12.2 lacks, as its effects. */
    @Nullable
    private static List<PotionEffect> effectsOnly(String id) {
        if ("minecraft:strong_slowness".equals(id)) {
            // 1.13's Slowness IV, 20 seconds.
            return Collections.singletonList(new PotionEffect(MobEffects.SLOWNESS, 400, 3));
        }
        return null;
    }

    /** @return a potion type, or the effects list (wrapped) for a potion only 1.21 has; null when neither */
    @Nullable
    private static Object potionOf(String id) {
        PotionType type = ModernIds.potion(id);
        if (type != null) {
            return type;
        }
        return effectsOnly(id);
    }

    /** 1.21 gives any arrow a potion; 1.12.2 makes that a tipped arrow. */
    @Nullable
    @SuppressWarnings("unchecked")
    static ItemStack withPotion(ItemStack stack, String id) {
        Object potion = potionOf(id);
        if (potion == null) {
            return null;
        }
        if (stack.getItem() == Items.ARROW) {
            stack = new ItemStack(Items.TIPPED_ARROW, stack.getCount());
        }
        if (potion instanceof PotionType) {
            return PotionUtils.addPotionToItemStack(stack, (PotionType) potion);
        }
        List<PotionEffect> effects = (List<PotionEffect>) potion;
        PotionUtils.appendEffects(stack, effects);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.setInteger("CustomPotionColor", PotionUtils.getPotionColorFromEffectList(effects));
        }
        return stack;
    }

    // ------------------------------------------------------------------------------------------
    // Conditions and numbers
    // ------------------------------------------------------------------------------------------

    private static boolean conditions(JsonObject holder, Random random) {
        if (!holder.has("conditions")) {
            return true;
        }
        for (JsonElement c : holder.getAsJsonArray("conditions")) {
            JsonObject cond = c.getAsJsonObject();
            String name = string(cond, "condition", "");
            if (name.endsWith("random_chance")) {
                if (random.nextFloat() >= floatOf(cond.get("chance"), random, 1.0F)) {
                    return false;
                }
            } else {
                TrickyTrialsBackport.LOGGER.warn("Loot condition {} is not supported; treated as failed", name);
                return false;
            }
        }
        return true;
    }

    static int intOf(@Nullable JsonElement e, Random random, int fallback) {
        if (e == null) {
            return fallback;
        }
        if (e.isJsonPrimitive()) {
            return MathHelper.floor(e.getAsFloat());
        }
        JsonObject o = e.getAsJsonObject();
        String type = string(o, "type", "minecraft:uniform");
        if (type.endsWith("constant")) {
            return MathHelper.floor(o.get("value").getAsFloat());
        }
        if (type.endsWith("binomial")) {
            int n = intOf(o.get("n"), random, 0);
            float p = floatOf(o.get("p"), random, 0.5F);
            int k = 0;
            for (int i = 0; i < n; i++) {
                if (random.nextFloat() < p) {
                    k++;
                }
            }
            return k;
        }
        return MathHelper.getInt(random, intOf(o.get("min"), random, 0), intOf(o.get("max"), random, 0));
    }

    static float floatOf(@Nullable JsonElement e, Random random, float fallback) {
        if (e == null) {
            return fallback;
        }
        if (e.isJsonPrimitive()) {
            return e.getAsFloat();
        }
        JsonObject o = e.getAsJsonObject();
        String type = string(o, "type", "minecraft:uniform");
        if (type.endsWith("constant")) {
            return o.get("value").getAsFloat();
        }
        if (type.endsWith("binomial")) {
            return intOf(e, random, 0);
        }
        return MathHelper.nextFloat(random, floatOf(o.get("min"), random, 0.0F), floatOf(o.get("max"), random, 0.0F));
    }

    static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
    }
}
