package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P24a (docs/PATCHES.md): 1.21.11's zombie horse (stream 24, mounts).
 *
 * <p>{@code EntityZombieHorse} gets three methods it did not override, each returning the hook's answer:
 * {@code wearsArmor()} ({@code MountsHooks.zombieHorseWearsArmor()}), {@code isArmor(stack)}
 * ({@code zombieHorseIsArmor(stack)}) and {@code canDespawn()} ({@code zombieHorseCanDespawn(this)}). Its
 * {@code processInteract} gets, at the head, {@code switch (MountsHooks.zombieHorseInteract(this, player, hand))}: 1
 * returns true, 2 returns false, 0 runs 1.12.2's body. Every hook answers 1.12.2's way while
 * {@code mounts.zombieHorses} is off (false, false, false, 0).
 */
public final class ZombieHorseTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.passive.EntityZombieHorse";
    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/mounts/MountsHooks";
    private static final String SELF = "net/minecraft/entity/passive/EntityZombieHorse";
    private static final String INTERACT_DESC = "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Z";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode interact = Asm.method(node, "processInteract", "func_184645_a", INTERACT_DESC);
        if (interact == null) {
            Asm.LOG.warn("P24a zombie horse: processInteract not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        boolean dev = "processInteract".equals(interact.name);
        for (String[] m : new String[][] {{"wearsArmor", "func_190677_dK", "()Z"}, {"isArmor", "func_190682_f", "(Lnet/minecraft/item/ItemStack;)Z"},
                {"canDespawn", "func_70692_ba", "()Z"}}) {
            if (Asm.method(node, m[0], m[1], m[2]) != null) {
                Asm.LOG.warn("P24a zombie horse: {} already overridden in {}; patch skipped.", m[0], TARGET);
                return basicClass;
            }
        }

        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new VarInsnNode(Opcodes.ALOAD, 2));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "zombieHorseInteract",
                "(L" + SELF + ";Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)I", false));
        LabelNode vanilla = new LabelNode();
        LabelNode pass = new LabelNode();
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        head.add(new InsnNode(Opcodes.ICONST_1));
        head.add(new JumpInsnNode(Opcodes.IF_ICMPNE, pass));
        head.add(new InsnNode(Opcodes.ICONST_1));
        head.add(new InsnNode(Opcodes.IRETURN));
        head.add(pass);
        head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        head.add(new InsnNode(Opcodes.ICONST_0));
        head.add(new InsnNode(Opcodes.IRETURN));
        head.add(vanilla);
        head.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER}));
        head.add(new InsnNode(Opcodes.POP));
        interact.instructions.insert(head);

        node.methods.add(simple(Opcodes.ACC_PUBLIC, dev ? "wearsArmor" : "func_190677_dK", "()Z", "zombieHorseWearsArmor", "()Z", -1));
        node.methods.add(simple(Opcodes.ACC_PUBLIC, dev ? "isArmor" : "func_190682_f", "(Lnet/minecraft/item/ItemStack;)Z",
                "zombieHorseIsArmor", "(Lnet/minecraft/item/ItemStack;)Z", 1));
        node.methods.add(simple(Opcodes.ACC_PROTECTED, dev ? "canDespawn" : "func_70692_ba", "()Z",
                "zombieHorseCanDespawn", "(L" + SELF + ";)Z", 0));
        applied = true;
        Asm.LOG.info("P24a zombie horse: patched {}.", TARGET);
        return Asm.write(node);
    }

    /** {@code return MountsHooks.hook(<local load or nothing>);} */
    private static MethodNode simple(int access, String name, String desc, String hook, String hookDesc, int load) {
        MethodNode m = new MethodNode(access, name, desc, null, null);
        if (load >= 0) {
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, load));
        }
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, hookDesc, false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        return m;
    }
}
