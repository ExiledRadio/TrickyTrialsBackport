package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P4 (docs/PATCHES.md): a strafing mob that cannot step sideways walks forward at its strafing speed, as
 * today's {@code MoveControl}, not at its full speed.
 *
 * <p>In {@code EntityMoveHelper.onUpdateMoveHelper}'s strafe branch, {@code this.moveStrafe = 0.0F; f1 = f;} becomes
 * {@code this.moveStrafe = 0.0F; f1 = TweaksHooks.blockedStrafeSpeed(f, f1);}. The site is found by its shape (a
 * float field of the class set to 0, then a float local copied to another), so no field name is needed. Both locals
 * are the method's own floats, live there; no parameter is read.
 */
public final class StrafeSpeedTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.ai.EntityMoveHelper";
    private static final String OWNER = "net/minecraft/entity/ai/EntityMoveHelper";
    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/tweaks/TweaksHooks";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "onUpdateMoveHelper", "func_75641_c", "()V");
        if (m == null) {
            Asm.LOG.warn("P4 strafe speed: onUpdateMoveHelper not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        VarInsnNode load = null;
        int sites = 0;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.PUTFIELD || !OWNER.equals(((FieldInsnNode) insn).owner) || !"F".equals(((FieldInsnNode) insn).desc)) {
                continue;
            }
            AbstractInsnNode before = previous(insn);
            AbstractInsnNode a = next(insn);
            AbstractInsnNode b = a == null ? null : next(a);
            if (before != null && before.getOpcode() == Opcodes.FCONST_0 && a != null && a.getOpcode() == Opcodes.FLOAD
                    && b != null && b.getOpcode() == Opcodes.FSTORE && ((VarInsnNode) a).var != ((VarInsnNode) b).var) {
                load = (VarInsnNode) a;
                sites++;
            }
        }
        if (sites != 1) {
            Asm.LOG.warn("P4 strafe speed: found {} candidate sites in {} (expected 1); patch skipped.", sites, TARGET);
            return basicClass;
        }
        VarInsnNode store = (VarInsnNode) next(load);
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.FLOAD, store.var));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "blockedStrafeSpeed", "(FF)F", false));
        m.instructions.insert(load, call);
        applied = true;
        Asm.LOG.info("P4 strafe speed: patched {}.", TARGET);
        return Asm.write(node);
    }

    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }

    private static AbstractInsnNode previous(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getPrevious();
        while (n != null && n.getOpcode() < 0) {
            n = n.getPrevious();
        }
        return n;
    }
}
