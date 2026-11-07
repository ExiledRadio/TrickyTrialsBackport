package com.exiledradio.trickytrialsbackport.crafter.client;

import com.exiledradio.trickytrialsbackport.common.Guis;
import com.exiledradio.trickytrialsbackport.crafter.ContainerCrafter;
import com.exiledradio.trickytrialsbackport.crafter.CrafterContent;
import com.exiledradio.trickytrialsbackport.crafter.TileEntityCrafter;

import net.minecraft.tileentity.TileEntity;

/** The crafter's screen; called from the mod's pre-init on a client only. */
public final class CrafterClient {

    private CrafterClient() {
    }

    public static void init() {
        if (!CrafterContent.enabled()) {
            return;
        }
        Guis.screen(CrafterContent.GUI, (player, world, pos) -> {
            TileEntity te = world.getTileEntity(pos);
            return te instanceof TileEntityCrafter ? new GuiCrafter(player.inventory, new ContainerCrafter(player.inventory, (TileEntityCrafter) te)) : null;
        });
    }
}
