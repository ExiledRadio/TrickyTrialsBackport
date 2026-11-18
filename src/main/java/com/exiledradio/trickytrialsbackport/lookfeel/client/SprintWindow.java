package com.exiledradio.trickytrialsbackport.lookfeel.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.lookfeel.LookAndFeel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * 1.21.9's Sprint Window ({@code LocalPlayer.aiStep}: {@code sprintTriggerTime = options.sprintWindow()}): the ticks
 * in which a second tap of forward starts a sprint. 1.12.2 always gives 7.
 *
 * <p>1.12.2's player counts its {@code sprintToggleTimer} down at the start of each update and sets it to 7 on the
 * first tap. A 7 the countdown from last tick's value cannot give is a tap made this tick; the timer is set to the
 * option's value then, the same tick, as 1.21.9 sets it. At 0 a double tap never sprints, as in 1.21.9.
 */
@SideOnly(Side.CLIENT)
public final class SprintWindow {

    private static Field timer;
    private static boolean failed;

    private EntityPlayerSP lastPlayer;
    private int last;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        // A paused game does not update the player, so its timer did not count down.
        // A hidden option (Core's Look & Feel page turned its part off) leaves 1.12.2's 7 ticks.
        if (player == null || mc.isGamePaused() || !LookAndFeel.SPRINT_WINDOW.isShown()) {
            return;
        }
        int window = (int) Math.round(LookAndFeel.SPRINT_WINDOW.get());
        try {
            if (timer == null) {
                timer = ObfuscationReflectionHelper.findField(EntityPlayerSP.class, "field_71156_d");
            }
            int value = timer.getInt(player);
            int counted = player == this.lastPlayer && this.last > 0 ? this.last - 1 : 0;
            if (value == 7 && counted != 7 && window != 7) {
                value = window;
                timer.setInt(player, value);
            }
            this.lastPlayer = player;
            this.last = value;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            TrickyTrialsBackport.LOGGER.warn("Look and feel: Sprint Window cannot reach the player's sprint timer.", e);
        }
    }
}
