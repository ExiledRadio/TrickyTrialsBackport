package com.exiledradio.trickytrialsbackport.shelf.client;

import com.exiledradio.trickytrialsbackport.shelf.BlockShelf;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockWall;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Items;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;

/**
 * 1.21.11's {@code on_shelf} display transforms ({@code ItemDisplayContext.ON_SHELF}), which 1.12.2's models do not
 * have, chosen by what the item is:
 *
 * <ul>
 *   <li>none for flat items and for special-rendered ones without one (chests, banners, shulker boxes);</li>
 *   <li>a half turn for block models ({@code block/block.json}); a quarter turn for fences, walls and anvils
 *       ({@code fence_inventory}, {@code wall_inventory}, {@code template_anvil}); a half turn and 4 pixels back for
 *       shelves ({@code template_shelf_inventory});</li>
 *   <li>heads ({@code template_skull}, {@code dragon_head}), the shield and beds ({@code template_bed}) with their
 *       own.</li>
 * </ul>
 * Applied as vanilla's {@code ItemTransform.apply}: translate (pixels), rotate X then Y then Z, scale, then the
 * model's half-block offset (which 1.12.2's {@code renderItem} adds itself).
 */
final class OnShelfTransform {

    static final OnShelfTransform NONE = new OnShelfTransform(0, 0, 0, 0, 0, 0, 1);
    private static final OnShelfTransform BLOCK = new OnShelfTransform(0, 180, 0, 0, 0, 0, 1);
    private static final OnShelfTransform QUARTER = new OnShelfTransform(0, 90, 0, 0, 0, 0, 1);
    private static final OnShelfTransform SHELF = new OnShelfTransform(0, 180, 0, 0, 0, 4, 1);
    private static final OnShelfTransform SKULL = new OnShelfTransform(0, 0, 0, 0, 8, 0, 2);
    private static final OnShelfTransform DRAGON_HEAD = new OnShelfTransform(0, 0, 0, 0, 2, 0, 1.25F);
    private static final OnShelfTransform SHIELD = new OnShelfTransform(0, 0, 0, 11, 18.5F, 8.7F, 1.4F);
    private static final OnShelfTransform BED = new OnShelfTransform(90, 180, 0, 0, 14.5F, 2.5F, 0.9375F);

    /** What 1.12.2's special renderers draw, in model space (0 to 1), for the transformed bounding box. */
    private static final float[] SKULL_EXTENTS = {0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F};
    private static final float[] DRAGON_HEAD_EXTENTS = {0.125F, 0.0F, 0.0F, 0.875F, 0.625F, 1.0F};
    private static final float[] SHIELD_EXTENTS = {0.125F, -0.1875F, 0.5625F, 0.875F, 1.1875F, 0.625F};
    private static final float[] BED_EXTENTS = {0.0F, 0.0F, -1.0F, 1.0F, 0.5625F, 1.0F};
    static final float[] UNIT = {0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};

    final float rx;
    final float ry;
    final float rz;
    final float tx;
    final float ty;
    final float tz;
    final float scale;

    private OnShelfTransform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.scale = scale;
    }

    static OnShelfTransform of(ItemStack stack, IBakedModel model) {
        if (stack.getItem() == Items.SKULL) {
            return stack.getMetadata() == 5 ? DRAGON_HEAD : SKULL;
        }
        if (stack.getItem() instanceof ItemShield) {
            return SHIELD;
        }
        if (stack.getItem() == Items.BED) {
            return BED;
        }
        if (model.isBuiltInRenderer() || !model.isGui3d()) {
            return NONE;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof BlockShelf) {
            return SHELF;
        }
        if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockAnvil || isModernWall(block)) {
            return QUARTER;
        }
        return BLOCK;
    }

    /** Backport Core's {@code ModernWall} (the family's walls), by name so no Core version is needed to link. */
    private static boolean isModernWall(Block block) {
        for (Class<?> c = block.getClass(); c != null && c != Block.class; c = c.getSuperclass()) {
            if ("com.exiledradio.backportcore.block.ModernWall".equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    /** @return the extents of a special-rendered item, or null to take them from the model's quads */
    static float[] specialExtents(ItemStack stack) {
        if (stack.getItem() == Items.SKULL) {
            return stack.getMetadata() == 5 ? DRAGON_HEAD_EXTENTS : SKULL_EXTENTS;
        }
        if (stack.getItem() instanceof ItemShield) {
            return SHIELD_EXTENTS;
        }
        if (stack.getItem() == Items.BED) {
            return BED_EXTENTS;
        }
        return null;
    }

    /** @return the height of a model-space point once transformed (the half-block offset included) */
    float transformedY(float x, float y, float z) {
        double px = (x - 0.5) * this.scale;
        double py = (y - 0.5) * this.scale;
        double pz = (z - 0.5) * this.scale;
        // Rx * Ry * Rz applied to the point: Z first, then Y, then X.
        double a = Math.toRadians(this.rz);
        double x1 = px * Math.cos(a) - py * Math.sin(a);
        double y1 = px * Math.sin(a) + py * Math.cos(a);
        double z1 = pz;
        a = Math.toRadians(this.ry);
        double z2 = -x1 * Math.sin(a) + z1 * Math.cos(a);
        double y2 = y1;
        a = Math.toRadians(this.rx);
        double y3 = y2 * Math.cos(a) - z2 * Math.sin(a);
        return (float) (y3 + this.ty / 16.0);
    }

    /** Applies the transform to the GL matrix; 1.12.2's renderItem then adds the half-block offset. */
    void apply() {
        GlStateManager.translate(this.tx / 16.0F, this.ty / 16.0F, this.tz / 16.0F);
        GlStateManager.rotate(this.rx, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(this.ry, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.rz, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(this.scale, this.scale, this.scale);
    }
}
