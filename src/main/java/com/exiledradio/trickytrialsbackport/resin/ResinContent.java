package com.exiledradio.trickytrialsbackport.resin;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.common.building.Shapes;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Stream 14: 1.21.4's resin.
 *
 * <ul>
 *   <li>Resin clump ({@link BlockResinClump}, four blocks behind one item), block of resin, resin bricks with
 *       stairs, slab and wall, chiseled resin bricks, and the resin brick item.</li>
 *   <li>Two sound types as 1.21.4: resin (clump and block) and resin bricks (the bricks, their shapes, chiseled).</li>
 *   <li>Recipes and smelting as 1.21.11 (a 1.14 stonecutter derives its cuts from these recipes by itself).</li>
 *   <li>Resin from a hurt creaking: {@link ResinSpread}, called by the creaking by name.</li>
 * </ul>
 * Everything is registered at normal event priority, so another mod's generic recipes never shadow ours.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class ResinContent {

    public static final BlockResinClump[] clumps = new BlockResinClump[4];
    public static Block resinBlock;
    public static Block resinBricks;
    public static Block chiseledResinBricks;
    public static Shapes brickShapes;
    public static Item resinBrick;
    /** Resin's sound type (the clump, the block, and the resin a hurt creaking spreads). */
    public static SoundType resinSound;

    private ResinContent() {
    }

    public static boolean enabled() {
        return ModConfig.resin.enabled;
    }

    /** Pre-init: this mod ships these first (1.21.4), so it owns them in Backport Core's table. */
    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrickyTrialsBackport.MOD_ID, "resin_clump", "resin_clump_down", "resin_clump_up",
                    "resin_clump_up_down", "resin_block", "resin_bricks", "resin_brick_stairs", "resin_brick_slab",
                    "resin_brick_double_slab", "resin_brick_wall", "chiseled_resin_bricks");
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();
        // 1.21.4's SoundType.RESIN has no hit sound; nothing with it takes long enough to break to play one.
        resinSound = new SoundType(1.0F, 1.0F, Content.sound("block.resin.break"), Content.sound("block.resin.step"),
                Content.sound("block.resin.place"), Content.sound("block.resin.step"), Content.sound("block.resin.fall"));
        SoundType brickSound = Content.soundType("resin_bricks", 1.0F, 1.0F);

        for (int i = 0; i < 4; i++) {
            clumps[i] = Content.block(registry, BlockResinClump.NAMES[i], new BlockResinClump(i, resinSound), null);
            Content.multipartFile(clumps[i]);
            if (i > 0) {
                clumps[i].setCreativeTab(null);
            }
        }
        resinBlock = Content.block(registry, "resin_block", new BlockResin(resinSound), null);
        resinBricks = Content.block(registry, "resin_bricks", new BlockResinBricks(brickSound), "pickaxe");
        brickShapes = Shapes.register(registry, "resin_brick", resinBricks, true, true, true);
        chiseledResinBricks = Content.block(registry, "chiseled_resin_bricks", new BlockResinBricks(brickSound), "pickaxe");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        Content.blockItem(registry, clumps[0], new ItemResinClump(clumps[0]));
        Content.registerBlockItem(registry, resinBlock);
        Content.registerBlockItem(registry, resinBricks);
        brickShapes.registerItems(registry);
        Content.registerBlockItem(registry, chiseledResinBricks);
        resinBrick = Content.item(registry, "resin_brick", new Item());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        Item clump = Item.getItemFromBlock(clumps[0]);
        shaped(registry, "resin_block", new ItemStack(resinBlock), "###", "###", "###", '#', clump);
        ShapelessOreRecipe back = new ShapelessOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "resin"),
                new ItemStack(clump, 9), resinBlock);
        back.setRegistryName(Content.id("resin_clump"));
        registry.register(back);
        shaped(registry, "resin_bricks", new ItemStack(resinBricks), "##", "##", '#', resinBrick);
        shaped(registry, "resin_brick_stairs", new ItemStack(brickShapes.stairs, 4), "#  ", "## ", "###", '#', resinBricks);
        shaped(registry, "resin_brick_slab", new ItemStack(brickShapes.slab, 6), "###", '#', resinBricks);
        shaped(registry, "resin_brick_wall", new ItemStack(brickShapes.wall, 6), "###", "###", '#', resinBricks);
        shaped(registry, "chiseled_resin_bricks", new ItemStack(chiseledResinBricks), "#", "#", '#', brickShapes.slab);
        // recipe/resin_brick: smelting, 200 ticks (1.12.2's only furnace time), 0.1 experience.
        GameRegistry.addSmelting(clump, new ItemStack(resinBrick), 0.1F);
    }

    private static void shaped(IForgeRegistry<IRecipe> registry, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe r = new ShapedOreRecipe(new ResourceLocation(TrickyTrialsBackport.MOD_ID, "resin"), out, recipe);
        r.setRegistryName(Content.id(name));
        registry.register(r);
    }

    /**
     * The block of resin: 1.21.4's {@code Properties.of()} with resin's sound, so it breaks at once and drops
     * itself with any tool. Rock, for the note block's bass drum (its {@code BASEDRUM} instrument).
     */
    public static final class BlockResin extends Block {
        public BlockResin(SoundType sound) {
            super(Material.ROCK, MapColor.ORANGE_STAINED_HARDENED_CLAY);
            setHardness(0.0F);
            setResistance(0.0F);
            setSoundType(sound);
        }

        @Override
        public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
            return true;
        }
    }

    /**
     * Resin bricks and chiseled resin bricks: rock, needs a pickaxe, hardness 1.5, blast resistance 6, orange
     * terracotta on maps.
     */
    public static final class BlockResinBricks extends Block {
        public BlockResinBricks(SoundType sound) {
            super(Material.ROCK, MapColor.ORANGE_STAINED_HARDENED_CLAY);
            setHardness(1.5F);
            // 1.12.2 stores resistance tripled and reports it fifthed: 10 here is a modern 6.
            setResistance(10.0F);
            setSoundType(sound);
            setHarvestLevel("pickaxe", 0);
        }
    }
}
