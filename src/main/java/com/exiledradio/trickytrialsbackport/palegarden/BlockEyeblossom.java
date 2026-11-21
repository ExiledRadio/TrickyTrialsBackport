package com.exiledradio.trickytrialsbackport.palegarden;

import com.exiledradio.trickytrialsbackport.ModConfig;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Open and closed eyeblossoms: 1.21.4's {@code EyeblossomBlock}, a flower that opens at night and closes by
 * day.
 *
 * <ul>
 *   <li><b>When</b>: open from day time 12600 to 23400, as 1.21.11's {@code timeline/day} sets
 *       {@code eyeblossom_open}; in a world with no day cycle (the Nether, the End) they stay as they are.</li>
 *   <li><b>Switching</b>: on a random tick (with the long sound), or on a scheduled tick (the short sound).
 *       A switch schedules every eyeblossom of the old kind within 3 blocks across and 2 up or down to check
 *       too, after 5 to 10 ticks per block of distance, so a field opens as a wave.</li>
 *   <li>An open eyeblossom on pale moss sometimes makes a sound (client, one time in 700).</li>
 *   <li>Bees of any mod (an entity named {@code bee}) touching one get 25 ticks of Poison, except on
 *       Peaceful.</li>
 * </ul>
 * Map colors: open {@code COLOR_ORANGE} (1.12.2's adobe), closed the pale oak leaves' {@code METAL} (iron).
 *
 * <p>Not vanilla, on request: with {@code paleGarden.eyeblossomLight} on (the default) an open eyeblossom and its pot
 * give off a dim block light, {@link #LIGHT}; closed ones give none. Open and closed are separate blocks, so the
 * switch at dusk and dawn is a block change and {@code World.setBlockState} rechecks the light. The level comes from
 * the block state ({@code getLightValue(IBlockState)}), so the lighting engine, Phosphor, OptiFine and Celeritas
 * all see it.
 */
public class BlockEyeblossom extends BlockBush {

    /** {@code Block.column(6, 0, 11)}: a flower's shape. */
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.6875D, 0.6875D);

    /** The light an open eyeblossom gives off with {@code paleGarden.eyeblossomLight} on: fairly dim. */
    public static final int LIGHT = 5;

    public final boolean open;

    public BlockEyeblossom(boolean open) {
        super(net.minecraft.block.material.Material.PLANTS, open ? MapColor.ADOBE : MapColor.IRON);
        this.open = open;
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setTickRandomly(true);
    }

    /** @return true at the time of day when eyeblossoms are open, false when closed; {@code current} where there is no day */
    public static boolean openNow(World world, boolean current) {
        if (!world.provider.isSurfaceWorld()) {
            return current;
        }
        long time = world.getWorldTime() % 24000L;
        if (time < 0) {
            time += 24000L;
        }
        return time >= 12600L && time < 23401L;
    }

    /** @return {@link #LIGHT} for an open eyeblossom with {@code paleGarden.eyeblossomLight} on, else 0 */
    public static int light(boolean open) {
        return open && ModConfig.paleGarden.eyeblossomLight ? LIGHT : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return light(this.open);
    }

    /** @return the eyeblossom of the other kind */
    public BlockEyeblossom other() {
        return this.open ? PaleGardenContent.closedEyeblossom : PaleGardenContent.openEyeblossom;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE.offset(state.getOffset(world, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (trySwitch(world, pos, random)) {
            world.playSound(null, pos, this.other().open ? PaleGardenContent.EYEBLOSSOM_OPEN_LONG : PaleGardenContent.EYEBLOSSOM_CLOSE_LONG,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        super.updateTick(world, pos, state, random);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (trySwitch(world, pos, rand)) {
            world.playSound(null, pos, this.other().open ? PaleGardenContent.EYEBLOSSOM_OPEN : PaleGardenContent.EYEBLOSSOM_CLOSE,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        super.updateTick(world, pos, state, rand);
    }

    /** {@code tryChangingState}. */
    private boolean trySwitch(World world, BlockPos pos, Random rand) {
        if (world.isRemote || world.getBlockState(pos).getBlock() != this || openNow(world, this.open) == this.open) {
            return false;
        }
        BlockEyeblossom to = other();
        world.setBlockState(pos, to.getDefaultState(), 3);
        for (BlockPos p : BlockPos.getAllInBox(pos.add(-3, -2, -3), pos.add(3, 2, 3))) {
            if (world.getBlockState(p).getBlock() == this) {
                double d = Math.sqrt(pos.distanceSq(p));
                int lo = (int) (d * 5.0D);
                int hi = (int) (d * 10.0D);
                world.scheduleUpdate(p.toImmutable(), this, lo + rand.nextInt(hi - lo + 1));
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (this.open && rand.nextInt(700) == 0 && world.getBlockState(pos.down()).getBlock() == PaleGardenContent.mossBlock) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), PaleGardenContent.EYEBLOSSOM_IDLE, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
        }
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && world.getDifficulty() != EnumDifficulty.PEACEFUL && entity instanceof EntityLivingBase) {
            ResourceLocation id = EntityList.getKey(entity);
            EntityLivingBase living = (EntityLivingBase) entity;
            if (id != null && "bee".equals(id.getPath()) && !living.isPotionActive(MobEffects.POISON)) {
                living.addPotionEffect(new PotionEffect(MobEffects.POISON, 25));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** A potted eyeblossom: switches with the time of day on random ticks, with the long sound. */
    public static final class Potted extends BlockPotted {
        public final boolean open;

        public Potted(boolean open) {
            this.open = open;
            setTickRandomly(true);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getLightValue(IBlockState state) {
            return light(this.open);
        }

        @Override
        public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
            boolean now = openNow(world, this.open);
            if (now != this.open) {
                Potted to = this.open ? PaleGardenContent.pottedClosedEyeblossom : PaleGardenContent.pottedOpenEyeblossom;
                world.setBlockState(pos, to.getDefaultState(), 3);
                SoundEvent sound = now ? PaleGardenContent.EYEBLOSSOM_OPEN_LONG : PaleGardenContent.EYEBLOSSOM_CLOSE_LONG;
                world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
}
