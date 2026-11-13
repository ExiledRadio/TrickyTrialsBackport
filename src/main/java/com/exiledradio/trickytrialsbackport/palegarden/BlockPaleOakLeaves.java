package com.exiledradio.trickytrialsbackport.palegarden;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pale oak leaves.
 *
 * <h2>1.21.4's rules where 1.12.2's differ</h2>
 * <ul>
 *   <li><b>Decay distance 7.</b> 1.12.2's leaves decay unless a log is within 4; these decay only when no
 *       log is reachable through leaves within 6 steps (modern {@code distance=7}). A pale oak's canopy
 *       reaches 4 from its branches, so either rule keeps it; the modern one is kept for player builds.</li>
 *   <li><b>Drops</b> ({@code blocks/pale_oak_leaves}): a sapling at 5% (6.25%, 8.33%, 10% with Fortune),
 *       1-2 sticks at 2% (2.2%, 2.5%, 3.3%, 10%), no apples; itself with shears or Silk Touch.</li>
 *   <li><b>Falling pale leaves</b> (client), untinted: leaves are never colored by the biome.</li>
 * </ul>
 * Fancy and fast graphics follow vanilla's own leaves, which the game switches with the option.
 */
public class BlockPaleOakLeaves extends BlockLeaves {

    private static final float[] SAPLING = {0.05F, 0.0625F, 0.083333336F, 0.1F};
    private static final float[] STICKS = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    private Item sapling;

    public BlockPaleOakLeaves() {
        setSoundType(SoundType.PLANT);
        setDefaultState(this.blockState.getBaseState().withProperty(CHECK_DECAY, true).withProperty(DECAYABLE, true));
    }

    void setSapling(Item sapling) {
        this.sapling = sapling;
    }

    /** 1.21.4: {@code MapColor.METAL}, 1.12.2's iron. */
    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.block.material.MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return net.minecraft.block.material.MapColor.IRON;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CHECK_DECAY, DECAYABLE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DECAYABLE, (meta & 4) == 0).withProperty(CHECK_DECAY, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (!state.getValue(DECAYABLE)) {
            meta |= 4;
        }
        if (state.getValue(CHECK_DECAY)) {
            meta |= 8;
        }
        return meta;
    }

    /** Placed by a player: never decays (1.20's {@code persistent=true}). */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(DECAYABLE, false).withProperty(CHECK_DECAY, false);
    }

    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {
        // Only vanilla's own leaf code asks, and never for this block.
        return BlockPlanks.EnumType.OAK;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.sapling;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int f = Math.max(0, fortune);
        if (this.sapling != null && rand.nextFloat() < SAPLING[Math.min(f, SAPLING.length - 1)]) {
            drops.add(new ItemStack(this.sapling));
        }
        if (rand.nextFloat() < STICKS[Math.min(f, STICKS.length - 1)]) {
            drops.add(new ItemStack(Items.STICK, 1 + rand.nextInt(2)));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Decay

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote || !state.getValue(CHECK_DECAY) || !state.getValue(DECAYABLE)) {
            return;
        }
        if (!world.isAreaLoaded(pos, 7)) {
            return;
        }
        if (reachesLog(world, pos)) {
            world.setBlockState(pos, state.withProperty(CHECK_DECAY, false), 4);
        } else {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    /** @return true if a log is reachable from here through leaves in at most 6 steps (1.20's distance < 7) */
    static boolean reachesLog(World world, BlockPos start) {
        Set<BlockPos> seen = new HashSet<BlockPos>();
        Deque<BlockPos> frontier = new ArrayDeque<BlockPos>();
        frontier.add(start);
        seen.add(start);
        for (int step = 1; step <= 6 && !frontier.isEmpty(); step++) {
            List<BlockPos> next = new ArrayList<BlockPos>();
            for (BlockPos at : frontier) {
                for (EnumFacing face : EnumFacing.values()) {
                    BlockPos n = at.offset(face);
                    if (!seen.add(n)) {
                        continue;
                    }
                    IBlockState s = world.getBlockState(n);
                    Block b = s.getBlock();
                    if (b.canSustainLeaves(s, world, n)) {
                        return true;
                    }
                    if (b.isLeaves(s, world, n)) {
                        next.add(n);
                    }
                }
            }
            frontier.clear();
            frontier.addAll(next);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Looks: follow vanilla's leaves, whose fancy flag the game sets from the graphics option.

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return Blocks.LEAVES.isOpaqueCube(Blocks.LEAVES.getDefaultState());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return Blocks.LEAVES.getRenderLayer();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("deprecation")
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (!Blocks.LEAVES.isOpaqueCube(Blocks.LEAVES.getDefaultState())) {
            // Fancy: draw every face, as vanilla's fancy leaves do (neighbors of the same kind included).
            IBlockState other = world.getBlockState(pos.offset(side));
            return !other.doesSideBlockRendering(world, pos.offset(side), side.getOpposite());
        }
        return super.shouldSideBeRendered(state, world, pos, side);
    }

    /**
     * 1.21.4's {@code UntintedParticleLeavesBlock(0.02F, PALE_OAK_LEAVES)}: vanilla's drips, then a pale leaf
     * one time in fifty when nothing under the block has a full top face.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        super.randomDisplayTick(state, world, pos, rand);
        if (rand.nextFloat() < 0.02F) {
            BlockPos below = pos.down();
            IBlockState under = world.getBlockState(below);
            if (under.getBlockFaceShape(world, below, EnumFacing.UP) != net.minecraft.block.state.BlockFaceShape.SOLID) {
                com.exiledradio.trickytrialsbackport.palegarden.client.PaleGardenClient.leaf(world, pos, rand);
            }
        }
    }
}
