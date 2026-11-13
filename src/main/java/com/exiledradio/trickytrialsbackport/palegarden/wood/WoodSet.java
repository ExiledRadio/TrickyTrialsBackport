package com.exiledradio.trickytrialsbackport.palegarden.wood;

import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every block of one wood, registered the same way for cherry and bamboo.
 *
 * <h2>Two shapes</h2>
 * A tree wood ({@link #tree}) has a log, wood (all bark) and their stripped forms. Bamboo
 * ({@link #bamboo}) has a block of bamboo and a block of stripped bamboo in their place, no wood, and
 * a second set of planks, the mosaic, with its own stairs and slab. Everything else is the same.
 *
 * <h2>Numbers</h2>
 * 1.20's recipes (read from the 1.20.6 jar's recipe files), fuel times ({@code AbstractFurnaceBlockEntity}:
 * 300 for wooden blocks, 150 slabs, 200 doors and signs, 100 buttons), flammability (5/20 for planks,
 * stairs, slabs, fences and gates; 5/5 logs) and vanilla wood's ore names, so other mods' wood recipes
 * take these planks and logs.
 */
public final class WoodSet {

    public final WoodKind kind;
    private final boolean bambooShape;

    public Block log;
    @Nullable
    public Block wood;
    public Block strippedLog;
    @Nullable
    public Block strippedWood;
    public Block planks;
    @Nullable
    public Block mosaic;
    public Block stairs;
    @Nullable
    public Block mosaicStairs;
    public WoodBlocks.Slab.Half slab;
    public WoodBlocks.Slab.Double doubleSlab;
    @Nullable
    public WoodBlocks.Slab.Half mosaicSlab;
    @Nullable
    public WoodBlocks.Slab.Double mosaicDoubleSlab;
    public Block fence;
    public Block fenceGate;
    public WoodBlocks.Door door;
    public Block trapdoor;
    public Block pressurePlate;
    public Block button;
    public WoodSign.Standing sign;
    public WoodSign.Wall wallSign;

    public Item doorItem;
    public Item signItem;

    private WoodSet(WoodKind kind, boolean bambooShape) {
        this.kind = kind;
        this.bambooShape = bambooShape;
    }

    public static WoodSet tree(WoodKind kind) {
        return new WoodSet(kind, false);
    }

    public static WoodSet bamboo(WoodKind kind) {
        return new WoodSet(kind, true);
    }

    private String n(String suffix) {
        return this.kind.name + "_" + suffix;
    }

    // ---------------------------------------------------------------------------------------------

    public void registerBlocks(IForgeRegistry<Block> r) {
        String k = this.kind.name;
        SoundType sound = this.kind.sound;
        if (this.bambooShape) {
            this.log = Content.block(r, k + "_block", new WoodBlocks.Log(this.kind, this.kind.planksColor, this.kind.barkColor, sound), "axe");
            this.strippedLog = Content.block(r, "stripped_" + k + "_block", new WoodBlocks.Log(this.kind, this.kind.planksColor, this.kind.planksColor, sound), "axe");
        } else {
            this.log = Content.block(r, n("log"), new WoodBlocks.Log(this.kind, this.kind.planksColor, this.kind.barkColor, sound), "axe");
            this.wood = Content.block(r, n("wood"), new WoodBlocks.Log(this.kind, this.kind.barkColor, this.kind.barkColor, sound), "axe");
            this.strippedLog = Content.block(r, "stripped_" + n("log"), new WoodBlocks.Log(this.kind, this.kind.planksColor, this.kind.planksColor, sound), "axe");
            this.strippedWood = Content.block(r, "stripped_" + n("wood"), new WoodBlocks.Log(this.kind, this.kind.planksColor, this.kind.planksColor, sound), "axe");
        }
        this.planks = Content.block(r, n("planks"), new WoodBlocks.Planks(this.kind), "axe");
        IBlockState planksState = this.planks.getDefaultState();
        this.stairs = Content.block(r, n("stairs"), new WoodBlocks.Stairs(this.kind, planksState), "axe");
        this.slab = Content.block(r, n("slab"), new WoodBlocks.Slab.Half(this.kind), "axe");
        this.doubleSlab = Content.block(r, n("double_slab"), new WoodBlocks.Slab.Double(this.kind, this.slab), "axe");
        if (this.bambooShape) {
            this.mosaic = Content.block(r, k + "_mosaic", new WoodBlocks.Planks(this.kind), "axe");
            this.mosaicStairs = Content.block(r, k + "_mosaic_stairs", new WoodBlocks.Stairs(this.kind, this.mosaic.getDefaultState()), "axe");
            this.mosaicSlab = Content.block(r, k + "_mosaic_slab", new WoodBlocks.Slab.Half(this.kind), "axe");
            this.mosaicDoubleSlab = Content.block(r, k + "_mosaic_double_slab", new WoodBlocks.Slab.Double(this.kind, this.mosaicSlab), "axe");
        }
        this.fence = Content.block(r, n("fence"), new WoodBlocks.Fence(this.kind), "axe");
        Content.multipart(this.fence);
        this.fenceGate = Content.block(r, n("fence_gate"), new WoodBlocks.FenceGate(this.kind), "axe");
        this.door = Content.block(r, n("door"), new WoodBlocks.Door(this.kind), "axe");
        this.trapdoor = Content.block(r, n("trapdoor"), new WoodBlocks.TrapDoor(this.kind), "axe");
        this.pressurePlate = Content.block(r, n("pressure_plate"), new WoodBlocks.PressurePlate(this.kind), "axe");
        this.button = Content.block(r, n("button"), new WoodBlocks.Button(this.kind), "axe");
        this.sign = Content.block(r, n("sign"), new WoodSign.Standing(this.kind), "axe");
        this.wallSign = Content.block(r, n("wall_sign"), new WoodSign.Wall(this.kind), "axe");
        WoodSign.registerTile();
        // A sign's translation key is its item's; the wall form is never in an inventory.
        this.sign.setCreativeTab(null);
        this.wallSign.setCreativeTab(null);
        this.doubleSlab.setCreativeTab(null);
        if (this.mosaicDoubleSlab != null) {
            this.mosaicDoubleSlab.setCreativeTab(null);
        }
        this.door.setCreativeTab(null);

        fire(this.log, 5, 5);
        fire(this.strippedLog, 5, 5);
        fire(this.wood, 5, 5);
        fire(this.strippedWood, 5, 5);
        for (Block b : new Block[] {this.planks, this.mosaic, this.stairs, this.mosaicStairs, this.slab, this.doubleSlab,
                this.mosaicSlab, this.mosaicDoubleSlab, this.fence, this.fenceGate}) {
            fire(b, 5, 20);
        }
    }

    private static void fire(@Nullable Block block, int encouragement, int flammability) {
        if (block != null) {
            Blocks.FIRE.setFireInfo(block, encouragement, flammability);
        }
    }

    public void registerItems(IForgeRegistry<Item> r) {
        for (Block b : new Block[] {this.log, this.wood, this.strippedLog, this.strippedWood, this.planks, this.mosaic,
                this.stairs, this.mosaicStairs, this.fence, this.fenceGate, this.trapdoor}) {
            if (b != null) {
                Content.blockItem(r, b, new Fuel(b, 300));
            }
        }
        Content.blockItem(r, this.slab, new FuelSlab(this.slab, this.doubleSlab, 150));
        if (this.mosaicSlab != null) {
            Content.blockItem(r, this.mosaicSlab, new FuelSlab(this.mosaicSlab, this.mosaicDoubleSlab, 150));
        }
        Content.blockItem(r, this.pressurePlate, new Fuel(this.pressurePlate, 300));
        Content.blockItem(r, this.button, new Fuel(this.button, 100));

        this.doorItem = Content.item(r, n("door"), new FuelDoor(this.door, 200));
        this.door.setItem(this.doorItem);
        this.signItem = Content.item(r, n("sign"), new WoodSign.SignItem(this.sign, this.wallSign, 200));

        ore("logWood", this.log, this.wood, this.strippedLog, this.strippedWood);
        ore("plankWood", this.planks, this.mosaic);
        ore("slabWood", this.slab, this.mosaicSlab);
        ore("stairWood", this.stairs, this.mosaicStairs);
        ore("fenceWood", this.fence);
        ore("fenceGateWood", this.fenceGate);
    }

    private static void ore(String name, Block... blocks) {
        for (Block b : blocks) {
            if (b != null) {
                OreDictionary.registerOre(name, new ItemStack(b));
            }
        }
    }

    /**
     * 1.20's recipes for this wood. {@code logs} is what makes planks: for a tree, the wood's four log
     * blocks; for bamboo, its two blocks (and only two planks each).
     */
    public void registerRecipes(IForgeRegistry<IRecipe> r) {
        VanillaWoodRecipes.narrowToOak(r);
        Block[] logs = this.bambooShape ? new Block[] {this.log, this.strippedLog}
                : new Block[] {this.log, this.wood, this.strippedLog, this.strippedWood};
        for (Block log : logs) {
            shapeless(r, n("planks_from_") + log.getRegistryName().getPath(),
                    new ItemStack(this.planks, this.bambooShape ? 2 : 4), log);
        }
        if (!this.bambooShape) {
            shaped(r, n("wood"), new ItemStack(this.wood, 3), "##", "##", '#', this.log);
            shaped(r, "stripped_" + n("wood"), new ItemStack(this.strippedWood, 3), "##", "##", '#', this.strippedLog);
        }
        shaped(r, n("stairs"), new ItemStack(this.stairs, 4), "#  ", "## ", "###", '#', this.planks);
        shaped(r, n("slab"), new ItemStack(this.slab, 6), "###", '#', this.planks);
        if (this.bambooShape) {
            shaped(r, this.kind.name + "_mosaic", new ItemStack(this.mosaic), "#", "#", '#', this.slab);
            shaped(r, this.kind.name + "_mosaic_stairs", new ItemStack(this.mosaicStairs, 4), "#  ", "## ", "###", '#', this.mosaic);
            shaped(r, this.kind.name + "_mosaic_slab", new ItemStack(this.mosaicSlab, 6), "###", '#', this.mosaic);
        }
        shaped(r, n("fence"), new ItemStack(this.fence, 3), "W#W", "W#W", 'W', this.planks, '#', "stickWood");
        shaped(r, n("fence_gate"), new ItemStack(this.fenceGate), "#W#", "#W#", 'W', this.planks, '#', "stickWood");
        shaped(r, n("door"), new ItemStack(this.doorItem, 3), "##", "##", "##", '#', this.planks);
        shaped(r, n("trapdoor"), new ItemStack(this.trapdoor, 2), "###", "###", '#', this.planks);
        shaped(r, n("pressure_plate"), new ItemStack(this.pressurePlate), "##", '#', this.planks);
        shapeless(r, n("button"), new ItemStack(this.button), this.planks);
        shaped(r, n("sign"), new ItemStack(this.signItem, 3), "###", "###", " X ", '#', this.planks, 'X', "stickWood");
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        r.register(new ShapedOreRecipe(id, out, recipe).setRegistryName(id));
    }

    private static void shapeless(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        r.register(new ShapelessOreRecipe(id, out, recipe).setRegistryName(id));
    }

    /** @return log to stripped log, for this wood's axe stripping */
    public Map<Block, Block> stripping() {
        Map<Block, Block> map = new LinkedHashMap<Block, Block>();
        map.put(this.log, this.strippedLog);
        if (this.wood != null) {
            map.put(this.wood, this.strippedWood);
        }
        return map;
    }

    /** @return every block, for the stream's ownership declaration and tests */
    public Block[] blocks() {
        java.util.List<Block> all = new java.util.ArrayList<Block>();
        for (Block b : new Block[] {this.log, this.wood, this.strippedLog, this.strippedWood, this.planks, this.mosaic,
                this.stairs, this.mosaicStairs, this.slab, this.doubleSlab, this.mosaicSlab, this.mosaicDoubleSlab,
                this.fence, this.fenceGate, this.door, this.trapdoor, this.pressurePlate, this.button, this.sign,
                this.wallSign}) {
            if (b != null) {
                all.add(b);
            }
        }
        return all.toArray(new Block[0]);
    }

    // ---------------------------------------------------------------------------------------------
    // Items that burn for 1.20's times.

    static final class Fuel extends ItemBlock {
        private final int burn;

        Fuel(Block block, int burn) {
            super(block);
            this.burn = burn;
        }

        @Override
        public int getItemBurnTime(ItemStack stack) {
            return this.burn;
        }
    }

    static final class FuelSlab extends ItemSlab {
        private final int burn;

        FuelSlab(WoodBlocks.Slab.Half half, WoodBlocks.Slab.Double full, int burn) {
            super(half, half, full);
            this.burn = burn;
        }

        @Override
        public int getItemBurnTime(ItemStack stack) {
            return this.burn;
        }
    }

    static final class FuelDoor extends ItemDoor {
        private final int burn;

        FuelDoor(Block door, int burn) {
            super(door);
            this.burn = burn;
        }

        @Override
        public int getItemBurnTime(ItemStack stack) {
            return this.burn;
        }
    }
}
