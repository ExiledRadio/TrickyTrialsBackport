package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.palegarden.BlockCreakingHeart;
import com.exiledradio.trickytrialsbackport.palegarden.TileCreakingHeart;
import com.exiledradio.trickytrialsbackport.resin.BlockResinClump;
import com.exiledradio.trickytrialsbackport.resin.ItemResinClump;
import com.exiledradio.trickytrialsbackport.resin.ResinSpread;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Method;

/** Stream 14: resin clumps, the block of resin, resin bricks, and resin from a hurt creaking. */
public final class ResinSelfTest implements SelfTest {

    private static final String[] WITH_ITEMS = {"resin_clump", "resin_block", "resin_bricks", "resin_brick_stairs",
            "resin_brick_slab", "resin_brick_wall", "chiseled_resin_bricks"};
    private static final String[] NO_ITEMS = {"resin_clump_down", "resin_clump_up", "resin_clump_up_down", "resin_brick_double_slab"};

    @Override
    public String name() {
        return "resin";
    }

    @Override
    public void run(Check c) {
        registration(c);
        numbers(c);
        recipes(c);
        placing(c);
        support(c);
        spread(c);
        heartDrops(c);
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(Content.id(name));
    }

    private static Item item(String name) {
        Item i = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return i != null && Content.id(name).equals(i.getRegistryName()) ? i : null;
    }

    private static ItemStack stack(String name, int n) {
        return new ItemStack(item(name), n);
    }

