package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stream 16's patches P16-P19 (docs/PATCHES.md). Each adds a call to {@code animals.AnimalsHooks} and keeps the
 * stack as it was; none adds a jump, so no frame changes. A patch whose method is not found is skipped and logged.
 *
 * <ul>
 *   <li>P16 {@code EntityLiving.onInitialSpawn}: before each return, {@code AnimalsHooks.initialSpawn(this)}.</li>
 *   <li>P17 {@code EntitySheep.onInitialSpawn}: before each return, {@code AnimalsHooks.sheepSpawn(this)}.</li>
 *   <li>P18 {@code EntityAnimal.readEntityFromNBT} / {@code writeEntityToNBT}: at the head,
 *       {@code AnimalsHooks.read(this, tag)} / {@code write(this, tag)}.</li>
 *   <li>P19 {@code EntityWolf.getAmbientSound}, {@code getHurtSound}, {@code getDeathSound}: what each returns
 *       goes through {@code AnimalsHooks.wolfSound(sound, this)}.</li>
 * </ul>
 */
public final class AnimalsTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/animals/AnimalsHooks";
    private static final String SPAWN_DESC = "(Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/IEntityLivingData;)"
            + "Lnet/minecraft/entity/IEntityLivingData;";
    private static final String NBT_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)V";
    private static final String SOUND = "Lnet/minecraft/util/SoundEvent;";

    /** The patches applied so far ("P16", ...); the self-test reads it. */
    public static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        switch (transformedName) {
            case "net.minecraft.entity.EntityLiving":
                return patch(bytes, "P16", n -> beforeReturns(n, "onInitialSpawn", "func_180482_a", SPAWN_DESC,
                        "initialSpawn", "(Lnet/minecraft/entity/EntityLiving;)V"));
            case "net.minecraft.entity.passive.EntitySheep":
                return patch(bytes, "P17", n -> beforeReturns(n, "onInitialSpawn", "func_180482_a", SPAWN_DESC,
                        "sheepSpawn", "(Lnet/minecraft/entity/passive/EntitySheep;)V"));
            case "net.minecraft.entity.passive.EntityAnimal":
                return patch(bytes, "P18", AnimalsTransformer::nbt);
            case "net.minecraft.entity.passive.EntityWolf":
                return patch(bytes, "P19", AnimalsTransformer::wolfSounds);
            default:
                return bytes;
        }
    }

    interface Patch {
        boolean apply(ClassNode node);
    }

    private static byte[] patch(byte[] bytes, String id, Patch p) {
        ClassNode node = Asm.read(bytes);
        try {
            if (!p.apply(node)) {
                Asm.LOG.warn("{}: site not found in {}; patch skipped.", id, node.name);
                return bytes;
            }
        } catch (RuntimeException e) {
            Asm.LOG.error("{}: failed on {}; patch skipped.", id, node.name, e);
            return bytes;
        }
        APPLIED.add(id);
        Asm.LOG.info("{}: patched {}.", id, node.name);
        return Asm.write(node);
    }

    /** Before every ARETURN of the method: {@code aload 0; invokestatic hook}. */
    private static boolean beforeReturns(ClassNode node, String mcp, String srg, String desc, String hook, String hookDesc) {
        MethodNode m = Asm.method(node, mcp, srg, desc);
        if (m == null) {
            return false;
        }
        int found = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, hookDesc, false));
                m.instructions.insertBefore(insn, call);
                found++;
            }
        }
        return found > 0;
    }

    private static boolean nbt(ClassNode node) {
        MethodNode read = Asm.method(node, "readEntityFromNBT", "func_70037_a", NBT_DESC);
        MethodNode write = Asm.method(node, "writeEntityToNBT", "func_70014_b", NBT_DESC);
        if (read == null || write == null) {
            return false;
        }
        String desc = "(Lnet/minecraft/entity/passive/EntityAnimal;Lnet/minecraft/nbt/NBTTagCompound;)V";
        for (Object[] site : new Object[][]{{read, "read"}, {write, "write"}}) {
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, (String) site[1], desc, false));
            ((MethodNode) site[0]).instructions.insert(call);
        }
        return true;
    }

    private static boolean wolfSounds(ClassNode node) {
        MethodNode[] methods = {
                Asm.method(node, "getAmbientSound", "func_184639_G", "()" + SOUND),
                Asm.method(node, "getHurtSound", "func_184601_bQ", "(Lnet/minecraft/util/DamageSource;)" + SOUND),
                Asm.method(node, "getDeathSound", "func_184615_bR", "()" + SOUND)};
        for (MethodNode m : methods) {
            if (m == null) {
                return false;
            }
        }
        String desc = "(" + SOUND + "Lnet/minecraft/entity/passive/EntityWolf;)" + SOUND;
        for (MethodNode m : methods) {
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.ARETURN) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "wolfSound", desc, false));
                    m.instructions.insertBefore(insn, call);
                }
            }
        }
        return true;
    }
}
