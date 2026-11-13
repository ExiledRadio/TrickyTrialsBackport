package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The creaking heart's tile: 1.21.4's {@code CreakingHeartBlockEntity} without the creaking.
 *
 * <h2>What it does here</h2>
 * Every 20-24 ticks while the heart is not uprooted it rechecks its state ({@code updateCreakingState}):
 * uprooted when its logs are gone and no creaking is linked, otherwise awake at night and dormant by day.
 * It keeps {@code natural} (grown by a tree) and its comparator output.
 *
 * <h2>The hook for the creaking (stream 13)</h2>
 * Streams never link against each other, so the creaking plugs in by name. If the class
 * {@value #HOOK} exists, the heart calls its public static methods, all with vanilla types only:
 * <ul>
 *   <li>{@code boolean tick(TileEntity heart, NBTTagCompound link)}: every tick on the server while not
 *       uprooted, after the state check. Spawning, linking and unlinking the creaking are the hook's;
 *       it returns whether a creaking is linked now (an uprooted heart keeps its state while one is).</li>
 *   <li>{@code int signal(TileEntity heart, NBTTagCompound link)}: the comparator output (vanilla: 15 at
 *       the heart, falling to 0 at 32 blocks; 0 without a creaking).</li>
 *   <li>{@code void removed(TileEntity heart, NBTTagCompound link, Entity by)}: the heart was broken or blown
 *       up ({@code by} may be null).</li>
 * </ul>
 * {@code link} is this tile's own compound, saved with it under {@code Creaking}, for the hook to keep the
 * creaking's id and anything else in. Without the class nothing is called and no creaking ever appears.
 */
public class TileCreakingHeart extends TileEntity implements ITickable {

    /** The creaking stream's hook class, by name. */
    public static final String HOOK = "com.exiledradio.trickytrialsbackport.creaking.CreakingHeartHook";

    private static boolean hookLooked;
    @Nullable
    private static Method hookTick;
    @Nullable
    private static Method hookSignal;
    @Nullable
    private static Method hookRemoved;

    private boolean natural;
    private int ticker;
    private int outputSignal;
    private boolean linked;
    private NBTTagCompound link = new NBTTagCompound();

    public boolean isNatural() {
        return this.natural;
    }

    public void setNatural(boolean natural) {
        this.natural = natural;
        markDirty();
    }

    /** The creaking stream's data for this heart (saved with the tile). */
    public NBTTagCompound link() {
        return this.link;
    }

    public int outputSignal() {
        return this.outputSignal;
    }

    private static void findHook() {
        if (hookLooked) {
            return;
        }
        hookLooked = true;
        try {
            Class<?> c = Class.forName(HOOK);
            hookTick = c.getMethod("tick", TileEntity.class, NBTTagCompound.class);
            hookSignal = c.getMethod("signal", TileEntity.class, NBTTagCompound.class);
            hookRemoved = c.getMethod("removed", TileEntity.class, NBTTagCompound.class, Entity.class);
        } catch (ClassNotFoundException e) {
            // No creaking stream in this build: hearts only wake and sleep.
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("The creaking heart's hook {} is not usable; hearts will spawn no creaking.", HOOK, e);
            hookTick = hookSignal = hookRemoved = null;
        }
    }

    @Override
    public void update() {
        World world = this.world;
        if (world == null || world.isRemote) {
            return;
        }
        IBlockState state = world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockCreakingHeart)) {
            return;
        }
        // Vanilla's ticker exists only while the heart is not uprooted.
        if (state.getValue(BlockCreakingHeart.STATE) == BlockCreakingHeart.State.UPROOTED) {
            return;
        }
        findHook();
        int signal = call(hookSignal, 0);
        if (signal != this.outputSignal) {
            this.outputSignal = signal;
            world.updateComparatorOutputLevel(this.pos, state.getBlock());
        }
        if (this.ticker-- < 0) {
            this.ticker = world.rand.nextInt(5) + 20;
            IBlockState now = updateCreakingState(state, world, this.pos, this.linked);
            if (now != state) {
                world.setBlockState(this.pos, now, 3);
                if (now.getValue(BlockCreakingHeart.STATE) == BlockCreakingHeart.State.UPROOTED) {
                    return;
                }
            }
        }
        if (hookTick != null) {
            this.linked = call(hookTick, Boolean.FALSE);
            markDirty();
        }
    }

    /** {@code CreakingHeartBlockEntity.updateCreakingState}. */
    static IBlockState updateCreakingState(IBlockState state, World world, BlockPos pos, boolean linked) {
        if (!BlockCreakingHeart.hasRequiredLogs(state, world, pos) && !linked) {
            return state.withProperty(BlockCreakingHeart.STATE, BlockCreakingHeart.State.UPROOTED);
        }
        return state.withProperty(BlockCreakingHeart.STATE,
                BlockCreakingHeart.active(world) ? BlockCreakingHeart.State.AWAKE : BlockCreakingHeart.State.DORMANT);
    }

    @SuppressWarnings("unchecked")
    private <T> T call(@Nullable Method method, T otherwise) {
        if (method == null) {
            return otherwise;
        }
        try {
            Object out = method.invoke(null, this, this.link);
            return out == null ? otherwise : (T) out;
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("The creaking heart's hook failed at {}.", this.pos, e);
            return otherwise;
        }
    }

    /** The heart is being broken or blown up. */
    void removed(@Nullable Entity by) {
        findHook();
        if (hookRemoved != null && this.world != null && !this.world.isRemote) {
            try {
                hookRemoved.invoke(null, this, this.link, by);
            } catch (ReflectiveOperationException | RuntimeException e) {
                TrickyTrialsBackport.LOGGER.warn("The creaking heart's hook failed at {}.", this.pos, e);
            }
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.natural = tag.getBoolean("Natural");
        this.linked = tag.getBoolean("Linked");
        this.link = tag.getCompoundTag("Creaking");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Natural", this.natural);
        tag.setBoolean("Linked", this.linked);
        tag.setTag("Creaking", this.link);
        return tag;
    }
}
