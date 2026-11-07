package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.crafter.BlockCrafter;
import com.exiledradio.trickytrialsbackport.crafter.ContainerCrafter;
import com.exiledradio.trickytrialsbackport.crafter.CrafterContent;
import com.exiledradio.trickytrialsbackport.crafter.CrafterOrientation;
import com.exiledradio.trickytrialsbackport.crafter.TileEntityCrafter;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Stream 6: the crafter. */
public final class CrafterSelfTest implements SelfTest {

    @Override
    public String name() {
        return "crafter";
    }

    @Override
    public void run(Check c) {
        basics(c);
        BlockPos area = c.area();
        redstone(c, area);
        containers(c, area);
        hoppers(c, area);
        screen(c, area);
        advancement(c, area);
    }

    private static TileEntityCrafter place(Check c, BlockPos pos, CrafterOrientation o) {
        c.world.setBlockState(pos, CrafterContent.crafter.getDefaultState().withProperty(BlockCrafter.ORIENTATION, o));
        return (TileEntityCrafter) c.world.getTileEntity(pos);
    }

    private void basics(Check c) {
        c.eq(Content.id("crafter"), CrafterContent.crafter.getRegistryName(), "crafter");
        ItemStack i = new ItemStack(Items.IRON_INGOT);
        ItemStack r = new ItemStack(Items.REDSTONE);
        Crafting.expect(c, Crafting.grid(3, 3, i, i, i, i, new ItemStack(Blocks.CRAFTING_TABLE), i, r, new ItemStack(Blocks.DROPPER), r),
                new ItemStack(CrafterContent.crafter), "crafter recipe");
        for (CrafterOrientation o : CrafterOrientation.values()) {
            IBlockState s = CrafterContent.crafter.getDefaultState().withProperty(BlockCrafter.ORIENTATION, o);
            c.eq(s, CrafterContent.crafter.getStateFromMeta(CrafterContent.crafter.getMetaFromState(s)), o + " round trip");
        }
        c.eq(CrafterOrientation.NORTH_UP, CrafterOrientation.forPlacement(EnumFacing.NORTH, EnumFacing.SOUTH), "front toward the player");
        c.eq(CrafterOrientation.UP_SOUTH, CrafterOrientation.forPlacement(EnumFacing.UP, EnumFacing.SOUTH), "looking down: top the way they face");
        c.eq(CrafterOrientation.DOWN_NORTH, CrafterOrientation.forPlacement(EnumFacing.DOWN, EnumFacing.SOUTH), "looking up: top toward them");
        c.eq(1.5F, CrafterContent.crafter.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "hardness 1.5");
        c.ok(Math.abs(CrafterContent.crafter.getExplosionResistance(null) - 3.5F) < 0.01F, "blast resistance 3.5");
    }

