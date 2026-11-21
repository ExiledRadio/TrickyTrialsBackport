package com.exiledradio.trickytrialsbackport.decor;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Painting items that carry their variant (1.19.4): the creative inventory lists the random painting, then one painting
 * per placeable variant (1.12.2's 26 and our 21), ordered as 1.21.11's {@code CreativeModeTabs} orders them, by height
 * then width. The variant is kept as 1.19.4 kept it, {@code EntityTag:{variant:"minecraft:<name>"}} (1.21.5 moved the
 * same id into its {@code painting/variant} component). Such a painting hangs exactly that picture where it fits and
 * fails elsewhere; pick-block on a hung painting gives its variant. A painting without a variant stays random.
 */
public final class PaintingItems {

    /** One placeable painting: 1.12.2's art or one of ours. */
    public static final class Entry {
        public final String name;
        public final int width;
        public final int height;
        @Nullable
        public final EntityPainting.EnumArt art;
        @Nullable
        public final Paintings.Variant variant;

        Entry(String name, int width, int height, @Nullable EntityPainting.EnumArt art, @Nullable Paintings.Variant variant) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.art = art;
            this.variant = variant;
        }

        /** The painting this entry hangs at {@code pos} (the block in front of the wall), sized for its picture. */
        public EntityHanging create(World world, BlockPos pos, EnumFacing facing) {
            if (this.variant != null) {
                return new EntityModernPainting(world, pos, facing, this.variant);
            }
            EntityPainting painting = new EntityPainting(world, pos, facing);
            painting.art = this.art;
            DecorEvents.setFacing(painting, facing);
            return painting;
        }

        public ItemStack stack() {
            ItemStack stack = new ItemStack(Items.PAINTING);
            NBTTagCompound entity = new NBTTagCompound();
            entity.setString("variant", "minecraft:" + this.name);
            stack.setTagInfo("EntityTag", entity);
            return stack;
        }
    }

    private static final List<Entry> ALL = new ArrayList<Entry>();

    static {
        for (EntityPainting.EnumArt art : EntityPainting.EnumArt.values()) {
            ALL.add(new Entry(artName(art), art.sizeX / 16, art.sizeY / 16, art, null));
        }
        for (Paintings.Variant v : Paintings.all()) {
            ALL.add(new Entry(v.name, v.width, v.height, null, v));
        }
        // List.sort is stable: equal sizes keep the placeable tag's order, 1.12.2's paintings first.
        Collections.sort(ALL, Comparator.<Entry>comparingInt(e -> e.height).thenComparingInt(e -> e.width));
    }

    private PaintingItems() {
    }

    /** Every placeable painting, in creative order. */
    public static List<Entry> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** 1.12.2's art title as its modern id: {@code SkullAndRoses} becomes {@code skull_and_roses}. */
    public static String artName(EntityPainting.EnumArt art) {
        return art.title.replaceAll("(?<=.)([A-Z])", "_$1").toLowerCase(Locale.ROOT);
    }

    /** A variant id without its {@code minecraft:} namespace. */
    public static String bare(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    @Nullable
    public static Entry byName(String id) {
        String name = bare(id);
        for (Entry e : ALL) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return null;
    }

    /** The variant a painting item carries, or null for a random painting (no variant, or one we do not know). */
    @Nullable
    public static Entry of(ItemStack stack) {
        if (stack.getItem() != Items.PAINTING) {
            return null;
        }
        NBTTagCompound entity = stack.getSubCompound("EntityTag");
        return entity == null || !entity.hasKey("variant", 8) ? null : byName(entity.getString("variant"));
    }

    /**
     * Patch P26 (docs/PATCHES.md): {@code ItemHangingEntity.getSubItems}, after 1.12.2's own entry. Adds the variant
     * paintings after the random one.
     */
    public static void subItems(Item item, CreativeTabs tab, NonNullList<ItemStack> items) {
        if (item != Items.PAINTING || !DecorContent.enabled() || !inTab(item, tab)) {
            return;
        }
        for (Entry e : ALL) {
            items.add(e.stack());
        }
    }

    private static boolean inTab(Item item, CreativeTabs tab) {
        for (CreativeTabs t : item.getCreativeTabs()) {
            if (t == tab) {
                return true;
            }
        }
        return item.getCreativeTab() != null && tab == CreativeTabs.SEARCH;
    }

    /**
     * Patch P27 (docs/PATCHES.md): the head of {@code Entity.getPickedResult}. A vanilla painting gives its variant;
     * null runs 1.12.2's body (a plain painting).
     */
    @Nullable
    public static ItemStack pickedResult(Entity entity) {
        if (!DecorContent.enabled() || !(entity instanceof EntityPainting) || ((EntityPainting) entity).art == null) {
            return null;
        }
        Entry e = byName(artName(((EntityPainting) entity).art));
        return e == null ? null : e.stack();
    }
}
