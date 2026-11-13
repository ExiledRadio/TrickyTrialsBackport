package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P1 (docs/PATCHES.md): cacti grow flowers, as 1.21.5's {@code CactusBlock.randomTick}.
 *
 * <p>Head of {@code BlockCactus.updateTick}: {@code if (FloraHooks.cactusTick(this, world, pos, state, rand)) return;}.
 * The hook runs 1.21.5's tick and answers true, or false (1.12.2's own tick runs) while the spring plants are off.
 */
public final class CactusFlowerTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.block.BlockCactus";
    private static final String DESC = "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode tick = Asm.method(node, "updateTick", "func_180650_b", DESC);
        if (tick == null) {
            Asm.LOG.warn("P1 cactus flowers: updateTick not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        InsnList loads = new InsnList();
        for (int i = 0; i <= 4; i++) {
            loads.add(new VarInsnNode(Opcodes.ALOAD, i));
        }
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.RETURN));
        Asm.headReturnIf(tick, loads, Asm.FLORA_HOOKS, "cactusTick", "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;"
                + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)Z", ret);
        applied = true;
        Asm.LOG.info("P1 cactus flowers: patched {}.", TARGET);
        return Asm.write(node);
    }
}
