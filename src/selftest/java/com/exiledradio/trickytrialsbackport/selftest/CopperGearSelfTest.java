package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.asm.MobArmorTransformer;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.Owners;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearBlocks;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearContent;
import com.exiledradio.trickytrialsbackport.coppergear.CopperGearHooks;

import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Random;

/** Stream 20: copper tools, armor, horse armor, nuggets, torch, bars, chains, lanterns, and mobs wearing copper. */
public final class CopperGearSelfTest implements SelfTest {

    @Override
    public String name() {
        return "coppergear";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(CopperGearContent.enabled(), "copperGear.enabled")) {
            return;
        }
        registration(c);
        tools(c);
        armor(c);
        recipes(c);
        smelting(c);
        BlockPos area = c.area();
        blocks(c, area);
        waxing(c, area);
        aging(c, area);
        mobs(c);
        loot(c);
    }

    private static Item item(String name) {
        Item i = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return i != null && Content.id(name).equals(i.getRegistryName()) ? i : null;
    }

    private static Block block(String name) {
        return Owners.ourBlock(name);
    }

    private static ItemStack stack(String name) {
        return new ItemStack(item(name));
    }

    private void registration(Check c) {
        c.eq(25, CopperGearContent.BLOCKS.size(), "blocks: a torch and 3 shapes x 4 stages x waxed");
        for (String name : CopperGearContent.BLOCKS.keySet()) {
            c.ok(block(name) != null, name + " registered");
            c.eq(TrickyTrialsBackport.MOD_ID, BlockOwnership.owner(name), name + " owned by this mod");
            c.ok(item(name) != null, name + " has an item");
        }
        for (String name : new String[] {"copper_nugget", "copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel",
                "copper_hoe", "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots", "copper_horse_armor"}) {
            c.ok(item(name) != null, name + " registered");
        }
        c.ok(OreDictionary.containsMatch(false, OreDictionary.getOres("nuggetCopper"), stack("copper_nugget")), "nuggetCopper");
        Item ingot = CopperGearContent.ingot();
        if (Owners.present(Owners.CAVES_AND_CLIFFS)) {
            c.eq(Owners.CAVES_AND_CLIFFS, ingot == null ? null : ingot.getRegistryName().getNamespace(),
                    "the ingot is Caves & Cliffs' when it is installed");
        } else {
            c.eq(Content.id("copper_ingot"), ingot == null ? null : ingot.getRegistryName(), "the ingot is the copper stream's copy");
        }
    }

    private static double modifier(Item item, String attribute) {
        Multimap<String, AttributeModifier> map = item.getAttributeModifiers(EntityEquipmentSlot.MAINHAND, new ItemStack(item));
        double sum = 0;
        for (AttributeModifier m : map.get(attribute)) {
            sum += m.getAmount();
        }
        return sum;
    }

    private void tools(Check c) {
        Item.ToolMaterial m = CopperGearContent.toolMaterial;
        c.eq(190, m.getMaxUses(), "tool durability 190");
        c.eq(5.0F, m.getEfficiency(), "tool speed 5");
        c.eq(1.0F, m.getAttackDamage(), "tool damage bonus 1");
        c.eq(13, m.getEnchantability(), "tool enchantability 13");
        c.eq(1, m.getHarvestLevel(), "stone's mining level");
        String dmg = SharedMonsterAttributes.ATTACK_DAMAGE.getName();
        String spd = SharedMonsterAttributes.ATTACK_SPEED.getName();
        // Player base damage 1 and speed 4: sword 5 / 1.6, pickaxe 3 / 1.2, axe 9 / 0.8, shovel 3.5 / 1.0, hoe 1 / 2.0.
        double[][] want = {{4.0, -2.4}, {2.0, -2.8}, {8.0, -3.2}, {2.5, -3.0}, {0.0, -2.0}};
        for (int i = 0; i < CopperGearContent.TOOLS.length; i++) {
            Item tool = item(CopperGearContent.TOOLS[i]);
            c.ok(Math.abs(modifier(tool, dmg) - want[i][0]) < 1.0E-4, CopperGearContent.TOOLS[i] + " damage " + (want[i][0] + 1)
                    + " got " + (modifier(tool, dmg) + 1));
            c.ok(Math.abs(modifier(tool, spd) - want[i][1]) < 1.0E-4, CopperGearContent.TOOLS[i] + " speed " + (4 + want[i][1])
                    + " got " + (4 + modifier(tool, spd)));
            c.eq(190, tool.getMaxDamage(), CopperGearContent.TOOLS[i] + " uses");
        }
        Item pick = item("copper_pickaxe");
        ItemStack p = new ItemStack(pick);
        c.ok(pick.canHarvestBlock(Blocks.IRON_ORE.getDefaultState(), p), "a copper pickaxe mines iron ore");
        c.ok(!pick.canHarvestBlock(Blocks.DIAMOND_ORE.getDefaultState(), p), "but not diamond ore");
        c.eq(5.0F, pick.getDestroySpeed(p, Blocks.STONE.getDefaultState()), "mines stone at speed 5");
        c.eq(5.0F, item("copper_axe").getDestroySpeed(stack("copper_axe"), Blocks.LOG.getDefaultState()), "axe on logs at 5");
        c.ok(item("copper_axe") instanceof ItemTool && item("copper_axe").getToolClasses(stack("copper_axe")).contains("axe"), "the axe is an axe");
        Item ingot = CopperGearContent.ingot();
        if (ingot != null) {
            c.ok(item("copper_sword").getIsRepairable(stack("copper_sword"), new ItemStack(ingot)), "a copper ingot repairs a sword");
            c.ok(item("copper_helmet").getIsRepairable(stack("copper_helmet"), new ItemStack(ingot)), "and armor");
            c.ok(!item("copper_sword").getIsRepairable(stack("copper_sword"), new ItemStack(Items.IRON_INGOT)), "iron does not");
        }
    }

    private void armor(Check c) {
        int[] defense = {2, 4, 3, 1};
        int[] durability = {121, 176, 165, 143};
        for (int i = 0; i < 4; i++) {
            ItemArmor a = (ItemArmor) item(CopperGearContent.ARMOR[i]);
            c.eq(defense[i], a.damageReduceAmount, CopperGearContent.ARMOR[i] + " defense");
            c.eq(durability[i], a.getMaxDamage(), CopperGearContent.ARMOR[i] + " durability");
            c.eq(0.0F, a.toughness, CopperGearContent.ARMOR[i] + " toughness");
        }
        c.eq(8, CopperGearContent.armorMaterial.getEnchantability(), "armor enchantability 8");
        c.eq(Content.sound("item.armor.equip_copper"), CopperGearContent.armorMaterial.getSoundEvent(), "equip sound");
        ItemStack horse = stack("copper_horse_armor");
        c.ok(HorseArmorType.isHorseArmor(horse), "copper horse armor fits a horse");
        c.eq(4, HorseArmorType.getByItemStack(horse).getProtection(), "horse armor protection 4");
        c.eq(1, horse.getMaxStackSize(), "one to a stack");
    }

    private void recipes(Check c) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack nug = stack("copper_nugget");
        ItemStack stick = new ItemStack(Items.STICK);
        Item ingotItem = CopperGearContent.ingot();
        if (c.ok(ingotItem != null, "a copper ingot exists")) {
            ItemStack i = new ItemStack(ingotItem);
            ItemStack nine = nug.copy();
            nine.setCount(9);
            Crafting.expect(c, Crafting.grid(1, 1, i), nine, "an ingot makes 9 nuggets");
            Crafting.expect(c, Crafting.grid(3, 3, nug, nug, nug, nug, nug, nug, nug, nug, nug), i, "9 nuggets make an ingot");
            Crafting.expect(c, Crafting.grid(1, 3, i, i, stick), stack("copper_sword"), "sword");
            Crafting.expect(c, Crafting.grid(3, 3, i, i, i, e, stick, e, e, stick, e), stack("copper_pickaxe"), "pickaxe");
            Crafting.expect(c, Crafting.grid(2, 3, i, i, i, stick, e, stick), stack("copper_axe"), "axe");
            Crafting.expect(c, Crafting.grid(1, 3, i, stick, stick), stack("copper_shovel"), "shovel");
            Crafting.expect(c, Crafting.grid(2, 3, i, i, e, stick, e, stick), stack("copper_hoe"), "hoe");
            Crafting.expect(c, Crafting.grid(3, 2, i, i, i, i, e, i), stack("copper_helmet"), "helmet: 5 ingots");
            Crafting.expect(c, Crafting.grid(3, 3, i, e, i, i, i, i, i, i, i), stack("copper_chestplate"), "chestplate: 8");
            Crafting.expect(c, Crafting.grid(3, 3, i, i, i, i, e, i, i, e, i), stack("copper_leggings"), "leggings: 7");
            Crafting.expect(c, Crafting.grid(3, 2, i, e, i, i, e, i), stack("copper_boots"), "boots: 4");
            ItemStack bars = stack("copper_bars");
            bars.setCount(16);
            Crafting.expect(c, Crafting.grid(3, 2, i, i, i, i, i, i), bars, "6 ingots make 16 bars");
            Crafting.expect(c, Crafting.grid(1, 3, nug, i, nug), stack("copper_chain"), "a chain: an ingot between two nuggets");
        }
        ItemStack torches = stack("copper_torch");
        torches.setCount(4);
        Crafting.expect(c, Crafting.grid(1, 3, nug, new ItemStack(Items.COAL), stick), torches, "4 torches with coal");
        Crafting.expect(c, Crafting.grid(1, 3, nug, new ItemStack(Items.COAL, 1, 1), stick), torches, "and with charcoal");
        ItemStack t = stack("copper_torch");
        Crafting.expect(c, Crafting.grid(3, 3, nug, nug, nug, nug, t, nug, nug, nug, nug), stack("copper_lantern"),
                "a lantern: 8 nuggets around a copper torch");
        OreDictionary.registerOre("honeycomb", Items.SLIME_BALL);
        Crafting.expect(c, Crafting.grid(2, 1, stack("weathered_copper_chain"), new ItemStack(Items.SLIME_BALL)),
                stack("waxed_weathered_copper_chain"), "honeycomb waxes a chain in the grid");
        Crafting.expect(c, Crafting.grid(2, 1, stack("oxidized_copper_lantern"), new ItemStack(Items.SLIME_BALL)),
                stack("waxed_oxidized_copper_lantern"), "and a lantern");
        Crafting.expect(c, Crafting.grid(2, 1, stack("copper_bars"), new ItemStack(Items.SLIME_BALL)),
                stack("waxed_copper_bars"), "and bars");
    }

    private void smelting(Check c) {
        String[] all = {"copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe", "copper_helmet",
                "copper_chestplate", "copper_leggings", "copper_boots", "copper_horse_armor"};
        for (String name : all) {
            ItemStack in = stack(name);
            if (in.isItemStackDamageable()) {
                in.setItemDamage(37);
            }
            ItemStack out = FurnaceRecipes.instance().getSmeltingResult(in);
            c.ok(!out.isEmpty() && out.getItem() == item("copper_nugget") && out.getCount() == 1, name + " (worn) smelts to a nugget, got " + out);
            c.eq(0.1F, FurnaceRecipes.instance().getSmeltingExperience(out), name + " 0.1 experience");
        }
    }

    private static void click(FakePlayer player, BlockPos pos) {
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.UP, new Vec3d(pos)));
    }

    private void blocks(Check c, BlockPos area) {
        Block torch = block("copper_torch");
        c.eq(14, torch.getDefaultState().getLightValue(), "copper torch light 14");
        c.eq(0.0F, torch.getDefaultState().getBlockHardness(c.world, area), "a torch breaks at once");
        for (int stage = 0; stage < 4; stage++) {
            for (boolean waxed : new boolean[] {false, true}) {
                Block lantern = block(CopperGearContent.name("copper_lantern", stage, waxed));
                c.eq(15, lantern.getDefaultState().getLightValue(), lantern.getRegistryName() + " light 15");
                c.eq(3.5F, lantern.getDefaultState().getBlockHardness(c.world, area), "lantern hardness 3.5");
                Block bars = block(CopperGearContent.name("copper_bars", stage, waxed));
                c.eq(5.0F, bars.getDefaultState().getBlockHardness(c.world, area), "bars hardness 5");
                c.eq(5.0F, block(CopperGearContent.name("copper_chain", stage, waxed)).getDefaultState().getBlockHardness(c.world, area),
                        "chain hardness 5");
                c.eq("pickaxe", bars.getHarvestTool(bars.getDefaultState()), "bars need a pickaxe");
            }
        }
        // Torch on a wall.
        BlockPos wall = area.add(2, 0, 2);
        c.world.setBlockState(wall, Blocks.STONE.getDefaultState());
        c.world.setBlockState(wall.east(), torch.getDefaultState().withProperty(net.minecraft.block.BlockTorch.FACING, EnumFacing.EAST));
        c.eq(torch, c.world.getBlockState(wall.east()).getBlock(), "a copper torch stands on a wall");
        // Bars connect to bars, iron bars and stone.
        BlockPos b = area.add(6, 0, 2);
        c.world.setBlockState(b, block("copper_bars").getDefaultState());
        c.world.setBlockState(b.east(), block("exposed_copper_bars").getDefaultState());
        c.world.setBlockState(b.west(), Blocks.IRON_BARS.getDefaultState());
        c.world.setBlockState(b.north(), Blocks.STONE.getDefaultState());
        IBlockState actual = c.world.getBlockState(b).getActualState(c.world, b);
        c.ok(actual.getValue(BlockPane.EAST) && actual.getValue(BlockPane.WEST) && actual.getValue(BlockPane.NORTH)
                && !actual.getValue(BlockPane.SOUTH), "copper bars join copper bars, iron bars and stone");
        // Chain axis from the face clicked.
        Block chain = block("copper_chain");
        IBlockState placed = chain.getStateForPlacement(c.world, b.up(2), EnumFacing.EAST, 0, 0, 0, 0, null);
        c.eq(EnumFacing.Axis.X, placed.getValue(CopperGearBlocks.Chain.AXIS), "a chain lies along the face clicked");
        // Lanterns: hanging under a chain, standing on stone, dropping when the support goes.
        BlockPos top = area.add(10, 4, 2);
        Block lantern = block("copper_lantern");
        c.world.setBlockState(top, chain.getDefaultState());
        IBlockState hang = lantern.getStateForPlacement(c.world, top.down(), EnumFacing.DOWN, 0, 0, 0, 0, null);
        c.ok(hang.getValue(CopperGearBlocks.Lantern.HANGING), "placed under a chain, a lantern hangs");
        c.world.setBlockState(top.down(), hang);
        IBlockState stand = lantern.getStateForPlacement(c.world, area.add(12, 0, 2), EnumFacing.UP, 0, 0, 0, 0, null);
        c.ok(!stand.getValue(CopperGearBlocks.Lantern.HANGING), "on the floor, it stands");
        c.ok(!lantern.canPlaceBlockAt(c.world, area.add(14, 3, 2)), "nothing to stand on or hang from: not placeable");
        c.world.setBlockToAir(top);
        c.eq(Blocks.AIR, c.world.getBlockState(top.down()).getBlock(), "the hanging lantern drops when its chain goes");
    }

    private void waxing(Check c, BlockPos area) {
        FakePlayer player = c.player("coppergear", area.up());
        BlockPos pos = area.add(16, 0, 2);
        c.world.setBlockState(pos, block("weathered_copper_lantern").getDefaultState().withProperty(CopperGearBlocks.Lantern.HANGING, false));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        click(player, pos);
        c.eq(block("exposed_copper_lantern"), c.world.getBlockState(pos).getBlock(), "an axe scrapes a lantern back a stage");
        c.eq(1, player.getHeldItemMainhand().getItemDamage(), "wearing the axe");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL, 2));
        click(player, pos);
        c.eq(block("waxed_exposed_copper_lantern"), c.world.getBlockState(pos).getBlock(), "honeycomb waxes it");
        c.eq(1, player.getHeldItemMainhand().getCount(), "using one");
        BlockPos chainPos = pos.east(2);
        c.world.setBlockState(chainPos, block("oxidized_copper_chain").getDefaultState().withProperty(CopperGearBlocks.Chain.AXIS, EnumFacing.Axis.Z));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        click(player, chainPos);
        IBlockState s = c.world.getBlockState(chainPos);
        c.eq(block("weathered_copper_chain"), s.getBlock(), "an axe scrapes a chain");
        c.eq(EnumFacing.Axis.Z, s.getValue(CopperGearBlocks.Chain.AXIS), "keeping its axis");
    }

    private void aging(Check c, BlockPos area) {
        c.eq(block("exposed_copper_bars"), Oxidation.next(block("copper_bars")), "bars oxidize to exposed bars");
        c.eq(block("waxed_weathered_copper_chain"), Oxidation.waxedOf(block("weathered_copper_chain")), "waxed chains");
        c.eq(-1, Oxidation.stage(block("waxed_copper_lantern")), "waxed lanterns do not age");
        c.ok(!block("oxidized_copper_bars").getTickRandomly(), "oxidized bars stop");
        if (!ModConfig.copper.oxidation) {
            return;
        }
        BlockPos pos = area.add(20, 0, 20);
        c.world.setBlockState(pos, block("copper_lantern").getDefaultState());
        Random rand = new Random(20);
        for (int i = 0; i < 3000; i++) {
            IBlockState s = c.world.getBlockState(pos);
            s.getBlock().randomTick(c.world, pos, s, rand);
        }
        c.eq(block("oxidized_copper_lantern"), c.world.getBlockState(pos).getBlock(), "a lone lantern ends oxidized after 3000 ticks");
    }

    private void mobs(Check c) {
        c.ok(MobArmorTransformer.applied, "P16 applied to EntityLiving");
        if (!ModConfig.copperGear.mobArmor) {
            return;
        }
        // Hard, long-inhabited chunk: clamped additional difficulty 1, so 15% of mobs get armor.
        DifficultyInstance hard = new DifficultyInstance(EnumDifficulty.HARD, 10000000L, 10000000L, 1.0F);
        c.eq(1.0F, hard.getClampedAdditionalDifficulty(), "test difficulty clamped to 1");
        EntityZombie z = new EntityZombie(c.world);
        int[] tiers = new int[7];
        int armored = 0;
        int headless = 0;
        for (int n = 0; n < 30000; n++) {
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                z.setItemStackToSlot(slot, ItemStack.EMPTY);
            }
            CopperGearHooks.equipArmor(z, hard);
            ItemStack head = z.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if (head.isEmpty()) {
                if (!z.getItemStackFromSlot(EntityEquipmentSlot.FEET).isEmpty()) {
                    headless++;
                }
                continue;
            }
            armored++;
            tiers[tier(head.getItem())]++;
        }
        c.eq(0, headless, "no helmet, no armor at all (the helmet comes first)");
        double share = armored / 30000.0;
        c.ok(Math.abs(share - 0.15) < 0.015, "15% armored, got " + share);
        double[] want = {0.2360, 0.3223, 0.3329, 0.0973, 0.0110, 0.0004};
        String[] names = {"leather", "copper", "gold", "chainmail", "iron", "diamond"};
        for (int t = 0; t < 4; t++) {
            double got = tiers[t] / (double) armored;
            c.ok(Math.abs(got - want[t]) < 0.035, names[t] + " " + want[t] + ", got " + got);
        }
        c.eq(0, tiers[6], "nothing but the six tiers");
        // Through the patch: zombies spawned on Hard wear copper sometimes.
        int copper = 0;
        Item helmet = CopperGearContent.ITEMS.get("copper_helmet");
        for (int n = 0; n < 3000; n++) {
            EntityZombie fresh = new EntityZombie(c.world);
            fresh.onInitialSpawn(hard, null);
            if (fresh.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == helmet) {
                copper++;
            }
        }
        c.ok(copper > 50, "zombies spawn in copper helmets (" + copper + " of 3000)");
    }

    private static int tier(Item item) {
        if (item == Items.LEATHER_HELMET) {
            return 0;
        }
        if (item == CopperGearContent.ITEMS.get("copper_helmet")) {
            return 1;
        }
        if (item == Items.GOLDEN_HELMET) {
            return 2;
        }
        if (item == Items.CHAINMAIL_HELMET) {
            return 3;
        }
        if (item == Items.IRON_HELMET) {
            return 4;
        }
        return item == Items.DIAMOND_HELMET ? 5 : 6;
    }

    private void loot(Check c) {
        if (!ModConfig.copperGear.horseArmorLoot) {
            return;
        }
        for (String name : new String[] {"chests/simple_dungeon", "chests/desert_pyramid", "chests/jungle_temple",
                "chests/nether_bridge", "chests/stronghold_corridor", "chests/end_city_treasure", "chests/village_blacksmith"}) {
            LootTable table = c.world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(name));
            boolean found = false;
            for (String pool : new String[] {"main", "pool0", "pool1", "pool2", "pool3"}) {
                LootPool p = table.getPool(pool);
                if (p != null && p.getEntry(TrickyTrialsBackport.MOD_ID + ":copper_horse_armor") != null) {
                    found = true;
                }
            }
            c.ok(found, "copper horse armor in " + name);
        }
    }
}
