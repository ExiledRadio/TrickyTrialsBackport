package com.exiledradio.trickytrialsbackport.flora;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.palegarden.BlockPaleOakLeaves;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which leaves get 1.21.5's tinted falling leaf ({@code TintedParticleLeavesBlock}, drawn by
 * {@code FloraClient.fallingLeaf} through P3), and in which color. Nothing here touches a client type, so the
 * server self-test can ask it.
 *
 * <p>In 26.3 oak, birch, jungle, acacia, dark oak and mangrove leaves are {@code TintedParticleLeavesBlock(0.01F)}, and
 * azalea and flowering azalea {@code UntintedParticleLeavesBlock(0.01F)} with the same {@code tinted_leaves} particle
 * in a fixed {@code -9399763} ({@code #70922D}). Spruce has none (26.3 took its particles away); pale oak, cherry and
 * poplar have their own particle types, which their own mods draw. Every other {@link BlockLeaves}, other mods' and
 * Dynamic Trees' included, is taken as tinted leaves. A Dynamic Trees leaves block is judged by its primitive leaves:
 * Dynamic Trees - Backport hands its leaves' display tick to the primitive's, so a dynamic pale oak or cherry already
 * drops its primitive's own leaves.
 */
public final class FallingLeaves {

    /**
     * The chance per display tick, {@code Blocks.java}'s {@code new TintedParticleLeavesBlock(0.01F, ...)} for oak, birch,
     * jungle, acacia, dark oak and mangrove, and the azaleas' {@code UntintedParticleLeavesBlock(0.01F, ...)}.
     */
    public static final float CHANCE = 0.01F;

    /** {@code Blocks.AZALEA_LEAVES}' particle color, {@code -9399763}. */
    public static final int AZALEA = 0x70922D;

    /** Leaves whose own mod draws their own falling leaves. */
    private static final Set<ResourceLocation> OWN_PARTICLES = new HashSet<ResourceLocation>(Arrays.asList(
            new ResourceLocation("trailsandtalesbackport", "cherry_leaves"),
            new ResourceLocation("gamedropsbackport", "red_poplar_leaves"),
            new ResourceLocation("gamedropsbackport", "orange_poplar_leaves"),
            new ResourceLocation("gamedropsbackport", "yellow_poplar_leaves")));

    /** Caves & Cliffs Backport's azalea leaves: no block color handler, 26.3's fixed color. */
    private static final Set<ResourceLocation> AZALEA_LEAVES = new HashSet<ResourceLocation>(Arrays.asList(
            new ResourceLocation("cavesandcliffsbackport", "azalea_leaves"),
            new ResourceLocation("cavesandcliffsbackport", "flowering_azalea_leaves")));

    private static final Map<IBlockState, IBlockState> SOURCES = new ConcurrentHashMap<IBlockState, IBlockState>();
    /** Stands for "no tinted leaf" in {@link #SOURCES}, which cannot hold null. */
    private static final IBlockState NONE = Blocks.AIR.getDefaultState();

    private static boolean dtLooked;
    private static Class<?> dtLeaves;
    private static Method dtProperties;
    private static Method dtPrimitive;

    private FallingLeaves() {
    }

    /** The stream is on and {@code flora.fallingLeaves} is set. */
    public static boolean on() {
        return FloraContent.enabled() && ModConfig.flora.fallingLeaves;
    }

    /**
     * @return the leaves this block's falling leaves are judged by (the primitive leaves of a Dynamic Trees leaves
     *         block, otherwise the state itself), or {@code null} where no tinted leaf falls
     */
    public static IBlockState source(IBlockState state) {
        if (!(state.getBlock() instanceof BlockLeaves)) {
            return null;
        }
        // Block states are singletons; the answer for one never changes, and this runs for every leaves block the
        // client's ambient loop picks (about 1300 blocks a tick).
        IBlockState known = SOURCES.get(state);
        if (known == null) {
            IBlockState found = judge(state);
            known = found == null ? NONE : found;
            SOURCES.put(state, known);
        }
        return known == NONE ? null : known;
    }

    private static IBlockState judge(IBlockState state) {
        IBlockState source = primitive(state);
        Block block = source.getBlock();
        if (!(block instanceof BlockLeaves) || block instanceof BlockPaleOakLeaves) {
            return null;
        }
        if (block == Blocks.LEAVES && source.getValue(BlockOldLeaf.VARIANT) == BlockPlanks.EnumType.SPRUCE) {
            return null;
        }
        ResourceLocation id = block.getRegistryName();
        if (id != null && OWN_PARTICLES.contains(id)) {
            return null;
        }
        return source;
    }

    /** @return the fixed particle color for these leaves, or {@code null} to take the block color handler's tint 0 */
    public static Integer fixedColor(IBlockState source) {
        ResourceLocation id = source.getBlock().getRegistryName();
        return id != null && AZALEA_LEAVES.contains(id) ? Integer.valueOf(AZALEA) : null;
    }

    /** A Dynamic Trees {@code BlockDynamicLeaves}' {@code getProperties(state).getPrimitiveLeaves()}, else the state. */
    static IBlockState primitive(IBlockState state) {
        if (!dtLooked) {
            dtLooked = true;
            try {
                dtLeaves = Class.forName("com.ferreusveritas.dynamictrees.blocks.BlockDynamicLeaves", false,
                        FallingLeaves.class.getClassLoader());
                dtProperties = dtLeaves.getMethod("getProperties", IBlockState.class);
                dtPrimitive = Class.forName("com.ferreusveritas.dynamictrees.api.treedata.ILeavesProperties", false,
                        FallingLeaves.class.getClassLoader()).getMethod("getPrimitiveLeaves");
            } catch (ClassNotFoundException e) {
                dtLeaves = null;
            } catch (ReflectiveOperationException | LinkageError e) {
                TrickyTrialsBackport.LOGGER.warn("Falling leaves: Dynamic Trees' leaves API not found; dynamic leaves count as tinted.", e);
                dtLeaves = null;
            }
        }
        if (dtLeaves == null || !dtLeaves.isInstance(state.getBlock())) {
            return state;
        }
        try {
            Object properties = dtProperties.invoke(state.getBlock(), state);
            Object primitive = properties == null ? null : dtPrimitive.invoke(properties);
            return primitive instanceof IBlockState ? (IBlockState) primitive : state;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return state;
        }
    }
}
