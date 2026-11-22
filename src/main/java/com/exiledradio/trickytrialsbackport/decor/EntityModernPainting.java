package com.exiledradio.trickytrialsbackport.decor;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * A painting of 1.21's ({@link Paintings}): vanilla's painting in every respect but its picture, which 1.12.2's fixed
 * painting atlas cannot hold. It hangs, breaks, drops a painting and sounds as 1.12.2's does; the variant is saved
 * as {@code variant} (its name) and sent with the spawn packet. A custom name is kept as 1.21's and 26.3's are
 * ({@link PaintingNames}).
 */
public class EntityModernPainting extends EntityHanging implements IEntityAdditionalSpawnData {

    private Paintings.Variant variant = Paintings.byIndex(0);

    public EntityModernPainting(World world) {
        super(world);
    }

    public EntityModernPainting(World world, BlockPos pos, EnumFacing facing, Paintings.Variant variant) {
        super(world, pos);
        this.variant = variant;
        updateFacingWithBoundingBox(facing);
    }

    public Paintings.Variant getVariant() {
        return this.variant;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.setString("variant", this.variant.name);
        super.writeEntityToNBT(compound);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        Paintings.Variant read = Paintings.byName(PaintingItems.bare(compound.getString("variant")));
        this.variant = read != null ? read : Paintings.byIndex(0);
        super.readEntityFromNBT(compound);
    }

    @Override
    public int getWidthPixels() {
        return this.variant.width * 16;
    }

    @Override
    public int getHeightPixels() {
        return this.variant.height * 16;
    }

    @Override
    public void onBroken(@Nullable Entity brokenEntity) {
        if (this.world.getGameRules().getBoolean("doEntityDrops")) {
            this.playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
            if (brokenEntity instanceof EntityPlayer && ((EntityPlayer) brokenEntity).capabilities.isCreativeMode) {
                return;
            }
            this.entityDropItem(PaintingNames.drop(this), 0.0F);
        }
    }

    @Override
    public void playPlaceSound() {
        this.playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        PaintingItems.Entry entry = PaintingItems.byName(this.variant.name);
        return entry != null ? entry.stack() : new ItemStack(Items.PAINTING);
    }

    @Override
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        BlockPos pos = this.hangingPosition.add(x - this.posX, y - this.posY, z - this.posZ);
        this.setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeLong(this.hangingPosition.toLong());
        buffer.writeByte(this.facingDirection == null ? EnumFacing.SOUTH.getHorizontalIndex() : this.facingDirection.getHorizontalIndex());
        buffer.writeByte(this.variant.index);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.hangingPosition = BlockPos.fromLong(buffer.readLong());
        EnumFacing facing = EnumFacing.byHorizontalIndex(buffer.readByte());
        this.variant = Paintings.byIndex(buffer.readByte());
        updateFacingWithBoundingBox(facing);
    }
}
