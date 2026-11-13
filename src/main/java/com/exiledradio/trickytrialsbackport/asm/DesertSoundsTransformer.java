package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch P3 (docs/PATCHES.md): 1.21.5's desert ambience on 1.12.2's own blocks: sand's idle sound and the dead bush's
 * ({@code AmbientDesertBlockSoundsPlayer}). Client only.
 *
 * <p>In {@code WorldClient}, the ambient loop's call {@code block.randomDisplayTick(state, world, pos, random)} becomes
 * {@code FloraHooks.displayTick(block, state, world, pos, random)}, which makes the same call and then plays the desert
 * sounds for sand and dead bushes. The stack is the same before and after, so nothing else in the method moves.
 */
public final class DesertSoundsTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.client.multiplayer.WorldClient";
    private static final String DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V";

    /** Set once the class has been patched. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        List<MethodInsnNode> calls = new ArrayList<MethodInsnNode>();
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && "net/minecraft/block/Block".equals(call.owner)
                            && DESC.equals(call.desc)
                            && ("randomDisplayTick".equals(call.name) || "func_180655_c".equals(call.name))) {
                        calls.add(call);
                    }
                }
            }
        }
        if (calls.size() != 1) {
            Asm.LOG.warn("P3 desert sounds: {} randomDisplayTick calls in {} (1 expected); patch skipped.", calls.size(), TARGET);
            return basicClass;
        }
        MethodInsnNode call = calls.get(0);
        for (MethodNode m : node.methods) {
            if (m.instructions.contains(call)) {
                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, Asm.FLORA_HOOKS, "displayTick",
                        "(Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
                                + "Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V", false));
                break;
            }
        }
        applied = true;
        Asm.LOG.info("P3 desert sounds: patched {}.", TARGET);
        return Asm.write(node);
    }
}
