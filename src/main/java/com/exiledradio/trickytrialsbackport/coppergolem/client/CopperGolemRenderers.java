package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperGolemStatue;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperChest;
import com.exiledradio.trickytrialsbackport.coppergolem.TileEntityCopperGolemStatue;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * The copper chest's and statue's renderers (1.21.11's {@code ChestRenderer} with the copper materials, and
 * {@code CopperGolemStatueBlockRenderer}), and their items' ({@code ChestSpecialRenderer},
 * {@code CopperGolemStatueSpecialRenderer}).
 */
public final class CopperGolemRenderers {

    static final String[] CHEST_TEXTURES = {"copper", "copper_exposed", "copper_weathered", "copper_oxidized"};

    private CopperGolemRenderers() {
    }

    static ResourceLocation chestTexture(int stage, ModelCopperChest.Kind kind) {
        String suffix = kind == ModelCopperChest.Kind.LEFT ? "_left" : kind == ModelCopperChest.Kind.RIGHT ? "_right" : "";
        return new ResourceLocation(TrickyTrialsBackport.MOD_ID, "textures/entity/chest/" + CHEST_TEXTURES[stage] + suffix + ".png");
    }

    private static void destroyTexture(boolean begin) {
        GlStateManager.matrixMode(5890);
        if (begin) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
        } else {
            GlStateManager.popMatrix();
        }
        GlStateManager.matrixMode(5888);
    }

    /** 1.21's lid easing: {@code 1 - (1 - open)^3}. */
    static float ease(float open) {
        float f = 1.0F - open;
        return 1.0F - f * f * f;
    }

    /** Each half draws itself (1.21's left and right models); a double chest opens as far as its more open half. */
    public static final class Chest extends TileEntitySpecialRenderer<TileEntityCopperChest> {
        private final Map<ModelCopperChest.Kind, ModelCopperChest> models = new EnumMap<ModelCopperChest.Kind, ModelCopperChest>(ModelCopperChest.Kind.class);

        public Chest() {
            for (ModelCopperChest.Kind k : ModelCopperChest.Kind.values()) {
                this.models.put(k, new ModelCopperChest(k));
            }
        }

        @Override
        public void render(TileEntityCopperChest te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            EnumFacing facing = EnumFacing.SOUTH;
            int stage = 0;
            ModelCopperChest.Kind kind = ModelCopperChest.Kind.SINGLE;
            float open = te.prevLidAngle + (te.lidAngle - te.prevLidAngle) * partialTicks;
            if (te.hasWorld()) {
                IBlockState state = te.getWorld().getBlockState(te.getPos());
                Block block = state.getBlock();
                if (!(block instanceof BlockCopperChest)) {
                    return;
                }
                stage = ((BlockCopperChest) block).stage;
                facing = state.getValue(BlockChest.FACING);
                te.checkForAdjacentChests();
                TileEntityChest other = null;
                EnumFacing side = null;
                if (te.adjacentChestXNeg != null) {
                    other = te.adjacentChestXNeg;
                    side = EnumFacing.WEST;
                } else if (te.adjacentChestXPos != null) {
                    other = te.adjacentChestXPos;
                    side = EnumFacing.EAST;
                } else if (te.adjacentChestZNeg != null) {
                    other = te.adjacentChestZNeg;
                    side = EnumFacing.NORTH;
                } else if (te.adjacentChestZPos != null) {
                    other = te.adjacentChestZPos;
                    side = EnumFacing.SOUTH;
                }
                if (side != null) {
                    // 1.21: the left half has its partner clockwise of its facing, the right half counterclockwise.
                    kind = side == facing.rotateY() ? ModelCopperChest.Kind.LEFT
                            : side == facing.rotateYCCW() ? ModelCopperChest.Kind.RIGHT : ModelCopperChest.Kind.SINGLE;
                    open = Math.max(open, other.prevLidAngle + (other.lidAngle - other.prevLidAngle) * partialTicks);
                }
            }
            if (destroyStage >= 0) {
                bindTexture(DESTROY_STAGES[destroyStage]);
                destroyTexture(true);
            } else {
                bindTexture(chestTexture(stage, kind));
            }
            draw(this.models.get(kind), facing, ease(open), x, y, z);
            if (destroyStage >= 0) {
                destroyTexture(false);
            }
        }
    }

    static void draw(ModelCopperChest model, EnumFacing facing, float open, double x, double y, double z) {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(-facing.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        GlStateManager.disableCull();
        model.render(open, 0.0625F);
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The statue: its pose's model in its stage's texture, facing its way. */
    public static final class Statue extends TileEntitySpecialRenderer<TileEntityCopperGolemStatue> {
        private final Map<BlockCopperGolemStatue.Pose, ModelCopperGolemStatue> models = new EnumMap<BlockCopperGolemStatue.Pose, ModelCopperGolemStatue>(
                BlockCopperGolemStatue.Pose.class);

        public Statue() {
            for (BlockCopperGolemStatue.Pose p : BlockCopperGolemStatue.Pose.values()) {
                this.models.put(p, new ModelCopperGolemStatue(p));
            }
        }

        ModelCopperGolemStatue model(BlockCopperGolemStatue.Pose pose) {
            return this.models.get(pose);
        }

        @Override
        public void render(TileEntityCopperGolemStatue te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            if (!te.hasWorld()) {
                return;
            }
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            if (!(state.getBlock() instanceof BlockCopperGolemStatue)) {
                return;
            }
            int stage = ((BlockCopperGolemStatue) state.getBlock()).stage;
            if (destroyStage >= 0) {
                bindTexture(DESTROY_STAGES[destroyStage]);
                destroyTexture(true);
            } else {
                bindTexture(RenderCopperGolem.TEXTURES[stage]);
            }
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.translate(x + 0.5D, y, z + 0.5D);
            GlStateManager.disableCull();
            this.models.get(state.getValue(BlockCopperGolemStatue.POSE))
                    .render(state.getValue(BlockCopperGolemStatue.FACING).getOpposite().getHorizontalAngle(), true, 0.0625F);
            GlStateManager.enableCull();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
            if (destroyStage >= 0) {
                destroyTexture(false);
            }
        }
    }

    /** The items: a closed single chest facing south, and the statue in its item's pose. */
    public static final class Items extends TileEntityItemStackRenderer {
        private final Map<ModelCopperChest.Kind, ModelCopperChest> chest = new EnumMap<ModelCopperChest.Kind, ModelCopperChest>(ModelCopperChest.Kind.class);
        private final Map<BlockCopperGolemStatue.Pose, ModelCopperGolemStatue> statues = new EnumMap<BlockCopperGolemStatue.Pose, ModelCopperGolemStatue>(
                BlockCopperGolemStatue.Pose.class);

        public Items() {
            this.chest.put(ModelCopperChest.Kind.SINGLE, new ModelCopperChest(ModelCopperChest.Kind.SINGLE));
            for (BlockCopperGolemStatue.Pose p : BlockCopperGolemStatue.Pose.values()) {
                this.statues.put(p, new ModelCopperGolemStatue(p));
            }
        }

        @Override
        public void renderByItem(ItemStack stack, float partialTicks) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block instanceof BlockCopperChest) {
                int stage = ((BlockCopperChest) block).stage;
                Minecraft.getMinecraft().getTextureManager().bindTexture(chestTexture(stage, ModelCopperChest.Kind.SINGLE));
                drawItem(this.chest.get(ModelCopperChest.Kind.SINGLE));
            } else if (block instanceof BlockCopperGolemStatue) {
                int stage = ((BlockCopperGolemStatue) block).stage;
                Minecraft.getMinecraft().getTextureManager().bindTexture(RenderCopperGolem.TEXTURES[stage]);
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.5F, 1.5F, 0.5F);
                GlStateManager.scale(-1.0F, -1.0F, 1.0F);
                GlStateManager.disableCull();
                this.statues.get(BlockCopperGolemStatue.Pose.byId(stack.getMetadata())).render(EnumFacing.NORTH.getHorizontalAngle(), true, 0.0625F);
                GlStateManager.enableCull();
                GlStateManager.popMatrix();
            }
        }

        private static void drawItem(ModelCopperChest model) {
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableCull();
            model.render(0.0F, 0.0625F);
            GlStateManager.enableCull();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
    }
}
