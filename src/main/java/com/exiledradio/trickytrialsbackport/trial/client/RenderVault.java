package com.exiledradio.trickytrialsbackport.trial.client;

import com.exiledradio.trickytrialsbackport.trial.TileEntityVault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/** The item turning inside a lit vault: 1.21's {@code VaultRenderer}, 10 degrees a tick, 0.4 blocks up. */
final class RenderVault extends TileEntitySpecialRenderer<TileEntityVault> {

    @Override
    public void render(TileEntityVault te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        ItemStack stack = te.getDisplayItem();
        if (stack.isEmpty() || te.getWorld() == null) {
            return;
        }
        float spin = te.previousSpin + MathHelper.wrapDegrees(te.currentSpin - te.previousSpin) * partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.4F, (float) z + 0.5F);
        GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
}
