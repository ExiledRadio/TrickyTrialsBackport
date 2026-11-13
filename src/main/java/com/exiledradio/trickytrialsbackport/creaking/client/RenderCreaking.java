package com.exiledradio.trickytrialsbackport.creaking.client;

import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21.11's {@code CreakingRenderer}: the model, shadow 0.6, and the glowing eyes (the head in the eyes texture,
 * drawn full bright and additive) while the creaking is active. A twitching creaking draws no death tilt and no
 * red tint, and its eyes flicker instead.
 */
public final class RenderCreaking extends RenderLiving<EntityCreaking> {

    static final ResourceLocation TEXTURE = Content.id("textures/entity/creaking/creaking.png");
    static final ResourceLocation EYES = Content.id("textures/entity/creaking/creaking_eyes.png");

    public RenderCreaking(RenderManager manager) {
        super(manager, new ModelCreaking(), 0.6F);
        addLayer(new Eyes());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCreaking entity) {
        return TEXTURE;
    }

    @Override
    public void doRender(EntityCreaking entity, double x, double y, double z, float yaw, float partialTicks) {
        if (!entity.isTearingDown()) {
            super.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }
        // Vanilla sets deathTime 0 and hasRedOverlay false in the render state of a twitching creaking.
        int deathTime = entity.deathTime;
        entity.deathTime = 0;
        try {
            super.doRender(entity, x, y, z, yaw, partialTicks);
        } finally {
            entity.deathTime = deathTime;
        }
    }

    private final class Eyes implements LayerRenderer<EntityCreaking> {
        @Override
        public void doRenderLayer(EntityCreaking entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            boolean glowing = entity.isTearingDown() ? entity.hasGlowingEyes() : entity.isActive();
            if (!glowing) {
                return;
            }
            bindTexture(EYES);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!entity.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            ((ModelCreaking) getMainModel()).renderHeadOnly(scale);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            int light = entity.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            setLightmap(entity);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
