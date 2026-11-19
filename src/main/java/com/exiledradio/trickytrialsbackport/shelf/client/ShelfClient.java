package com.exiledradio.trickytrialsbackport.shelf.client;

import com.exiledradio.trickytrialsbackport.shelf.ShelfContent;
import com.exiledradio.trickytrialsbackport.shelf.TileEntityShelf;

import net.minecraftforge.fml.client.registry.ClientRegistry;

/** The shelf's item renderer; called from the mod's pre-init on a client only. */
public final class ShelfClient {

    private ShelfClient() {
    }

    public static void init() {
        if (ShelfContent.enabled()) {
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityShelf.class, new RenderShelf());
        }
    }
}
