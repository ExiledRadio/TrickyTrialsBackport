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
import java.util.WeakHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.21.9's {@code ShelfRenderer}: each item at a quarter of its size, 5/16 apart, a quarter block in front of the
 * shelf's center (its back board), centered on the shelf's middle height or standing on the bottom board when the
 * shelf aligns its items to the bottom.
 *
 * <p>1.12.2 has no {@code on_shelf} display transform: {@link OnShelfTransform} carries 1.21.11's, and the item is
 * lifted by its transformed extents as vanilla's {@code getModelBoundingBox} does.
 */
public class RenderShelf extends TileEntitySpecialRenderer<TileEntityShelf> {

    private static final float ITEM_SIZE = 0.25F;

    /** model -> {minY, maxY} of its quads after its on_shelf transform; weak, so reloaded models go */
    private final Map<IBakedModel, float[]> heights = new WeakHashMap<IBakedModel, float[]>();

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
            OnShelfTransform transform = OnShelfTransform.of(stack, model);
            GlStateManager.pushMatrix();
            GlStateManager.translate((i - 1) * 0.3125F, shelf.alignItemsToBottom() ? -0.25F : 0.0F, -0.25F);
            GlStateManager.scale(ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
            float[] height = height(stack, model, transform);
            double lift = -height[0];
            if (!shelf.alignItemsToBottom()) {
                lift -= (height[1] - height[0]) / 2.0D;
            }
            GlStateManager.translate(0.0D, lift, 0.0D);
            transform.apply();
            renderItem.renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /**
     * Vanilla's {@code getModelBoundingBox}: the lowest and highest point of the item's extents after its
     * {@code on_shelf} transform (quads for a baked model, the drawn shape for a special-rendered one).
     */
    private float[] height(ItemStack stack, IBakedModel model, OnShelfTransform transform) {
        float[] special = OnShelfTransform.specialExtents(stack);
        if (special != null || model.isBuiltInRenderer()) {
            return boxHeight(special != null ? special : OnShelfTransform.UNIT, transform);
        }
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
            for (int v = 0; v < 4 && v * stride + 2 < data.length; v++) {
                float y = transform.transformedY(Float.intBitsToFloat(data[v * stride]), Float.intBitsToFloat(data[v * stride + 1]),
                        Float.intBitsToFloat(data[v * stride + 2]));
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        float[] out = min <= max ? new float[] {min, max} : boxHeight(OnShelfTransform.UNIT, transform);
        this.heights.put(model, out);
        return out;
    }

    private static float[] boxHeight(float[] box, OnShelfTransform transform) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            float y = transform.transformedY(box[(corner & 1) == 0 ? 0 : 3], box[(corner & 2) == 0 ? 1 : 4], box[(corner & 4) == 0 ? 2 : 5]);
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return new float[] {min, max};
    }
}