    private void registration(Check c) {
        for (String name : WITH_ITEMS) {
            Block b = block(name);
            c.ok(b != null && Content.id(name).equals(b.getRegistryName()), name + " registered");
            c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(name), name + " owned by this mod");
            c.ok(item(name) != null, name + " has an item");
        }
        for (String name : NO_ITEMS) {
            Block b = block(name);
            c.ok(b != null && Content.id(name).equals(b.getRegistryName()), name + " registered");
            c.ok(item(name) == null, name + " has no item");
        }
        c.ok(item("resin_brick") != null, "resin brick item registered");
        c.ok(item("resin_clump") instanceof ItemResinClump, "the clump's item adds faces");
        c.ok(item("resin_brick_slab") instanceof ItemSlab, "resin brick slab item joins slabs");
        c.ok(block("resin_brick_stairs") instanceof BlockStairs && block("resin_brick_wall") instanceof BlockWall
                && block("resin_brick_slab") instanceof BlockSlab, "shapes are vanilla kinds");
        for (String name : new String[] {"resin_clump", "resin_clump_down", "resin_clump_up", "resin_clump_up_down",
                "resin_block", "resin_bricks", "chiseled_resin_bricks"}) {
            Block b = block(name);
            for (IBlockState state : b.getBlockState().getValidStates()) {
                c.eq(state, b.getStateFromMeta(b.getMetaFromState(state)), name + " metadata round trip");
            }
        }
        // One six-bit face mask for the four clump blocks.
        boolean all = true;
        for (int mask = 1; mask < 64; mask++) {
            all &= BlockResinClump.faces(BlockResinClump.of(mask)) == mask;
        }
        c.ok(all, "every face mask 1-63 is a clump state and back");
        c.eq(Blocks.AIR.getDefaultState(), BlockResinClump.of(0), "no faces is air");
        c.eq(block("resin_clump_up_down"), BlockResinClump.of(1 | 2 | 4).getBlock(), "up and down faces pick the up-down block");
    }

    private void numbers(Check c) {
        BlockPos o = BlockPos.ORIGIN;
        for (String name : new String[] {"resin_clump", "resin_block"}) {
            Block b = block(name);
            c.eq(0.0F, b.getBlockHardness(b.getDefaultState(), c.world, o), name + " breaks at once");
            c.eq(Content.id("block.resin.break"), b.getSoundType().getBreakSound().getRegistryName(), name + " resin sound");
            c.eq(Content.id("block.resin.place"), b.getSoundType().getPlaceSound().getRegistryName(), name + " resin place sound");
            c.eq(MapColor.ORANGE_STAINED_HARDENED_CLAY, b.getDefaultState().getMapColor(c.world, o), name + " orange terracotta on maps");
        }
        c.eq(0.0F, block("resin_block").getExplosionResistance(null), "block of resin blast resistance 0");
        for (String name : new String[] {"resin_bricks", "chiseled_resin_bricks", "resin_brick_stairs", "resin_brick_slab",
                "resin_brick_wall", "resin_brick_double_slab"}) {
            Block b = block(name);
            IBlockState s = b.getDefaultState();
            c.eq(1.5F, b.getBlockHardness(s, c.world, o), name + " hardness");
            c.ok(Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-4, name + " blast resistance 6, got " + b.getExplosionResistance(null));
            c.eq("pickaxe", b.getHarvestTool(s), name + " harvest tool");
            c.eq(Content.id("block.resin_bricks.hit"), b.getSoundType().getHitSound().getRegistryName(), name + " resin brick sound");
        }
        c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("block.resin.fall"))
                && ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("block.resin_bricks.fall")), "sound events registered");
        Block clump = block("resin_clump");
        c.eq(EnumPushReaction.DESTROY, clump.getDefaultState().getPushReaction(), "pistons destroy clumps");
        c.ok(clump.getDefaultState().getMaterial().getCanBurn(), "lava can set clumps alight");
        c.ok(Blocks.FIRE.getFlammability(clump) == 0, "fire does not spread to clumps");
        c.ok(clump.getDefaultState().getCollisionBoundingBox(c.world, o) == null, "clumps have no collision");
        c.ok(clump.isReplaceable(c.world, o), "clumps are replaceable");
    }

    private void recipes(Check c) {
        ItemStack clump = stack("resin_clump", 1);
        ItemStack bricks = stack("resin_bricks", 1);
        ItemStack brick = stack("resin_brick", 1);
        ItemStack e = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, clump, clump, clump, clump, clump, clump, clump, clump, clump),
                stack("resin_block", 1), "9 clumps to a block of resin");
        Crafting.expect(c, Crafting.grid(1, 1, stack("resin_block", 1)), stack("resin_clump", 9), "a block of resin to 9 clumps");
        Crafting.expect(c, Crafting.grid(2, 2, brick, brick, brick, brick), stack("resin_bricks", 1), "4 resin bricks to resin bricks");
        Crafting.expect(c, Crafting.grid(3, 3, bricks, e, e, bricks, bricks, e, bricks, bricks, bricks), stack("resin_brick_stairs", 4), "stairs");
        Crafting.expect(c, Crafting.grid(3, 1, bricks, bricks, bricks), stack("resin_brick_slab", 6), "slabs");
        Crafting.expect(c, Crafting.grid(3, 2, bricks, bricks, bricks, bricks, bricks, bricks), stack("resin_brick_wall", 6), "walls");
        Crafting.expect(c, Crafting.grid(1, 2, stack("resin_brick_slab", 1), stack("resin_brick_slab", 1)),
                stack("chiseled_resin_bricks", 1), "2 slabs to chiseled resin bricks");
        ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(clump);
        c.ok(smelted.getItem() == item("resin_brick") && smelted.getCount() == 1, "a clump smelts into a resin brick: " + smelted);
        c.eq(0.1F, FurnaceRecipes.instance().getSmeltingExperience(smelted), "smelting gives 0.1 experience");
        // The pale garden's heart recipe takes our block of resin now that it exists.
        if (block("creaking_heart") != null) {
            c.ok(CraftingManager.getRecipe(Content.id("creaking_heart")) != null, "creaking heart recipe registered with resin");
        }
    }

    private void placing(Check c) {
        BlockPos area = c.area();
        FakePlayer player = c.player("resin", area.up());
        player.rotationPitch = 90.0F; // looking down
        BlockPos floor = area.add(4, 0, 4);
        c.world.setBlockState(floor, Blocks.STONE.getDefaultState());
        player.setHeldItem(EnumHand.MAIN_HAND, stack("resin_clump", 4));

        // On the top of a block: a clump with its down face, in the space above.
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos at = floor.up();
        c.eq(1 << EnumFacing.DOWN.getIndex(), BlockResinClump.faces(c.world.getBlockState(at)), "clump on the floor has its down face");
        c.eq(3, player.getHeldItem(EnumHand.MAIN_HAND).getCount(), "one clump used");

        // A wall to the north: clicking the clump adds the north face to the same space.
        c.world.setBlockState(at.north(), Blocks.STONE.getDefaultState());
        Object[] target = ItemResinClump.addTarget(c.world, at, EnumFacing.UP, player);
        c.ok(target != null && at.equals(target[0]), "a click on a clump adds to that clump (the client's check)");
        player.rotationPitch = 0.0F;
        player.rotationYaw = 180.0F; // facing north
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.1F, 0.5F);
        int mask = BlockResinClump.faces(c.world.getBlockState(at));
        c.ok(BlockResinClump.has(mask, EnumFacing.DOWN) && BlockResinClump.has(mask, EnumFacing.NORTH) && Integer.bitCount(mask) == 2,
                "second clump joins on the north face: mask " + mask);

        // The side of a block: the face against it, in front of it.
        BlockPos wall = area.add(10, 1, 10);
        c.world.setBlockState(wall, Blocks.STONE.getDefaultState());
        player.getHeldItem(EnumHand.MAIN_HAND).onItemUse(player, c.world, wall, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.5F, 1.0F);
        c.eq(1 << EnumFacing.NORTH.getIndex(), BlockResinClump.faces(c.world.getBlockState(wall.south())), "clump on a wall's south side");

        // Nothing to hold: no clump in mid-air, and none in water.
        c.ok(ItemResinClump.addTarget(c.world, area.add(20, 20, 20), EnumFacing.UP, player) == null, "no clump in mid-air");
        BlockPos pool = area.add(16, 1, 4);
        c.world.setBlockState(pool.down(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(pool, Blocks.WATER.getDefaultState());
        c.ok(ItemResinClump.addTarget(c.world, pool.down(), EnumFacing.UP, player) == null, "no clump in water (not waterloggable yet)");

        // Breaking: one clump per face, by hand.
        EntityPlayerMP real = Players.real(c.world, "resin");
        real.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 2.5D);
        real.interactionManager.tryHarvestBlock(at);
        c.eq(Blocks.AIR, c.world.getBlockState(at).getBlock(), "clump broken");
        c.eq(2, Drops.count(c.world, at, item("resin_clump")), "two faces drop two clumps");
        NonNullList<ItemStack> drops = NonNullList.create();
        block("resin_clump").getDrops(drops, c.world, at, BlockResinClump.of(63), 0);
        c.ok(drops.size() == 1 && drops.get(0).getCount() == 6, "six faces drop six: " + drops);

        // The block of resin drops itself by hand; resin bricks need a pickaxe.
        c.ok(block("resin_block").canHarvestBlock(c.world, floor, real), "block of resin drops by hand");
        c.world.setBlockState(floor, block("resin_bricks").getDefaultState());
        c.ok(!block("resin_bricks").canHarvestBlock(c.world, floor, real), "no resin bricks by hand");
        real.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        c.ok(block("resin_bricks").canHarvestBlock(c.world, floor, real), "a wooden pickaxe mines resin bricks");
    }

    private void support(Check c) {
        BlockPos area = c.area();
        BlockPos at = area.add(5, 5, 5);
        c.world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(at.east(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(at, BlockResinClump.of((1 << EnumFacing.DOWN.getIndex()) | (1 << EnumFacing.EAST.getIndex())));
        c.world.setBlockToAir(at.east());
        c.eq(1 << EnumFacing.DOWN.getIndex(), BlockResinClump.faces(c.world.getBlockState(at)), "a face loses its support");
        c.eq(0, Drops.count(c.world, at, item("resin_clump")), "a lost face drops nothing");
        c.world.setBlockToAir(at.down());
        c.eq(Blocks.AIR, c.world.getBlockState(at).getBlock(), "the last face gone, the clump goes");
        c.eq(1, Drops.count(c.world, at, item("resin_clump")), "and drops its one clump");
    }

    /** By name, as the creaking calls it. */
    private static int hurt(TileEntity heart) throws ReflectiveOperationException {
        Method m = Class.forName(ResinSpread.CLASS).getMethod("creakingHurt", TileEntity.class);
        return (Integer) m.invoke(null, heart);
    }

    private void spread(Check c) {
        Block heart = block("creaking_heart");
        Block log = block("pale_oak_log");
        if (heart == null || log == null) {
            c.ok(true, "no pale garden: nothing to spread from");
            return;
        }
        try {
            c.eq(0, hurt(null), "a null heart spreads nothing");
            BlockPos corner = c.area();
            BlockPos at = corner.add(10, 5, 10);
            c.world.setWorldTime(1000L);
            c.world.setBlockState(at.up(), log.getDefaultState());
            c.world.setBlockState(at.up(2), log.getDefaultState());
            c.world.setBlockState(at.down(), log.getDefaultState());
            c.world.setBlockState(at, heart.getDefaultState());
            heart.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
            c.eq(BlockCreakingHeart.State.DORMANT, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "dormant by day");
            c.eq(0, hurt(c.world.getTileEntity(at)), "a dormant heart spreads no resin");

            c.world.setWorldTime(14000L);
            for (int i = 0; i < 30; i++) {
                ((TileCreakingHeart) c.world.getTileEntity(at)).update();
            }
            c.eq(BlockCreakingHeart.State.AWAKE, c.world.getBlockState(at).getValue(BlockCreakingHeart.STATE), "awake at night");
            int total = 0;
            boolean counts = true;
            for (int round = 0; round < 4; round++) {
                int n = hurt(c.world.getTileEntity(at));
                counts &= n >= 2 && n <= 3;
                total += n;
            }
            c.ok(counts, "each hurt spreads 2-3 resin while there is room");
            int faces = 0;
            boolean onLogs = true;
            for (BlockPos p : BlockPos.getAllInBox(at.add(-3, -3, -3), at.add(3, 4, 3))) {
                int mask = BlockResinClump.faces(c.world.getBlockState(p));
                for (EnumFacing f : EnumFacing.values()) {
                    if (BlockResinClump.has(mask, f)) {
                        faces++;
                        onLogs &= BlockCreakingHeart.isPaleOakLog(c.world.getBlockState(p.offset(f)));
                    }
                }
            }
            c.eq(total, faces, "every spread is a face of resin");
            c.ok(onLogs, "every face of resin is against a pale oak log");
            // Three logs have 4 + 5 + 5 = 14 open faces in all (the heart and each other close the rest).
            for (int round = 0; round < 10; round++) {
                hurt(c.world.getTileEntity(at));
            }
            int full = 0;
            for (BlockPos p : BlockPos.getAllInBox(at.add(-3, -3, -3), at.add(3, 4, 3))) {
                full += Integer.bitCount(BlockResinClump.faces(c.world.getBlockState(p)));
            }
            c.eq(14, full, "resin covers every open face of the logs within two steps, and stops");
            c.eq(0, hurt(c.world.getTileEntity(at)), "no room, no resin");
            // A log beyond two steps of the heart gets none.
            c.world.setBlockState(at.up(3), log.getDefaultState());
            for (int round = 0; round < 10; round++) {
                hurt(c.world.getTileEntity(at));
            }
            int far = 0;
            for (EnumFacing f : EnumFacing.HORIZONTALS) {
                far += Integer.bitCount(BlockResinClump.faces(c.world.getBlockState(at.up(3).offset(f))));
            }
            c.eq(0, far, "no resin on a log three steps away");
            c.world.setWorldTime(1000L);
        } catch (ReflectiveOperationException ex) {
            c.fail("creakingHurt by name: " + ex);
        }
    }

    private void heartDrops(Check c) {
        Block heart = block("creaking_heart");
        if (heart == null) {
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < 200; i++) {
            drops.clear();
            heart.getDrops(drops, c.world, BlockPos.ORIGIN, heart.getDefaultState(), 0);
            int n = 0;
            for (ItemStack s : drops) {
                n += s.getItem() == item("resin_clump") ? s.getCount() : 100;
            }
            min = Math.min(min, n);
            max = Math.max(max, n);
        }
        c.ok(min == 1 && max == 3, "a broken heart drops 1-3 resin clumps (" + min + "-" + max + ")");
    }
}
