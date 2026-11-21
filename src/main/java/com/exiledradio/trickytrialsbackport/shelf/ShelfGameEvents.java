package com.exiledradio.trickytrialsbackport.shelf;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The shelf's game events, as 1.21.9 posts them, sent through Backport Core's game event module, which Wild Update's
 * sculk sensors (and anything else listening) hear.
 *
 * <ul>
 *   <li>{@code block_activate}: any change of the contents ({@code ShelfBlockEntity.setChanged()}: hoppers, commands);
 *       and the shelf being powered, with {@code block_deactivate} when it loses power ({@code neighborChanged}).</li>
 *   <li>{@code item_interact_finish}: a single swap ({@code swapSingleItem}).</li>
 *   <li>{@code entity_interact}: each shelf of a powered row swapping with the hotbar ({@code swapHotbar}).</li>
 * </ul>
 * Vanilla's context is the shelf's state with no source entity, and so is this one. A row part changing posts
 * nothing ({@code setBlock} posts no event).
 *
 * <p>The module is Core's from 1.4.0; this mod builds against 1.3.0, so it is reached by name. Every mod with a
 * listener (Wild Update) needs 1.4.0 itself, so whenever something can hear the shelf, the module is there; without
 * it, nothing listens and nothing is sent.
 */
public final class ShelfGameEvents {

    public static final String BLOCK_ACTIVATE = "block_activate";
    public static final String BLOCK_DEACTIVATE = "block_deactivate";
    public static final String ITEM_INTERACT_FINISH = "item_interact_finish";
    public static final String ENTITY_INTERACT = "entity_interact";

    private static boolean looked;
    @Nullable
    private static Method get;
    @Nullable
    private static Method context;
    @Nullable
    private static Method emit;

    @Nullable
    private static String last;
    @Nullable
    private static BlockPos lastPos;

    private ShelfGameEvents() {
    }

    /** @return true if Backport Core's game event module is in this game */
    public static synchronized boolean available() {
        if (!looked) {
            looked = true;
            try {
                Class<?> events = Class.forName("com.exiledradio.backportcore.gameevent.GameEvents");
                Class<?> event = Class.forName("com.exiledradio.backportcore.gameevent.GameEvent");
                Class<?> ctx = Class.forName("com.exiledradio.backportcore.gameevent.GameEventContext");
                get = event.getMethod("get", ResourceLocation.class);
                context = ctx.getMethod("of", IBlockState.class);
                emit = events.getMethod("emit", World.class, event, BlockPos.class, ctx);
            } catch (ReflectiveOperationException | LinkageError e) {
                get = null;
                context = null;
                emit = null;
            }
        }
        return emit != null;
    }

    /** Posts a vanilla game event at the shelf, from the server. */
    static void post(World world, BlockPos pos, String name, IBlockState state) {
        if (world == null || world.isRemote) {
            return;
        }
        synchronized (ShelfGameEvents.class) {
            last = name;
            lastPos = pos.toImmutable();
        }
        if (!available()) {
            return;
        }
        try {
            Object event = get.invoke(null, new ResourceLocation(name));
            if (event != null) {
                emit.invoke(null, world, event, pos, context.invoke(null, state));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            TrickyTrialsBackport.LOGGER.warn("Shelf game event {} not sent", name, e);
        }
    }

    /** For the self-test: the last event a shelf posted (whether or not Core's module was there to send it). */
    @Nullable
    public static synchronized String last() {
        return last;
    }

    @Nullable
    public static synchronized BlockPos lastPos() {
        return lastPos;
    }

    public static synchronized void clearLast() {
        last = null;
        lastPos = null;
    }
}
