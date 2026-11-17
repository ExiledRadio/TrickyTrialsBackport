package com.exiledradio.trickytrialsbackport.tweaks.client;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.tweaks.Tweaks;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The client half of stream 25: gliding ends sprinting (1.21.4's {@code LocalPlayer.aiStep}). */
@SideOnly(Side.CLIENT)
public final class TweaksClient {

    private TweaksClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(TweaksClient.class);
    }

    /**
     * Sprinting is the client's own state; the player's tick decides it, then reports it to the server after this
     * event, so a sprint ended here is never sent.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isClient() && event.player == Minecraft.getMinecraft().player
                && event.player.isElytraFlying() && event.player.isSprinting() && Tweaks.on(ModConfig.tweaks.elytraEndsSprint)) {
            event.player.setSprinting(false);
        }
    }
}
