package com.exiledradio.trickytrialsbackport.palegarden.boat;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Places a boat of a {@link BoatWood}: vanilla's {@code ItemBoat} placement
 * (a 5-block look ray that sees water, refused where an entity is in the way or the boat would not fit).
 * Stacks to 1; burns 1200 ticks as vanilla's boats.
 */
public class ItemModBoat extends Item {

    private final BoatWood wood;
    public ItemModBoat(BoatWood wood) {
        this.wood = wood;
        this.maxStackSize = 1;
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return 1200;
    }

    /** @return the boat this item would place at a point, facing like the player */
    public EntityModBoat create(World world, double x, double y, double z, float yaw) {
        EntityModBoat boat = new EntityModBoat(world, x, y, z, this.wood);
        boat.rotationYaw = yaw;
        return boat;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch);
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw);
        Vec3d eye = new Vec3d(player.prevPosX + (player.posX - player.prevPosX),
                player.prevPosY + (player.posY - player.prevPosY) + player.getEyeHeight(),
                player.prevPosZ + (player.posZ - player.prevPosZ));
        float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-pitch * 0.017453292F);
        Vec3d end = eye.add(sinYaw * cosPitch * 5.0D, sinPitch * 5.0D, cosYaw * cosPitch * 5.0D);
        RayTraceResult hit = world.rayTraceBlocks(eye, end, true);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        }
        Vec3d look = player.getLook(1.0F);
        List<Entity> near = world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().expand(look.x * 5.0D, look.y * 5.0D, look.z * 5.0D).grow(1.0D));
        for (Entity e : near) {
            if (e.canBeCollidedWith()) {
                AxisAlignedBB box = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
                if (box.contains(eye)) {
                    return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
                }
            }
        }
        Block block = world.getBlockState(hit.getBlockPos()).getBlock();
        boolean water = block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        EntityModBoat boat = create(world, hit.hitVec.x, water ? hit.hitVec.y - 0.12D : hit.hitVec.y, hit.hitVec.z, player.rotationYaw);
        if (!world.getCollisionBoxes(boat, boat.getEntityBoundingBox().grow(-0.1D)).isEmpty()) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        if (!world.isRemote) {
            world.spawnEntity(boat);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
