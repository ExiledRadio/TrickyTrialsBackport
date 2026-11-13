package com.exiledradio.trickytrialsbackport.decor.client;

import com.exiledradio.trickytrialsbackport.decor.DecorContent;
import com.exiledradio.trickytrialsbackport.decor.EntityModernPainting;

import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client side of stream 9: the painting renderer. */
public final class DecorClient {

    private DecorClient() {
    }

    public static void init() {
        if (DecorContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityModernPainting.class, RenderModernPainting::new);
        }
    }
}
