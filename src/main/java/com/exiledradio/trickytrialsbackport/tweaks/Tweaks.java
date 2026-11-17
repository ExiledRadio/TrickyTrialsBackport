package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Stream 25's shared bits: whether a change is on (the stream's switch and the change's own), and the few private
 * vanilla fields and methods the changes reach, looked up once by their MCP (dev) or SRG (release) names.
 */
public final class Tweaks {

    private Tweaks() {
    }

    /** @return the change's own setting, while the stream itself is on */
    public static boolean on(boolean setting) {
        return ModConfig.tweaks.enabled && setting;
    }

    /** @return true for a server world */
    public static boolean server(World world) {
        return world != null && !world.isRemote;
    }

    static Field field(Class<?> owner, String mcp, String srg) {
        try {
            return ReflectionHelper.findField(owner, mcp, srg);
        } catch (RuntimeException e) {
            TrickyTrialsBackport.LOGGER.error("Tweaks: field {}.{} not found", owner.getName(), mcp, e);
            return null;
        }
    }

    static Method method(Class<?> owner, String mcp, String srg, Class<?>... params) {
        try {
            Method m = ReflectionHelper.findMethod(owner, mcp, srg, params);
            m.setAccessible(true);
            return m;
        } catch (RuntimeException e) {
            TrickyTrialsBackport.LOGGER.error("Tweaks: method {}.{} not found", owner.getName(), mcp, e);
            return null;
        }
    }

    static Object get(Field f, Object owner) {
        try {
            return f == null ? null : f.get(owner);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    static void set(Field f, Object owner, Object value) {
        if (f == null) {
            return;
        }
        try {
            f.set(owner, value);
        } catch (IllegalAccessException e) {
            TrickyTrialsBackport.LOGGER.error("Tweaks: could not set {}", f.getName(), e);
        }
    }

    static Object call(Method m, Object owner, Object... args) {
        try {
            return m == null ? null : m.invoke(owner, args);
        } catch (Exception e) {
            TrickyTrialsBackport.LOGGER.error("Tweaks: call to {} failed", m.getName(), e);
            return null;
        }
    }

    private static final Field EXPLODER = field(net.minecraft.world.Explosion.class, "exploder", "field_77283_e");

    /** @return the entity that exploded (1.12.2 keeps it private) */
    public static Entity exploder(net.minecraft.world.Explosion explosion) {
        Object e = get(EXPLODER, explosion);
        return e instanceof Entity ? (Entity) e : null;
    }

    /** {@code Entity.inPortal}: set by {@code setPortal}, read by vanilla's portal countdown. */
    static final Field IN_PORTAL = field(Entity.class, "inPortal", "field_71087_bX");
}
