package com.exiledradio.trickytrialsbackport;

import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tricky Trials Backport (working name): Minecraft 1.21 and every later 1.21 drop for 1.12.2.
 *
 * <h2>Layout</h2>
 * One package per stream of content (see {@code docs/PLAN.md}): each registers its own blocks, items,
 * entities and sounds from its own {@code @Mod.EventBusSubscriber} class, behind its own config
 * category's {@code enabled} switch. {@code common} holds what several streams share. This class only
 * calls each stream's lifecycle hooks, one line per stream.
 *
 * <h2>Behavior source</h2>
 * Current Java Edition as minecraft.wiki describes it; the 1.21.x client jars for assets and names.
 */
@Mod(
        modid = TrickyTrialsBackport.MOD_ID,
        name = TrickyTrialsBackport.MOD_NAME,
        version = TrickyTrialsBackport.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:backportcore@[1.3.0,);before:quark"
)
public final class TrickyTrialsBackport {

    public static final String MOD_ID = "trickytrialsbackport";
    /** From gradle.properties' mod_name, the one place the (not yet final) display name is written. */
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static TrickyTrialsBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // This mod's spawn eggs in Miscellaneous where vanilla lists each mob's (Backport Core's creative
        // placement).
        com.exiledradio.backportcore.creative.CreativePlacement.placeSpawnEggs(MOD_ID);
        NetworkHandler.init();
        com.exiledradio.trickytrialsbackport.common.WindBurst.FALL_AFTER_WIND_CHARGE.register();
        com.exiledradio.trickytrialsbackport.chambers.ChambersContent.preInit();
        com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent.preInit();
        com.exiledradio.trickytrialsbackport.creaking.CreakingContent.preInit();
        com.exiledradio.trickytrialsbackport.resin.ResinContent.preInit();
        com.exiledradio.trickytrialsbackport.flora.FloraContent.preInit();
        com.exiledradio.trickytrialsbackport.animals.AnimalsContent.preInit();
        com.exiledradio.trickytrialsbackport.ghast.GhastContent.preInit();
        com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel.preInit();

        // Client-only setup lives in a class a dedicated server never loads. The call names no client
        // type, so this class verifies on a server - a guarded method here would not.
        if (event.getSide().isClient()) {
            com.exiledradio.trickytrialsbackport.client.ClientSetup.init();
            com.exiledradio.trickytrialsbackport.breeze.client.BreezeClient.init();
            com.exiledradio.trickytrialsbackport.bogged.client.BoggedClient.init();
            com.exiledradio.trickytrialsbackport.crafter.client.CrafterClient.init();
            com.exiledradio.trickytrialsbackport.trial.client.TrialFx.init();
            com.exiledradio.trickytrialsbackport.bundle.client.BundleClient.init();
            com.exiledradio.trickytrialsbackport.effects.client.EffectsClient.init();
            com.exiledradio.trickytrialsbackport.decor.client.DecorClient.init();
            com.exiledradio.trickytrialsbackport.palegarden.client.PaleGardenClient.init();
            com.exiledradio.trickytrialsbackport.creaking.client.CreakingClient.init();
            com.exiledradio.trickytrialsbackport.flora.client.FloraClient.init();
            com.exiledradio.trickytrialsbackport.animals.client.AnimalsClient.init();
            com.exiledradio.trickytrialsbackport.ghast.client.GhastClient.init();
            com.exiledradio.trickytrialsbackport.lookfeel.client.LookAndFeelClient.init();
            com.exiledradio.trickytrialsbackport.tweaks.client.TweaksClient.init();
            com.exiledradio.trickytrialsbackport.locator.client.LocatorClient.init();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.exiledradio.trickytrialsbackport.common.Guis.init();
        com.exiledradio.trickytrialsbackport.copper.CopperContent.init();
        com.exiledradio.trickytrialsbackport.effects.EffectsContent.init();
        com.exiledradio.trickytrialsbackport.chambers.ChambersContent.init();
        com.exiledradio.trickytrialsbackport.palegarden.PaleGardenContent.init();
        com.exiledradio.trickytrialsbackport.animals.AnimalsContent.init();
        com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel.init();
        com.exiledradio.trickytrialsbackport.tweaks.EnderPearlChunks.init();
        com.exiledradio.trickytrialsbackport.locator.LocatorBar.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        com.exiledradio.trickytrialsbackport.bogged.BoggedContent.postInit();
        com.exiledradio.trickytrialsbackport.decor.DecorContent.postInit();
        com.exiledradio.trickytrialsbackport.flora.FloraContent.postInit();
        com.exiledradio.trickytrialsbackport.tweaks.SpawnChunks.postInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    }

    /**
     * Starts the headless self-test when a test build asks for one ({@code -Pselftest=<stream>}).
     *
     * <p>Reached by name, so a release jar - which has no test classes - never links against them.
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        String selftest = System.getProperty(MOD_ID + ".selftest");
        if (selftest == null || selftest.isEmpty()) {
            return;
        }
        try {
            Class.forName("com.exiledradio.trickytrialsbackport.selftest.SelfTestRunner")
                    .getMethod("start", String.class).invoke(null, selftest);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("SELFTEST DONE pass=0 fail=1: '{}' requested but the test classes are not in this build.", selftest, e);
            // A test run must always end, or it holds its port and its log files.
            net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}
