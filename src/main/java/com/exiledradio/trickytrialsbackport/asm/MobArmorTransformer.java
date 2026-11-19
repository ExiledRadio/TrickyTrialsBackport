package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P16 (docs/PATCHES.md): mobs' spawn armor as 1.21.9 rolls it, with copper.
 *
 * <p>Head of {@code EntityLiving.setEquipmentBasedOnDifficulty}:
 * {@code if (CopperGearHooks.equipArmor(this, difficulty)) return;}. The hook answers false while the copper gear
 * stream or its {@code mobArmor} switch is off, and 1.12.2's roll runs as before.
 */
public final class MobArmorTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.EntityLiving";
    private static final String DESC = "(Lnet/minecraft/world/DifficultyInstance;)V";
    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/coppergear/CopperGearHooks";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "setEquipmentBasedOnDifficulty", "func_180481_a", DESC);
        if (m == null) {
            Asm.LOG.warn("P16 mob armor: setEquipmentBasedOnDifficulty not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        InsnList loads = new InsnList();
        loads.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 1));
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.RETURN));
        Asm.headReturnIf(m, loads, HOOKS, "equipArmor",
                "(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/world/DifficultyInstance;)Z", ret);
        applied = true;
        Asm.LOG.info("P16 mob armor: patched {}.", TARGET);
        return Asm.write(node);
    }
}
