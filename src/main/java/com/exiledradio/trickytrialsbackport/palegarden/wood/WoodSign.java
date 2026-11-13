package com.exiledradio.trickytrialsbackport.palegarden.wood;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Standing and wall signs of a {@link WoodKind}.
 *
 * <p>Both extend vanilla's sign blocks, so support, rotation, mirroring and the outline behave exactly
 * as an oak sign's. The tile is {@link Tile}, a {@link TileEntitySign} of our own class: vanilla's
 * renderer draws only vanilla's sign texture, so ours draws these, and every {@code instanceof
 * TileEntitySign} check (editing, other mods' sign dyeing) still holds.
 */
public final class WoodSign {

    private static boolean tileRegistered;

    private WoodSign() {
    }

    /** Registers {@link Tile} as {@code trickytrialsbackport:sign}, once, whichever wood asks first. */
    public static void registerTile() {
        if (!tileRegistered) {
            tileRegistered = true;
            net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(Tile.class,
                    com.exiledradio.trickytrialsbackport.common.Content.id("sign"));
        }
    }

    /** @return the wood of a sign block of ours, or null if it is not one */
    @Nullable
    public static WoodKind kindOf(Block block) {
        if (block instanceof Standing) {
            return ((Standing) block).kind;
        }
        if (block instanceof Wall) {
            return ((Wall) block).kind;
        }
        return null;
    }

    public static final class Standing extends BlockStandingSign {
        final WoodKind kind;
        Item item;

        public Standing(WoodKind kind) {
            this.kind = kind;
            setHardness(1.0F);
            setSoundType(kind.sound);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new Tile();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return this.item;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.item);
        }
    }

    public static final class Wall extends BlockWallSign {
        final WoodKind kind;
        Item item;

        public Wall(WoodKind kind) {
            this.kind = kind;
            setHardness(1.0F);
            setSoundType(kind.sound);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new Tile();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return this.item;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.item);
        }
    }

    /** The sign tile of every wood of ours; which wood it is comes from its block. */
    public static final class Tile extends TileEntitySign {
    }

    /** The sign item: vanilla's {@code ItemSign} placement with this wood's two blocks; burns for 200 ticks. */
    public static final class SignItem extends Item {
        private final Standing standing;
        private final Wall wall;
        private final int burn;

        public SignItem(Standing standing, Wall wall, int burn) {
            this.standing = standing;
            this.wall = wall;
            this.burn = burn;
            this.maxStackSize = 16;
            standing.item = this;
            wall.item = this;
        }

        @Override
        public int getItemBurnTime(ItemStack stack) {
            return this.burn;
        }

        @Override
        public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                          float hitX, float hitY, float hitZ) {
            IBlockState clicked = world.getBlockState(pos);
            boolean replaceable = clicked.getBlock().isReplaceable(world, pos);
            if (facing == EnumFacing.DOWN || !(clicked.getMaterial().isSolid() || replaceable)
                    || (replaceable && facing != EnumFacing.UP)) {
                return EnumActionResult.FAIL;
            }
            pos = pos.offset(facing);
            ItemStack stack = player.getHeldItem(hand);
            if (!player.canPlayerEdit(pos, facing, stack) || !this.standing.canPlaceBlockAt(world, pos)) {
                return EnumActionResult.FAIL;
            }
            if (world.isRemote) {
                return EnumActionResult.SUCCESS;
            }
            pos = replaceable ? pos.down() : pos;
            if (facing == EnumFacing.UP) {
                int rotation = MathHelper.floor((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15;
                world.setBlockState(pos, this.standing.getDefaultState().withProperty(BlockStandingSign.ROTATION, rotation), 11);
            } else {
                world.setBlockState(pos, this.wall.getDefaultState().withProperty(BlockWallSign.FACING, facing), 11);
            }
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntitySign && !ItemBlock.setTileEntityNBT(world, player, pos, stack)) {
                player.openEditSign((TileEntitySign) tile);
            }
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
            stack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
    }
}
