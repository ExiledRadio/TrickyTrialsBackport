package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperGolemStatue;
import com.exiledradio.trickytrialsbackport.coppergolem.CopperGolemContent;
import com.exiledradio.trickytrialsbackport.coppergolem.EntityCopperGolem;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperGolemStatue;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** The copper golem stream's client side, from the mod's pre-init on a client only. */
public final class CopperGolemClient {

    private CopperGolemClient() {
    }

    public static void init() {
        if (!CopperGolemContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityCopperGolem.class, RenderCopperGolem::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCopperChest.class, new CopperGolemRenderers.Chest());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCopperGolemStatue.class, new CopperGolemRenderers.Statue());
        MinecraftForge.EVENT_BUS.register(new CopperGolemClient.Events());
    }

    public static final class Events {
        /** The chest and statue items are drawn by their own renderer ({@code builtin/entity} models). */
        @SubscribeEvent
        public void models(ModelRegistryEvent event) {
            CopperGolemRenderers.Items items = new CopperGolemRenderers.Items();
            for (Block[][] set : new Block[][][] {CopperGolemContent.CHESTS, CopperGolemContent.STATUES}) {
                for (Block[] row : set) {
                    for (Block b : row) {
                        Item item = Item.getItemFromBlock(b);
                        if (b instanceof BlockCopperChest || b instanceof BlockCopperGolemStatue) {
                            item.setTileEntityItemStackRenderer(items);
                        }
                    }
                }
            }
        }
    }
}
