package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.bundle.BundleClickMessage;
import com.exiledradio.trickytrialsbackport.bundle.BundleClicks;
import com.exiledradio.trickytrialsbackport.bundle.BundleContent;
import com.exiledradio.trickytrialsbackport.bundle.BundleContents;
import com.exiledradio.trickytrialsbackport.bundle.BundleSelectMessage;
import com.exiledradio.trickytrialsbackport.bundle.ItemBundle;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.tileentity.TileEntityDispenser;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Stream 11: bundles. */
final class BundleSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bundles";
    }

    private static ItemBundle bundleItem() {
        return BundleContent.BUNDLES.get("bundle");
    }

    private static ItemStack bundle(ItemStack... contents) {
        ItemStack bundle = new ItemStack(bundleItem());
        BundleContents.Mutable m = new BundleContents.Mutable(bundle);
        // Oldest first, as a player would put them in.
        for (int i = 0; i < contents.length; i++) {
            m.tryInsert(contents[i].copy());
        }
        m.save(bundle);
        return bundle;
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        weights(c);
        insertAndRemove(c);
        grid(c);
        clicks(c);
        selection(c);
        use(c);
        recipes(c);
        bar(c);
        dispenser(c);
        destroyed(c);
        loot(c);
        nbt(c);
        tooltipLines(c);
    }

    private void registration(Check c) {
        c.eq(17, BundleContent.BUNDLES.size(), "bundles registered");
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black"};
        for (String color : colors) {
            Item item = Item.REGISTRY.getObject(new ResourceLocation(TrickyTrialsBackport.MOD_ID, color + "_bundle"));
            c.ok(item instanceof ItemBundle, color + "_bundle registered");
        }
        c.eq(1, new ItemStack(bundleItem()).getMaxStackSize(), "bundles do not stack");
        c.eq(EnumDyeColor.SILVER, ((ItemBundle) BundleContent.BUNDLES.get("light_gray_bundle")).color, "light gray is silver's");
        c.eq("item.trickytrialsbackport.red_bundle", BundleContent.BUNDLES.get("red_bundle").getTranslationKey(), "red bundle's key");
    }

    private void weights(Check c) {
        c.eq(Fraction.getFraction(1, 64), BundleContents.weightOfOne(new ItemStack(Blocks.COBBLESTONE)), "cobblestone weighs 1/64");
        c.eq(Fraction.getFraction(1, 16), BundleContents.weightOfOne(new ItemStack(Items.ENDER_PEARL)), "an ender pearl weighs 1/16");
        c.eq(Fraction.ONE, BundleContents.weightOfOne(new ItemStack(Items.DIAMOND_SWORD)), "a sword weighs 1");
        c.eq(Fraction.getFraction(1, 16), BundleContents.weightOfOne(new ItemStack(bundleItem())), "an empty bundle weighs 4/64");
        ItemStack half = bundle(new ItemStack(Blocks.COBBLESTONE, 32));
        c.eq(Fraction.getFraction(1, 2), BundleContents.weight(half), "32 cobblestone fill half");
        c.eq(Fraction.getFraction(9, 16), BundleContents.weightOfOne(half), "a half-full bundle weighs 4/64 + 32/64");
        c.ok(!BundleContents.canItemBeInBundle(new ItemStack(Blocks.PURPLE_SHULKER_BOX)), "no shulker boxes");
        c.ok(BundleContents.canItemBeInBundle(new ItemStack(bundleItem())), "bundles go in bundles");
        ItemStack hive = new ItemStack(Blocks.CHEST);
        NBTTagCompound bees = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagCompound());
        bees.setTag("Bees", list);
        hive.setTagInfo("BlockEntityTag", bees);
        c.eq(Fraction.ONE, BundleContents.weightOfOne(hive), "a block with bees weighs 1");

        ItemStack full = bundle(new ItemStack(Blocks.COBBLESTONE, 64));
        ItemStack outer = new ItemStack(bundleItem());
        BundleContents.Mutable m = new BundleContents.Mutable(outer);
        c.eq(0, m.tryInsert(full.copy()), "a full bundle (17/16) does not fit in another");
        c.eq(1, m.tryInsert(half.copy()), "a half-full bundle fits");
        c.eq(Fraction.getFraction(9, 16), m.weight(), "and weighs 9/16 there");
        c.eq(7, m.tryInsert(new ItemStack(Items.ENDER_PEARL, 16)), "then 7 pearls fit in the 7/16 left");
        c.eq(Fraction.ONE, m.weight(), "full");
    }

    private void insertAndRemove(Check c) {
        ItemStack bundle = new ItemStack(bundleItem());
        BundleContents.Mutable m = new BundleContents.Mutable(bundle);
        ItemStack cobble = new ItemStack(Blocks.COBBLESTONE, 40);
        c.eq(40, m.tryInsert(cobble), "40 cobblestone go in");
        c.ok(cobble.isEmpty(), "and leave the stack empty");
        ItemStack dirt = new ItemStack(Blocks.DIRT, 64);
        c.eq(24, m.tryInsert(dirt), "24 dirt fill it");
        c.eq(40, dirt.getCount(), "40 dirt stay out");
        c.eq(0, m.tryInsert(new ItemStack(Blocks.SAND)), "nothing more fits");
        c.ok(m.items().get(0).getItem() == Item.getItemFromBlock(Blocks.DIRT), "the newest is first");
        c.eq(-1, BundleContents.selectedItem(bundle), "nothing picked");
        ItemStack out = m.removeOne();
        c.ok(out != null && out.getItem() == Item.getItemFromBlock(Blocks.DIRT) && out.getCount() == 24, "the newest comes out first");
        // A stack of the same item merges and moves to the front.
        m.tryInsert(new ItemStack(Blocks.SAND, 8));
        m.tryInsert(new ItemStack(Blocks.COBBLESTONE, 4));
        c.eq(2, m.items().size(), "cobblestone merged");
        c.eq(44, m.items().get(0).getCount(), "into one stack of 44, now first");
        m.toggleSelectedItem(1);
        ItemStack picked = m.removeOne();
        c.ok(picked != null && picked.getItem() == Item.getItemFromBlock(Blocks.SAND), "the picked stack comes out");
        m.save(bundle);
        c.eq(-1, BundleContents.selectedItem(bundle), "and the pick is cleared");
        c.eq(1, BundleContents.items(bundle).size(), "saved");
        m.removeOne();
        m.save(bundle);
        c.ok(!bundle.hasTagCompound(), "an emptied bundle has no tag");
    }

    private void grid(Check c) {
        int[][] table = {{0, 0}, {1, 1}, {4, 4}, {5, 5}, {12, 12}, {13, 8}, {16, 11}, {17, 8}, {64, 11}};
        for (int[] row : table) {
            c.eq(row[1], BundleContents.numberOfItemsToShow(row[0]), "stacks shown for " + row[0]);
        }
        c.eq(0, BundleContents.nextScrollWheelSelection(-1, -1, 5), "wheel down from nothing picks the first");
        c.eq(4, BundleContents.nextScrollWheelSelection(1, -1, 5), "wheel up from nothing picks the last");
        c.eq(0, BundleContents.nextScrollWheelSelection(-1, 4, 5), "wheel down wraps");
        c.eq(2, BundleContents.nextScrollWheelSelection(1, 3, 5), "wheel up picks the one before");
    }

    private EntityPlayerMP player(Check c, String name) {
        EntityPlayerMP player = Players.real(c.world, name);
        BlockPos at = c.area();
        player.setPosition(at.getX() + 16.5, at.getY(), at.getZ() + 16.5);
        player.inventory.clear();
        // Crafting sends the recipe book, which asks the connection's channel: give it one that goes nowhere.
        new io.netty.channel.embedded.EmbeddedChannel(player.connection.getNetworkManager());
        return player;
    }

    private void clicks(Check c) {
        EntityPlayerMP player = player(c, "bundleclicks");
        Container inv = player.inventoryContainer;
        Slot main = inv.getSlot(9);
        Slot other = inv.getSlot(10);

        // A bundle on the cursor, left click on cobblestone: it takes all that fits.
        main.putStack(new ItemStack(Blocks.COBBLESTONE, 64));
        player.inventory.setItemStack(bundle(new ItemStack(Blocks.DIRT, 16)));
        c.eq(BundleClicks.Outcome.BUNDLE, BundleClicks.decide(player.inventory.getItemStack(), main.getStack(), true), "left click with a bundle is a bundle click");
        c.ok(BundleClickMessage.apply(player, inv.windowId, 9, true), "the server plays it out");
        c.eq(16, main.getStack().getCount(), "48 cobblestone taken, 16 left");
        c.eq(Fraction.ONE, BundleContents.weight(player.inventory.getItemStack()), "the bundle is full");
        // Left click again: nothing fits, the click is still the bundle's (and the stack stays).
        c.ok(BundleClicks.click(main, true, player), "a full bundle still takes the click");
        c.eq(16, main.getStack().getCount(), "nothing taken");
        // Right click on an empty slot: the newest stack comes out into it.
        c.ok(BundleClicks.click(other, false, player), "right click on an empty slot");
        c.eq(48, other.getStack().getCount(), "48 cobblestone put down");
        c.eq(1, BundleContents.items(player.inventory.getItemStack()).size(), "the dirt is left");
        // Right click on a full slot with a bundle: an ordinary click (a swap).
        c.eq(BundleClicks.Outcome.UNSELECT, BundleClicks.decide(player.inventory.getItemStack(), new ItemStack(bundleItem()), false),
                "right click with a bundle on a bundle is ordinary");
        c.eq(BundleClicks.Outcome.NONE, BundleClicks.decide(player.inventory.getItemStack(), new ItemStack(Blocks.DIRT), false),
                "right click with a bundle on dirt is ordinary");
        c.eq(BundleClicks.Outcome.NONE, BundleClicks.decide(player.inventory.getItemStack(), ItemStack.EMPTY, true),
                "left click with a bundle on an empty slot puts it down");

        // A bundle in the slot, cobblestone on the cursor: left click packs the cursor.
        player.inventory.setItemStack(new ItemStack(Blocks.COBBLESTONE, 10));
        main.putStack(new ItemStack(bundleItem()));
        c.ok(BundleClickMessage.apply(player, inv.windowId, 9, true), "left click on a bundle with cobblestone");
        c.ok(player.inventory.getItemStack().isEmpty(), "the cursor is empty");
        c.eq(10, BundleContents.items(main.getStack()).get(0).getCount(), "10 cobblestone in the bundle");
        // Right click on it with an empty cursor: unpacks into the cursor.
        c.ok(BundleClicks.click(main, false, player), "right click on a bundle, empty cursor");
        c.eq(10, player.inventory.getItemStack().getCount(), "the cursor holds the 10");
        c.ok(BundleContents.items(main.getStack()).isEmpty(), "the bundle is empty");
        // Left click on a bundle with an empty cursor: an ordinary pickup.
        player.inventory.setItemStack(ItemStack.EMPTY);
        c.eq(BundleClicks.Outcome.UNSELECT, BundleClicks.decide(ItemStack.EMPTY, main.getStack(), true), "left click on a bundle picks it up");
        // A shulker box is refused (the click is still the bundle's).
        player.inventory.setItemStack(new ItemStack(Blocks.RED_SHULKER_BOX));
        c.ok(BundleClicks.click(main, true, player), "a shulker box click");
        c.ok(!player.inventory.getItemStack().isEmpty() && BundleContents.items(main.getStack()).isEmpty(), "the shulker box stays out");

        // The crafting result: a bundle there takes nothing (vanilla's allowModification), and a bundle on the
        // cursor takes the result whole or not at all.
        Slot result = inv.getSlot(0);
        player.inventory.setItemStack(new ItemStack(Blocks.COBBLESTONE, 5));
        inv.getSlot(1).putStack(new ItemStack(Blocks.LOG, 1));
        inv.onCraftMatrixChanged(player.inventory);
        c.ok(result.getHasStack() && result.getStack().getCount() == 4, "logs make 4 planks in the result slot");
        player.inventory.setItemStack(bundle(new ItemStack(Blocks.COBBLESTONE, 62)));
        c.ok(BundleClicks.click(result, true, player), "a nearly full bundle on the result");
        c.ok(result.getHasStack() && result.getStack().getCount() == 4, "the result is not split");
        player.inventory.setItemStack(new ItemStack(bundleItem()));
        c.ok(BundleClicks.click(result, true, player), "an empty bundle on the result");
        c.eq(4, BundleContents.items(player.inventory.getItemStack()).get(0).getCount(), "takes the 4 planks");
        c.ok(inv.getSlot(1).getStack().isEmpty(), "and the log is used");

        // A wrong window id does nothing.
        c.ok(!BundleClickMessage.apply(player, 99, 9, true), "a click for another window is ignored");
        player.inventory.clear();
        player.inventory.setItemStack(ItemStack.EMPTY);
        for (int i = 0; i < 5; i++) {
            inv.getSlot(i).putStack(ItemStack.EMPTY);
        }
    }

    private void selection(Check c) {
        EntityPlayerMP player = player(c, "bundlepick");
        Container inv = player.inventoryContainer;
        inv.detectAndSendChanges();
        Slot main = inv.getSlot(9);
        main.putStack(bundle(new ItemStack(Blocks.DIRT, 4), new ItemStack(Blocks.SAND, 4), new ItemStack(Blocks.GRAVEL, 4)));
        inv.detectAndSendChanges();
        BundleSelectMessage.apply(player, inv.windowId, 9, 2);
        c.eq(2, BundleContents.selectedItem(main.getStack()), "the server records the pick");
        c.ok(ItemStack.areItemStacksEqual(inv.inventoryItemStacks.get(9), main.getStack()), "and does not send it back");
        c.ok(BundleContents.selectedStack(main.getStack()).getItem() == Item.getItemFromBlock(Blocks.DIRT), "index 2 is the oldest, dirt");
        BundleSelectMessage.apply(player, inv.windowId, 9, 2);
        c.eq(-1, BundleContents.selectedItem(main.getStack()), "the same index again clears it (vanilla toggles)");
        BundleSelectMessage.apply(player, inv.windowId, 9, 7);
        c.eq(-1, BundleContents.selectedItem(main.getStack()), "an index past the end picks nothing");
        BundleSelectMessage.apply(player, inv.windowId, 9, 1);
        player.inventory.setItemStack(ItemStack.EMPTY);
        c.ok(BundleClicks.click(main, false, player), "right click unpacks");
        c.ok(player.inventory.getItemStack().getItem() == Item.getItemFromBlock(Blocks.SAND), "the picked sand");
        c.eq(-1, BundleContents.selectedItem(main.getStack()), "and the pick is gone");
        BundleSelectMessage.apply(player, inv.windowId, 9, 0);
        BundleContent.clearPicks(inv);
        c.eq(-1, BundleContents.selectedItem(main.getStack()), "closing the screen clears picks");
        player.inventory.clear();
        player.inventory.setItemStack(ItemStack.EMPTY);
    }

    private List<EntityItem> itemsNear(Check c, Entity at) {
        return c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at.getPosition()).grow(4),
                e -> e != null && !e.isDead);
    }

    private void use(Check c) {
        EntityPlayerMP player = player(c, "bundleuse");
        ItemStack bundle = bundle(new ItemStack(Blocks.DIRT, 4), new ItemStack(Blocks.SAND, 4), new ItemStack(Blocks.GRAVEL, 4));
        player.setHeldItem(EnumHand.MAIN_HAND, bundle);
        c.eq(200, bundle.getMaxItemUseDuration(), "use lasts 200 ticks");
        List<Integer> dropped = new ArrayList<Integer>();
        for (int count = 200; count > 180; count--) {
            int before = BundleContents.items(bundle).size();
            bundle.getItem().onUsingTick(bundle, player, count);
            if (BundleContents.items(bundle).size() < before) {
                dropped.add(count);
            }
        }
        c.eq("[200, 188, 186]", dropped.toString(), "drops at once, then every 2 ticks after 10");
        List<EntityItem> items = itemsNear(c, player);
        c.eq(3, items.size(), "three stacks thrown");
        c.ok(!BundleContent.BUNDLES.get("bundle").canContinueUsing(bundle, new ItemStack(Items.STICK)), "another item stops the use");
        c.ok(BundleContent.BUNDLES.get("bundle").canContinueUsing(bundle, bundle(new ItemStack(Blocks.DIRT))), "a changed bundle keeps it going");
        for (EntityItem e : items) {
            e.setDead();
        }
        c.ok(!ItemBundle.dropContent(bundle, player), "an empty bundle drops nothing");
    }

    private void recipes(Check c) {
        Crafting.expect(c, Crafting.grid(1, 2, new ItemStack(Items.STRING), new ItemStack(Items.LEATHER)),
                new ItemStack(bundleItem()), "string over leather");
        ItemStack filled = bundle(new ItemStack(Blocks.DIRT, 5));
        InventoryCrafting dye = Crafting.grid(2, 1, filled, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
        IRecipe recipe = CraftingManager.findMatchingRecipe(dye, c.world);
        ItemStack red = recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(dye);
        c.ok(red.getItem() == BundleContent.BUNDLES.get("red_bundle"), "bundle + red dye = red bundle, got " + red);
        c.eq(5, BundleContents.items(red).isEmpty() ? 0 : BundleContents.items(red).get(0).getCount(), "it keeps the 5 dirt");
        InventoryCrafting again = Crafting.grid(2, 1, red, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
        c.ok(CraftingManager.findMatchingRecipe(again, c.world) == null, "a red bundle with red dye makes nothing");
        InventoryCrafting blue = Crafting.grid(2, 1, red, new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()));
        c.ok(Crafting.same(CraftingManager.findMatchingResult(blue, c.world), new ItemStack(BundleContent.BUNDLES.get("blue_bundle"))),
                "a red bundle dyes blue");
        InventoryCrafting gray = Crafting.grid(2, 1, red, new ItemStack(Items.DYE, 1, EnumDyeColor.SILVER.getDyeDamage()));
        c.ok(Crafting.same(CraftingManager.findMatchingResult(gray, c.world), new ItemStack(BundleContent.BUNDLES.get("light_gray_bundle"))),
                "light gray dye makes the light gray bundle");
        InventoryCrafting two = Crafting.grid(3, 1, red, filled, new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()));
        c.ok(CraftingManager.findMatchingRecipe(two, c.world) == null, "two bundles and a dye make nothing");
    }

    private void bar(Check c) {
        ItemBundle item = bundleItem();
        ItemStack empty = new ItemStack(item);
        c.ok(!item.showDurabilityBar(empty), "an empty bundle has no bar");
        ItemStack one = bundle(new ItemStack(Blocks.DIRT, 1));
        c.eq(1, ItemBundle.barWidth(one), "1/64: 1 pixel");
        ItemStack half = bundle(new ItemStack(Blocks.DIRT, 32));
        c.eq(7, ItemBundle.barWidth(half), "half: 7 pixels");
        c.eq(7, Math.round(13.0F - (float) item.getDurabilityForDisplay(half) * 13.0F), "1.12.2 draws 7");
        c.eq(0x7087FF, item.getRGBDurabilityForDisplay(half), "blue while not full");
        ItemStack full = bundle(new ItemStack(Blocks.DIRT, 64));
        c.eq(13, ItemBundle.barWidth(full), "full: 13 pixels");
        c.eq(0xFF5454, item.getRGBDurabilityForDisplay(full), "red when full");
    }

    private void dispenser(Check c) {
        IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(bundleItem());
        c.ok(behavior != null && behavior.getClass() == BehaviorDefaultDispenseItem.class, "a dispenser drops bundles (the default)");
        BlockPos at = c.area().add(8, 0, 8);
        IBlockState state = Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.UP);
        c.world.setBlockState(at, state, 2);
        TileEntityDispenser tile = (TileEntityDispenser) c.world.getTileEntity(at);
        ItemStack filled = bundle(new ItemStack(Blocks.DIRT, 7));
        tile.setInventorySlotContents(0, filled);
        ItemStack out = behavior.dispense(new BlockSourceImpl(c.world, at), tile.getStackInSlot(0));
        c.ok(out.isEmpty(), "the dispenser's bundle is gone");
        List<EntityItem> items = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(3), e -> !e.isDead);
        boolean whole = false;
        for (EntityItem e : items) {
            if (BundleContents.isBundle(e.getItem()) && BundleContents.items(e.getItem()).size() == 1
                    && BundleContents.items(e.getItem()).get(0).getCount() == 7) {
                whole = true;
            }
            e.setDead();
        }
        c.ok(whole, "it flew out whole, with its 7 dirt");
    }

    private void destroyed(Check c) {
        BlockPos at = c.area().add(8, 0, 8);
        EntityItem entity = new EntityItem(c.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                bundle(new ItemStack(Blocks.DIRT, 3), new ItemStack(Items.ENDER_PEARL, 2)));
        c.world.spawnEntity(entity);
        // Despawned or picked up (dead, health left): nothing spills.
        entity.setDead();
        new BundleContent.DestroyedBundles().onEntityRemoved(entity);
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(3), e -> !e.isDead).size(),
                "a bundle that leaves without damage spills nothing");
        EntityItem burnt = new EntityItem(c.world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                bundle(new ItemStack(Blocks.DIRT, 3), new ItemStack(Items.ENDER_PEARL, 2)));
        c.world.spawnEntity(burnt);
        burnt.attackEntityFrom(DamageSource.LAVA, 10.0F);
        c.ok(burnt.isDead, "lava kills the bundle");
        c.eq(0, BundleContent.health(burnt) > 0 ? 1 : 0, "its health is gone");
        new BundleContent.DestroyedBundles().onEntityRemoved(burnt);
        List<EntityItem> spilled = c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(3), e -> !e.isDead);
        c.eq(2, spilled.size(), "both stacks spill");
        int pearls = 0;
        for (EntityItem e : spilled) {
            if (e.getItem().getItem() == Items.ENDER_PEARL) {
                pearls += e.getItem().getCount();
            }
            e.setDead();
        }
        c.eq(2, pearls, "the 2 pearls among them");
    }

    private void loot(Check c) {
        LootTable table = c.world.getLootTableManager().getLootTableFromLocation(new ResourceLocation("minecraft", "chests/village_blacksmith"));
        LootPool pool = table.getPool(TrickyTrialsBackport.MOD_ID + ":bundle");
        c.ok(pool != null, "the blacksmith chest has the bundle pool (no Village & Pillage here)");
        if (pool == null) {
            return;
        }
        Random random = new Random(42);
        LootContext context = new LootContext.Builder(c.world).build();
        int bundles = 0;
        for (int i = 0; i < 3000; i++) {
            List<ItemStack> out = new ArrayList<ItemStack>();
            pool.generateLoot(out, random, context);
            for (ItemStack s : out) {
                if (s.getItem() == bundleItem()) {
                    bundles += s.getCount();
                }
            }
        }
        c.ok(bundles > 900 && bundles < 1100, "one in three chests, got " + bundles + " of 3000");
        LootTable plains = c.world.getLootTableManager().getLootTableFromLocation(new ResourceLocation("minecraft", "chests/simple_dungeon"));
        c.ok(plains.getPool(TrickyTrialsBackport.MOD_ID + ":bundle") == null, "dungeons have no bundle");
    }

    /**
     * The tooltip image's placeholder lines are found where a screen draws them. GuiScreen.getItemToolTip puts
     * the rarity color before the name and gray before every other line, so a placeholder never starts with
     * its marker by the time it is drawn (the image was never drawn while the search looked at the start).
     */
    private void tooltipLines(Check c) {
        List<String> lines = new ArrayList<String>();
        lines.add("White Bundle (#5173)");
        for (int i = 0; i < 5; i++) {
            lines.add(com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.PLACEHOLDER);
        }
        lines.add("trickytrialsbackport:white_bundle");
        lines.add("NBT: 1 tag(s)");
        lines.add("");
        // What GuiScreen.getItemToolTip does to the lines.
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, (i == 0 ? net.minecraft.util.text.TextFormatting.WHITE : net.minecraft.util.text.TextFormatting.GRAY) + lines.get(i));
        }
        c.ok(!lines.get(1).startsWith(com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.MARKER),
                "a screen's gray comes before the marker");
        c.eq(1, com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.firstPlaceholder(lines), "the first placeholder is found after the name");
        // Another mod's own codes in front as well.
        lines.set(1, "§o§r" + lines.get(1));
        c.eq(1, com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.firstPlaceholder(lines), "and with more codes in front");
        c.ok(!com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.isPlaceholder(lines.get(7)), "an ordinary line is not one");
        c.eq(-1, com.exiledradio.trickytrialsbackport.bundle.BundleTooltipLines.firstPlaceholder(lines.subList(6, lines.size())), "none among the others");
    }

    private void nbt(Check c) {
        ItemStack nested = bundle(bundle(new ItemStack(Blocks.DIRT, 10)), new ItemStack(Items.ENDER_PEARL, 3));
        ItemStack copy = new ItemStack(nested.writeToNBT(new NBTTagCompound()));
        c.ok(ItemStack.areItemStacksEqual(nested, copy), "a nested bundle survives NBT");
        List<ItemStack> items = BundleContents.items(copy);
        c.eq(2, items.size(), "two stacks");
        c.ok(BundleContents.isBundle(items.get(1)) && BundleContents.items(items.get(1)).get(0).getCount() == 10, "the inner bundle keeps its dirt");
        c.eq(Fraction.getReducedFraction(14, 64), BundleContents.weightOfOne(items.get(1)), "the inner bundle weighs 4/64 + 10/64");
        c.eq(Fraction.getReducedFraction(26, 64), BundleContents.weight(copy), "the outer holds 14/64 + 12/64 of pearls");
    }
}
