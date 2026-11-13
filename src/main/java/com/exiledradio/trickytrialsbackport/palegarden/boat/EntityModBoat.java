package com.exiledradio.trickytrialsbackport.palegarden.boat;

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A boat of one of this mod's woods: vanilla's boat in every way - rowing, water, ice, passengers,
 * breaking - with its own wood, item and look.
 *
 * <ul>
 *   <li>The wood is synced data ({@link #WOOD}) and saved as {@code Wood} (the {@link BoatWood} name), so a
 *       later move onto Backport Core's shared boat registry is a rename.</li>
 *   <li>It drops its own item when broken, and its own planks (not oak) when it crashes from a fall.</li>
 * </ul>
 */
public class EntityModBoat extends EntityBoat {

    private static final DataParameter<Integer> WOOD = EntityDataManager.createKey(EntityModBoat.class, DataSerializers.VARINT);

    public EntityModBoat(World world) {
        super(world);
    }

    public EntityModBoat(World world, double x, double y, double z, BoatWood wood) {
        super(world, x, y, z);
        setWood(wood);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(WOOD, 0);
    }

    public BoatWood wood() {
        return BoatWood.byIndex(this.dataManager.get(WOOD));
    }

    public void setWood(BoatWood wood) {
        this.dataManager.set(WOOD, wood.ordinal());
    }

    @Override
    public Item getItemBoat() {
        Item item = BoatContent.item(wood());
        return item != null ? item : super.getItemBoat();
    }

    /** Vanilla's fall crash drops oak planks by metadata; ours drops this wood's planks instead. */
    @Nullable
    @Override
    public EntityItem entityDropItem(ItemStack stack, float offsetY) {
        if (!stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(Blocks.PLANKS)) {
            ItemStack planks = wood().planks(stack.getCount());
            if (planks != null) {
                stack = planks;
            }
        }
        return super.entityDropItem(stack, offsetY);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("Wood", wood().name);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        BoatWood wood = BoatWood.byName(tag.getString("Wood"));
        if (wood != null) {
            setWood(wood);
        }
    }

    @Override
    public ItemStack getPickedResult(net.minecraft.util.math.RayTraceResult target) {
        return new ItemStack(getItemBoat());
    }
}
