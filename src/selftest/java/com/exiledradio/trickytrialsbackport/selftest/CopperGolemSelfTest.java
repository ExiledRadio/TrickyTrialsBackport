package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperGolemStatue;
import com.exiledradio.trickytrialsbackport.coppergolem.CopperGolemContent;
import com.exiledradio.trickytrialsbackport.coppergolem.CopperGolemEvents;
import com.exiledradio.trickytrialsbackport.coppergolem.CopperGolemTransport;
import com.exiledradio.trickytrialsbackport.coppergolem.EntityCopperGolem;
import com.exiledradio.trickytrialsbackport.coppergolem.ItemCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperGolemStatue;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAILookAtVillager;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LockCode;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.Random;

/**
 * Stream 19: the copper golem, copper chests and copper golem statues: building, oxidation and the statue, waxing and
 * scraping, the sorting behavior (finding chests, moving matching items, its timing) and the copper chest.
 */
public final class CopperGolemSelfTest implements SelfTest {

    @Override
    public String name() {
        return "coppergolem";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(CopperGolemContent.enabled(), "copperGolem.enabled")) {
            return;
        }
        if (!OreDictionary.containsMatch(false, OreDictionary.getOres("honeycomb", false), new ItemStack(Items.SLIME_BALL))) {
            OreDictionary.registerOre("honeycomb", Items.SLIME_BALL);
        }
        registration(c);
        recipes(c);
        chests(c, c.area());
        statues(c, c.area());
        building(c, c.area());
        golem(c, c.area());
        weathering(c, c.area());
        sorting(c, c.area());
        sortingRules(c, c.area());
        ironGolems(c, c.area());
    }

    private static Block block(String name) {
        return Owners.ourBlock(name);
    }

    private static Item item(String name) {
        Item i = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return i != null && Content.id(name).equals(i.getRegistryName()) ? i : null;
    }

    private static BlockCopperChest chest(int stage, boolean waxed) {
        return CopperGolemContent.CHESTS[waxed ? 1 : 0][stage];
    }

    private static BlockCopperGolemStatue statue(int stage, boolean waxed) {
        return CopperGolemContent.STATUES[waxed ? 1 : 0][stage];
    }

    /** Like {@link Drops#count}, but leaves other items where they are. */
    private static int count(Check c, BlockPos at, Item want) {
        int n = 0;
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(2))) {
            if (!e.isDead && e.getItem().getItem() == want) {
                n += e.getItem().getCount();
                e.setDead();
            }
        }
        return n;
    }

    private static void clearEntities(Check c, BlockPos area) {
        for (Entity e : c.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(area.add(-8, -40, -8), area.add(40, 60, 40)))) {
            if (!(e instanceof net.minecraft.entity.player.EntityPlayer)) {
                e.setDead();
            }
        }
    }

    // ------------------------------------------------------------------------------------------

    private void registration(Check c) {
        int n = 0;
        for (int w = 0; w < 2; w++) {
            for (int stage = 0; stage < 4; stage++) {
                boolean waxed = w == 1;
                String cn = CopperGolemContent.chestName(stage, waxed);
                String sn = CopperGolemContent.statueName(stage, waxed);
                for (String name : new String[] {cn, sn}) {
                    Block b = block(name);
                    if (c.ok(b != null, name + " registered")) {
                        n++;
                        c.ok(item(name) != null, name + " has an item");
                        c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(name), name + " owned by this mod");
                        c.eq(3.0F, b.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), name + " hardness 3");
                        c.ok(Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-3, name + " resistance 6");
                        c.eq(!waxed && stage < 3, b.getTickRandomly(), name + " ages only unwaxed and not oxidized");
                    }
                }
                c.eq(waxed ? null : (stage < 3 ? chest(stage + 1, false) : null), Oxidation.next(chest(stage, waxed)), cn + " ages into the next");
                c.eq(waxed ? chest(stage, false) : null, Oxidation.unwaxed(chest(stage, waxed)), cn + " unwaxes");
                c.eq(waxed ? null : statue(stage, true), Oxidation.waxedOf(statue(stage, waxed)), sn + " waxes");
            }
        }
        c.eq(16, n, "16 blocks: 8 chests and 8 statues");
        c.ok(chest(0, false).chestType != BlockChest.Type.BASIC && chest(0, false).chestType != BlockChest.Type.TRAP,
                "copper chests have their own chest type (they never join a wooden chest)");
        c.eq(50, EntityRegistry.instance().lookupModSpawn(EntityCopperGolem.class, false).getModEntityId(), "entity id 50");
        c.ok(TileEntity.getKey(TileEntityCopperChest.class) != null && TileEntity.getKey(TileEntityCopperGolemStatue.class) != null,
                "tile entities registered");
        for (int stage = 0; stage < 4; stage++) {
            Block copper = Owners.block(new String[] {"copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"}[stage]);
            if (c.ok(copper != null, "copper block of stage " + stage + " exists")) {
                c.eq(stage, CopperGolemContent.copperBlockStage(copper), "copper block stage " + stage);
                if (Owners.present(Owners.CAVES_AND_CLIFFS)) {
                    c.eq(Owners.CAVES_AND_CLIFFS, copper.getRegistryName().getNamespace(), "the copper block is Caves & Cliffs'");
                }
            }
        }
        c.eq(-1, CopperGolemContent.copperBlockStage(Blocks.IRON_BLOCK), "iron is not copper");
        Item ingot = CopperGolemContent.ingot();
        c.ok(ingot != null, "a copper ingot exists");
        if (ingot != null) {
            c.eq(Owners.present(Owners.CAVES_AND_CLIFFS) ? Owners.CAVES_AND_CLIFFS : TrickyTrialsBackport.MOD_ID,
                    ingot.getRegistryName().getNamespace(), "the ingot is the owner's when installed");
        }
    }

    private void recipes(Check c) {
        Item ingot = CopperGolemContent.ingot();
        if (ingot != null) {
            ItemStack i = new ItemStack(ingot);
            Crafting.expect(c, Crafting.grid(3, 3, i, i, i, i, new ItemStack(Blocks.CHEST), i, i, i, i),
                    new ItemStack(chest(0, false)), "a copper chest: 8 ingots around a chest");
        }
        for (int stage = 0; stage < 4; stage++) {
            Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(chest(stage, false)), new ItemStack(Items.SLIME_BALL)),
                    new ItemStack(chest(stage, true)), "honeycomb waxes chest " + stage + " in the grid");
            Crafting.expect(c, Crafting.grid(2, 1, new ItemStack(statue(stage, false)), new ItemStack(Items.SLIME_BALL)),
                    new ItemStack(statue(stage, true)), "and statue " + stage);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Copper chests
    // ------------------------------------------------------------------------------------------

    private static FakePlayer holding(Check c, String name, BlockPos at, ItemStack stack) {
        FakePlayer p = c.player(name, at);
        p.setHeldItem(EnumHand.MAIN_HAND, stack);
        return p;
    }

    private static void rightClick(Check c, FakePlayer p, BlockPos pos) {
        PlayerInteractEvent.RightClickBlock e = new PlayerInteractEvent.RightClickBlock(p, EnumHand.MAIN_HAND, pos, EnumFacing.UP,
                new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5));
        CopperGolemEvents.rightClick(e);
        if (!e.isCanceled()) {
            IBlockState s = c.world.getBlockState(pos);
            s.getBlock().onBlockActivated(c.world, pos, s, p, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        }
    }

    private static TileEntityChest te(Check c, BlockPos pos) {
        TileEntity te = c.world.getTileEntity(pos);
        return te instanceof TileEntityChest ? (TileEntityChest) te : null;
    }

    private static boolean place(Check c, Block b, BlockPos pos) {
        FakePlayer p = c.player("place", pos.up(3));
        ItemStack stack = new ItemStack(b);
        return ((net.minecraft.item.ItemBlock) stack.getItem()).placeBlockAt(stack, p, c.world, pos, EnumFacing.UP, 0.5F, 1.0F, 0.5F,
                b.getStateForPlacement(c.world, pos, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, p, EnumHand.MAIN_HAND));
    }

    private void chests(Check c, BlockPos a) {
        BlockCopperChest plain = chest(0, false);
        FakePlayer hand = holding(c, "hand", a, ItemStack.EMPTY);
        c.ok(!plain.canHarvestBlock(c.world, a, hand), "a copper chest needs a pickaxe");
        FakePlayer pick = holding(c, "pick", a, new ItemStack(Items.STONE_PICKAXE));
        c.ok(plain.canHarvestBlock(c.world, a, pick), "a stone pickaxe mines it");
        c.ok(item(plain.getRegistryName().getPath()) instanceof ItemCopperChest, "the chest's item places by 1.21's rule");

        // Joining: the least oxidized of the two, unwaxed when only one was waxed.
        BlockPos p1 = a.add(2, 1, 2);
        BlockPos p2 = p1.east();
        c.ok(place(c, chest(2, false), p1), "a weathered chest placed");
        c.ok(place(c, chest(1, true), p2), "a waxed exposed chest placed beside it");
        c.eq(chest(1, false), c.world.getBlockState(p1).getBlock(), "both halves become exposed, unwaxed (the old one)");
        c.eq(chest(1, false), c.world.getBlockState(p2).getBlock(), "both halves become exposed, unwaxed (the new one)");
        TileEntityChest t1 = te(c, p1);
        TileEntityChest t2 = te(c, p2);
        c.ok(t1 instanceof TileEntityCopperChest && t2 instanceof TileEntityCopperChest, "both halves have copper chest tile entities");
        if (t1 == null || t2 == null) {
            return;
        }
        t1.checkForAdjacentChests();
        c.ok(t1.adjacentChestXPos == t2, "1.12.2's chest code sees them as one double chest");
        c.ok(!plain.canPlaceBlockAt(c.world, p2.east()), "no third chest beside a double copper chest");
        BlockPos wood = p1.add(0, 0, 3);
        c.world.setBlockState(wood, Blocks.CHEST.getDefaultState(), 3);
        c.world.setBlockState(wood.east(), plain.getDefaultState(), 3);
        TileEntityChest w = te(c, wood);
        w.checkForAdjacentChests();
        c.ok(w.adjacentChestXPos == null, "a wooden chest never joins a copper chest");
        c.world.setBlockToAir(wood);
        c.world.setBlockToAir(wood.east());

        t1.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 5));
        // Scraping one half: the other follows, the contents stay.
        rightClick(c, holding(c, "axe", p1, new ItemStack(Items.IRON_AXE)), p1);
        c.eq(chest(0, false), c.world.getBlockState(p1).getBlock(), "an axe scrapes the half it hits");
        c.eq(chest(0, false), c.world.getBlockState(p2).getBlock(), "the other half follows");
        c.eq(t1, te(c, p1), "the tile entity stays");
        c.eq(5, t1.getStackInSlot(0).getCount(), "the contents stay");
        // Waxing: honeycomb (here a slime ball with the ore name) waxes both halves, using one.
        ItemStack comb = new ItemStack(Items.SLIME_BALL, 3);
        FakePlayer waxer = holding(c, "wax", p2, comb);
        rightClick(c, waxer, p2);
        c.eq(chest(0, true), c.world.getBlockState(p1).getBlock(), "honeycomb waxes the double chest");
        c.eq(chest(0, true), c.world.getBlockState(p2).getBlock(), "both halves");
        c.eq(2, waxer.getHeldItemMainhand().getCount(), "one honeycomb used");
        rightClick(c, holding(c, "axe2", p1, new ItemStack(Items.IRON_AXE)), p1);
        c.eq(chest(0, false), c.world.getBlockState(p2).getBlock(), "an axe takes the wax off both");

        // Aging: a lone chest, 3000 random ticks, ends oxidized with its contents; waxed never changes.
        BlockPos lone = a.add(10, 1, 10);
        c.world.setBlockState(lone, plain.getDefaultState(), 3);
        te(c, lone).setInventorySlotContents(4, new ItemStack(Items.APPLE, 7));
        Random r = new Random(19);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(lone);
            s.getBlock().randomTick(c.world, lone, s, r);
        }
        c.eq(chest(3, false), c.world.getBlockState(lone).getBlock(), "a lone copper chest oxidizes fully in 3000 random ticks");
        c.eq(7, te(c, lone).getStackInSlot(4).getCount(), "and keeps its contents");
        BlockPos waxed = a.add(14, 1, 10);
        c.world.setBlockState(waxed, chest(0, true).getDefaultState(), 3);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(waxed);
            s.getBlock().randomTick(c.world, waxed, s, r);
        }
        c.eq(chest(0, true), c.world.getBlockState(waxed).getBlock(), "a waxed copper chest never ages");
        // An open chest does not age.
        BlockPos open = a.add(18, 1, 10);
        c.world.setBlockState(open, plain.getDefaultState(), 3);
        te(c, open).numPlayersUsing = 1;
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(open);
            s.getBlock().randomTick(c.world, open, s, r);
        }
        c.eq(plain, c.world.getBlockState(open).getBlock(), "an open copper chest does not age");
        te(c, open).numPlayersUsing = 0;
        // A double chest ages from one half (1.21's: not the right one), both halves together.
        BlockPos d1 = a.add(2, 1, 20);
        BlockPos d2 = d1.east();
        c.world.setBlockState(d1, plain.getDefaultState().withProperty(BlockChest.FACING, EnumFacing.SOUTH), 3);
        c.world.setBlockState(d2, plain.getDefaultState().withProperty(BlockChest.FACING, EnumFacing.SOUTH), 3);
        // Facing south, the half with its partner counterclockwise (east) is the right half: d1.
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(d1);
            s.getBlock().randomTick(c.world, d1, s, r);
        }
        c.eq(plain, c.world.getBlockState(d1).getBlock(), "the right half's ticks do nothing");
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(d2);
            s.getBlock().randomTick(c.world, d2, s, r);
        }
        c.eq(chest(3, false), c.world.getBlockState(d2).getBlock(), "the left half ages");
        c.eq(chest(3, false), c.world.getBlockState(d1).getBlock(), "and the right half with it");
        c.eq(EnumFacing.SOUTH, c.world.getBlockState(d1).getValue(BlockChest.FACING), "keeping its facing");

        // Breaking drops the chest and its contents.
        NonNullList<ItemStack> drops = NonNullList.create();
        c.world.getBlockState(lone).getBlock().getDrops(drops, c.world, lone, c.world.getBlockState(lone), 0);
        c.ok(drops.size() == 1 && drops.get(0).getItem() == Item.getItemFromBlock(chest(3, false)), "an oxidized chest drops itself");
        c.world.destroyBlock(lone, false);
        c.eq(7, Drops.count(c.world, lone, Items.APPLE), "breaking it spills its contents");
        // Comparator and hoppers see the double chest as one inventory.
        IInventory both = chest(3, false).getContainer(c.world, d1, true);
        c.eq(54, both == null ? 0 : both.getSizeInventory(), "the double chest holds 54 stacks");
    }

    // ------------------------------------------------------------------------------------------
    // Statues
    // ------------------------------------------------------------------------------------------

    private void statues(Check c, BlockPos a) {
        BlockCopperGolemStatue s0 = statue(0, false);
        BlockPos p = a.add(4, 1, 4);
        IBlockState placed = s0.getStateForPlacement(c.world, p, EnumFacing.UP, 0.5F, 0.0F, 0.5F, 2, c.player("st", p.north(3)), EnumHand.MAIN_HAND);
        c.eq(BlockCopperGolemStatue.Pose.RUNNING, placed.getValue(BlockCopperGolemStatue.POSE), "the item's damage value is the pose");
        for (int meta = 0; meta < 16; meta++) {
            c.eq(meta, s0.getMetaFromState(s0.getStateFromMeta(meta)), "statue metadata " + meta + " round-trips");
        }
        c.world.setBlockState(p, s0.getDefaultState().withProperty(BlockCopperGolemStatue.FACING, EnumFacing.EAST), 3);
        TileEntityCopperGolemStatue te = (TileEntityCopperGolemStatue) c.world.getTileEntity(p);
        c.ok(te != null, "a statue has a tile entity");
        c.eq(1, s0.getComparatorInputOverride(c.world.getBlockState(p), c.world, p), "comparator 1 standing");
        FakePlayer hand = holding(c, "pose", p, ItemStack.EMPTY);
        String[] want = {"sitting", "running", "star", "standing"};
        for (int i = 0; i < 4; i++) {
            rightClick(c, hand, p);
            c.eq(want[i], c.world.getBlockState(p).getValue(BlockCopperGolemStatue.POSE).getName(), "using it poses it: " + want[i]);
        }
        rightClick(c, hand, p);
        c.eq(2, s0.getComparatorInputOverride(c.world.getBlockState(p), c.world, p), "comparator 2 sitting");
        c.eq(EnumFacing.EAST, c.world.getBlockState(p).getValue(BlockCopperGolemStatue.FACING), "posing keeps its facing");
        // Name and drops.
        ((TileEntityCopperGolemStatue) c.world.getTileEntity(p)).readFromNBT(nameTag(p, "Bob"));
        NonNullList<ItemStack> drops = NonNullList.create();
        s0.getDrops(drops, c.world, p, c.world.getBlockState(p), 0);
        c.ok(drops.size() == 1 && drops.get(0).getMetadata() == 1 && "Bob".equals(drops.get(0).getDisplayName()),
                "it drops itself in its pose (sitting, 1) with its name: " + drops);
        // Aging and waxing keep the pose and tile entity.
        Random r = new Random(3);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(p);
            s.getBlock().randomTick(c.world, p, s, r);
        }
        c.eq(statue(3, false), c.world.getBlockState(p).getBlock(), "a lone statue oxidizes in 3000 random ticks");
        c.eq("Bob", ((TileEntityCopperGolemStatue) c.world.getTileEntity(p)).getCustomName(), "keeping its name");
        c.eq(BlockCopperGolemStatue.Pose.SITTING, c.world.getBlockState(p).getValue(BlockCopperGolemStatue.POSE), "and its pose");
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        FakePlayer axer = holding(c, "saxe", p, axe);
        rightClick(c, axer, p);
        c.eq(statue(2, false), c.world.getBlockState(p).getBlock(), "an axe scrapes a stage off");
        c.eq(1, axer.getHeldItemMainhand().getItemDamage(), "using the axe once");
        FakePlayer waxer = holding(c, "swax", p, new ItemStack(Items.SLIME_BALL, 2));
        rightClick(c, waxer, p);
        c.eq(statue(2, true), c.world.getBlockState(p).getBlock(), "honeycomb waxes it");
        rightClick(c, waxer, p);
        c.eq(BlockCopperGolemStatue.Pose.RUNNING, c.world.getBlockState(p).getValue(BlockCopperGolemStatue.POSE),
                "honeycomb on a waxed statue poses it instead");
        rightClick(c, axer, p);
        c.eq(statue(2, false), c.world.getBlockState(p).getBlock(), "an axe takes the wax off");
        rightClick(c, axer, p);
        rightClick(c, axer, p);
        c.eq(statue(0, false), c.world.getBlockState(p).getBlock(), "scraped back to unaffected");
        // The axe on an unaffected, unwaxed statue: a copper golem with the name, the block gone.
        rightClick(c, axer, p);
        c.ok(c.world.isAirBlock(p), "an axe on an unaffected statue removes it");
        List<EntityCopperGolem> golems = c.world.getEntitiesWithinAABB(EntityCopperGolem.class, new AxisAlignedBB(p).grow(1));
        if (c.eq(1, golems.size(), "and brings back a copper golem")) {
            EntityCopperGolem g = golems.get(0);
            c.eq("Bob", g.getCustomNameTag(), "with its name");
            c.eq(0, g.getWeatherState(), "unaffected");
            c.ok(Math.abs(g.posX - (p.getX() + 0.5)) < 1e-6 && Math.abs(g.posY - p.getY()) < 1e-6, "at the statue's bottom center");
            c.ok(Math.abs(g.rotationYaw - EnumFacing.EAST.getHorizontalAngle()) < 1e-3, "facing the statue's way");
            g.setDead();
        }
        c.eq(5, axer.getHeldItemMainhand().getItemDamage(), "the axe wore once more");
    }

    private static net.minecraft.nbt.NBTTagCompound nameTag(BlockPos p, String name) {
        net.minecraft.nbt.NBTTagCompound t = new net.minecraft.nbt.NBTTagCompound();
        t.setInteger("x", p.getX());
        t.setInteger("y", p.getY());
        t.setInteger("z", p.getZ());
        t.setString("CustomName", name);
        return t;
    }

    // ------------------------------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------------------------------

    private static EntityCopperGolem golemNear(Check c, BlockPos p) {
        List<EntityCopperGolem> list = c.world.getEntitiesWithinAABB(EntityCopperGolem.class, new AxisAlignedBB(p).grow(1.5));
        return list.isEmpty() ? null : list.get(0);
    }

    private void building(Check c, BlockPos a) {
        Block exposed = Owners.block("exposed_copper");
        Block waxedWeathered = Owners.block("waxed_weathered_copper");
        if (!c.ok(exposed != null && waxedWeathered != null, "copper blocks to build with")) {
            return;
        }
        // Upright: copper, then a pumpkin on it facing west.
        BlockPos base = a.add(3, 1, 3);
        c.world.setBlockState(base, exposed.getDefaultState(), 3);
        c.world.setBlockState(base.up(), Blocks.PUMPKIN.getDefaultState().withProperty(BlockPumpkin.FACING, EnumFacing.WEST), 3);
        EntityCopperGolem g = golemNear(c, base.up());
        if (c.ok(g != null, "a pumpkin on a copper block builds a copper golem")) {
            c.eq(1, g.getWeatherState(), "at the copper's stage (exposed)");
            c.ok(Math.abs(g.posX - (base.getX() + 0.5)) < 1e-6 && Math.abs(g.posY - (base.getY() + 1.05)) < 1e-6,
                    "where the pumpkin was, 0.05 up");
            c.eq(12.0F, g.getMaxHealth(), "12 health");
            c.ok(Math.abs(g.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() - 0.2) < 1e-6,
                    "speed 0.2");
            c.ok(!g.isWaxed() && g.getNextWeatheringTick() == -1L, "unwaxed, its aging not yet timed");
            g.setDead();
        }
        c.ok(c.world.isAirBlock(base.up()), "the pumpkin is used up");
        IBlockState chestState = c.world.getBlockState(base);
        c.eq(chest(1, false), chestState.getBlock(), "the copper block becomes an exposed copper chest");
        if (chestState.getBlock() instanceof BlockCopperChest) {
            c.eq(EnumFacing.WEST, chestState.getValue(BlockChest.FACING), "facing the pumpkin's way");
        }
        // Waxed copper, a jack o'lantern, sideways: the golem and chest come out unwaxed.
        BlockPos side = a.add(10, 1, 3);
        c.world.setBlockState(side, waxedWeathered.getDefaultState(), 3);
        c.world.setBlockState(side.east(), Blocks.LIT_PUMPKIN.getDefaultState(), 3);
        g = golemNear(c, side.east());
        if (c.ok(g != null, "a jack o'lantern beside waxed copper builds one too")) {
            c.eq(2, g.getWeatherState(), "weathered");
            g.setDead();
        }
        c.eq(chest(2, false), c.world.getBlockState(side).getBlock(), "an unwaxed weathered chest");
        // Beside a single copper chest the new chest joins it, least oxidized.
        BlockPos next = a.add(3, 1, 10);
        c.world.setBlockState(next.west(), chest(3, false).getDefaultState(), 3);
        c.world.setBlockState(next, Owners.block("copper_block").getDefaultState(), 3);
        c.world.setBlockState(next.up(), Blocks.PUMPKIN.getDefaultState(), 3);
        c.eq(chest(0, false), c.world.getBlockState(next).getBlock(), "the new chest is unaffected");
        c.eq(chest(0, false), c.world.getBlockState(next.west()).getBlock(), "and the oxidized chest beside it joins as unaffected");
        EntityCopperGolem g3 = golemNear(c, next.up());
        c.ok(g3 != null, "that golem spawned");
        if (g3 != null) {
            g3.setDead();
        }
        // The pumpkin must be last.
        BlockPos order = a.add(16, 1, 3);
        c.world.setBlockState(order.up(), Blocks.PUMPKIN.getDefaultState(), 3);
        c.world.setBlockState(order, exposed.getDefaultState(), 3);
        c.ok(golemNear(c, order.up()) == null, "a copper block put under a pumpkin builds nothing");
        // Iron blocks are not copper; the setting stops building.
        BlockPos iron = a.add(20, 1, 3);
        c.world.setBlockState(iron, Blocks.IRON_BLOCK.getDefaultState(), 3);
        c.world.setBlockState(iron.up(), Blocks.PUMPKIN.getDefaultState(), 3);
        c.ok(golemNear(c, iron.up()) == null, "a pumpkin on one iron block builds nothing");
        BlockPos off = a.add(24, 1, 3);
        ModConfig.copperGolem.building = false;
        try {
            c.world.setBlockState(off, exposed.getDefaultState(), 3);
            c.world.setBlockState(off.up(), Blocks.PUMPKIN.getDefaultState(), 3);
            c.ok(golemNear(c, off.up()) == null, "copperGolem.building=false builds nothing");
        } finally {
            ModConfig.copperGolem.building = true;
        }
        // A dispenser places a pumpkin onto copper (1.21's canSpawnGolem), and the golem is built.
        BlockPos disp = a.add(3, 1, 20);
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.UP), 3);
        c.world.setBlockState(disp.up(2), exposed.getDefaultState(), 3);
        ItemStack pumpkins = new ItemStack(Blocks.PUMPKIN, 2);
        ItemStack left = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Item.getItemFromBlock(Blocks.PUMPKIN))
                .dispense(new net.minecraft.block.BlockSourceImpl(c.world, disp), pumpkins);
        c.eq(1, left.getCount(), "the dispenser used a pumpkin");
        c.ok(golemNear(c, disp.up()) != null, "and a golem was built under the copper block (upside down)");
        clearEntities(c, a);
    }

    // ------------------------------------------------------------------------------------------
    // The golem: interaction, waxing, lightning, drops
    // ------------------------------------------------------------------------------------------

    private static EntityCopperGolem spawn(Check c, BlockPos p) {
        EntityCopperGolem g = new EntityCopperGolem(c.world);
        g.setLocationAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0F, 0.0F);
        c.world.spawnEntity(g);
        return g;
    }

    private void golem(Check c, BlockPos a) {
        BlockPos p = a.add(8, 0, 8);
        EntityCopperGolem g = spawn(c, p);
        g.setWeatherState(2);
        FakePlayer pl = holding(c, "golem", p.north(2), new ItemStack(Items.SLIME_BALL, 2));
        c.ok(g.processInteract(pl, EnumHand.MAIN_HAND), "honeycomb waxes a golem");
        c.ok(g.isWaxed(), "waxed (next_weather_age -2)");
        c.eq(1, pl.getHeldItemMainhand().getCount(), "using one");
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        pl.setHeldItem(EnumHand.MAIN_HAND, axe);
        c.ok(g.processInteract(pl, EnumHand.MAIN_HAND), "an axe takes the wax off");
        c.ok(!g.isWaxed() && g.getNextWeatheringTick() == -1L, "unwaxed, aging re-timed");
        c.eq(2, g.getWeatherState(), "still weathered");
        c.ok(g.processInteract(pl, EnumHand.MAIN_HAND), "an axe scrapes it");
        c.eq(1, g.getWeatherState(), "exposed");
        c.eq(2, pl.getHeldItemMainhand().getItemDamage(), "the axe wore twice");
        g.setWeatherState(0);
        c.ok(!g.processInteract(pl, EnumHand.MAIN_HAND), "nothing to scrape from an unaffected golem");
        // Lightning scrapes one stage per bolt.
        g.setWeatherState(3);
        EntityLightningBolt bolt = new EntityLightningBolt(c.world, g.posX, g.posY, g.posZ, true);
        g.onStruckByLightning(bolt);
        g.setHealth(12.0F);
        g.onStruckByLightning(bolt);
        g.setHealth(12.0F);
        c.eq(2, g.getWeatherState(), "a lightning bolt scrapes one stage, however often it hits");
        g.onStruckByLightning(new EntityLightningBolt(c.world, g.posX, g.posY, g.posZ, true));
        c.eq(1, g.getWeatherState(), "another bolt another stage");
        g.extinguish();
        g.setHealth(12.0F);
        // Taking its item: an empty hand makes it throw what it carries.
        g.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE, 9));
        pl.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.ok(g.processInteract(pl, EnumHand.MAIN_HAND), "an empty hand takes its item");
        c.ok(g.getHeldItemMainhand().isEmpty(), "it lets go");
        c.eq(9, Drops.count(c.world, p, Item.getItemFromBlock(Blocks.COBBLESTONE)), "it throws the 9 cobblestone");
        // Breathing and falling (1.21's tags).
        c.ok(g.canBreatheUnderwater(), "breathes under water");
        float before = g.getHealth();
        g.fall(20.0F, 1.0F);
        c.eq(before, g.getHealth(), "takes no fall damage");
        // Saving.
        g.setWeatherState(2);
        g.setNextWeatheringTick(123456L);
        g.setAntenna(new ItemStack(Blocks.RED_FLOWER));
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        g.writeEntityToNBT(tag);
        c.eq("weathered", tag.getString("weather_state"), "weather_state saved by name");
        c.eq(123456L, tag.getLong("next_weather_age"), "next_weather_age saved");
        EntityCopperGolem copy = new EntityCopperGolem(c.world);
        copy.readEntityFromNBT(tag);
        c.eq(2, copy.getWeatherState(), "weather state read back");
        c.eq(123456L, copy.getNextWeatheringTick(), "aging time read back");
        c.ok(copy.getAntenna().getItem() == Item.getItemFromBlock(Blocks.RED_FLOWER), "antenna read back");
        // Shearing the poppy.
        pl.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        c.ok(g.processInteract(pl, EnumHand.MAIN_HAND), "shears take the poppy");
        c.ok(g.getAntenna().isEmpty(), "the antenna is bare");
        c.eq(1, Drops.count(c.world, p, Item.getItemFromBlock(Blocks.RED_FLOWER)), "one poppy dropped");
        c.ok(!g.processInteract(pl, EnumHand.MAIN_HAND), "nothing more to shear");
        // Death drops 1-3 copper ingots (and its carried stack).
        Item ingot = CopperGolemContent.ingot();
        int lo = Integer.MAX_VALUE;
        int hi = 0;
        for (int i = 0; i < 40 && ingot != null; i++) {
            EntityCopperGolem d = spawn(c, p.add(0, 0, 4));
            d.attackEntityFrom(DamageSource.GENERIC, 100.0F);
            int n = Drops.count(c.world, p.add(0, 0, 4), ingot);
            lo = Math.min(lo, n);
            hi = Math.max(hi, n);
            d.setDead();
        }
        if (ingot != null) {
            c.ok(lo >= 1 && hi <= 3 && hi > lo, "a dead golem drops 1-3 copper ingots (saw " + lo + "-" + hi + ")");
        }
        g.setDead();
        clearEntities(c, a);
    }

    // ------------------------------------------------------------------------------------------
    // Aging over time and the statue
    // ------------------------------------------------------------------------------------------

    private void weathering(Check c, BlockPos a) {
        BlockPos p = a.add(8, 0, 8);
        EntityCopperGolem g = spawn(c, p);
        long t = 1000L;
        g.updateWeathering(c.world, t);
        long first = g.getNextWeatheringTick();
        c.ok(first >= t + 504000 && first <= t + 552000, "its first stage is timed 504000-552000 ticks ahead: " + (first - t));
        g.updateWeathering(c.world, first - 1);
        c.eq(0, g.getWeatherState(), "not a tick early");
        g.updateWeathering(c.world, first);
        c.eq(1, g.getWeatherState(), "exposed on time");
        long second = g.getNextWeatheringTick();
        c.ok(second - first >= 504000 && second - first <= 552000, "the next stage timed from the last: " + (second - first));
        g.updateWeathering(c.world, second);
        g.updateWeathering(c.world, g.getNextWeatheringTick());
        c.eq(3, g.getWeatherState(), "oxidized after three stages");
        c.eq(0L, g.getNextWeatheringTick(), "next_weather_age 0 once oxidized");
        // Waxed: no aging.
        EntityCopperGolem w = spawn(c, p.add(6, 0, 0));
        w.setNextWeatheringTick(-2L);
        w.updateWeathering(c.world, 10000000L);
        c.eq(0, w.getWeatherState(), "a waxed golem never ages");
        w.setWeatherState(3);
        for (int i = 0; i < 3000; i++) {
            w.updateWeathering(c.world, 10000000L);
        }
        c.ok(!w.isDead, "a waxed oxidized golem never turns to a statue");
        // The statue: 0.0058 per tick, in air, with its name, its carried stack and poppy dropped.
        g.setCustomNameTag("Cuprum");
        g.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BONE, 5));
        g.setGuaranteedDrop();
        g.setAntenna(new ItemStack(Blocks.RED_FLOWER));
        g.rotationYaw = 90.0F;
        int ticks = 0;
        while (!g.isDead && ticks < 20000) {
            g.updateWeathering(c.world, 5L);
            ticks++;
        }
        c.ok(g.isDead, "an oxidized golem turns into a statue (after " + ticks + " ticks; 1.21 averages 172)");
        c.ok(ticks > 5 && ticks < 2000, "at about 0.58% a tick");
        IBlockState s = c.world.getBlockState(p);
        c.eq(statue(3, false), s.getBlock(), "an oxidized statue where it stood");
        if (s.getBlock() instanceof BlockCopperGolemStatue) {
            c.eq(EnumFacing.WEST, s.getValue(BlockCopperGolemStatue.FACING), "facing its way (yaw 90 is west)");
            TileEntity te = c.world.getTileEntity(p);
            c.ok(te instanceof TileEntityCopperGolemStatue && "Cuprum".equals(((TileEntityCopperGolemStatue) te).getCustomName()),
                    "keeping its name");
        }
        c.eq(5, count(c, p, Items.BONE), "its carried stack drops");
        c.eq(1, count(c, p, Item.getItemFromBlock(Blocks.RED_FLOWER)), "and its poppy");
        // Not in water, nor in any other block.
        BlockPos wet = a.add(16, 0, 8);
        c.world.setBlockState(wet, Blocks.WATER.getDefaultState(), 3);
        EntityCopperGolem sub = spawn(c, wet);
        sub.setWeatherState(3);
        sub.setNextWeatheringTick(0L);
        for (int i = 0; i < 3000; i++) {
            sub.updateWeathering(c.world, 5L);
        }
        c.ok(!sub.isDead, "never under water");
        // Statues switched off.
        EntityCopperGolem off = spawn(c, a.add(20, 0, 8));
        off.setWeatherState(3);
        off.setNextWeatheringTick(0L);
        ModConfig.copperGolem.statues = false;
        try {
            for (int i = 0; i < 3000; i++) {
                off.updateWeathering(c.world, 5L);
            }
            c.ok(!off.isDead, "copperGolem.statues=false keeps it a golem");
        } finally {
            ModConfig.copperGolem.statues = true;
        }
        clearEntities(c, a);
    }

    // ------------------------------------------------------------------------------------------
    // Sorting
    // ------------------------------------------------------------------------------------------

    private static void tickGolem(EntityCopperGolem g, int n) {
        for (int i = 0; i < n && !g.isDead; i++) {
            g.onUpdate();
        }
    }

    private void sorting(Check c, BlockPos a) {
        BlockPos y = a.add(0, 0, 0);
        BlockPos copperPos = y.add(6, 0, 10);
        BlockPos near = y.add(10, 0, 4);
        BlockPos far = y.add(18, 0, 4);
        c.world.setBlockState(copperPos, chest(0, false).getDefaultState(), 3);
        c.world.setBlockState(near, Blocks.CHEST.getDefaultState(), 3);
        c.world.setBlockState(far, Blocks.TRAPPED_CHEST.getDefaultState(), 3);
        TileEntityChest source = te(c, copperPos);
        TileEntityChest nearChest = te(c, near);
        TileEntityChest farChest = te(c, far);
        source.setInventorySlotContents(3, new ItemStack(Blocks.COBBLESTONE, 20));
        source.setInventorySlotContents(5, new ItemStack(Items.APPLE, 2));
        nearChest.setInventorySlotContents(0, new ItemStack(Blocks.DIRT, 1));
        farChest.setInventorySlotContents(0, new ItemStack(Blocks.COBBLESTONE, 60));
        EntityCopperGolem g = spawn(c, y.add(6, 0, 4));
        g.transportCooldown = 0;

        c.eq(copperPos, g.transport.findTarget(c.world), "empty-handed it looks for a copper chest");
        int gettingStart = -1;
        int pickedAt = -1;
        int openedSeen = 0;
        for (int t = 0; t < 1200 && g.getHeldItemMainhand().isEmpty(); t++) {
            g.onUpdate();
            if (gettingStart < 0 && g.getGolemState() == EntityCopperGolem.STATE_GETTING_ITEM) {
                gettingStart = t;
            }
            if (g.getGolemState() == EntityCopperGolem.STATE_GETTING_ITEM && source.numPlayersUsing == 1) {
                openedSeen++;
            }
            if (!g.getHeldItemMainhand().isEmpty()) {
                pickedAt = t;
            }
        }
        c.ok(gettingStart >= 0, "it reached the copper chest and began taking an item");
        c.ok(pickedAt > 0, "it took an item");
        c.eq(59, pickedAt - gettingStart, "60 ticks at the chest (the interaction's first tick to its 60th)");
        c.eq(59, openedSeen, "the chest's lid open the whole time");
        c.eq(0, source.numPlayersUsing, "and shut again after");
        ItemStack held = g.getHeldItemMainhand();
        c.ok(held.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE) && held.getCount() == 16, "16 of the first stack: " + held);
        c.eq(4, source.getStackInSlot(3).getCount(), "4 left behind");
        c.eq(2, source.getStackInSlot(5).getCount(), "the other stack untouched");
        c.ok(g.visitedPositions().isEmpty(), "a successful pickup clears what it remembered");
        c.eq(near, g.transport.findTarget(c.world), "holding something it looks for the nearest chest");

        // The near chest holds only dirt: it stands there, cannot drop, remembers it and goes on to the trapped chest.
        boolean sawNoDrop = false;
        for (int t = 0; t < 2400 && !g.getHeldItemMainhand().isEmpty(); t++) {
            g.onUpdate();
            if (g.getGolemState() == EntityCopperGolem.STATE_DROPPING_NO_ITEM) {
                sawNoDrop = true;
            }
        }
        c.ok(sawNoDrop, "at the dirt chest it cannot place its item");
        c.ok(g.getHeldItemMainhand().isEmpty(), "and then puts it down elsewhere");
        c.eq(1, nearChest.getStackInSlot(0).getCount(), "the dirt chest unchanged");
        c.eq(64, farChest.getStackInSlot(0).getCount(), "the trapped chest's cobblestone stack filled to 64");
        c.eq(12, farChest.getStackInSlot(1).getCount(), "the rest in the next slot");
        c.ok(g.visitedPositions().isEmpty(), "a successful drop clears what it remembered");

        // It goes back for more: the rest of the cobblestone, then the apples, which fit nowhere but an empty chest.
        tickGolem(g, 1);
        source.clear();
        nearChest.clear();
        farChest.clear();
        source.setInventorySlotContents(0, new ItemStack(Items.APPLE, 3));
        nearChest.setInventorySlotContents(0, new ItemStack(Items.APPLE, 1));
        // A chest someone has open makes it wait its turn.
        g.transportCooldown = 0;
        boolean queued = false;
        boolean dropped = false;
        for (int t = 0; t < 3000 && !dropped; t++) {
            if (!queued && !g.getHeldItemMainhand().isEmpty()) {
                nearChest.numPlayersUsing = 1;
            }
            g.onUpdate();
            if (g.transport.state() == CopperGolemTransport.State.QUEUING) {
                queued = true;
                nearChest.numPlayersUsing = 0;
            }
            dropped = nearChest.getStackInSlot(0).getCount() == 4;
        }
        nearChest.numPlayersUsing = 0;
        c.ok(queued, "it waits behind someone using the chest");
        c.ok(dropped, "then puts the apples onto the apple stack");

        // Nothing left anywhere: it rests 140 ticks.
        source.clear();
        int cooldown = -1;
        for (int t = 0; t < 1500 && cooldown < 0; t++) {
            g.onUpdate();
            if (g.transportCooldown > 0) {
                cooldown = g.transportCooldown;
            }
        }
        c.ok(cooldown > 0 && cooldown <= 140, "with no item to fetch it rests up to 140 ticks: " + cooldown);
        clearEntities(c, a);
        // The next area is within a golem's 32 blocks.
        for (BlockPos p : new BlockPos[] {copperPos, near, far}) {
            c.world.setBlockToAir(p);
        }
    }

    private void sortingRules(Check c, BlockPos a) {
        // Search box: 32 across, 8 up or down.
        EntityCopperGolem g = spawn(c, a.add(1, 0, 1));
        g.transportCooldown = 1000;
        BlockPos inRange = a.add(1, 8, 1 + 30);
        c.world.setBlockState(inRange.down(), Blocks.STONE.getDefaultState(), 3);
        c.world.setBlockState(inRange, chest(0, false).getDefaultState(), 3);
        te(c, inRange).setInventorySlotContents(0, new ItemStack(Items.STICK));
        c.eq(inRange, g.transport.findTarget(c.world), "a copper chest 30 away and 8 up is found");
        c.world.setBlockToAir(inRange);
        BlockPos tooHigh = a.add(1, 9, 1 + 4);
        c.world.setBlockState(tooHigh, chest(0, false).getDefaultState(), 3);
        c.eq(null, g.transport.findTarget(c.world), "one 9 up is not");
        c.world.setBlockToAir(tooHigh);
        BlockPos edge = a.add(1 + 32, 0, 1);
        c.world.setBlockState(edge, chest(0, false).getDefaultState(), 3);
        c.eq(edge, g.transport.findTarget(c.world), "one 32 across is found");
        c.world.setBlockToAir(edge);
        BlockPos beyond = a.add(1 + 33, 0, 1);
        c.world.setBlockState(beyond, chest(0, false).getDefaultState(), 3);
        c.eq(null, g.transport.findTarget(c.world), "one 33 across is not");
        c.world.setBlockToAir(beyond);
        // Wooden chests are never sources; barrels and the rest are never anything.
        BlockPos wood = a.add(4, 0, 1);
        c.world.setBlockState(wood, Blocks.CHEST.getDefaultState(), 3);
        te(c, wood).setInventorySlotContents(0, new ItemStack(Items.STICK));
        c.eq(null, g.transport.findTarget(c.world), "an empty-handed golem ignores wooden chests");
        g.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        c.eq(wood, g.transport.findTarget(c.world), "a golem with an item wants it");
        BlockPos copper = a.add(2, 0, 1);
        c.world.setBlockState(copper, chest(0, false).getDefaultState(), 3);
        c.eq(wood, g.transport.findTarget(c.world), "and never puts things into copper chests");
        // A locked chest is skipped.
        te(c, wood).setLockCode(new LockCode("key"));
        c.eq(null, g.transport.findTarget(c.world), "a locked chest is skipped");
        te(c, wood).setLockCode(LockCode.EMPTY_CODE);
        // Riding, it only looks 1 block around.
        c.world.setBlockToAir(copper);
        g.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        c.world.setBlockState(a.add(4, 0, 4), chest(0, false).getDefaultState(), 3);
        net.minecraft.entity.item.EntityMinecartEmpty cart = new net.minecraft.entity.item.EntityMinecartEmpty(c.world, g.posX, g.posY, g.posZ);
        c.world.spawnEntity(cart);
        g.startRiding(cart, true);
        c.eq(null, g.transport.findTarget(c.world), "while riding it searches only 1 block around");
        g.dismountRidingEntity();
        c.eq(a.add(4, 0, 4), g.transport.findTarget(c.world), "and 32 when not");
        cart.setDead();

        // Matching: the same item whatever its damage; 1.12.2's colors by metadata.
        ItemStack worn = new ItemStack(Items.IRON_SWORD);
        worn.setItemDamage(100);
        c.ok(CopperGolemTransport.isSameItem(worn, new ItemStack(Items.IRON_SWORD)), "a worn sword matches a new one");
        c.ok(!CopperGolemTransport.isSameItem(new ItemStack(Blocks.WOOL, 1, 1), new ItemStack(Blocks.WOOL, 1, 2)), "orange wool is not magenta wool");
        c.ok(CopperGolemTransport.isSameItem(new ItemStack(Blocks.WOOL, 1, 3), new ItemStack(Blocks.WOOL, 5, 3)), "light blue matches light blue");
        // Placing: the first empty slot or a matching stack before it.
        InventoryBasic inv = new InventoryBasic("t", false, 4);
        inv.setInventorySlotContents(1, new ItemStack(Items.STICK, 60));
        ItemStack left = CopperGolemTransport.addItemsToContainer(new ItemStack(Items.STICK, 16), inv);
        c.ok(left.isEmpty(), "all 16 placed");
        c.eq(16, inv.getStackInSlot(0).getCount(), "into the first slot, which was empty (1.21 does not look for the stack first)");
        c.eq(60, inv.getStackInSlot(1).getCount(), "the later stack untouched");
        inv.clear();
        inv.setInventorySlotContents(0, new ItemStack(Items.STICK, 60));
        inv.setInventorySlotContents(1, new ItemStack(Items.APPLE));
        inv.setInventorySlotContents(2, new ItemStack(Items.APPLE));
        inv.setInventorySlotContents(3, new ItemStack(Items.APPLE));
        left = CopperGolemTransport.addItemsToContainer(new ItemStack(Items.STICK, 16), inv);
        c.eq(64, inv.getStackInSlot(0).getCount(), "topping up a matching stack");
        c.eq(12, left.getCount(), "and keeping what does not fit");
        inv.clear();
        inv.setInventorySlotContents(2, new ItemStack(Blocks.STONE, 40));
        c.eq(16, CopperGolemTransport.pickupItemFromContainer(inv).getCount(), "taking at most 16");
        c.eq(24, inv.getStackInSlot(2).getCount(), "from the first stack");
        inv.setInventorySlotContents(0, new ItemStack(Items.ENDER_PEARL, 16));
        c.eq(16, CopperGolemTransport.pickupItemFromContainer(inv).getCount(), "16 pearls (the stack's own limit not lower)");
        // Off: no sorting at all.
        g.setDead();
        EntityCopperGolem idle = spawn(c, a.add(10, 0, 10));
        idle.transportCooldown = 0;
        BlockPos src = a.add(10, 0, 14);
        c.world.setBlockState(src, chest(0, false).getDefaultState(), 3);
        te(c, src).setInventorySlotContents(0, new ItemStack(Items.STICK, 5));
        ModConfig.copperGolem.sorting = false;
        try {
            tickGolem(idle, 400);
            c.ok(idle.getHeldItemMainhand().isEmpty(), "copperGolem.sorting=false: it never takes anything");
        } finally {
            ModConfig.copperGolem.sorting = true;
        }
        clearEntities(c, a);
    }

    // ------------------------------------------------------------------------------------------
    // Iron golems' poppies
    // ------------------------------------------------------------------------------------------

    private void ironGolems(Check c, BlockPos a) {
        EntityIronGolem iron = new EntityIronGolem(c.world);
        iron.setLocationAndAngles(a.getX() + 5, a.getY(), a.getZ() + 5, 0.0F, 0.0F);
        c.world.spawnEntity(iron);
        CopperGolemEvents.OfferFlower offer = null;
        boolean vanilla = false;
        for (EntityAITasks.EntityAITaskEntry e : iron.tasks.taskEntries) {
            if (e.action instanceof CopperGolemEvents.OfferFlower) {
                offer = (CopperGolemEvents.OfferFlower) e.action;
                c.eq(5, e.priority, "the offer at 1.12.2's priority 5");
            }
            vanilla |= e.action instanceof EntityAILookAtVillager;
        }
        c.ok(offer != null && !vanilla, "iron golems offer flowers to copper golems too (1.21.9's goal)");
        EntityCopperGolem g = spawn(c, a.add(7, 0, 5));
        if (offer != null) {
            offer.startExecuting();
            offer.force(g, 1);
            offer.updateTask();
            c.ok(!offer.shouldContinueExecuting(), "the offer runs its 400 ticks out");
            offer.resetTask();
            c.ok(g.getAntenna().getItem() == Item.getItemFromBlock(Blocks.RED_FLOWER), "the copper golem gets the poppy on its antenna");
            g.setAntenna(new ItemStack(Blocks.YELLOW_FLOWER));
            offer.force(g, 1);
            offer.updateTask();
            offer.resetTask();
            c.ok(g.getAntenna().getItem() == Item.getItemFromBlock(Blocks.YELLOW_FLOWER), "not when it already wears something");
            g.setAntenna(ItemStack.EMPTY);
            offer.force(g, 5);
            offer.updateTask();
            offer.resetTask();
            c.ok(g.getAntenna().isEmpty(), "nor when the offer is cut short");
            g.setPosition(a.getX() + 20, a.getY(), a.getZ() + 5);
            offer.force(g, 1);
            offer.updateTask();
            offer.resetTask();
            c.ok(g.getAntenna().isEmpty(), "nor when the copper golem walked off");
        }
        clearEntities(c, a);
    }
}
