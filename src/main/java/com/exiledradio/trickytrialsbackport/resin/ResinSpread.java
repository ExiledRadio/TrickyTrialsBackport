package com.exiledradio.trickytrialsbackport.resin;

import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Resin from a hurt creaking: the resin half of 1.21.4's {@code CreakingHeartBlockEntity.creakingHurt}.
 *
 * <h2>For the creaking (stream 13)</h2>
 * The creaking calls {@link #creakingHurt(TileEntity)} by name ({@value #CLASS}), with vanilla types only, at the
 * moment vanilla does: when a player hurts a creaking linked to this heart and the heart's emitter is 0 (the same
 * moment it sends the 20 trail particles and sets the emitter to 100). The particles and the emitter are the
 * creaking's; the resin is here. It is safe to call with the resin stream off (nothing happens) and with any tile.
 */
public final class ResinSpread {

    /** This class, for the creaking's lookup by name. */
    public static final String CLASS = "com.exiledradio.trickytrialsbackport.resin.ResinSpread";

    private ResinSpread() {
    }

    /**
     * When the heart is awake, 2-3 times: a face of resin on a pale oak log near it, with the resin place sound.
     *
     * @param heart the creaking heart's tile (on the server)
     * @return how many faces of resin were placed
     */
    public static int creakingHurt(@Nullable TileEntity heart) {
        if (heart == null || heart.getWorld() == null || heart.getWorld().isRemote || !BlockResinClump.registered()) {
            return 0;
        }
        World world = heart.getWorld();
        IBlockState state = world.getBlockState(heart.getPos());
        if (!(state.getBlock() instanceof BlockCreakingHeart) || state.getValue(BlockCreakingHeart.STATE) != BlockCreakingHeart.State.AWAKE) {
            return 0;
        }
        int n = 2 + world.rand.nextInt(2);
        int placed = 0;
        for (int i = 0; i < n; i++) {
            BlockPos at = spreadResin(world, heart.getPos(), world.rand);
            if (at != null) {
                world.playSound(null, at, ResinContent.resinSound.getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                placed++;
            }
        }
        return placed;
    }

    /**
     * {@code spreadResin}: a breadth-first walk from the heart through pale oak logs (depth 2, at most 64 places);
     * at the first log with an air neighbor (or a clump missing that face), in shuffled order, resin goes on the
     * face against the log.
     *
     * @return where resin was placed, or null
     */
    @Nullable
    public static BlockPos spreadResin(World world, BlockPos heart, Random rand) {
        Queue<Object[]> queue = new ArrayDeque<Object[]>();
        Set<BlockPos> seen = new HashSet<BlockPos>();
        queue.add(new Object[] {heart, 0});
        int count = 0;
        while (!queue.isEmpty()) {
            Object[] node = queue.poll();
            BlockPos pos = (BlockPos) node[0];
            int depth = (Integer) node[1];
            if (!seen.add(pos)) {
                continue;
            }
            // The visitor: a pale oak log tries its neighbors; anything else (the heart) is accepted as it is.
            if (BlockCreakingHeart.isPaleOakLog(world.getBlockState(pos))) {
                for (EnumFacing d : shuffled(rand)) {
                    BlockPos next = pos.offset(d);
                    IBlockState s = world.getBlockState(next);
                    EnumFacing face = d.getOpposite();
                    boolean air = s.getBlock().isAir(s, world, next);
                    if ((air || s.getBlock() instanceof BlockResinClump) && !BlockResinClump.has(BlockResinClump.faces(s), face)) {
                        world.setBlockState(next, BlockResinClump.of(BlockResinClump.faces(s) | (1 << face.getIndex())), 3);
                        return next;
                    }
                }
            }
            if (++count >= 64) {
                return null;
            }
            if (depth < 2) {
                for (EnumFacing d : shuffled(rand)) {
                    BlockPos next = pos.offset(d);
                    if (BlockCreakingHeart.isPaleOakLog(world.getBlockState(next))) {
                        queue.add(new Object[] {next, depth + 1});
                    }
                }
            }
        }
        return null;
    }

    /** {@code Util.shuffledCopy(Direction.values(), random)}. */
    private static List<EnumFacing> shuffled(Random rand) {
        List<EnumFacing> list = new ArrayList<EnumFacing>(Arrays.asList(EnumFacing.values()));
        Collections.shuffle(list, rand);
        return list;
    }
}
