package com.exiledradio.trickytrialsbackport.coppergolem;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.trickytrialsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.21.9's copper chest ({@code CopperChestBlock}, {@code WeatheringCopperChestBlock}): a chest of copper, hardness 3,
 * resistance 6, copper sounds, mined with a pickaxe. The unwaxed ones oxidize; honeycomb waxes them and an axe
 * scrapes them ({@link CopperGolemEvents}). Copper chests of any stage join each other, never a wooden chest.
 *
 * <h2>Two halves, one block</h2>
 * 1.21 joins copper chests of different stages and makes both halves the least oxidized one (unwaxed when only one
 * was waxed); after that, whenever one half changes (oxidation, scraping, waxing, lightning) the other copies it.
 * 1.12.2's chest code joins only identical blocks, so the same rule keeps them identical: {@link ItemCopperChest}
 * places the least oxidized block on both sides, and {@link #neighborChanged} makes a half follow its partner. A
 * double chest ages from one half only (1.21's: not the {@code right} one) and never while anyone has it open.
 */
public class BlockCopperChest extends BlockChest implements Waterloggable {

    /** 0 unaffected to 3 oxidized. */
    public final int stage;
    public final boolean waxed;

    public BlockCopperChest(Type type, int stage, boolean waxed) {
        super(type);
        this.stage = stage;
        this.waxed = waxed;
        setHardness(3.0F);
        // 1.21's blast resistance 6: 1.12.2 stores three times this and divides by five.
        setResistance(10.0F);
        setSoundType(CopperGolemContent.COPPER_SOUND);
        setTickRandomly(!waxed && stage < 3);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return CopperGolemContent.MAP_COLORS[this.stage];
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCopperChest();
    }

    /** 1.21's {@code requiresCorrectToolForDrops}: a pickaxe, or nothing drops (1.12.2's wood material would not ask). */
    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty()) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) {
            state = getDefaultState();
        }
        return held.getItem().getHarvestLevel(held, "pickaxe", player, state) >= getHarvestLevel(state);
    }

    /** Mined as slowly by hand as any block that needs a tool (Forge would ask the wood material instead). */
    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        float hardness = state.getBlockHardness(world, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        return player.getDigSpeed(state, pos) / hardness / (canHarvestBlock(world, pos, player) ? 30.0F : 100.0F);
    }

    /** @return whether a block is any copper chest (1.21's {@code #copper_chests}) */
    public static boolean isCopperChest(Block block) {
        return block instanceof BlockCopperChest;
    }

    /** 1.12.2's rule (at most two chests side by side) for copper chests of every stage. */
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            return false;
        }
        int n = 0;
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(side);
            if (isCopperChest(world.getBlockState(p).getBlock())) {
                if (partner(world, p) != null) {
                    return false;
                }
                n++;
            }
        }
        return n <= 1;
    }

    /** @return the side the other half of this chest is on, or null for a single chest */
    @Nullable
    public static EnumFacing partnerSide(IBlockAccess world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            if (isCopperChest(world.getBlockState(pos.offset(side)).getBlock())) {
                return side;
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos partner(IBlockAccess world, BlockPos pos) {
        EnumFacing side = partnerSide(world, pos);
        return side == null ? null : pos.offset(side);
    }

    /**
     * 1.21's {@code getLeastOxidizedChestOfConnectedBlocks}: the block both halves become, the least oxidized of the
     * two, unwaxed when only one of them is waxed.
     */
    public static BlockCopperChest leastOxidized(BlockCopperChest a, BlockCopperChest b) {
        BlockCopperChest x = a;
        BlockCopperChest y = b;
        if (a.waxed != b.waxed) {
            x = unwaxed(a);
            y = unwaxed(b);
        }
        return x.stage <= y.stage ? x : y;
    }

    static BlockCopperChest unwaxed(BlockCopperChest c) {
        return c.waxed ? CopperGolemContent.CHESTS[0][c.stage] : c;
    }

    /** Makes the chest at {@code pos} (and its partner, if any) the given block, keeping facings and contents. */
    static void setChestBlock(World world, BlockPos pos, BlockCopperChest to, int flags) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != to) {
            world.setBlockState(pos, Oxidation.copy(state, to), flags);
        }
    }

    /** A half follows its partner when the partner turns into another copper chest (1.21's {@code updateShape}). */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (world.isRemote || fromPos.getY() != pos.getY() || pos.distanceSq(fromPos) != 1.0D) {
            return;
        }
        Block other = world.getBlockState(fromPos).getBlock();
        if (other != this && other instanceof BlockCopperChest && fromPos.equals(partner(world, pos))) {
            world.setBlockState(pos, Oxidation.copy(state, other), 3);
        }
    }

    /**
     * A copper chest turning into another (aging, scraping, waxing) keeps its contents and tile entity: 1.12.2 calls
     * this for every block change, and a chest's would drop everything.
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (isCopperChest(world.getBlockState(pos).getBlock())) {
            return;
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (this.waxed || !ModConfig.copperGolem.oxidation || world.isRemote) {
            return;
        }
        EnumFacing side = partnerSide(world, pos);
        EnumFacing facing = state.getValue(FACING);
        // 1.21 ages a double chest from its left half (the right one has its partner counterclockwise).
        if (side != null && side == facing.rotateYCCW()) {
            return;
        }
        if (isOpen(world, pos) || (side != null && isOpen(world, pos.offset(side)))) {
            return;
        }
        Oxidation.randomTick(world, pos, state, random);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    }

    static boolean isOpen(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityCopperChest && ((TileEntityCopperChest) te).numPlayersUsing > 0;
    }

    /** 1.21's {@code ChestBlock.isChestBlockedAt}: a block above that stops the lid, or a sitting cat. */
    public static boolean isChestBlockedAt(World world, BlockPos pos) {
        BlockPos up = pos.up();
        if (world.getBlockState(up).doesSideBlockChestOpening(world, up, EnumFacing.DOWN)) {
            return true;
        }
        for (EntityOcelot cat : world.getEntitiesWithinAABB(EntityOcelot.class,
                new AxisAlignedBB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (cat.isSitting()) {
                return true;
            }
        }
        return false;
    }
}
