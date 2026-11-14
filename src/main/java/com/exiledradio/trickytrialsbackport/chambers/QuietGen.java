package com.exiledradio.trickytrialsbackport.chambers;

import net.minecraft.world.World;

/**
 * Block changes as today's world generation makes them: {@code WorldGenRegion.setBlock} never updates neighbors, so
 * a ladder, a door or a redstone wire placed before its support stays where it is. Forge's block snapshot capture does
 * the same in 1.12.2 ({@code World.setBlockState} skips {@code markAndNotifyBlock}); the snapshots taken are dropped
 * afterwards rather than restored. Nested inside someone else's capture, it leaves their snapshots alone.
 */
final class QuietGen {

    private QuietGen() {
    }

    static void run(World world, Runnable body) {
        boolean was = world.captureBlockSnapshots;
        int before = world.capturedBlockSnapshots.size();
        world.captureBlockSnapshots = true;
        try {
            body.run();
        } finally {
            world.captureBlockSnapshots = was;
            while (world.capturedBlockSnapshots.size() > before) {
                world.capturedBlockSnapshots.remove(world.capturedBlockSnapshots.size() - 1);
            }
        }
    }
}
