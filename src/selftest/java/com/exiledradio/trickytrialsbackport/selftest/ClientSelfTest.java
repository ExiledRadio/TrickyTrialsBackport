package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * {@code runClient -Pselftest=client}: what the headless suite cannot see, on a real client. From the title screen it
 * creates a superflat creative world and runs {@link ClientScenes} (the spear's held model and poses, steering a
 * happy ghast and a nautilus, the creaking's walk and freeze, shelves, and this mod's screens), then quits. Photos go
 * to {@code eclipse/trclient/screenshots}; the result is logged as {@code CLIENT SELFTEST DONE pass= fail=}.
 *
 * <p>Unattended: nothing here presses a key or moves the mouse, and the user's own input is kept out. The run task
 * passes {@code -Dfml.noGrab=true}, so the window never grabs the cursor, and this class keeps
 * {@code inGameHasFocus} false at every point 1.12.2 reads it, so the user's mouse never turns the player and
 * leaving the game for a screen never re-centers the user's cursor ({@code MouseHelper.ungrabMouseCursor}). A
 * window in the background does not pause the integrated server.
 */
public final class ClientSelfTest {

    private static final String WORLD = "trickytrials-client-test";
    /** The superflat preset: grass on dirt, nothing generated. */
    private static final String FLAT = "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;";

    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private File shots;
    private ClientScenes scenes;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientSelfTest());
        TrickyTrialsBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            TrickyTrialsBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            TrickyTrialsBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    // ---- keeping the user's mouse out ----------------------------------------------------------------------------

    private static void unfocus() {
        Minecraft.getMinecraft().inGameHasFocus = false;
    }

    /** Before 1.12.2 leaves the game for a screen: with no focus to give up, the cursor is left where it is. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        // Only on the way to a screen: going back to the game (null) is setIngameFocus's own call, which it would repeat.
        if (event.getGui() != null) {
            unfocus();
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        unfocus();
    }

    // ---- the run -------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        unfocus();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 100) {
                this.phase = 1;
                this.shots = new File(mc.gameDir, "trclient");
                FileUtils.deleteQuietly(new File(this.shots, "screenshots"));
                this.shots.mkdirs();
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                mc.gameSettings.pauseOnLostFocus = false;
                this.ticks = 0;
                // The integrated server would otherwise run the server self-tests for "client" and stop itself.
                System.clearProperty(TrickyTrialsBackport.MOD_ID + ".selftest");
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.setGeneratorOptions(FLAT);
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && mc.getIntegratedServer() != null && this.ticks >= 40) {
                this.phase = 2;
                this.scenes = new ClientScenes(mc, this.shots, this::ok);
            } else if (this.phase == 2) {
                if (mc.currentScreen instanceof GuiIngameMenu) {
                    mc.displayGuiScreen(null);
                }
                if (this.scenes.tick()) {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            TrickyTrialsBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        TrickyTrialsBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
