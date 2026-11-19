package com.exiledradio.trickytrialsbackport.coppergolem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.21.9's {@code CopperGolemStatueBlockEntity}: the name the golem had, given back to the golem an axe revives and to
 * the statue's item. Aging, waxing, scraping and posing keep it.
 */
public class TileEntityCopperGolemStatue extends net.minecraft.tileentity.TileEntity {

    @Nullable
    String customName;

    @Nullable
    public String getCustomName() {
        return this.customName;
    }

    /** 1.21's {@code createStatue}: keeps the golem's name. */
    void createStatue(EntityCopperGolem golem) {
        this.customName = golem.hasCustomName() ? golem.getCustomNameTag() : null;
        markDirty();
    }

    /**
     * 1.21's {@code removeStatue}: a copper golem at the statue's bottom center, facing its way, with its name.
     *
     * @return the golem, not yet spawned
     */
    EntityCopperGolem removeStatue(IBlockState state) {
        EntityCopperGolem golem = new EntityCopperGolem(this.world);
        if (this.customName != null) {
            golem.setCustomNameTag(this.customName);
        }
        BlockPos p = getPos();
        float yaw = state.getValue(BlockCopperGolemStatue.FACING).getHorizontalAngle();
        golem.setLocationAndAngles(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, yaw, 0.0F);
        golem.rotationYawHead = yaw;
        golem.renderYawOffset = yaw;
        golem.playSpawnSound();
        return golem;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return !(newState.getBlock() instanceof BlockCopperGolemStatue);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.customName = tag.hasKey("CustomName", 8) ? tag.getString("CustomName") : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.customName != null) {
            tag.setString("CustomName", this.customName);
        }
        return tag;
    }
}
