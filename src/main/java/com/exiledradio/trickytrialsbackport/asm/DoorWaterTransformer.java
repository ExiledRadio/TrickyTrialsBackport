package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P25 (docs/PATCHES.md): water stops at our doors (the copper doors and the pale oak door) under Fluidlogged API.
 *
 * <p>Fluidlogged API 3.3.3 replaces vanilla's liquid flow. Its {@code ISpecializedFluidNeighborInfo.Vanilla.canDisplaceI}
 * is vanilla's {@code BlockDynamicLiquid.isBlocked} turned around, except the door/sign/ladder/sugar cane branch, which
 * still answers {@code true}: "water may displace this". For vanilla's doors that never shows, as its built-in list
 * waterlogs them and a waterloggable block is not displaced. Backport Core keeps the family's doors dry (1.13 and
 * later never waterlog a door), so water washed ours away: placed in water they broke at once, and beside water as
 * soon as it spread.
 *
 * <p>The branch's {@code return true} becomes {@code return DoorWater.displaced(state)}, which answers {@code false}
 * for our doors and {@code true} (Fluidlogged API's own answer) for everything else.
 */
public final class DoorWaterTransformer implements IClassTransformer {

    private static final String TARGET = "git.jbredwards.fluidlogged_api.mod.common.fluid.util.ISpecializedFluidNeighborInfo$Vanilla";
    private static final String HOOK = "com/exiledradio/trickytrialsbackport/copper/DoorWater";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "canDisplaceI", "canDisplaceI", "(III)Z");
        if (m == null || !patch(m)) {
            Asm.LOG.warn("P25 door water: canDisplaceI's door branch not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        applied = true;
        Asm.LOG.info("P25 door water: patched {}.", TARGET);
        return Asm.write(node);
    }

    /**
     * Finds {@code ALOAD state; getBlock; INSTANCEOF BlockDoor; IFNE branch}, and at the branch
     * {@code ICONST_1; IRETURN}; puts {@code ALOAD state; INVOKESTATIC DoorWater.displaced} in place of the
     * {@code ICONST_1}. The state's local is live at the branch (javac's frame there keeps it), so no frame changes.
     */
    private static boolean patch(MethodNode m) {
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.INSTANCEOF || !"net/minecraft/block/BlockDoor".equals(((TypeInsnNode) insn).desc)) {
                continue;
            }
            AbstractInsnNode getBlock = insn.getPrevious();
            AbstractInsnNode load = getBlock == null ? null : getBlock.getPrevious();
            AbstractInsnNode jump = insn.getNext();
            if (!(getBlock instanceof MethodInsnNode) || load == null || load.getOpcode() != Opcodes.ALOAD
                    || jump == null || jump.getOpcode() != Opcodes.IFNE) {
                return false;
            }
            AbstractInsnNode value = next(((JumpInsnNode) jump).label);
            if (value == null || value.getOpcode() != Opcodes.ICONST_1 || next(value) == null
                    || next(value).getOpcode() != Opcodes.IRETURN) {
                return false;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, ((VarInsnNode) load).var));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "displaced", "(Lnet/minecraft/block/state/IBlockState;)Z", false));
            m.instructions.insert(value, hook);
            m.instructions.remove(value);
            return true;
        }
        return false;
    }

    /** The next real instruction (not a label, line number or frame). */
    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }
}