    private static int items(Check c, BlockPos near, Item item) {
        int n = 0;
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(near).grow(1.5))) {
            if (!e.isDead && e.getItem().getItem() == item) {
                n += e.getItem().getCount();
                e.setDead();
            }
        }
        return n;
    }

    private void redstone(Check c, BlockPos area) {
        BlockPos pos = area.add(4, 1, 4);
        TileEntityCrafter t = place(c, pos, CrafterOrientation.EAST_UP);
        t.set(0, new ItemStack(Blocks.LOG, 2));
        c.world.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        c.ok(t.isTriggered(), "a redstone pulse triggers it");
        IBlockState shown = c.world.getBlockState(pos).getActualState(c.world, pos);
        c.ok(shown.getValue(BlockCrafter.TRIGGERED), "and shows it");
        Pools.settle(c.world);
        c.eq(4, items(c, pos.east(), Item.getItemFromBlock(Blocks.PLANKS)), "a log makes four planks out of the east face");
        c.eq(1, t.get(0).getCount(), "one log used");
        c.ok(t.isCrafting(), "crafting shown after a craft");
        for (int i = 0; i < 6; i++) {
            t.update();
        }
        c.ok(!t.isCrafting(), "for 6 ticks");

        // Still powered: nothing more until the power drops and comes back.
        c.world.notifyNeighborsOfStateChange(pos.west(), Blocks.REDSTONE_BLOCK, true);
        Pools.settle(c.world);
        c.eq(0, items(c, pos.east(), Item.getItemFromBlock(Blocks.PLANKS)), "one craft per pulse");
        c.world.setBlockToAir(pos.west());
        c.ok(!t.isTriggered(), "trigger cleared when the power drops");
        c.world.setBlockState(pos.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        Pools.settle(c.world);
        c.eq(4, items(c, pos.east(), Item.getItemFromBlock(Blocks.PLANKS)), "the next pulse crafts again");
        c.ok(t.get(0).isEmpty(), "both logs used");

        // An empty grid makes nothing.
        c.ok(!t.craft(), "an empty grid fails");
        c.world.setBlockToAir(pos.west());
        c.world.setBlockToAir(pos);
    }

    private void containers(Check c, BlockPos area) {
        BlockPos pos = area.add(8, 1, 4);
        TileEntityCrafter t = place(c, pos, CrafterOrientation.UP_NORTH);
        c.world.setBlockState(pos.up(), Blocks.CHEST.getDefaultState());
        // A cake: the milk buckets come back as empty buckets.
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        ItemStack sugar = new ItemStack(Items.SUGAR);
        ItemStack wheat = new ItemStack(Items.WHEAT);
        ItemStack[] grid = {milk, milk, milk, sugar, new ItemStack(Items.EGG), sugar, wheat, wheat, wheat};
        for (int i = 0; i < 9; i++) {
            t.set(i, grid[i].copy());
        }
        c.ok(t.craft(), "a cake is made");
        TileEntityChest chest = (TileEntityChest) c.world.getTileEntity(pos.up());
        int cakes = 0;
        int buckets = 0;
        for (int i = 0; i < chest.getSizeInventory(); i++) {
            ItemStack s = chest.getStackInSlot(i);
            if (s.getItem() == Items.CAKE) {
                cakes += s.getCount();
            } else if (s.getItem() == Items.BUCKET) {
                buckets += s.getCount();
            }
        }
        c.eq(1, cakes, "the cake goes into the chest above (an upward front)");
        c.eq(3, buckets, "and the three empty buckets");
        for (int i = 0; i < 9; i++) {
            c.ok(t.get(i).isEmpty(), "slot " + i + " used up");
        }

        // Saved and read back.
        t.set(2, new ItemStack(Items.DIAMOND, 5));
        t.setDisabled(7, true);
        NBTTagCompound tag = t.writeToNBT(new NBTTagCompound());
        TileEntityCrafter back = new TileEntityCrafter();
        back.readFromNBT(tag);
        c.ok(back.get(2).getCount() == 5 && back.isDisabled(7) && !back.isDisabled(2), "NBT round trip");

        // Breaking it drops what it holds.
        c.world.destroyBlock(pos, true);
        c.eq(5, items(c, pos, Items.DIAMOND), "breaking drops the grid");
    }

    private void hoppers(Check c, BlockPos area) {
        BlockPos pos = area.add(12, 1, 4);
        TileEntityCrafter t = place(c, pos, CrafterOrientation.NORTH_UP);
        t.setDisabled(4, true);
        IItemHandler handler = t.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        for (int i = 0; i < 16; i++) {
            ItemStack rest = ItemHandlerHelper.insertItem(handler, new ItemStack(Items.STICK), false);
            c.ok(rest.isEmpty(), "stick " + i + " goes in");
        }
        boolean even = true;
        for (int i = 0; i < 9; i++) {
            int want = i == 4 ? 0 : 2;
            even &= t.get(i).getCount() == want;
        }
        c.ok(even, "16 sticks spread two to each of the 8 enabled slots, none in the switched-off one");
        c.ok(handler.extractItem(0, 1, false).isEmpty() && t.get(0).getCount() == 2, "hoppers cannot take from it");
        c.eq(9, t.signal(), "comparator: 8 filled + 1 switched off");
        ItemStack odd = ItemHandlerHelper.insertItem(handler, new ItemStack(Items.FLINT), false);
        c.ok(!odd.isEmpty(), "a different item finds no room once every enabled slot has sticks");
        c.eq(9, CrafterContent.crafter.getComparatorInputOverride(c.world.getBlockState(pos), c.world, pos), "the block reports it");
        c.world.setBlockToAir(pos);
    }

    private void screen(Check c, BlockPos area) {
        BlockPos pos = area.add(16, 1, 4);
        TileEntityCrafter t = place(c, pos, CrafterOrientation.NORTH_UP);
        FakePlayer player = c.player("crafter", pos.south());
        ContainerCrafter container = new ContainerCrafter(player.inventory, t);
        container.slotClick(3, 0, ClickType.PICKUP, player);
        c.ok(t.isDisabled(3) && container.disabled(3), "clicking an empty slot switches it off");
        container.slotClick(3, 0, ClickType.PICKUP, player);
        c.ok(!t.isDisabled(3), "clicking again switches it on");
        player.inventory.setItemStack(new ItemStack(Items.APPLE));
        container.slotClick(3, 0, ClickType.PICKUP, player);
        c.ok(!t.isDisabled(3) && t.get(3).getItem() == Items.APPLE, "clicking with an item places it");
        t.setDisabled(5, true);
        player.inventory.setItemStack(new ItemStack(Items.APPLE));
        c.ok(!container.getSlot(5).isItemValid(new ItemStack(Items.APPLE)), "a switched-off slot takes nothing");
        player.inventory.setItemStack(ItemStack.EMPTY);
        c.world.setBlockToAir(pos);
    }

    private void advancement(Check c, BlockPos area) {
        Advancement adv = c.world.getMinecraftServer().getAdvancementManager()
                .getAdvancement(Content.id("adventure/crafters_crafting_crafters"));
        c.ok(adv != null, "Crafters Crafting Crafters loaded");
        EntityPlayerMP player = Players.real(c.world, "crafterfan");
        BlockPos pos = area.add(20, 1, 4);
        player.setPosition(pos.getX() + 3.5, pos.getY(), pos.getZ() + 0.5);
        c.world.spawnEntity(player);
        try {
            TileEntityCrafter t = place(c, pos, CrafterOrientation.NORTH_UP);
            ItemStack i = new ItemStack(Items.IRON_INGOT);
            ItemStack r = new ItemStack(Items.REDSTONE);
            ItemStack[] grid = {i, i, i, i, new ItemStack(Blocks.CRAFTING_TABLE), i, r, new ItemStack(Blocks.DROPPER), r};
            t.set(0, new ItemStack(Items.STICK));
            t.craft();
            c.ok(!player.getAdvancements().getProgress(adv).isDone(), "not for a stick");
            for (int k = 0; k < 9; k++) {
                t.set(k, grid[k].copy());
            }
            c.ok(t.craft(), "a crafter crafts a crafter");
            c.ok(player.getAdvancements().getProgress(adv).isDone(), "Crafters Crafting Crafters earned nearby");
        } finally {
            c.world.removeEntityDangerously(player);
        }
    }
}
