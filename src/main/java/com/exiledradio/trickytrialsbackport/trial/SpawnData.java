package com.exiledradio.trickytrialsbackport.trial;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.21's {@code SpawnData}: the entity to spawn (its NBT, 1.21 ids allowed), optional light limits, and
 * optional equipment from a loot table, with the chance each slot drops its item.
 */
public final class SpawnData {

    /** The entity's NBT, as written (its {@code id} may be a 1.21 name). */
    final NBTTagCompound entity;
    @Nullable
    final String equipmentTable;
    /** Per slot (EntityEquipmentSlot ordinal), the drop chance of equipment put there. */
    final float[] dropChances;
    final int[] blockLight;
    final int[] skyLight;

    SpawnData(NBTTagCompound entity, @Nullable String equipmentTable, float[] dropChances, @Nullable int[] blockLight,
              @Nullable int[] skyLight) {
        this.entity = entity;
        this.equipmentTable = equipmentTable;
        this.dropChances = dropChances;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    /** An empty spawn: nothing to spawn. */
    static SpawnData empty() {
        return new SpawnData(new NBTTagCompound(), null, null, null, null);
    }

    static SpawnData fromJson(JsonObject data) {
        NBTTagCompound entity = data.has("entity") ? Json.toNbt(data.getAsJsonObject("entity")) : new NBTTagCompound();
        String table = null;
        float[] chances = null;
        if (data.has("equipment")) {
            JsonElement eq = data.get("equipment");
            if (eq.isJsonPrimitive()) {
                table = eq.getAsString();
            } else {
                JsonObject o = eq.getAsJsonObject();
                table = o.get("loot_table").getAsString();
                chances = chances(o.get("slot_drop_chances"));
            }
        }
        int[] block = null;
        int[] sky = null;
        if (data.has("custom_spawn_rules")) {
            JsonObject rules = data.getAsJsonObject("custom_spawn_rules");
            block = range(rules.get("block_light_limit"));
            sky = range(rules.get("sky_light_limit"));
        }
        return new SpawnData(entity, table, chances, block, sky);
    }

    @Nullable
    private static float[] chances(@Nullable JsonElement e) {
        if (e == null) {
            return null;
        }
        float[] out = new float[EntityEquipmentSlot.values().length];
        if (e.isJsonPrimitive()) {
            java.util.Arrays.fill(out, e.getAsFloat());
            return out;
        }
        java.util.Arrays.fill(out, 0.085F);
        for (java.util.Map.Entry<String, JsonElement> s : e.getAsJsonObject().entrySet()) {
            EntityEquipmentSlot slot = slot(s.getKey());
            if (slot != null) {
                out[slot.ordinal()] = s.getValue().getAsFloat();
            }
        }
        return out;
    }

    @Nullable
    private static EntityEquipmentSlot slot(String name) {
        if ("body".equals(name)) {
            return null;
        }
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    private static int[] range(@Nullable JsonElement e) {
        if (e == null) {
            return null;
        }
        if (e.isJsonPrimitive()) {
            return new int[] {0, e.getAsInt()};
        }
        if (e.isJsonArray()) {
            return new int[] {e.getAsJsonArray().get(0).getAsInt(), e.getAsJsonArray().get(1).getAsInt()};
        }
        JsonObject o = e.getAsJsonObject();
        return new int[] {o.has("min_inclusive") ? o.get("min_inclusive").getAsInt() : 0,
                o.has("max_inclusive") ? o.get("max_inclusive").getAsInt() : 15};
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("entity", this.entity.copy());
        if (this.equipmentTable != null) {
            NBTTagCompound eq = new NBTTagCompound();
            eq.setString("loot_table", this.equipmentTable);
            if (this.dropChances != null) {
                NBTTagCompound chances = new NBTTagCompound();
                for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) {
                    chances.setFloat(s.getName(), this.dropChances[s.ordinal()]);
                }
                eq.setTag("slot_drop_chances", chances);
            }
            tag.setTag("equipment", eq);
        }
        if (this.blockLight != null || this.skyLight != null) {
            NBTTagCompound rules = new NBTTagCompound();
            if (this.blockLight != null) {
                rules.setIntArray("block_light_limit", this.blockLight);
            }
            if (this.skyLight != null) {
                rules.setIntArray("sky_light_limit", this.skyLight);
            }
            tag.setTag("custom_spawn_rules", rules);
        }
        return tag;
    }

    static SpawnData read(NBTTagCompound tag) {
        return fromJson(Json.toJson(tag).getAsJsonObject());
    }

    /** @return the entity's id in this game, or null when it has none or this game lacks the mob */
    @Nullable
    ResourceLocation entityId() {
        String id = this.entity.getString("id");
        return id.isEmpty() ? null : ModernIds.entity(id);
    }

    boolean hasId() {
        return !this.entity.getString("id").isEmpty();
    }

    /** As 1.21: "only an id" means the mob is finalized like a natural spawn (its random gear). */
    boolean onlyId() {
        return this.entity.getSize() == 1 && hasId();
    }

    /** @return the entity, made but not yet in the world, or null when this game can't make it */
    @Nullable
    Entity create(World world) {
        ResourceLocation id = entityId();
        if (id == null) {
            return null;
        }
        NBTTagCompound tag = this.entity.copy();
        tag.setString("id", id.toString());
        return EntityList.createEntityFromNBT(tag, world);
    }

    boolean lightAllows(World world, BlockPos pos) {
        if (this.blockLight != null) {
            int light = world.getLightFor(EnumSkyBlock.BLOCK, pos);
            if (light < this.blockLight[0] || light > this.blockLight[1]) {
                return false;
            }
        }
        if (this.skyLight != null) {
            int light = world.getLightFor(EnumSkyBlock.SKY, pos);
            return light >= this.skyLight[0] && light <= this.skyLight[1];
        }
        return true;
    }

    /** 1.21's {@code Mob.equip(EquipmentTable)}: each item into the slot it belongs in. */
    void equip(EntityLiving mob) {
        if (this.equipmentTable == null || !(mob.world instanceof WorldServer)) {
            return;
        }
        List<ItemStack> items = ModernLoot.roll((WorldServer) mob.world, this.equipmentTable, mob.getRNG(), 0.0F);
        for (ItemStack stack : items) {
            EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
            mob.setItemStackToSlot(slot, stack);
            if (this.dropChances != null) {
                mob.setDropChance(slot, this.dropChances[slot.ordinal()]);
            }
        }
    }
}
