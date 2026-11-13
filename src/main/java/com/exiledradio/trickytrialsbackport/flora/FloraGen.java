package com.exiledradio.trickytrialsbackport.flora;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * 1.12.2's decoration: after each new overworld chunk's biome decorator ({@code DecorateBiomeEvent.Post}), the
 * chunk's biome gets 1.21.5's spring plants, in vanilla's order, with the chunk's own population random. Positions
 * are drawn in the 16x16 square at the chunk corner + 8, as 1.12.2 decorates, so nothing reaches an ungenerated chunk.
 *
 * <ul>
 *   <li>The patches: {@link FloraFeatures#vegetation}.</li>
 *   <li>A tree selector of the biome: the forest's and dark forest's trees get their leaf litter
 *       ({@link FallenTrees#litterUnderTrees}), then the selector's draws place its fallen trees
 *       ({@link FallenTrees#fallenTreeAttempts}). 1.12.2's own trees stay as they are.</li>
 *   <li>1.12.2 grows its own cacti in deserts and badlands; 1.21.5's cactus columns carry a flower one time in four,
 *       so each cactus standing in the square with air above gets one with that chance ({@link #flowerCacti}).</li>
 * </ul>
 *
 * <p>Old chunks are never touched. A generator that does not run 1.12.2's biome decorator (Caves &amp; Cliffs' 1.18
 * terrain) never fires this event; it calls {@link FloraFeatures} and {@link FallenTrees} itself.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class FloraGen {

    private FloraGen() {
    }

    @SubscribeEvent
    public static void decorated(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        if (!FloraContent.enabled() || FloraContent.bush == null || world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        BlockPos corner = event.getChunkPos().getBlock(0, 0, 0);
        Biome biome = world.getBiome(corner.add(16, 0, 16));
        decorate(world, event.getRand(), corner, FloraBiomes.modernName(biome));
    }

    /** Everything this stream adds to one 1.12.2 chunk whose biome (at the chunk corner + 16, as 1.12.2 decorates) is given. */
    public static void decorate(World world, Random rand, BlockPos corner, String biome) {
        if (biome == null) {
            return;
        }
        BlockPos origin = new BlockPos(corner.getX() + 8, 0, corner.getZ() + 8);
        for (String feature : FloraBiomes.features(biome)) {
            if (feature.startsWith("[")) {
                String selector = feature.substring(1, feature.length() - 1);
                if (FallenTrees.dropsLitter(selector)) {
                    FallenTrees.litterUnderTrees(world, rand, origin);
                }
                FallenTrees.fallenTreeAttempts(world, rand, origin, selector, feature);
            } else if (feature.startsWith("patch_cactus")) {
                flowerCacti(world, rand, origin);
            } else {
                FloraFeatures.place(world, rand, origin, feature);
            }
        }
    }

    /** One time in four, a cactus flower on each cactus column in the square that has air above it. */
    static void flowerCacti(World world, Random rand, BlockPos origin) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos above = FloraFeatures.oceanFloor(world, origin.add(dx, 0, dz));
                if (world.getBlockState(above.down()).getBlock() == Blocks.CACTUS && world.isAirBlock(above)
                        && rand.nextInt(4) == 0) {
                    world.setBlockState(above, FloraContent.cactusFlower.getDefaultState(), FloraFeatures.FLAGS);
                }
            }
        }
    }
}
