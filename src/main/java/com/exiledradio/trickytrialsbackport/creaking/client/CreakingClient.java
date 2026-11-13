package com.exiledradio.trickytrialsbackport.creaking.client;

import com.exiledradio.trickytrialsbackport.creaking.CreakingContent;
import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;

import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** The creaking stream's renderer; called from the mod's pre-init on a client only. */
public final class CreakingClient {

    private CreakingClient() {
    }

    public static void init() {
        if (CreakingContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityCreaking.class, RenderCreaking::new);
        }
    }
}
