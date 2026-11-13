package com.exiledradio.trickytrialsbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 1.21.5's {@code VegetationBlock}: a plant that stays on the ground it may be placed on and breaks when that goes.
 *
 * <p>The ground is 1.21.5's {@code mayPlaceOn}: {@code #dirt} or farmland unless a subclass says otherwise. 1.12.2's
 * {@code BlockBush} asks the soil through Forge's {@code canSustainPlant}, which comes back to
 * {@link #canSustainBush}, so another mod's soil that answers for plains plants holds these too.
 */
public class BlockVegetation extends BlockBush {

    private final AxisAlignedBB box;
    private final boolean replaceable;
    private final boolean shearsOnly;

    /**
     * @param box        the outline, before any offset
     * @param replaceable 1.21.5's {@code replaceable()}: placing a block here replaces the plant
     * @param shearsOnly the loot table drops the plant only to shears or Silk Touch
     */
    BlockVegetation(Material material, MapColor color, SoundType sound, AxisAlignedBB box, boolean replaceable, boolean shearsOnly) {
        super(material, color);
        setSoundType(sound);
        setHardness(0.0F);
        this.box = box;
        this.replaceable = replaceable;
        this.shearsOnly = shearsOnly;
    }

    /** {@code Block.column(width, minY, maxY)} in sixteenths. */
    static AxisAlignedBB column(double width, double minY, double maxY) {
        double half = width / 2.0D;
        return new AxisAlignedBB((8.0D - half) / 16.0D, minY / 16.0D, (8.0D - half) / 16.0D, (8.0D + half) / 16.0D, maxY / 16.0D,
                (8.0D + half) / 16.0D);
    }

    /** {@code VegetationBlock.mayPlaceOn}: {@code #dirt} or farmland. */
    @Override
    protected boolean canSustainBush(IBlockState state) {
        return isDirtTag(state) || state.getBlock() == Blocks.FARMLAND;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.box.offset(state.getOffset(source, pos));
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return this.replaceable;
    }

    @Override
    public net.minecraft.item.Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.shearsOnly ? net.minecraft.init.Items.AIR : super.getItemDropped(state, rand, fortune);
    }

    /**
     * The loot table's {@code match_tool shears or silk_touch} for a shears-only plant: 1.12.2's shears break a
     * non-{@code IShearable} block as any tool would, so the drop is given here, once.
     */
    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        if (!this.shearsOnly) {
            super.harvestBlock(world, player, pos, state, te, stack);
            return;
        }
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
        boolean shears = stack.getItem() instanceof ItemShears || stack.getItem().getToolClasses(stack).contains("shears");
        if (shears || net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.SILK_TOUCH, stack) > 0) {
            List<ItemStack> drops = new ArrayList<ItemStack>(Collections.singletonList(new ItemStack(this)));
            net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1.0F, true, player);
            for (ItemStack drop : drops) {
                spawnAsEntity(world, pos, drop);
            }
        }
    }

    // ===========================================================================================
    // Block tags, as 1.12.2 blocks (other mods' blocks by registry name)
    // ===========================================================================================

    /** {@code #dirt}: dirt, coarse dirt and podzol, grass, mycelium, moss and pale moss, rooted dirt, mud. */
    static boolean isDirtTag(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.MYCELIUM
                || named(b, "moss_block", "pale_moss_block", "rooted_dirt", "mud", "muddy_mangrove_roots");
    }

    /** {@code #sand}: sand, red sand, suspicious sand. */
    static boolean isSandTag(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.SAND || named(b, "suspicious_sand");
    }

    /** {@code #terracotta}: terracotta and the sixteen colors (1.12.2's hardened clay). */
    static boolean isTerracottaTag(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.HARDENED_CLAY || b == Blocks.STAINED_HARDENED_CLAY;
    }

    /** {@code #dry_vegetation_may_place_on}: {@code #sand}, {@code #terracotta}, {@code #dirt}, farmland. */
    static boolean isDryVegetationSoil(IBlockState state) {
        return isSandTag(state) || isTerracottaTag(state) || isDirtTag(state) || state.getBlock() == Blocks.FARMLAND;
    }

    /** 1.12.2's coarse dirt and podzol are dirt variants; every variant is {@code #dirt}. */
    static boolean isPlainDirt(IBlockState state) {
        return state.getBlock() == Blocks.DIRT && state.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT;
    }

    static boolean named(Block b, String... paths) {
        if (b.getRegistryName() == null) {
            return false;
        }
        String path = b.getRegistryName().getPath();
        for (String p : paths) {
            if (p.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /** {@code isFaceSturdy(UP)}. */
    static boolean sturdyTop(IBlockAccess world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlockFaceShape(world, pos, EnumFacing.UP) == net.minecraft.block.state.BlockFaceShape.SOLID;
    }

    /**
     * {@code BonemealableBlock.getSpreadableNeighbourPos}: the first horizontal neighbor, in this order, that is
     * empty and where {@code state} could stand; null if none.
     */
    @Nullable
    static BlockPos spreadable(World world, BlockPos pos, IBlockState state, List<EnumFacing> order) {
        BlockVegetation block = (BlockVegetation) state.getBlock();
        for (EnumFacing f : order) {
            BlockPos p = pos.offset(f);
            if (world.isAirBlock(p) && block.canBlockStay(world, p, state)) {
                return p;
            }
        }
        return null;
    }

    /** {@code Direction.Plane.HORIZONTAL}: north, east, south, west. */
    static List<EnumFacing> horizontal() {
        List<EnumFacing> list = new ArrayList<EnumFacing>(4);
        list.add(EnumFacing.NORTH);
        list.add(EnumFacing.EAST);
        list.add(EnumFacing.SOUTH);
        list.add(EnumFacing.WEST);
        return list;
    }

    /** {@code Direction.Plane.HORIZONTAL.shuffledCopy(random)}: {@code Util.shuffle}, from the end. */
    static List<EnumFacing> shuffledHorizontal(Random rand) {
        List<EnumFacing> list = horizontal();
        for (int i = list.size(); i > 1; i--) {
            int j = rand.nextInt(i);
            list.set(i - 1, list.set(j, list.get(i - 1)));
        }
        return list;
    }
}
