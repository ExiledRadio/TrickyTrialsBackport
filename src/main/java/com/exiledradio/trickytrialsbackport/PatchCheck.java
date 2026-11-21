package com.exiledradio.trickytrialsbackport;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The release-jar check behind {@code tools/release/boot-server.sh}: with
 * {@code -Dtrickytrialsbackport.loadPatched=true} it loads and initializes every class this mod's coremod patches
 * (P1 to P25, {@code docs/PATCHES.md}), once the server has started, so the JVM verifies each one, and logs
 * {@code PATCHCHECK ok <n>} or one {@code PATCHCHECK FAIL} line per class.
 *
 * <p>Why: Mojang's classes that Forge does not patch keep ProGuard's stack-map frames, which mark a local TOP once
 * vanilla stops reading it. A patch that loads such a local is a VerifyError only in the real game, and only once the
 * class loads: the dev runs recompile Minecraft with javac and never show it, and some targets (a block, an AI) may
 * never load during a short boot. Without the property this does nothing.
 *
 * <p>A transformer added to {@code TrickyTrialsCore} adds its target classes here.
 */
public final class PatchCheck {

    static final String PROPERTY = TrickyTrialsBackport.MOD_ID + ".loadPatched";

    /** Targets on both sides, by patch. */
    private static final String[] COMMON = {
            "net.minecraft.block.BlockCactus", // P1
            "net.minecraft.block.BlockDeadBush", // P2
            "net.minecraft.entity.ai.EntityMoveHelper", // P4
            "net.minecraft.entity.Entity", // P5
            "net.minecraft.entity.EntityLivingBase", // P6
            "net.minecraft.entity.EntityCreature", // P7
            "net.minecraft.item.ItemDoor", // P8
            "net.minecraft.block.BlockFire", // P9
            "net.minecraft.enchantment.EnchantmentProtection", // P10
            "net.minecraft.entity.item.EntityEnderCrystal", // P11
            "net.minecraft.entity.projectile.EntityPotion", // P12
            "net.minecraft.world.storage.MapData", // P13
            "net.minecraft.world.Teleporter", // P14
            "net.minecraft.world.World", // P15
            "net.minecraft.server.MinecraftServer",
            "net.minecraft.entity.EntityLiving", // P16 (animals and copper gear)
            "net.minecraft.entity.passive.EntitySheep", // P17
            "net.minecraft.entity.passive.EntityAnimal", // P18
            "net.minecraft.entity.passive.EntityWolf", // P19
            "net.minecraft.entity.monster.EntityZombie", // P20
            "net.minecraft.entity.monster.EntityPigZombie",
            "net.minecraft.entity.passive.EntityZombieHorse", // P24a
    };

    /** Client-only targets. */
    private static final String[] CLIENT = {
            "net.minecraft.client.multiplayer.WorldClient", // P3
            "net.minecraft.client.model.ModelBiped", // P21
            "net.minecraft.client.model.ModelZombie",
            "net.minecraft.client.model.ModelZombieVillager",
    };

    /** Targets in another mod, checked only when that mod is installed. */
    private static final String[] OPTIONAL = {
            "git.jbredwards.fluidlogged_api.mod.common.fluid.util.ISpecializedFluidNeighborInfo$Vanilla", // P25
    };

    private PatchCheck() {
    }

    /** Every vanilla class a transformer of this mod patches on the given side. */
    public static List<String> targets(boolean client) {
        List<String> out = new ArrayList<String>(Arrays.asList(COMMON));
        if (client) {
            out.addAll(Arrays.asList(CLIENT));
        }
        return out;
    }

    /** The other mods' patched classes; absent ones are skipped. */
    public static List<String> optionalTargets() {
        return Arrays.asList(OPTIONAL);
    }

    /** Runs the check when the property asks for it. */
    static void runIfAsked() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        boolean client = FMLCommonHandler.instance().getSide().isClient();
        List<String> targets = targets(client);
        int failed = 0;
        int loaded = 0;
        for (String target : targets) {
            if (load(target)) {
                loaded++;
            } else {
                failed++;
            }
        }
        ClassLoader loader = PatchCheck.class.getClassLoader();
        for (String target : OPTIONAL) {
            // Ask for the bytes without loading: an absent mod is not a failure.
            if (loader.getResource(target.replace('.', '/') + ".class") == null) {
                continue;
            }
            if (load(target)) {
                loaded++;
            } else {
                failed++;
            }
        }
        if (failed == 0) {
            TrickyTrialsBackport.LOGGER.info("PATCHCHECK ok {} patched classes loaded and verified ({}).", loaded,
                    client ? "client" : "server");
        }
    }

    private static boolean load(String target) {
        try {
            Class.forName(target, true, PatchCheck.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            TrickyTrialsBackport.LOGGER.error("PATCHCHECK FAIL {}: {}", target, t.toString().split("\n")[0], t);
            return false;
        }
    }
}
