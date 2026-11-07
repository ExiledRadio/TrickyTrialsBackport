package com.exiledradio.trickytrialsbackport.common;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The mod's one GUI handler. A stream registers its container factory here (server and client) and, from
 * its client class, its screen factory; the gui id is the stream's row number in {@code docs/PLAN.md}.
 *
 * <p>Factories return {@code Object} so no client type appears in a signature a server loads.
 */
public final class Guis implements IGuiHandler {

    /** Makes a container or a screen for a block position. */
    public interface Factory {
        @Nullable
        Object create(EntityPlayer player, World world, BlockPos pos);
    }

    private static final Map<Integer, Factory> SERVER = new HashMap<Integer, Factory>();
    private static final Map<Integer, Factory> CLIENT = new HashMap<Integer, Factory>();

    private Guis() {
    }

    /** Called once from the mod's init. */
    public static void init() {
        NetworkRegistry.INSTANCE.registerGuiHandler(TrickyTrialsBackport.instance, new Guis());
    }

    public static void container(int id, Factory factory) {
        SERVER.put(id, factory);
    }

    /** From a client class only. */
    public static void screen(int id, Factory factory) {
        CLIENT.put(id, factory);
    }

    public static void open(int id, EntityPlayer player, World world, BlockPos pos) {
        player.openGui(TrickyTrialsBackport.instance, id, world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    @Nullable
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        Factory f = SERVER.get(id);
        return f == null ? null : f.create(player, world, new BlockPos(x, y, z));
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        Factory f = CLIENT.get(id);
        return f == null ? null : f.create(player, world, new BlockPos(x, y, z));
    }
}
