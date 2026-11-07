package com.exiledradio.trickytrialsbackport.bogged.client;

import com.exiledradio.trickytrialsbackport.bogged.BoggedContent;
import com.exiledradio.trickytrialsbackport.bogged.EntityBogged;

import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** The bogged stream's renderer; called from the mod's pre-init on a client only. */
public final class BoggedClient {

    private BoggedClient() {
    }

    public static void init() {
        if (BoggedContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityBogged.class, RenderBogged::new);
        }
    }
}
