package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patches P20 and P21 (docs/PATCHES.md), for the spear stream.
 *
 * <ul>
 *   <li><b>P20</b>: before every return of {@code EntityZombie.setEquipmentBasedOnDifficulty} and
 *       {@code EntityPigZombie.setEquipmentBasedOnDifficulty}: {@code SpearEvents.spawnWeapon(this, difficulty)},
 *       which turns some of the weapons just rolled into spears as 1.21.11 rolls them. Off, it changes nothing.</li>
 *   <li><b>P21</b> (client): at the head of {@code ModelBiped.setRotationAngles}
 *       {@code SpearModelHooks.pre(this, entity)}, and before every return {@code SpearModelHooks.post(this)} (the entity is
 *       stashed by {@code pre}: Mojang's frames drop the parameter before the returns):
 *       the spear's third-person arm.</li>
 * </ul>
 */
public final class SpearTransformer implements IClassTransformer {

    private static final String EQUIP_DESC = "(Lnet/minecraft/world/DifficultyInstance;)V";
    private static final String EVENTS = "com/exiledradio/trickytrialsbackport/spear/SpearEvents";
    private static final String MODEL_DESC = "(FFFFFFLnet/minecraft/entity/Entity;)V";
    private static final String MODEL_HOOKS = "com/exiledradio/trickytrialsbackport/spear/client/SpearModelHooks";

    /** Set once each class has been patched; the self-test reads them. */
    public static volatile boolean zombie;
    public static volatile boolean pigZombie;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return basicClass;
        }
        if ("net.minecraft.entity.monster.EntityZombie".equals(transformedName)
                || "net.minecraft.entity.monster.EntityPigZombie".equals(transformedName)) {
            return equipment(transformedName, basicClass);
        }
        if ("net.minecraft.client.model.ModelBiped".equals(transformedName)) {
            return model(transformedName, basicClass);
        }
        return basicClass;
    }

    private static byte[] equipment(String target, byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "setEquipmentBasedOnDifficulty", "func_180481_a", EQUIP_DESC);
        if (m == null) {
            Asm.LOG.warn("P20 spear spawns: setEquipmentBasedOnDifficulty not found in {}; patch skipped.", target);
            return bytes;
        }
        int n = beforeReturns(m, () -> {
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EVENTS, "spawnWeapon",
                    "(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/world/DifficultyInstance;)V", false));
            return call;
        });
        if (target.endsWith("EntityZombie")) {
            zombie = true;
        } else {
            pigZombie = true;
        }
        Asm.LOG.info("P20 spear spawns: patched {} ({} returns).", target, n);
        return Asm.write(node);
    }

    private static byte[] model(String target, byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "setRotationAngles", "func_78087_a", MODEL_DESC);
        if (m == null) {
            Asm.LOG.warn("P21 spear pose: setRotationAngles not found in {}; patch skipped.", target);
            return bytes;
        }
        int n = beforeReturns(m, SpearTransformer::post);
        m.instructions.insert(hook("pre"));
        Asm.LOG.info("P21 spear pose: patched {} ({} returns).", target, n);
        return Asm.write(node);
    }

    private static InsnList hook(String name) {
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new VarInsnNode(Opcodes.ALOAD, 7));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MODEL_HOOKS, name,
                "(Lnet/minecraft/client/model/ModelBiped;Lnet/minecraft/entity/Entity;)V", false));
        return call;
    }

    private static InsnList post() {
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MODEL_HOOKS, "post",
                "(Lnet/minecraft/client/model/ModelBiped;)V", false));
        return call;
    }

    private interface Calls {
        InsnList make();
    }

    private static int beforeReturns(MethodNode m, Calls calls) {
        int n = 0;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                m.instructions.insertBefore(insn, calls.make());
                n++;
            }
        }
        return n;
    }
}
