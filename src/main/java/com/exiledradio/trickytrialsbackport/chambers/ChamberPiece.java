package com.exiledradio.trickytrialsbackport.chambers;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPiece;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.structure.TemplateBlock;
import com.exiledradio.backportcore.structure.TemplateProcessor;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.google.gson.JsonElement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.TemplateManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * A piece of a trial chamber: Backport Core's jigsaw piece, placed as 1.21's {@code SinglePoolElement} places it
 * ({@code getSettings}): the element's processor list, then the list's {@code protected_blocks}
 * ({@code #features_cannot_replace}), which Core does not read; {@code ignore_waterlogging}, so a block placed in an
 * aquifer stays dry; no air skipped. As 1.21's {@code PoolElementStructurePiece} it tells the terrain how it meets
 * the ground ({@link TerrainAdaptation.AdaptingPiece}).
 */
public class ChamberPiece extends JigsawPiece implements TerrainAdaptation.AdaptingPiece {

    /** 1.21's {@code #minecraft:features_cannot_replace}, by today's name; looked up as blocks when first needed. */
    static final String[] CANNOT_REPLACE = {"minecraft:bedrock", "minecraft:mob_spawner", "minecraft:chest", "minecraft:end_portal_frame",
            "minecraft:reinforced_deepslate", "trickytrialsbackport:trial_spawner", "trickytrialsbackport:vault"};
    private static Set<Block> cannotReplace;
    /** The one chamber processor list, whose last processor is {@code protected_blocks}. */
    static final String PROTECTING_LIST = "minecraft:trial_chambers_copper_bulb_degradation";

    /** 1.21's {@code JigsawJunction}s: source x, source ground y, source z, three ints each, in the order added. */
    private int[] junctions = new int[0];

    /** For loading. */
    public ChamberPiece() {
    }

    public ChamberPiece(JigsawElement element, BlockPos position, int groundLevelDelta, Rotation rotation, StructureBoundingBox box) {
        super(element, position, groundLevelDelta, rotation, box);
    }

    void setPivot(BlockPos pivot) {
        this.pivot = pivot;
    }

    /** {@code PoolElementStructurePiece.addJunction}. */
    void addJunction(int x, int groundY, int z) {
        int n = this.junctions.length;
        this.junctions = Arrays.copyOf(this.junctions, n + 3);
        this.junctions[n] = x;
        this.junctions[n + 1] = groundY;
        this.junctions[n + 2] = z;
    }

    @Override
    public boolean rigid() {
        return this.element == null || this.element.rigid();
    }

    @Override
    public int[] junctions() {
        return this.junctions.clone();
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        this.pivot = this.pivot.add(x, y, z);
        for (int n = 0; n < this.junctions.length; n += 3) {
            this.junctions[n] += x;
            this.junctions[n + 1] += y;
            this.junctions[n + 2] += z;
        }
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        super.writeStructureToNBT(tag);
        tag.setIntArray("junctions", this.junctions);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager manager) {
        super.readStructureFromNBT(tag, manager);
        int[] j = tag.getIntArray("junctions");
        this.junctions = j.length % 3 == 0 ? j : new int[0];
    }

    /** @return the template's name as the pool gives it, or null for an element without one */
    public ResourceLocation template() {
        return this.element == null ? null : this.element.template();
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        if (this.element == null || this.element.template() == null) {
            return true;
        }
        ModernTemplate template = ModernTemplate.load(JigsawPools.templateId(this.element.template()));
        TemplatePlacement placement = TemplatePlacement.of(this.pieceRotation, Mirror.NONE).clip(chunk).finalizeEntities(true)
                .keepWater(false);
        for (TemplateProcessor p : this.element.processors().build(this.pivot)) {
            placement.processor(p);
        }
        if (protects()) {
            placement.processor(ChamberPiece::protectedBlocks);
        }
        template.place(world, this.position, placement);
        return true;
    }

    /** 1.21's {@code protected_blocks} processor: what stands in the world there is never replaced. */
    static TemplateBlock protectedBlocks(World world, BlockPos pos, TemplateBlock block, Random random) {
        return world.isBlockLoaded(pos) && cannotReplace().contains(world.getBlockState(pos).getBlock()) ? null : block;
    }

    /** @return true for an element whose processor list is the one that ends in {@code protected_blocks} */
    private boolean protects() {
        JsonElement p = this.element.json() == null ? null : this.element.json().get("processors");
        return p != null && p.isJsonPrimitive() && PROTECTING_LIST.equals(ChamberSettings.full(p.getAsString()));
    }

    static synchronized Set<Block> cannotReplace() {
        if (cannotReplace == null) {
            Set<Block> set = new HashSet<Block>();
            for (String name : CANNOT_REPLACE) {
                Block b = Block.getBlockFromName(name);
                if (b != null && b != Blocks.AIR) {
                    set.add(b);
                }
            }
            cannotReplace = set;
        }
        return cannotReplace;
    }
}
