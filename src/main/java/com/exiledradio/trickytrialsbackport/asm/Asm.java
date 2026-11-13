package com.exiledradio.trickytrialsbackport.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Small helpers shared by the patches. Every name is matched as MCP (the dev environment) or SRG (a real instance). */
final class Asm {

    static final Logger LOG = LogManager.getLogger("Tricky Trials Backport Core");

    /** Hooks of the spring plants stream. */
    static final String FLORA_HOOKS = "com/exiledradio/trickytrialsbackport/flora/FloraHooks";

    private Asm() {
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /**
     * Writes a class back without recomputing frames: a patch that adds a jump adds the one frame its target needs
     * ({@link #headReturnIf}), so no class needs loading here.
     */
    static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Puts {@code if (hook(args...)) return [value];} at the head of a method: the loads, the static call, a jump over
     * the return to a new label carrying an {@code F_SAME} frame (the method's first frame, nothing changed yet).
     *
     * @param loads   the ALOAD/ILOAD instructions for the hook's arguments
     * @param ret     the return to take when the hook says true (RETURN, or ICONST_1 + IRETURN)
     */
    static void headReturnIf(MethodNode m, InsnList loads, String owner, String name, String desc, InsnList ret) {
        InsnList head = new InsnList();
        head.add(loads);
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false));
        LabelNode go = new LabelNode();
        head.add(new JumpInsnNode(Opcodes.IFEQ, go));
        head.add(ret);
        head.add(go);
        head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.insert(head);
    }

    static MethodNode method(ClassNode node, String mcp, String srg, String desc) {
        for (MethodNode m : node.methods) {
            if ((mcp.equals(m.name) || srg.equals(m.name)) && desc.equals(m.desc)) {
                return m;
            }
        }
        return null;
    }
}
