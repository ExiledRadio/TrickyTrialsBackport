package com.exiledradio.trickytrialsbackport.coppergolem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A copper chest's contents and lid: 1.12.2's chest, with the copper chest's open and close sounds for its stage
 * (1.21.9), its block's name, and copper golems counted among those who have it open.
 */
public class TileEntityCopperChest extends TileEntityChest {

    private int ticksSinceSync;
    /** Copper golems that have this chest open now (1.21's {@code ContainerUser}s that are not players). */
    int golemsUsing;

    @Override
    public String getName() {
        return hasCustomName() ? super.getName() : getBlockType().getTranslationKey() + ".name";
    }

    /** Aging or waxing swaps the block under the chest: the contents stay (1.21's {@code shouldChangedStateKeepBlockEntity}). */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return !(newState.getBlock() instanceof BlockCopperChest);
    }

    int stage() {
        return getBlockType() instanceof BlockCopperChest ? ((BlockCopperChest) getBlockType()).stage : 0;
    }

    /** 1.12.2's chest update, with copper sounds and golems kept in the periodic recount of openers. */
    @Override
    public void update() {
        checkForAdjacentChests();
        int i = this.pos.getX();
        int j = this.pos.getY();
        int k = this.pos.getZ();
        ++this.ticksSinceSync;
        if (!this.world.isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + i + j + k) % 200 == 0) {
            this.numPlayersUsing = this.golemsUsing;
            for (EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class,
                    new AxisAlignedBB(i - 5.0F, j - 5.0F, k - 5.0F, i + 1 + 5.0F, j + 1 + 5.0F, k + 1 + 5.0F))) {
                if (player.openContainer instanceof ContainerChest) {
                    IInventory inv = ((ContainerChest) player.openContainer).getLowerChestInventory();
                    if (inv == this || inv instanceof InventoryLargeChest && ((InventoryLargeChest) inv).isPartOfLargeChest(this)) {
                        ++this.numPlayersUsing;
                    }
                }
            }
        }
        this.prevLidAngle = this.lidAngle;
        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
            play(CopperGolemContent.CHEST_OPEN[stage()]);
        }
        if (this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F) {
            float before = this.lidAngle;
            this.lidAngle += this.numPlayersUsing > 0 ? 0.1F : -0.1F;
            if (this.lidAngle > 1.0F) {
                this.lidAngle = 1.0F;
            }
            if (this.lidAngle < 0.5F && before >= 0.5F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
                play(CopperGolemContent.CHEST_CLOSE[stage()]);
            }
            if (this.lidAngle < 0.0F) {
                this.lidAngle = 0.0F;
            }
        }
    }

    private void play(SoundEvent sound) {
        double x = this.pos.getX() + 0.5D;
        double z = this.pos.getZ() + 0.5D;
        if (this.adjacentChestZPos != null) {
            z += 0.5D;
        }
        if (this.adjacentChestXPos != null) {
            x += 0.5D;
        }
        this.world.playSound(null, x, this.pos.getY() + 0.5D, z, sound, SoundCategory.BLOCKS, 0.5F,
                this.world.rand.nextFloat() * 0.1F + 0.9F);
    }
}
