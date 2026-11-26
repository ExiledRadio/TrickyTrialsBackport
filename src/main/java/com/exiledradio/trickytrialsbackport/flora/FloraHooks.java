package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import java.util.Random;

/**
 * What the bytecode patches of this stream call ({@code asm}, {@code docs/PATCHES.md} P1-P3). Each one keeps
 * 1.12.2's own behavior while the stream is off or not yet registered.
 */
public final class FloraHooks {

    private FloraHooks() {
    }

    private static boolean on() {
        return FloraContent.cactusFlower != null && FloraContent.enabled();
    }

    // ===========================================================================================
    // P1: CactusBlock.randomTick (1.21.5)
    // ===========================================================================================

    /**
     * Called first in {@code BlockCactus.updateTick}; true means it was handled. 1.21.5's random tick: a cactus with
     * air above ages to 15 whatever its height (1.12.2 stops at three tall); at age 8 a flower may open on top, one
     * time in ten (one in four on a cactus three or more tall), where a cactus could grow; at 15 a cactus under three
     * tall grows one taller. Forge's crop events wrap it as they wrap 1.12.2's growth.
     */
    public static boolean cactusTick(Block cactus, World world, BlockPos pos, IBlockState state, Random rand) {
        if (!on()) {
            return false;
        }
        if (!world.isAreaLoaded(pos, 1)) {
            return true;
        }
        BlockPos up = pos.up();
        if (!world.isAirBlock(up)) {
            return true;
        }
        int age = state.getValue(BlockCactus.AGE);
        int height = 1;
        while (world.getBlockState(pos.down(height)).getBlock() == cactus) {
            if (++height == 3 && age == 15) {
                return true;
            }
        }
        if (!ForgeHooks.onCropsGrowPre(world, up, state, true)) {
            return true;
        }
        if (age == 8 && Blocks.CACTUS.canBlockStay(world, up)) {
            double chance = height >= 3 ? 0.25D : 0.1D;
            if (rand.nextDouble() <= chance) {
                world.setBlockState(up, FloraContent.cactusFlower.getDefaultState());
            }
        } else if (age == 15 && height < 3) {
            world.setBlockState(up, cactus.getDefaultState());
            IBlockState reset = state.withProperty(BlockCactus.AGE, 0);
            world.setBlockState(pos, reset, 4);
            reset.neighborChanged(world, up, cactus, pos);
        }
        if (age < 15) {
            world.setBlockState(pos, state.withProperty(BlockCactus.AGE, age + 1), 4);
        }
        ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        return true;
    }

    // ===========================================================================================
    // P2: the dead bush's ground (1.21.5's DryVegetationBlock.mayPlaceOn)
    // ===========================================================================================

    /**
     * Called first in {@code BlockDeadBush.canSustainBush}; true means the ground holds a dead bush. 1.21.5 lets it stand
     * on all of {@code #dry_vegetation_may_place_on}: sand, terracotta, {@code #dirt} (grass and mycelium too) and
     * farmland. False falls through to 1.12.2's own list.
     */
    public static boolean deadBushSoil(IBlockState soil) {
        return on() && BlockVegetation.isDryVegetationSoil(soil);
    }

    // ===========================================================================================
    // P3: desert ambience on the client (1.21.5's AmbientDesertBlockSoundsPlayer)
    // ===========================================================================================

    /**
     * Replaces the {@code randomDisplayTick} call in {@code WorldClient}'s ambient loop: the block's own display tick,
     * then 1.21.5's desert sounds for 1.12.2's sand and dead bush, which have no display tick of their own to override,
     * and 1.21.5's falling leaves ({@code TintedParticleLeavesBlock}) for every leaves block, any mod's, that does not
     * draw its own ({@link FallingLeaves}).
     */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void displayTick(Block block, IBlockState state, World world, BlockPos pos, Random rand) {
        block.randomDisplayTick(state, world, pos, rand);
        if (!on()) {
            return;
        }
        if (block == Blocks.SAND) {
            sand(world, pos, rand);
        } else if (block == Blocks.DEADBUSH) {
            deadBush(world, pos, rand);
        } else if (block instanceof BlockLeaves && FallingLeaves.on()) {
            IBlockState source = FallingLeaves.source(state);
            if (source != null) {
                com.exiledradio.trickytrialsbackport.flora.client.FloraClient.fallingLeaf(state, source, world, pos, rand);
            }
        }
    }

    /** {@code playAmbientSandSounds}: one time in 2100 under open air, if sand lies around on all four sides. */
    static void sand(World world, BlockPos pos, Random rand) {
        if (world.getBlockState(pos.up()).getBlock() == Blocks.AIR && rand.nextInt(2100) == 0 && sandAround(world, pos)) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), FloraContent.SAND_IDLE, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
        }
    }

    /** {@code playAmbientDeadBushSounds}: one time in 130 (a third as often on red sand or terracotta). */
    static void deadBush(World world, BlockPos pos, Random rand) {
        if (rand.nextInt(130) == 0) {
            IBlockState below = world.getBlockState(pos.down());
            if ((isRedSand(below) || BlockVegetation.isTerracottaTag(below)) && rand.nextInt(3) != 0) {
                return;
            }
            if (dryVegetationSounds(world, pos.down())) {
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), FloraContent.DEAD_BUSH_IDLE, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
            }
        }
    }

    /** {@code shouldPlayDesertDryVegetationBlockSounds}: this block and the one under it are sand or terracotta. */
    static boolean dryVegetationSounds(World world, BlockPos pos) {
        return triggersDry(world.getBlockState(pos)) && triggersDry(world.getBlockState(pos.down()));
    }

    /** {@code #triggers_ambient_desert_dry_vegetation_block_sounds}: terracotta, sand, red sand. */
    private static boolean triggersDry(IBlockState state) {
        return state.getBlock() == Blocks.SAND || BlockVegetation.isTerracottaTag(state);
    }

    private static boolean isRedSand(IBlockState state) {
        return state.getBlock() == Blocks.SAND && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND;
    }

    /** {@code shouldPlayAmbientSandSound}, as vanilla counts it. */
    private static boolean sandAround(World world, BlockPos pos) {
        int found = 0;
        int checked = 0;
        for (EnumFacing dir : new EnumFacing[] {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST}) {
            if (columnHasSand(world, pos.offset(dir, 8)) && found++ >= 3) {
                return true;
            }
            checked++;
            if (4 - checked + found < 3) {
                return false;
            }
        }
        return false;
    }

    /** {@code columnContainsTriggeringBlock}: sand at the surface of this column, near this height. */
    private static boolean columnHasSand(World world, BlockPos pos) {
        int top = FloraFeatures.worldSurface(world, pos).getY() - 1;
        if (Math.abs(top - pos.getY()) > 5) {
            BlockPos p = pos.up(6);
            IBlockState above = world.getBlockState(p);
            p = p.down();
            for (int i = 0; i < 10; i++) {
                IBlockState s = world.getBlockState(p);
                if (above.getMaterial() == net.minecraft.block.material.Material.AIR && s.getBlock() == Blocks.SAND) {
                    return true;
                }
                above = s;
                p = p.down();
            }
            return false;
        }
        BlockPos surface = new BlockPos(pos.getX(), top, pos.getZ());
        return world.isAirBlock(surface.up()) && world.getBlockState(surface).getBlock() == Blocks.SAND;
    }
}
