package com.exiledradio.trickytrialsbackport.locator.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Stream 18's client side: the waypoints the server sends and the bar that shows them. */
@SideOnly(Side.CLIENT)
public final class LocatorClient {

    private LocatorClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientWaypoints());
        MinecraftForge.EVENT_BUS.register(new LocatorBarHud());
    }
}
