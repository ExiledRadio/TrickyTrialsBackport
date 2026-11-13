package com.exiledradio.trickytrialsbackport.trial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.UUID;

/** JSON and NBT, both ways, as 1.21's codecs read the same data from either. */
final class Json {

    private Json() {
    }

    static NBTTagCompound toNbt(JsonObject json) {
        try {
            return JsonToNBT.getTagFromJson(json.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad data " + json, e);
        }
    }

    static JsonElement toJson(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            JsonObject o = new JsonObject();
            NBTTagCompound c = (NBTTagCompound) tag;
            for (String key : c.getKeySet()) {
                o.add(key, toJson(c.getTag(key)));
            }
            return o;
        }
        if (tag instanceof NBTTagList) {
            JsonArray a = new JsonArray();
            NBTTagList list = (NBTTagList) tag;
            for (int i = 0; i < list.tagCount(); i++) {
                a.add(toJson(list.get(i)));
            }
            return a;
        }
        if (tag instanceof NBTTagString) {
            return new JsonPrimitive(((NBTTagString) tag).getString());
        }
        if (tag instanceof NBTTagIntArray) {
            JsonArray a = new JsonArray();
            for (int v : ((NBTTagIntArray) tag).getIntArray()) {
                a.add(new JsonPrimitive(v));
            }
            return a;
        }
        if (tag instanceof NBTTagByteArray) {
            JsonArray a = new JsonArray();
            for (byte v : ((NBTTagByteArray) tag).getByteArray()) {
                a.add(new JsonPrimitive(v));
            }
            return a;
        }
        if (tag instanceof NBTPrimitive) {
            NBTPrimitive p = (NBTPrimitive) tag;
            switch (tag.getId()) {
                case 5:
                    return new JsonPrimitive(p.getFloat());
                case 6:
                    return new JsonPrimitive(p.getDouble());
                case 4:
                    return new JsonPrimitive(p.getLong());
                default:
                    return new JsonPrimitive(p.getInt());
            }
        }
        return new JsonPrimitive(tag.toString());
    }

    /** 1.16+'s UUID as four ints, most significant first. */
    static NBTTagIntArray uuid(UUID id) {
        long m = id.getMostSignificantBits();
        long l = id.getLeastSignificantBits();
        return new NBTTagIntArray(new int[] {(int) (m >> 32), (int) m, (int) (l >> 32), (int) l});
    }

    static UUID uuid(NBTBase tag) {
        int[] a = ((NBTTagIntArray) tag).getIntArray();
        return new UUID((long) a[0] << 32 | a[1] & 0xFFFFFFFFL, (long) a[2] << 32 | a[3] & 0xFFFFFFFFL);
    }

    static NBTTagList uuids(Iterable<UUID> ids) {
        NBTTagList list = new NBTTagList();
        for (UUID id : ids) {
            list.appendTag(uuid(id));
        }
        return list;
    }

    static void readUuids(NBTTagCompound tag, String key, java.util.Collection<UUID> into) {
        into.clear();
        NBTTagList list = tag.getTagList(key, 11);
        for (int i = 0; i < list.tagCount(); i++) {
            into.add(uuid(list.get(i)));
        }
    }
}
