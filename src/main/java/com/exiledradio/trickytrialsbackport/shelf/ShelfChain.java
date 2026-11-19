package com.exiledradio.trickytrialsbackport.shelf;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 1.21.9's {@code SideChainPartBlock} for shelves: powered shelves facing the same way join side by side into rows of
 * up to three, favoring the shelf on the left.
 *
 * <p>The part is kept in the shelf's tile entity ({@link TileEntityShelf#chain()}), not in metadata: facing and power
 * already take 3 of 1.12.2's 4 bits. Vanilla's {@code setBlock} of a neighbor's new part is a tile entity write
 * here, so vanilla's guard against its own {@code onPlace} re-entry ({@code isBeingUpdatedByNeighbor}) is never
 * needed: nothing re-enters.
 */
final class ShelfChain {

    static final int MAX_CHAIN_LENGTH = 3;

    private ShelfChain() {
    }

    /** {@code ShelfBlock.isConnectable}: any wooden shelf that is powered. */
    static boolean isConnectable(IBlockState state) {
        return state.getBlock() instanceof BlockShelf && state.getValue(BlockShelf.POWERED);
    }

    static SideChainPart part(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityShelf ? ((TileEntityShelf) te).chain() : SideChainPart.UNCONNECTED;
    }

    static void setPart(World world, BlockPos pos, SideChainPart part) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityShelf && ((TileEntityShelf) te).chain() != part) {
            ((TileEntityShelf) te).setChain(part);
        }
    }

    /** @return the shelves of the row this one is in, left to right; empty when it is not powered */
    static List<BlockPos> allConnectedTo(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!isConnectable(state)) {
            return Collections.emptyList();
        }
        Neighbors neighbors = new Neighbors(world, state.getValue(BlockShelf.FACING), pos);
        LinkedList<BlockPos> row = new LinkedList<BlockPos>();
        row.add(pos);
        for (int i = 1; i < MAX_CHAIN_LENGTH; i++) {
            Neighbor n = neighbors.left(i);
            if (n.connectsTowards(SideChainPart.LEFT)) {
                row.addFirst(n.pos);
            }
            if (n.isUnconnectableOrChainEnd()) {
                break;
            }
        }
        for (int i = 1; i < MAX_CHAIN_LENGTH; i++) {
            Neighbor n = neighbors.right(i);
            if (n.connectsTowards(SideChainPart.RIGHT)) {
                row.addLast(n.pos);
            }
            if (n.isUnconnectableOrChainEnd()) {
                break;
            }
        }
        return row;
    }

    static void updateNeighborsAfterPoweringDown(World world, BlockPos pos, EnumFacing facing) {
        Neighbors neighbors = new Neighbors(world, facing, pos);
        neighbors.left(1).disconnectFromRight();
        neighbors.right(1).disconnectFromLeft();
    }

    static void updateSelfAndNeighborsOnPoweringUp(World world, BlockPos pos, IBlockState state) {
        if (!isConnectable(state)) {
            return;
        }
        Neighbors neighbors = new Neighbors(world, state.getValue(BlockShelf.FACING), pos);
        SideChainPart part = SideChainPart.UNCONNECTED;
        Neighbor left = neighbors.left(1);
        Neighbor right = neighbors.right(1);
        int leftCount = left.connectable() ? allConnectedTo(world, left.pos).size() : 0;
        int rightCount = right.connectable() ? allConnectedTo(world, right.pos).size() : 0;
        int length = 1;
        if (canConnect(leftCount, length)) {
            part = part.whenConnectedToTheLeft();
            left.connectToTheRight();
            length += leftCount;
        }
        if (canConnect(rightCount, length)) {
            part = part.whenConnectedToTheRight();
            right.connectToTheLeft();
        }
        setPart(world, pos, part);
    }

    private static boolean canConnect(int count, int length) {
        return count > 0 && length + count <= MAX_CHAIN_LENGTH;
    }

    /** One place beside the shelf: a shelf of the row's kind (powered, facing the same way) or nothing. */
    private static final class Neighbor {
        final World world;
        final BlockPos pos;
        /** null: not a connectable shelf facing the same way */
        final SideChainPart part;

        Neighbor(World world, BlockPos pos, SideChainPart part) {
            this.world = world;
            this.pos = pos;
            this.part = part;
        }

        boolean connectable() {
            return this.part != null;
        }

        boolean isUnconnectableOrChainEnd() {
            return this.part == null || this.part.isChainEnd();
        }

        boolean connectsTowards(SideChainPart side) {
            return this.part != null && this.part.isConnectionTowards(side);
        }

        void connectToTheRight() {
            if (this.part != null) {
                setPart(this.world, this.pos, this.part.whenConnectedToTheRight());
            }
        }

        void connectToTheLeft() {
            if (this.part != null) {
                setPart(this.world, this.pos, this.part.whenConnectedToTheLeft());
            }
        }

        void disconnectFromRight() {
            if (this.part != null) {
                setPart(this.world, this.pos, this.part.whenDisconnectedFromTheRight());
            }
        }

        void disconnectFromLeft() {
            if (this.part != null) {
                setPart(this.world, this.pos, this.part.whenDisconnectedFromTheLeft());
            }
        }
    }

    /** Vanilla's {@code Neighbors}: left is the facing turned clockwise, right counterclockwise. */
    private static final class Neighbors {
        final World world;
        final EnumFacing facing;
        final BlockPos center;
        final Map<BlockPos, Neighbor> cache = new HashMap<BlockPos, Neighbor>();

        Neighbors(World world, EnumFacing facing, BlockPos center) {
            this.world = world;
            this.facing = facing;
            this.center = center;
        }

        Neighbor left(int distance) {
            return get(this.center.offset(this.facing.rotateY(), distance));
        }

        Neighbor right(int distance) {
            return get(this.center.offset(this.facing.rotateYCCW(), distance));
        }

        private Neighbor get(BlockPos pos) {
            Neighbor n = this.cache.get(pos);
            if (n == null) {
                IBlockState state = this.world.getBlockState(pos);
                boolean same = isConnectable(state) && state.getValue(BlockShelf.FACING) == this.facing;
                n = new Neighbor(this.world, pos, same ? part(this.world, pos) : null);
                this.cache.put(pos, n);
            }
            return n;
        }
    }
}
