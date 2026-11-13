package com.exiledradio.trickytrialsbackport.trial.client;

import com.exiledradio.trickytrialsbackport.trial.EntityOminousItemSpawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * The ominous item spawner's item: 1.21's {@code OminousItemSpawnerRenderer}, growing to full size over
 * its first 50 ticks, turning 40 degrees a tick, full bright.
 */
final class RenderOminousItemSpawner extends Render<EntityOminousItemSpawner> {

    RenderOminousItemSpawner(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityOminousItemSpawner entity, double x, double y, double z, float yaw, float partialTicks) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return;
        }
        float age = entity.ticksExisted + partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        if (age <= 50.0F) {
            float s = Math.min(age, 50.0F) / 50.0F;
            GlStateManager.scale(s, s, s);
        }
        GlStateManager.rotate(MathHelper.wrapDegrees(age * 40.0F), 0.0F, 1.0F, 0.0F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        bindEntityTexture(entity);
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityOminousItemSpawner entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
