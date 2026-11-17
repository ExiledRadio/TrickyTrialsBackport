package com.exiledradio.trickytrialsbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stream 25's patches P5-P15 (docs/PATCHES.md), one method each, dispatched by target class. Each only puts a call to
 * {@code tweaks.MechanicsHooks} (or a sibling) where 1.12.2 has a constant, a call or a test, keeps the stack as it
 * was, and adds no jump except P6's and the head tests, which jump to a label the method already has or to one with
 * the method's first frame. A patch whose site is not as expected is skipped and logged.
 */
public final class MechanicsTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/trickytrialsbackport/tweaks/MechanicsHooks";
    private static final String PORTALS = "com/exiledradio/trickytrialsbackport/tweaks/PortalTravel";
    private static final String LEADS = "com/exiledradio/trickytrialsbackport/tweaks/Leads";
    private static final String SPAWN = "com/exiledradio/trickytrialsbackport/tweaks/SpawnChunks";

    /** The patches applied so far ("P5", ...); the self-test reads it. */
    public static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        switch (transformedName) {
            case "net.minecraft.entity.Entity":
                return patch(bytes, "P5", MechanicsTransformer::portalCooldown);
            case "net.minecraft.entity.EntityLivingBase":
                return patch(bytes, "P6", MechanicsTransformer::knockback);
            case "net.minecraft.entity.EntityCreature":
                return patch(bytes, "P7", MechanicsTransformer::leashSnap);
            case "net.minecraft.item.ItemDoor":
                return patch(bytes, "P8", MechanicsTransformer::doors);
            case "net.minecraft.block.BlockFire":
                return patch(bytes, "P9", MechanicsTransformer::fire);
            case "net.minecraft.enchantment.EnchantmentProtection":
                return patch(bytes, "P10", MechanicsTransformer::protection);
            case "net.minecraft.entity.item.EntityEnderCrystal":
                return patch(bytes, "P11", MechanicsTransformer::crystal);
            case "net.minecraft.entity.projectile.EntityPotion":
                return patch(bytes, "P12", MechanicsTransformer::splash);
            case "net.minecraft.world.storage.MapData":
                return patch(bytes, "P13", MechanicsTransformer::map);
            case "net.minecraft.world.Teleporter":
                return patch(bytes, "P14", MechanicsTransformer::platform);
            case "net.minecraft.world.World":
                return trailsAndTales() ? bytes : patch(bytes, "P15", MechanicsTransformer::spawnChunk);
            case "net.minecraft.server.MinecraftServer":
                return trailsAndTales() ? bytes : patch(bytes, "P15b", MechanicsTransformer::spawnArea);
            default:
                return bytes;
        }
    }

    interface Patch {
        /** @return false when the site was not found */
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

    /**
     * Trails &amp; Tales Backport patches the spawn chunk sites itself (its P10) and exposes the radius this mod sets;
     * its coremod jar is on the class path before any game class loads.
     */
    static boolean trailsAndTales() {
        return Launch.classLoader != null
                && Launch.classLoader.getResource("com/exiledradio/trailsandtalesbackport/mechanics/SpawnChunks.class") != null;
    }

    private static boolean isCall(AbstractInsnNode insn, int opcode, String mcp, String srg, String desc) {
        if (insn.getOpcode() != opcode) {
            return false;
        }
        MethodInsnNode m = (MethodInsnNode) insn;
        return (mcp.equals(m.name) || srg.equals(m.name)) && desc.equals(m.desc);
    }

    // P5 ----------------------------------------------------------------------------------------

    /** {@code Entity.getPortalCooldown}'s {@code 300} becomes {@code PortalTravel.cooldown(this)}. */
    private static boolean portalCooldown(ClassNode node) {
        MethodNode m = Asm.method(node, "getPortalCooldown", "func_82147_ab", "()I");
        if (m == null) {
            return false;
        }
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.SIPUSH && ((IntInsnNode) insn).operand == 300) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PORTALS, "cooldown", "(Lnet/minecraft/entity/Entity;)I", false));
                m.instructions.insert(insn, call);
                m.instructions.remove(insn);
                return true;
            }
        }
        return false;
    }

    // P6 ----------------------------------------------------------------------------------------

    /**
     * In {@code EntityLivingBase.attackEntityFrom}, before {@code if (attacker != null) { ... knockBack(...) } else {
     * ... }}: {@code if (MechanicsHooks.projectileKnockback(this, source, attacker)) goto <after the if-else>}.
     */
    private static boolean knockback(ClassNode node) {
        MethodNode m = Asm.method(node, "attackEntityFrom", "func_70097_a", "(Lnet/minecraft/util/DamageSource;F)Z");
        if (m == null) {
            return false;
        }
        AbstractInsnNode call = null;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (isCall(insn, Opcodes.INVOKEVIRTUAL, "knockBack", "func_70653_a", "(Lnet/minecraft/entity/Entity;FDD)V")) {
                if (call != null) {
                    return false;
                }
                call = insn;
            }
        }
        if (call == null) {
            return false;
        }
        // The nearest IFNULL before the call guards the block; its target is the else branch, preceded by a GOTO
        // to the end of the if-else.
        JumpInsnNode guard = null;
        for (AbstractInsnNode insn = call.getPrevious(); insn != null; insn = insn.getPrevious()) {
            if (insn.getOpcode() == Opcodes.IFNULL) {
                guard = (JumpInsnNode) insn;
                break;
            }
        }
        if (guard == null || !(guard.getPrevious() instanceof VarInsnNode) || guard.getPrevious().getOpcode() != Opcodes.ALOAD) {
            return false;
        }
        int attacker = ((VarInsnNode) guard.getPrevious()).var;
        AbstractInsnNode beforeElse = guard.label.getPrevious();
        while (beforeElse != null && beforeElse.getOpcode() < 0) {
            beforeElse = beforeElse.getPrevious();
        }
        if (beforeElse == null || beforeElse.getOpcode() != Opcodes.GOTO) {
            return false;
        }
        LabelNode end = ((JumpInsnNode) beforeElse).label;
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, attacker));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "projectileKnockback",
                "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;Lnet/minecraft/entity/Entity;)Z", false));
        hook.add(new JumpInsnNode(Opcodes.IFNE, end));
        m.instructions.insertBefore(guard.getPrevious(), hook);
        return true;
    }

    // P7 ----------------------------------------------------------------------------------------

    /** {@code EntityCreature.updateLeashedState}'s two {@code 10.0F} become {@code Leads.snapDistance()}. */
    private static boolean leashSnap(ClassNode node) {
        MethodNode m = Asm.method(node, "updateLeashedState", "func_110159_bB", "()V");
        if (m == null) {
            return false;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn instanceof LdcInsnNode && Float.valueOf(10.0F).equals(((LdcInsnNode) insn).cst)) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, LEADS, "snapDistance", "()F", false));
                n++;
            }
        }
        return n == 2;
    }

    // P8 ----------------------------------------------------------------------------------------

    /**
     * {@code ItemDoor.placeDoor}'s four {@code world.getBlockState(p).getBlock() == door}: after each {@code getBlock()}
     * that is compared with the {@code door} parameter, {@code MechanicsHooks.doorMatch(found, door)}.
     */
    private static boolean doors(ClassNode node) {
        MethodNode m = Asm.method(node, "placeDoor", "func_179235_a",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/block/Block;Z)V");
        if (m == null) {
            return false;
        }
        List<AbstractInsnNode> sites = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (isCall(insn, Opcodes.INVOKEINTERFACE, "getBlock", "func_177230_c", "()Lnet/minecraft/block/Block;")) {
                AbstractInsnNode next = insn.getNext();
                AbstractInsnNode cmp = next == null ? null : next.getNext();
                if (next != null && next.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) next).var == 3 && cmp != null
                        && (cmp.getOpcode() == Opcodes.IF_ACMPEQ || cmp.getOpcode() == Opcodes.IF_ACMPNE)) {
                    sites.add(insn);
                }
            }
        }
        if (sites.size() != 4) {
            return false;
        }
        for (AbstractInsnNode insn : sites) {
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 3));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "doorMatch",
                    "(Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;)Lnet/minecraft/block/Block;", false));
            m.instructions.insert(insn, call);
        }
        return true;
    }

    // P9 ----------------------------------------------------------------------------------------

    /** Head of {@code BlockFire.updateTick}: {@code if (MechanicsHooks.fireIdle(this, world, pos, rand)) return;} */
    private static boolean fire(ClassNode node) {
        MethodNode m = Asm.method(node, "updateTick", "func_180650_b",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V");
        if (m == null) {
            return false;
        }
        InsnList loads = new InsnList();
        loads.add(new VarInsnNode(Opcodes.ALOAD, 0));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 1));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 2));
        loads.add(new VarInsnNode(Opcodes.ALOAD, 4));
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.RETURN));
        Asm.headReturnIf(m, loads, HOOKS, "fireIdle",
                "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)Z", ret);
        return true;
    }

    // P10 ---------------------------------------------------------------------------------------

    /**
     * Heads of {@code EnchantmentProtection.getFireTimeForEntity} and {@code getBlastDamageReduction}:
     * {@code if (MechanicsHooks.protectionStacks()) return MechanicsHooks.fireTime(living, ticks)} (and
     * {@code blastKnockback}).
     */
    private static boolean protection(ClassNode node) {
        MethodNode fire = Asm.method(node, "getFireTimeForEntity", "func_92093_a", "(Lnet/minecraft/entity/EntityLivingBase;I)I");
        MethodNode blast = Asm.method(node, "getBlastDamageReduction", "func_92092_a", "(Lnet/minecraft/entity/EntityLivingBase;D)D");
        if (fire == null || blast == null) {
            return false;
        }
        headReturnValue(fire, Opcodes.ILOAD, Opcodes.IRETURN, "fireTime", "(Lnet/minecraft/entity/EntityLivingBase;I)I");
        headReturnValue(blast, Opcodes.DLOAD, Opcodes.DRETURN, "blastKnockback", "(Lnet/minecraft/entity/EntityLivingBase;D)D");
        return true;
    }

    private static void headReturnValue(MethodNode m, int load, int ret, String hook, String desc) {
        InsnList head = new InsnList();
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "protectionStacks", "()Z", false));
        LabelNode go = new LabelNode();
        head.add(new JumpInsnNode(Opcodes.IFEQ, go));
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(load, 1));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, desc, false));
        head.add(new InsnNode(ret));
        head.add(go);
        head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.insert(head);
    }

    // P11 ---------------------------------------------------------------------------------------

    /** Head of {@code EntityEnderCrystal.attackEntityFrom}: {@code if (MechanicsHooks.crystalIgnores(source)) return false;} */
    private static boolean crystal(ClassNode node) {
        MethodNode m = Asm.method(node, "attackEntityFrom", "func_70097_a", "(Lnet/minecraft/util/DamageSource;F)Z");
        if (m == null) {
            return false;
        }
        InsnList loads = new InsnList();
        loads.add(new VarInsnNode(Opcodes.ALOAD, 1));
        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.ICONST_0));
        ret.add(new InsnNode(Opcodes.IRETURN));
        Asm.headReturnIf(m, loads, HOOKS, "crystalIgnores", "(Lnet/minecraft/util/DamageSource;)Z", ret);
        return true;
    }

    // P12 ---------------------------------------------------------------------------------------

    /** {@code EntityPotion.applySplash}'s {@code this.getDistanceSq(entity)} becomes {@code MechanicsHooks.splashDistanceSq(this, entity)}. */
    private static boolean splash(ClassNode node) {
        MethodNode m = Asm.method(node, "applySplash", "func_190543_a", "(Lnet/minecraft/util/math/RayTraceResult;Ljava/util/List;)V");
        if (m == null) {
            return false;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (isCall(insn, Opcodes.INVOKEVIRTUAL, "getDistanceSq", "func_70068_e", "(Lnet/minecraft/entity/Entity;)D")) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "splashDistanceSq",
                        "(Lnet/minecraft/entity/projectile/EntityPotion;Lnet/minecraft/entity/Entity;)D", false));
                n++;
            }
        }
        return n == 1;
    }

    // P13 ---------------------------------------------------------------------------------------

    /** Before each return of {@code MapData.updateVisiblePlayers}: {@code MechanicsHooks.hideMapMarkers(this, player)}. */
    private static boolean map(ClassNode node) {
        MethodNode m = Asm.method(node, "updateVisiblePlayers", "func_76191_a",
                "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V");
        if (m == null) {
            return false;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "hideMapMarkers",
                        "(Lnet/minecraft/world/storage/MapData;Lnet/minecraft/entity/player/EntityPlayer;)V", false));
                m.instructions.insertBefore(insn, call);
                n++;
            }
        }
        return n > 0;
    }

    // P14 ---------------------------------------------------------------------------------------

    /** {@code Teleporter.placeInPortal}'s {@code world.setBlockState(pos, state)} become {@code MechanicsHooks.platformBlock(world, pos, state)}. */
    private static boolean platform(ClassNode node) {
        MethodNode m = Asm.method(node, "placeInPortal", "func_180266_a", "(Lnet/minecraft/entity/Entity;F)V");
        if (m == null) {
            return false;
        }
        int n = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (isCall(insn, Opcodes.INVOKEVIRTUAL, "setBlockState", "func_175656_a",
                    "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z")) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "platformBlock",
                        "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z", false));
                n++;
            }
        }
        return n > 0;
    }

    // P15 ---------------------------------------------------------------------------------------

    /** {@code World.isSpawnChunk(x, z)}'s body becomes {@code return SpawnChunks.isSpawnChunk(this, x, z);}. */
    private static boolean spawnChunk(ClassNode node) {
        MethodNode m = Asm.method(node, "isSpawnChunk", "func_72916_c", "(II)Z");
        if (m == null) {
            return false;
        }
        m.instructions.clear();
        m.tryCatchBlocks.clear();
        if (m.localVariables != null) {
            m.localVariables.clear();
        }
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWN, "isSpawnChunk", "(Lnet/minecraft/world/World;II)Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        return true;
    }

    /** {@code MinecraftServer.initialWorldChunkLoad}'s 192, -192 and 625 become {@code SpawnChunks}' sizes. */
    private static boolean spawnArea(ClassNode node) {
        MethodNode m = Asm.method(node, "initialWorldChunkLoad", "func_71222_d", "()V");
        if (m == null) {
            return false;
        }
        List<AbstractInsnNode> plus = new ArrayList<AbstractInsnNode>();
        List<AbstractInsnNode> minus = new ArrayList<AbstractInsnNode>();
        List<AbstractInsnNode> count = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.SIPUSH) {
                int v = ((IntInsnNode) insn).operand;
                if (v == 192) {
                    plus.add(insn);
                } else if (v == -192) {
                    minus.add(insn);
                } else if (v == 625) {
                    count.add(insn);
                }
            }
        }
        // 1.12.2 also keeps the two sizes in unused locals (int k = 192; int l = 625;): every one is replaced.
        if (plus.size() < 2 || minus.size() != 2 || count.isEmpty()) {
            return false;
        }
        for (AbstractInsnNode insn : plus) {
            m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWN, "prepareBlocks", "()I", false));
        }
        for (AbstractInsnNode insn : minus) {
            m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWN, "prepareStart", "()I", false));
        }
        for (AbstractInsnNode insn : count) {
            m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWN, "prepareCount", "()I", false));
        }
        return true;
    }
}
