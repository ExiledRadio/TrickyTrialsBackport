package com.exiledradio.trickytrialsbackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (see {@code docs/PLAN.md}) and registers its packets here, inside
 * its own block, so two streams can never pick the same number and merges stay mechanical:
 *
 * <pre>
 *   0-4   common        35-39 trials       65-69 creaking      95-99   copperGolem
 *   5-9   tuff          40-44 effects      70-74 resin         100-104 copperGear
 *   10-14 copper        45-49 decor        75-79 flora         105-109 shelves
 *   15-19 breeze        50-54 chambers     80-84 animals       110-114 spears
 *   20-24 bogged        55-59 bundles      85-89 happyGhast    115-119 nautilus
 *   25-29 mace          60-64 paleGarden   90-94 locatorBar    120-124 mounts
 *   30-34 crafter                                              125-129 tweaks
 *   130-134 babies  135-139 sulfur  140-144 sulfurCube  145-149 sulfurCaves
 *   150-154 poplar  155-159 softBlocks  160-164 camps
 * </pre>
 *
 * <p>The channel name is short because Forge caps channel names at 20 characters, one fewer than the
 * mod id.
 */
public final class NetworkHandler {

    public static final String CHANNEL = "trbackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // Streams register their packets below, in id order.
        channel.registerMessage(com.exiledradio.trickytrialsbackport.common.GustMessage.Handler.class,
                com.exiledradio.trickytrialsbackport.common.GustMessage.class, 0, Side.CLIENT);
        channel.registerMessage(com.exiledradio.trickytrialsbackport.trial.TrialMessage.Handler.class,
                com.exiledradio.trickytrialsbackport.trial.TrialMessage.class, 35, Side.CLIENT);
    }
}
