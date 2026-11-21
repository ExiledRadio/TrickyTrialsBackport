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
 * Patches P26 and P27 (docs/PATCHES.md): painting items that carry their variant (stream 9, decor).
 *
 * <p>P26: {@code ItemHangingEntity} gets the {@code getSubItems} it did not override: 1.12.2's body
 * ({@code super.getSubItems}), then {@code PaintingItems.subItems(this, tab, items)}. P27: the head of Forge's
 * {@code Entity.getPickedResult} gets {@code ItemStack s = PaintingItems.pickedResult(this); if (s != null) return s;}.
 * Both hooks do nothing while {@code decor} is off.
 */
public final class PaintingItemTransformer implements IClassTransformer {

    private static final String ITEM = "net.minecraft.item.ItemHangingEntity";
    private static final String ENTITY = "net.minecraft.entity.Entity";
    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/decor/PaintingItems";
    private static final String SUB_DESC = "(Lnet/minecraft/creativetab/CreativeTabs;Lnet/minecraft/util/NonNullList;)V";
    private static final String USE_DESC = "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)"
            + "Lnet/minecraft/util/EnumActionResult;";
    private static final String STACK = "net/minecraft/item/ItemStack";

    /** Set once each class has been patched; the self-test reads them. */
    public static volatile boolean appliedSubItems;
    public static volatile boolean appliedPick;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (ITEM.equals(transformedName)) {
            return subItems(basicClass);
        }
        if (ENTITY.equals(transformedName)) {
            return pick(basicClass);
        }
        return basicClass;
    }

    private static byte[] subItems(byte[] basicClass) {
        ClassNode node = Asm.read(basicClass);
        MethodNode use = Asm.method(node, "onItemUse", "func_180614_a", USE_DESC);
        if (use == null || Asm.method(node, "getSubItems", "func_150895_a", SUB_DESC) != null) {
            Asm.LOG.warn("P26 painting items: {} is not 1.12.2's (onItemUse missing or getSubItems present); patch skipped.", ITEM);
            return basicClass;
        }
        String subName = "onItemUse".equals(use.name) ? "getSubItems" : "func_150895_a";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, subName, SUB_DESC, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/item/Item", subName, SUB_DESC, false));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "subItems",
                "(Lnet/minecraft/item/Item;Lnet/minecraft/creativetab/CreativeTabs;Lnet/minecraft/util/NonNullList;)V", false));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(m);
        appliedSubItems = true;
        Asm.LOG.info("P26 painting items: patched {}.", ITEM);
        return Asm.write(node);
    }

    private static byte[] pick(byte[] basicClass) {
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "getPickedResult", "getPickedResult",
                "(Lnet/minecraft/util/math/RayTraceResult;)L" + STACK + ";");
        if (m == null) {
            Asm.LOG.warn("P27 painting pick: getPickedResult not found in {}; patch skipped.", ENTITY);
            return basicClass;
        }
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "pickedResult",
                "(Lnet/minecraft/entity/Entity;)L" + STACK + ";", false));
        LabelNode vanilla = new LabelNode();
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        head.add(new InsnNode(Opcodes.ARETURN));
        head.add(vanilla);
        head.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {STACK}));
        head.add(new InsnNode(Opcodes.POP));
        m.instructions.insert(head);
        appliedPick = true;
        Asm.LOG.info("P27 painting pick: patched {}.", ENTITY);
        return Asm.write(node);
    }
}
