package com.exiledradio.trickytrialsbackport.breeze.client;

import com.exiledradio.trickytrialsbackport.breeze.BreezeContent;
import com.exiledradio.trickytrialsbackport.breeze.EntityBreeze;
import com.exiledradio.trickytrialsbackport.breeze.EntityBreezeWindCharge;
import com.exiledradio.trickytrialsbackport.breeze.EntityWindCharge;

import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** The breeze stream's renderers; called from the mod's pre-init on a client only. */
public final class BreezeClient {

    private BreezeClient() {
    }

    public static void init() {
        if (!BreezeContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityBreeze.class, RenderBreeze::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityWindCharge.class, RenderWindCharge::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityBreezeWindCharge.class, RenderWindCharge::new);
    }
}
