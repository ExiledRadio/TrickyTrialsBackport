package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;
import com.exiledradio.trickytrialsbackport.lookfeel.Rules;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;

/**
 * 1.21.2's "Reduce FPS when" ({@code FramerateLimitTracker}): 10 frames a second while the window is minimized;
 * with AFK, also 30 after a minute without a key, click or mouse movement and 10 after ten minutes.
 *
 * <p>The game's own limit (Max Framerate, through {@code Display.sync}) stays as it is; this waits out the rest of
 * a frame at the end of each one when the throttle is lower. 1.21.2's 60 for menus without a world is not here:
 * 1.12.2 holds those at 30 itself.
 */
@SideOnly(Side.CLIENT)
public final class FramerateLimiter {

    /** For the log and a look in game: the limit the last frame was held to (0 when none). */
    public static volatile int lastLimit;

    private long lastInput = Minecraft.getSystemTime();
    private long lastFrame = System.nanoTime();

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        this.lastInput = Minecraft.getSystemTime();
    }

    @SubscribeEvent
    public void mouse(InputEvent.MouseInputEvent event) {
        this.lastInput = Minecraft.getSystemTime();
    }

    @SubscribeEvent
    public void screenKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        this.lastInput = Minecraft.getSystemTime();
    }

    @SubscribeEvent
    public void screenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        this.lastInput = Minecraft.getSystemTime();
    }

    @SubscribeEvent
    public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean afk = "afk".equals(LookAndFeel.INACTIVITY_FPS_LIMIT.get());
        int limit = Rules.framerateLimit(afk, Minecraft.getSystemTime() - this.lastInput, !Display.isVisible(),
                mc.gameSettings.limitFramerate);
        lastLimit = limit;
        long now = System.nanoTime();
        if (limit > 0) {
            long wait = this.lastFrame + 1000000000L / limit - now;
            if (wait > 0) {
                try {
                    Thread.sleep(wait / 1000000L, (int) (wait % 1000000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            now = System.nanoTime();
        }
        this.lastFrame = now;
    }
}
