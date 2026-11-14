package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.trial.ModernLoot;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The chambers' chests, barrels, dispensers and decorated pots name 1.21's loot tables ({@code LootTable} in their
 * block entity data). Backport Core's palette renames each to ours, {@code trickytrialsbackport:chambers/<path>}, an
 * empty 1.12.2 table that this fills at load with one entry: a roll of vanilla's own file through the trials
 * stream's {@link ModernLoot}, which reads 1.21's functions and looks every item up when the container is opened.
 */
public final class ChamberLoot {

    private static final LootCondition[] NONE = new LootCondition[0];
    /** Our 1.12.2 table names to 1.21's. */
    static final Map<ResourceLocation, String> TABLES = new HashMap<ResourceLocation, String>();

    ChamberLoot() {
    }

    /** Pre-init: the palette's renames, from the list the stream's generator wrote. */
    static void preInit() {
        for (JsonElement e : ChamberSettings.readArray("worldgen/trial_chambers_loot.json")) {
            String path = e.getAsString();
            ResourceLocation ours = Content.id("chambers/" + path);
            TABLES.put(ours, "minecraft:" + path);
            ModernPalette.lootTable("minecraft:" + path, ours);
        }
    }

    @SubscribeEvent
    public void load(LootTableLoadEvent event) {
        String modern = TABLES.get(event.getName());
        if (modern != null) {
            event.getTable().addPool(new LootPool(new LootEntry[] {new Modern(modern)}, NONE, new RandomValueRange(1), new RandomValueRange(0),
                    Content.MOD_ID + ":chambers"));
        }
    }

    /** One entry that adds a whole roll of a 1.21 table. */
    static final class Modern extends LootEntry {
        final String table;

        Modern(String table) {
            super(1, 0, NONE, table);
            this.table = table;
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
            if (context.getWorld() instanceof WorldServer) {
                stacks.addAll(ModernLoot.roll((WorldServer) context.getWorld(), this.table, rand, context.getLuck()));
            }
        }

        @Override
        protected void serialize(JsonObject json, JsonSerializationContext context) {
            json.addProperty("table", this.table);
        }
    }
}
