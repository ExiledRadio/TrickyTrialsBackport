package com.exiledradio.trickytrialsbackport.shelf.client;

import com.exiledradio.trickytrialsbackport.shelf.BlockShelf;
import com.exiledradio.trickytrialsbackport.shelf.TileEntityShelf;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.21.9's {@code ShelfRenderer}: each item at a quarter of its size, 5/16 apart, a quarter block in front of the
 * shelf's center (its back board), centered on the shelf's middle height or standing on the bottom board when the
 * shelf aligns its items to the bottom.
 *
 * <p>1.12.2 has no {@code on_shelf} display transform; vanilla's default is no transform for flat items and a half
 * turn for block models ({@code block/block.json}'s {@code on_shelf}), which is what is drawn here.
 */
public class RenderShelf extends TileEntitySpecialRenderer<TileEntityShelf> {

    private static final float ITEM_SIZE = 0.25F;

    /** model -> {minY, maxY} of its quads, in model space (0 to 1) */
    private final Map<IBakedModel, float[]> heights = new IdentityHashMap<IBakedModel, float[]>();

    @Override
    public void render(TileEntityShelf shelf, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        NonNullList<ItemStack> items = shelf.items();
        if (shelf.isEmpty()) {
            return;
        }
        IBlockState state = shelf.getWorld().getBlockState(shelf.getPos());
        if (!(state.getBlock() instanceof BlockShelf)) {
            return;
        }
        EnumFacing facing = state.getValue(BlockShelf.FACING);
        float rotation = -facing.getHorizontalAngle();
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
        GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            IBakedModel model = renderItem.getItemModelWithOverrides(stack, shelf.getWorld(), null);
            GlStateManager.pushMatrix();
            GlStateManager.translate((i - 1) * 0.3125F, shelf.alignItemsToBottom() ? -0.25F : 0.0F, -0.25F);
            GlStateManager.scale(ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
            float[] height = height(model);
            // Model space is drawn from -0.5 to 0.5 here.
            double lift = -(height[0] - 0.5D);
            if (!shelf.alignItemsToBottom()) {
                lift -= (height[1] - height[0]) / 2.0D;
            }
            GlStateManager.translate(0.0D, lift, 0.0D);
            if (model.isGui3d()) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }
            renderItem.renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private float[] height(IBakedModel model) {
        float[] cached = this.heights.get(model);
        if (cached != null) {
            return cached;
        }
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        List<BakedQuad> quads = new ArrayList<BakedQuad>(model.getQuads(null, null, 0L));
        for (EnumFacing side : EnumFacing.values()) {
            quads.addAll(model.getQuads(null, side, 0L));
        }
        for (BakedQuad quad : quads) {
            VertexFormat format = quad.getFormat();
            int stride = format.getIntegerSize();
            int[] data = quad.getVertexData();
            // Position is the first element of every item format.
            if (format.getElementCount() == 0 || !format.getElement(0).isPositionElement()) {
                continue;
            }
            for (int v = 0; v < 4 && v * stride + 1 < data.length; v++) {
                float vy = Float.intBitsToFloat(data[v * stride + 1]);
                min = Math.min(min, vy);
                max = Math.max(max, vy);
            }
        }
        float[] out = min <= max ? new float[] {min, max} : new float[] {0.0F, 1.0F};
        this.heights.put(model, out);
        return out;
    }
}
