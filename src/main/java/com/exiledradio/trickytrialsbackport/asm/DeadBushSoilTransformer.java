package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P2 (docs/PATCHES.md): dead bushes stand on farmland, grass, mycelium and the rest of 1.21.5's
 * {@code #dry_vegetation_may_place_on}.
 *
 * <p>Head of {@code BlockDeadBush.canSustainBush}: {@code if (FloraHooks.deadBushSoil(state)) return true;}. Every
 * placement and survival check of the dead bush ends there (Forge's {@code canSustainPlant} asks it too), and
 * farmland under a dead bush stays farmland, as under a crop.
 */
public final class DeadBushSoilTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.block.BlockDeadBush";
    private static final String DESC = "(Lnet/minecraft/block/state/IBlockState;)Z";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode soil = Asm.method(node, "canSustainBush", "func_185514_i", DESC);
        if (soil == null) {
            Asm.LOG.warn("P2 dead bush ground: canSustainBush not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        InsnList loads = new InsnList();
        loads.add(new VarInsnNode(Opcodes.ALOAD, 1));
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.ICONST_1));
        ret.add(new InsnNode(Opcodes.IRETURN));
        Asm.headReturnIf(soil, loads, Asm.FLORA_HOOKS, "deadBushSoil", DESC, ret);
        applied = true;
        Asm.LOG.info("P2 dead bush ground: patched {}.", TARGET);
        return Asm.write(node);
    }
}
