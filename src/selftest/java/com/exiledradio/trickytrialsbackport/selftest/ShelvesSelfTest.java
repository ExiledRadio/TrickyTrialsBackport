package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.shelf.BlockShelf;
import com.exiledradio.trickytrialsbackport.shelf.ShelfContent;
import com.exiledradio.trickytrialsbackport.shelf.ShelfGameEvents;
import com.exiledradio.trickytrialsbackport.shelf.ShelfWood;
import com.exiledradio.trickytrialsbackport.shelf.SideChainPart;
import com.exiledradio.trickytrialsbackport.shelf.TileEntityShelf;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Stream 21: shelves. */
public final class ShelvesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "shelves";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(ShelfContent.enabled(), "shelves are switched on")) {
            return;
        }
        registration(c);
        numbers(c);
        recipes(c);
        BlockShelf oak = ShelfContent.shelves().get(ShelfWood.OAK);
        placement(c, oak);
        hitSlots(c);
        singleSwap(c, oak);
        serverPath(c, oak);
        comparator(c, oak);
        rows(c, oak);
        nbtAndDrops(c, oak);
        hopper(c, oak);
        gameEvents(c, oak);
    }

    private static TileEntityShelf tile(Check c, BlockPos p) {
        TileEntity te = c.world.getTileEntity(p);
        return te instanceof TileEntityShelf ? (TileEntityShelf) te : null;
    }

    private static IBlockState shelfState(BlockShelf b, EnumFacing facing) {
        return b.getDefaultState().withProperty(BlockShelf.FACING, facing);
    }

    private static boolean activate(Check c, BlockPos p, FakePlayer player, EnumFacing face, float hx, float hy, float hz) {
        IBlockState s = c.world.getBlockState(p);
        return s.getBlock().onBlockActivated(c.world, p, s, player, EnumHand.MAIN_HAND, face, hx, hy, hz);
    }

    private static SideChainPart part(Check c, BlockPos p) {
        IBlockState s = c.world.getBlockState(p);
        return s.getActualState(c.world, p).getValue(BlockShelf.SIDE_CHAIN);
    }

    // --- registration: which woods exist ---------------------------------------------------------------------------

    private void registration(Check c) {
        Map<ShelfWood, BlockShelf> shelves = ShelfContent.shelves();
        StringBuilder present = new StringBuilder();
        for (ShelfWood wood : ShelfWood.values()) {
            boolean expected;
            if (wood.owner == null) {
                expected = true;
            } else if (TrickyTrialsBackport.MOD_ID.equals(wood.owner)) {
                expected = Owners.ourBlock("pale_oak_planks") != null;
            } else {
                expected = Owners.present(wood.owner)
                        && ForgeRegistries.BLOCKS.containsKey(new net.minecraft.util.ResourceLocation(wood.owner, wood.name + "_planks"));
            }
            Block registered = ForgeRegistries.BLOCKS.getValue(Content.id(wood.shelfName()));
            boolean is = registered instanceof BlockShelf && Content.id(wood.shelfName()).equals(registered.getRegistryName());
            c.eq(expected, is, wood.shelfName() + " registered exactly when its wood exists");
            c.eq(expected, shelves.containsKey(wood), wood.shelfName() + " in the stream's table");
            c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(wood.shelfName()), wood.shelfName() + " owned by this mod");
            if (is) {
                present.append(' ').append(wood.name);
                Item item = Item.getItemFromBlock(registered);
                c.ok(item instanceof ShelfContent.ItemShelf, wood.shelfName() + " has its item");
                for (IBlockState state : registered.getBlockState().getValidStates()) {
                    if (state.getValue(BlockShelf.SIDE_CHAIN) == SideChainPart.UNCONNECTED) {
                        c.eq(state, registered.getStateFromMeta(registered.getMetaFromState(state)), wood.shelfName() + " metadata round trip");
                    }
                }
            }
        }
        TrickyTrialsBackport.LOGGER.info("SELFTEST shelves present:{} (Trails & Tales {}, Wild Update {}, Nether Update {}, V&P {})",
                present, Owners.present(Owners.TRAILS_AND_TALES), Owners.present(Owners.WILD_UPDATE),
                Owners.present(Owners.NETHER_UPDATE), Owners.present(Owners.VILLAGE_AND_PILLAGE));
        c.eq(32, ShelfContent.shelves().get(ShelfWood.OAK).getBlockState().getValidStates().size(),
                "shelf states: 4 facings x powered x 4 row parts, the part from the tile entity");
    }

    private void numbers(Check c) {
        BlockPos o = BlockPos.ORIGIN;
        for (BlockShelf b : ShelfContent.shelves().values()) {
            IBlockState s = b.getDefaultState();
            String n = b.wood.shelfName();
            c.eq(2.0F, b.getBlockHardness(s, c.world, o), n + " hardness 2");
            c.ok(Math.abs(b.getExplosionResistance(null) - 3.0F) < 1e-4, n + " blast resistance 3, got " + b.getExplosionResistance(null));
            c.eq("axe", b.getHarvestTool(s), n + " axe");
            c.ok(s.getMaterial().isToolNotRequired(), n + " drops by hand");
            c.eq(Content.id("block.shelf.break"), b.getSoundType().getBreakSound().getRegistryName(), n + " shelf sound");
            c.eq(Content.id("block.shelf.step"), b.getSoundType().getStepSound().getRegistryName(), n + " shelf step sound");
            int burn = TileEntityFurnace.getItemBurnTime(new ItemStack(b));
            if (b.wood.flammable) {
                c.eq(20, b.getFlammability(c.world, o, EnumFacing.UP), n + " flammability 20");
                c.eq(30, b.getFireSpreadSpeed(c.world, o, EnumFacing.UP), n + " fire encouragement 30");
                c.eq(300, burn, n + " burns 300 ticks");
            } else {
                c.eq(0, b.getFlammability(c.world, o, EnumFacing.UP), n + " does not burn");
                c.eq(0, burn, n + " is not fuel");
            }
            c.ok(s.getMaterial().getCanBurn(), n + " lava can set it alight");
            c.ok(!s.isOpaqueCube() && !s.isFullCube(), n + " not a full cube");
        }
        for (String event : new String[] {"activate", "deactivate", "multi_swap", "place_item", "single_swap", "take_item",
                "break", "step", "place", "hit", "fall"}) {
            c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("block.shelf." + event)), "sound block.shelf." + event + " registered");
        }
    }

    private void recipes(Check c) {
        for (BlockShelf b : ShelfContent.shelves().values()) {
            ItemStack log = b.wood.ingredient();
            if (!c.ok(log != null, b.wood.shelfName() + " has an ingredient")) {
                continue;
            }
            ItemStack one = new ItemStack(log.getItem(), 1, log.getMetadata() == 32767 ? 0 : log.getMetadata());
            ItemStack e = ItemStack.EMPTY;
            Crafting.expect(c, Crafting.grid(3, 3, one, one, one, e, e, e, one, one, one), new ItemStack(b, 6),
                    b.wood.shelfName() + " from six logs");
            ItemStack filled = net.minecraft.item.crafting.CraftingManager.findMatchingResult(
                    Crafting.grid(3, 3, one, one, one, one, one, one, e, e, e), c.world);
            c.ok(filled.getItem() != Item.getItemFromBlock(b), b.wood.shelfName() + " needs the gap row");
            Block stripped = b.wood.findStrippedLog();
            TrickyTrialsBackport.LOGGER.info("SELFTEST {} recipe takes {}", b.wood.shelfName(),
                    stripped != null ? stripped.getRegistryName() : log.getItem().getRegistryName() + "@" + log.getMetadata() + " (no stripped log)");
            if (Owners.present(Owners.VILLAGE_AND_PILLAGE) && b.wood.owner == null) {
                c.ok(stripped != null && Owners.VILLAGE_AND_PILLAGE.equals(stripped.getRegistryName().getNamespace()),
                        b.wood.shelfName() + " takes Village & Pillage's stripped log");
            }
        }
    }

    // --- placing and using -----------------------------------------------------------------------------------------

    private void placement(Check c, BlockShelf oak) {
        BlockPos o = c.area().add(2, 0, 2);
        FakePlayer p = c.player("shelfplace", o.north(3));
        for (EnumFacing look : EnumFacing.HORIZONTALS) {
            p.rotationYaw = look.getHorizontalAngle();
            IBlockState s = oak.getStateForPlacement(c.world, o, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, p, EnumHand.MAIN_HAND);
            c.eq(look.getOpposite(), s.getValue(BlockShelf.FACING), "placed shelf faces the player looking " + look);
            c.eq(false, s.getValue(BlockShelf.POWERED), "placed unpowered");
        }
        c.world.setBlockState(o.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        IBlockState powered = oak.getStateForPlacement(c.world, o, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, p, EnumHand.MAIN_HAND);
        c.eq(true, powered.getValue(BlockShelf.POWERED), "placed on a redstone block it is powered");
        // Through the item, as a player places it.
        p.rotationYaw = 0.0F;
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(oak));
        BlockPos at = o.add(4, 0, 0);
        EnumActionResult r = p.getHeldItemMainhand().onItemUse(p, c.world, at.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(EnumActionResult.SUCCESS, r, "the item places a shelf");
        c.eq(oak, c.world.getBlockState(at).getBlock(), "shelf placed");
        c.eq(EnumFacing.NORTH, c.world.getBlockState(at).getValue(BlockShelf.FACING), "facing the player (looking south)");
        c.ok(tile(c, at) != null, "shelf has its tile entity");
        // Shape: a back board and two front boards; a shelf holds up a torch on its back only.
        IBlockState s = c.world.getBlockState(at);
        c.eq(net.minecraft.block.state.BlockFaceShape.SOLID, s.getBlockFaceShape(c.world, at, EnumFacing.SOUTH), "back face is solid");
        c.eq(net.minecraft.block.state.BlockFaceShape.UNDEFINED, s.getBlockFaceShape(c.world, at, EnumFacing.NORTH), "front face is open");
        List<net.minecraft.util.math.AxisAlignedBB> boxes = new ArrayList<net.minecraft.util.math.AxisAlignedBB>();
        s.addCollisionBoxToList(c.world, at, new net.minecraft.util.math.AxisAlignedBB(at).grow(1), boxes, null, false);
        c.eq(3, boxes.size(), "three collision boxes");
        // Waterlogging (Backport Core's adapter, with Fluidlogged API).
        c.ok(oak instanceof com.exiledradio.backportcore.waterlog.Waterloggable, "shelves are waterloggable");
        if (com.exiledradio.backportcore.waterlog.Waterlogging.isAvailable()) {
            BlockPos wet = at.east(3);
            c.world.setBlockState(wet, Blocks.WATER.getDefaultState(), 3);
            c.ok(com.exiledradio.backportcore.waterlog.Waterlogging.placeKeepingWater(c.world, wet, shelfState(oak, EnumFacing.NORTH), 3),
                    "a shelf placed into water keeps it");
            c.ok(com.exiledradio.backportcore.waterlog.Waterlogging.hasWater(c.world, wet), "the shelf holds water");
            c.ok(tile(c, wet) != null, "the waterlogged shelf has its tile entity");
        }
    }

    private void hitSlots(Check c) {
        // Left to right as seen from in front.
        c.eq(0, BlockShelf.hitSlot(EnumFacing.NORTH, EnumFacing.NORTH, 0.9F, 0.5F, 0.0F), "north face, east end is slot 0");
        c.eq(1, BlockShelf.hitSlot(EnumFacing.NORTH, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "north face, middle is slot 1");
        c.eq(2, BlockShelf.hitSlot(EnumFacing.NORTH, EnumFacing.NORTH, 0.1F, 0.5F, 0.0F), "north face, west end is slot 2");
        c.eq(0, BlockShelf.hitSlot(EnumFacing.SOUTH, EnumFacing.SOUTH, 0.1F, 0.5F, 1.0F), "south face, west end is slot 0");
        c.eq(0, BlockShelf.hitSlot(EnumFacing.WEST, EnumFacing.WEST, 0.0F, 0.5F, 0.1F), "west face, north end is slot 0");
        c.eq(0, BlockShelf.hitSlot(EnumFacing.EAST, EnumFacing.EAST, 1.0F, 0.5F, 0.9F), "east face, south end is slot 0");
        c.eq(-1, BlockShelf.hitSlot(EnumFacing.UP, EnumFacing.NORTH, 0.5F, 1.0F, 0.5F), "top is no slot");
        c.eq(-1, BlockShelf.hitSlot(EnumFacing.SOUTH, EnumFacing.NORTH, 0.5F, 0.5F, 1.0F), "back is no slot");
        c.eq(2, BlockShelf.hitSlot(EnumFacing.NORTH, EnumFacing.NORTH, 0.0F, 0.5F, 0.0F), "edge clamps to slot 2");
    }

    private void singleSwap(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(4, 0, 4);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.NORTH), 3);
        TileEntityShelf shelf = tile(c, at);
        FakePlayer p = c.player("shelfswap", at.north(2));
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 5));
        c.ok(activate(c, at, p, EnumFacing.NORTH, 0.9F, 0.5F, 0.0F), "placing an item succeeds");
        c.ok(shelf.getStackInSlot(0).getItem() == Items.DIAMOND && shelf.getStackInSlot(0).getCount() == 5, "the whole stack goes on slot 0");
        c.ok(p.getHeldItemMainhand().isEmpty(), "the hand is empty");
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        c.ok(activate(c, at, p, EnumFacing.NORTH, 0.9F, 0.5F, 0.0F), "swapping succeeds");
        c.eq(Items.STICK, shelf.getStackInSlot(0).getItem(), "the stick is on the shelf");
        c.eq(Items.DIAMOND, p.getHeldItemMainhand().getItem(), "the diamonds are back in hand");
        p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.ok(activate(c, at, p, EnumFacing.NORTH, 0.9F, 0.5F, 0.0F), "taking succeeds");
        c.eq(Items.STICK, p.getHeldItemMainhand().getItem(), "the stick is taken");
        c.ok(shelf.getStackInSlot(0).isEmpty(), "slot 0 empty again");
        p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.ok(!activate(c, at, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "empty hand on an empty slot passes");
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.APPLE));
        c.ok(!activate(c, at, p, EnumFacing.SOUTH, 0.5F, 0.5F, 1.0F), "the back passes");
        c.ok(!activate(c, at, p, EnumFacing.UP, 0.5F, 1.0F, 0.5F), "the top passes");
        c.ok(shelf.isEmpty(), "nothing went on through other faces");
        IBlockState s = c.world.getBlockState(at);
        c.ok(!s.getBlock().onBlockActivated(c.world, at, s, p, EnumHand.OFF_HAND, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "the off hand passes");
        // Slots 1 and 2.
        c.ok(activate(c, at, p, EnumFacing.NORTH, 0.1F, 0.5F, 0.0F), "apple on slot 2");
        c.eq(Items.APPLE, shelf.getStackInSlot(2).getItem(), "slot 2 has the apple");
        // Creative: an item put on an empty slot stays in hand too.
        p.capabilities.isCreativeMode = true;
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BREAD));
        c.ok(activate(c, at, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "creative placing");
        c.eq(Items.BREAD, shelf.getStackInSlot(1).getItem(), "creative bread on slot 1");
        c.eq(Items.BREAD, p.getHeldItemMainhand().getItem(), "creative keeps the bread in hand");
        p.capabilities.isCreativeMode = false;
    }

    /** Through the server's own right-click handling, with a real player. */
    private void serverPath(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(4, 0, 4);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.NORTH), 3);
        TileEntityShelf shelf = tile(c, at);
        EntityPlayerMP p = Players.real(c.world, "shelfreal");
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
        EnumActionResult r = p.interactionManager.processRightClickBlock(p, c.world, p.getHeldItemMainhand(), EnumHand.MAIN_HAND,
                at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(EnumActionResult.SUCCESS, r, "a real right click puts the emeralds on");
        c.eq(Items.EMERALD, shelf.getStackInSlot(1).getItem(), "emeralds on the middle slot");
        // 1.12.2 answers PASS for any empty-handed click, after the block has acted.
        p.interactionManager.processRightClickBlock(p, c.world, ItemStack.EMPTY, EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.ok(shelf.isEmpty(), "an empty hand takes them back");
        c.eq(Items.EMERALD, p.getHeldItemMainhand().getItem(), "emeralds in hand");
        // Sneaking with an item in hand uses the item, as vanilla: here it places the block against the shelf.
        p.setSneaking(true);
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE));
        p.interactionManager.processRightClickBlock(p, c.world, p.getHeldItemMainhand(), EnumHand.MAIN_HAND, at, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.ok(shelf.isEmpty(), "sneaking with an item does not use the shelf");
        c.eq(Blocks.COBBLESTONE, c.world.getBlockState(at.north()).getBlock(), "sneaking places the held block instead");
        p.setSneaking(false);
    }

    private void comparator(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(8, 0, 8);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.NORTH), 3);
        TileEntityShelf shelf = tile(c, at);
        IBlockState s = c.world.getBlockState(at);
        c.ok(s.hasComparatorInputOverride(), "shelves have comparator output");
        IBlockState comparator = Blocks.UNPOWERED_COMPARATOR.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH);
        c.world.setBlockState(at.south(), comparator, 3);
        c.eq(0, s.getComparatorInputOverride(c.world, at), "empty: 0");
        int[][] cases = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}, {1, 0, 1}, {1, 1, 1}};
        for (int[] fill : cases) {
            for (int i = 0; i < 3; i++) {
                shelf.setInventorySlotContents(i, fill[i] == 1 ? new ItemStack(Items.PAPER) : ItemStack.EMPTY);
            }
            int want = fill[0] | fill[1] << 1 | fill[2] << 2;
            c.eq(want, s.getComparatorInputOverride(c.world, at), "behind: slots " + fill[0] + fill[1] + fill[2]);
        }
        // The comparator itself, after its scheduled update.
        Blocks.UNPOWERED_COMPARATOR.neighborChanged(comparator, c.world, at.south(), oak, at);
        IBlockState cs = c.world.getBlockState(at.south());
        cs.getBlock().updateTick(c.world, at.south(), cs, c.world.rand);
        cs = c.world.getBlockState(at.south());
        c.eq(7, cs.getWeakPower(c.world, at.south(), EnumFacing.NORTH), "the comparator puts out 7 for a full shelf");
        // In front, or beside: nothing.
        c.world.setBlockState(at.south(), Blocks.AIR.getDefaultState(), 3);
        c.world.setBlockState(at.north(), comparator.withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH), 3);
        c.eq(0, s.getComparatorInputOverride(c.world, at), "in front: 0");
        c.world.setBlockState(at.north(), Blocks.AIR.getDefaultState(), 3);
        c.world.setBlockState(at.east(), comparator.withProperty(BlockHorizontal.FACING, EnumFacing.WEST), 3);
        c.eq(0, s.getComparatorInputOverride(c.world, at), "beside: 0");
        c.world.setBlockState(at.east(), Blocks.AIR.getDefaultState(), 3);
        // Behind, through a solid block.
        c.world.setBlockState(at.south(), Blocks.STONE.getDefaultState(), 3);
        c.world.setBlockState(at.south(2), comparator, 3);
        c.eq(7, s.getComparatorInputOverride(c.world, at), "behind through a solid block: 7");
    }

    // --- powered rows ----------------------------------------------------------------------------------------------

    private void rows(Check c, BlockShelf oak) {
        BlockPos base = c.area().add(4, 0, 4);
        // North-facing: its left (seen from in front) is east.
        BlockPos left = base.east(2);
        BlockPos middle = base.east();
        BlockPos right = base;
        for (BlockPos p : new BlockPos[] {left, middle, right}) {
            c.world.setBlockState(p, shelfState(oak, EnumFacing.NORTH), 3);
        }
        c.eq(SideChainPart.UNCONNECTED, part(c, middle), "unpowered shelves stay apart");
        for (BlockPos p : new BlockPos[] {right, left, middle}) {
            c.world.setBlockState(p.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
            c.eq(true, c.world.getBlockState(p).getValue(BlockShelf.POWERED), "powered by the block below");
        }
        c.eq(SideChainPart.LEFT, part(c, left), "left end");
        c.eq(SideChainPart.CENTER, part(c, middle), "center");
        c.eq(SideChainPart.RIGHT, part(c, right), "right end");
        // A fourth powered shelf does not join a full row.
        BlockPos fourth = base.west();
        c.world.setBlockState(fourth.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(fourth, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.eq(SideChainPart.UNCONNECTED, part(c, fourth), "a fourth shelf stays alone");
        c.eq(SideChainPart.RIGHT, part(c, right), "the row keeps its right end");

        // The row swaps with all nine hotbar slots, left shelf first.
        FakePlayer p = c.player("shelfrow", base.north(2));
        Item[] hot = {Items.APPLE, Items.ARROW, Items.BONE, Items.BOOK, Items.BOWL, Items.BREAD, Items.BRICK, Items.CLAY_BALL, Items.COAL};
        for (int i = 0; i < 9; i++) {
            p.inventory.setInventorySlotContents(i, new ItemStack(hot[i], i + 1));
        }
        p.inventory.currentItem = 4;
        c.ok(activate(c, right, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "using any shelf of the row swaps");
        boolean ok = true;
        BlockPos[] order = {left, middle, right};
        for (int s = 0; s < 3; s++) {
            TileEntityShelf t = tile(c, order[s]);
            for (int j = 0; j < 3; j++) {
                ItemStack got = t.getStackInSlot(j);
                ok &= got.getItem() == hot[s * 3 + j] && got.getCount() == s * 3 + j + 1;
            }
        }
        c.ok(ok, "hotbar 0-8 on the row, left to right");
        boolean emptyHotbar = true;
        for (int i = 0; i < 9; i++) {
            emptyHotbar &= p.inventory.getStackInSlot(i).isEmpty();
        }
        c.ok(emptyHotbar, "the hotbar is empty");
        c.ok(activate(c, left, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "swap back from the other end");
        ok = true;
        for (int i = 0; i < 9; i++) {
            ok &= p.inventory.getStackInSlot(i).getItem() == hot[i];
        }
        c.ok(ok, "the hotbar is back");
        c.ok(activate(c, middle, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "swap again");
        c.ok(tile(c, middle).getStackInSlot(1).getItem() == Items.BOWL, "middle shelf holds hotbar 3-5");
        c.ok(activate(c, middle, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "and back");
        // Nothing to swap: consumed, nothing changes.
        p.inventory.clear();
        BlockPos alone = c.area().add(4, 0, 4);
        c.world.setBlockState(alone.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(alone, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.ok(activate(c, alone, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "an empty powered swap is still consumed");
        // A lone powered shelf swaps with the last three hotbar slots.
        p.inventory.setInventorySlotContents(5, new ItemStack(Items.FEATHER));
        p.inventory.setInventorySlotContents(6, new ItemStack(Items.FLINT));
        p.inventory.setInventorySlotContents(8, new ItemStack(Items.EGG));
        c.ok(activate(c, alone, p, EnumFacing.NORTH, 0.1F, 0.5F, 0.0F), "a lone powered shelf swaps");
        TileEntityShelf t = tile(c, alone);
        c.ok(t.getStackInSlot(0).getItem() == Items.FLINT && t.getStackInSlot(1).isEmpty() && t.getStackInSlot(2).getItem() == Items.EGG,
                "hotbar 6-8 on its three slots");
        c.eq(Items.FEATHER, p.inventory.getStackInSlot(5).getItem(), "hotbar 5 untouched");

        // Two powered shelves: hotbar 3-8.
        BlockPos pairRight = c.area().add(4, 0, 4);
        BlockPos pairLeft = pairRight.east();
        c.world.setBlockState(pairRight.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(pairLeft.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(pairRight, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.world.setBlockState(pairLeft, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.eq(SideChainPart.LEFT, part(c, pairLeft), "pair: left end");
        c.eq(SideChainPart.RIGHT, part(c, pairRight), "pair: right end");
        p.inventory.clear();
        p.inventory.setInventorySlotContents(2, new ItemStack(Items.STRING));
        p.inventory.setInventorySlotContents(3, new ItemStack(Items.SUGAR));
        p.inventory.setInventorySlotContents(8, new ItemStack(Items.SLIME_BALL));
        c.ok(activate(c, pairLeft, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F), "a pair swaps");
        c.eq(Items.SUGAR, tile(c, pairLeft).getStackInSlot(0).getItem(), "hotbar 3 on the left shelf's slot 0");
        c.eq(Items.SLIME_BALL, tile(c, pairRight).getStackInSlot(2).getItem(), "hotbar 8 on the right shelf's slot 2");
        c.eq(Items.STRING, p.inventory.getStackInSlot(2).getItem(), "hotbar 2 untouched");

        // Facing another way: no connection. Another wood: connects (any wooden shelf).
        BlockPos other = c.area().add(4, 0, 4);
        c.world.setBlockState(other.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(other.east().down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(other, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.world.setBlockState(other.east(), shelfState(oak, EnumFacing.SOUTH).withProperty(BlockShelf.POWERED, true), 3);
        c.eq(SideChainPart.UNCONNECTED, part(c, other), "a shelf facing another way does not join");
        BlockShelf spruce = ShelfContent.shelves().get(ShelfWood.SPRUCE);
        c.world.setBlockState(other.west().down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(other.west(), shelfState(spruce, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.eq(SideChainPart.RIGHT, part(c, other.west()), "a spruce shelf joins an oak one");

        // Losing power: the shelf leaves the row and its neighbors let go.
        c.world.setBlockState(middle.down(), Blocks.STONE.getDefaultState(), 3);
        c.eq(false, c.world.getBlockState(middle).getValue(BlockShelf.POWERED), "middle unpowered");
        c.eq(SideChainPart.UNCONNECTED, part(c, middle), "middle unconnected");
        c.eq(SideChainPart.UNCONNECTED, part(c, left), "left end let go");
        c.eq(SideChainPart.UNCONNECTED, part(c, right), "right end let go");
        // Re-powered: the row forms again.
        c.world.setBlockState(middle.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(SideChainPart.CENTER, part(c, middle), "row joined again");
        // Breaking a shelf of the row lets its neighbors go.
        c.world.destroyBlock(middle, false);
        c.eq(SideChainPart.UNCONNECTED, part(c, left), "after breaking, left let go");
        c.eq(SideChainPart.UNCONNECTED, part(c, right), "after breaking, right let go");
    }

    // --- saving, drops, hoppers ------------------------------------------------------------------------------------

    private void nbtAndDrops(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(4, 0, 4);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.EAST), 3);
        TileEntityShelf shelf = tile(c, at);
        shelf.setInventorySlotContents(0, new ItemStack(Items.DIAMOND_SWORD));
        shelf.setInventorySlotContents(2, new ItemStack(Items.GOLD_INGOT, 17));
        NBTTagCompound tag = shelf.writeToNBT(new NBTTagCompound());
        c.ok(tag.hasKey("Items", 9) && tag.getTagList("Items", 10).tagCount() == 2, "saved as vanilla's Items list");
        c.ok(tag.hasKey("align_items_to_bottom"), "saves align_items_to_bottom");
        tag.setBoolean("align_items_to_bottom", true);
        TileEntityShelf copy = new TileEntityShelf();
        copy.readFromNBT(tag);
        c.ok(copy.getStackInSlot(0).getItem() == Items.DIAMOND_SWORD && copy.getStackInSlot(1).isEmpty()
                && copy.getStackInSlot(2).getCount() == 17, "items read back");
        c.ok(copy.alignItemsToBottom(), "align_items_to_bottom read back");
        NBTTagCompound chain = new NBTTagCompound();
        chain.setString("side_chain", "center");
        copy.readFromNBT(chain);
        c.eq(SideChainPart.CENTER, copy.chain(), "row part read back");
        c.ok(copy.isEmpty(), "a tag without items empties the shelf");
        c.ok(shelf.getUpdateTag().hasKey("Items"), "clients get the items");
        // A shelf whose power changes keeps its tile entity and items.
        c.world.setBlockState(at.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.ok(tile(c, at) == shelf && !shelf.isEmpty(), "power change keeps the contents");
        c.world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 3);
        // Breaking drops the shelf and its contents.
        Drops.count(c.world, at, Items.GOLD_INGOT);
        c.world.destroyBlock(at, true);
        c.eq(17, Drops.count(c.world, at, Items.GOLD_INGOT), "contents dropped");
        c.world.setBlockState(at, shelfState(oak, EnumFacing.EAST), 3);
        c.world.destroyBlock(at, true);
        c.eq(1, Drops.count(c.world, at, Item.getItemFromBlock(oak)), "the shelf drops itself");
    }

    private void hopper(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(4, 0, 4);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.NORTH), 3);
        TileEntityShelf shelf = tile(c, at);
        // Hoppers move one item at a time: into the first slot that takes it, merging with what is there.
        shelf.setInventorySlotContents(0, new ItemStack(Items.REDSTONE, 64));
        for (int i = 0; i < 2; i++) {
            ItemStack rest = TileEntityHopper.putStackInInventoryAllSlots(null, shelf, new ItemStack(Items.REDSTONE), EnumFacing.UP);
            c.ok(rest.isEmpty(), "a hopper puts redstone in");
        }
        c.eq(2, shelf.getStackInSlot(1).getCount(), "a full slot 0 sends it on to slot 1, where it merges");
        ItemStack paper = TileEntityHopper.putStackInInventoryAllSlots(null, shelf, new ItemStack(Items.PAPER), EnumFacing.UP);
        c.ok(paper.isEmpty() && shelf.getStackInSlot(2).getItem() == Items.PAPER, "another item takes slot 2");
        c.ok(shelf.decrStackSize(0, 1).getCount() == 1 && shelf.getStackInSlot(0).getCount() == 63, "a hopper below takes from it");
        c.eq(7, BlockShelf.signal(shelf), "three filled slots read 7");
    }

    // --- game events -----------------------------------------------------------------------------------------------

    private void gameEvents(Check c, BlockShelf oak) {
        BlockPos at = c.area().add(8, 0, 8);
        c.world.setBlockState(at, shelfState(oak, EnumFacing.NORTH), 3);
        TileEntityShelf shelf = tile(c, at);
        FakePlayer p = c.player("shelfevents", at.north(2));
        List<String> heard = new ArrayList<String>();
        Object listener = coreListener(c, at, heard);

        ShelfGameEvents.clearLast();
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        activate(c, at, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(ShelfGameEvents.ITEM_INTERACT_FINISH, ShelfGameEvents.last(), "placing an item: item_interact_finish");
        c.eq(at, ShelfGameEvents.lastPos(), "at the shelf");
        ShelfGameEvents.clearLast();
        activate(c, at, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(ShelfGameEvents.ITEM_INTERACT_FINISH, ShelfGameEvents.last(), "taking an item: item_interact_finish");
        ShelfGameEvents.clearLast();
        shelf.setInventorySlotContents(0, new ItemStack(Items.PAPER));
        c.eq(ShelfGameEvents.BLOCK_ACTIVATE, ShelfGameEvents.last(), "contents changed (hopper, command): block_activate");
        ShelfGameEvents.clearLast();
        c.world.setBlockState(at.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(ShelfGameEvents.BLOCK_ACTIVATE, ShelfGameEvents.last(), "powered: block_activate");
        ShelfGameEvents.clearLast();
        c.world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 3);
        c.eq(ShelfGameEvents.BLOCK_DEACTIVATE, ShelfGameEvents.last(), "unpowered: block_deactivate");
        // A powered row swap: entity_interact, per shelf.
        BlockPos second = at.east();
        c.world.setBlockState(at.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(second.down(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        ShelfGameEvents.clearLast();
        c.world.setBlockState(second, shelfState(oak, EnumFacing.NORTH).withProperty(BlockShelf.POWERED, true), 3);
        c.eq(SideChainPart.RIGHT, part(c, at), "row formed");
        c.eq(null, ShelfGameEvents.last(), "a row part changing posts nothing");
        p.inventory.setInventorySlotContents(8, new ItemStack(Items.STICK));
        activate(c, at, p, EnumFacing.NORTH, 0.5F, 0.5F, 0.0F);
        c.eq(ShelfGameEvents.ENTITY_INTERACT, ShelfGameEvents.last(), "row swap: entity_interact");
        c.eq(at, ShelfGameEvents.lastPos(), "posted by the row's last shelf");

        if (listener != null) {
            c.ok(heard.contains("minecraft:item_interact_finish"), "a Core listener heard item_interact_finish: " + heard);
            c.ok(heard.contains("minecraft:block_activate") && heard.contains("minecraft:block_deactivate"),
                    "a Core listener heard block_activate and block_deactivate");
            c.ok(heard.contains("minecraft:entity_interact"), "a Core listener heard entity_interact");
            removeCoreListener(c, listener);
        }
        TrickyTrialsBackport.LOGGER.info("SELFTEST shelves game events: Core module {}, heard {}",
                ShelfGameEvents.available() ? "present" : "absent (Core before 1.4.0)", heard);
    }

    /**
     * With Core's game event module active (Wild Update turns it on), an immediate listener beside the shelf, made by
     * name so this test also builds against Core 1.3.0. @return it, or null when the module is absent or off
     */
    private static Object coreListener(Check c, BlockPos at, final List<String> heard) {
        if (!ShelfGameEvents.available()) {
            return null;
        }
        try {
            Class<?> events = Class.forName("com.exiledradio.backportcore.gameevent.GameEvents");
            if (!(Boolean) events.getMethod("active").invoke(null)) {
                return null;
            }
            Class<?> listenerType = Class.forName("com.exiledradio.backportcore.gameevent.GameEventListener");
            final Object position = Class.forName("com.exiledradio.backportcore.gameevent.PositionSource$Block")
                    .getConstructor(BlockPos.class).newInstance(at.up(2));
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Object rules = Enum.valueOf((Class) Class.forName("com.exiledradio.backportcore.gameevent.GameEventListener$Rules"), "V1_20");
            Object listener = java.lang.reflect.Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "position": return position;
                            case "radius": return 16;
                            case "rules": return rules;
                            case "immediate": return true;
                            case "active": return true;
                            case "handle":
                                heard.add(String.valueOf(args[1].getClass().getMethod("name").invoke(args[1])));
                                return true;
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            default: return null;
                        }
                    });
            events.getMethod("add", net.minecraft.world.World.class, listenerType).invoke(null, c.world, listener);
            return listener;
        } catch (ReflectiveOperationException e) {
            c.fail("Core listener: " + e);
            return null;
        }
    }

    private static void removeCoreListener(Check c, Object listener) {
        try {
            Class<?> events = Class.forName("com.exiledradio.backportcore.gameevent.GameEvents");
            Class<?> listenerType = Class.forName("com.exiledradio.backportcore.gameevent.GameEventListener");
            events.getMethod("remove", net.minecraft.world.World.class, listenerType).invoke(null, c.world, listener);
        } catch (ReflectiveOperationException e) {
            c.fail("Core listener removal: " + e);
        }
    }
}
