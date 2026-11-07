package com.exiledradio.trickytrialsbackport.breeze.client;

import com.exiledradio.trickytrialsbackport.breeze.EntityBreeze;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * The breeze: body texture, full-bright eyes, and the translucent tornado with its texture drifting
 * sideways, as 1.21 draws the wind.
 */
public final class RenderBreeze extends RenderLiving<EntityBreeze> {

    static final ResourceLocation TEXTURE = Content.id("textures/entity/breeze/breeze.png");
    static final ResourceLocation EYES = Content.id("textures/entity/breeze/breeze_eyes.png");
    static final ResourceLocation WIND = Content.id("textures/entity/breeze/breeze_wind.png");

    public RenderBreeze(RenderManager manager) {
        super(manager, new ModelBreeze(), 0.0F);
        addLayer(new Wind());
        addLayer(new Eyes());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBreeze entity) {
        return TEXTURE;
    }

    private final class Eyes implements LayerRenderer<EntityBreeze> {
        @Override
        public void doRenderLayer(EntityBreeze entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            bindTexture(EYES);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!entity.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680.0F, 0.0F);
            ((ModelBreeze) getMainModel()).renderHead(scale);
            int light = entity.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    private final class Wind implements LayerRenderer<EntityBreeze> {
        private final ModelBreezeWind model = new ModelBreezeWind();

        @Override
        public void doRenderLayer(EntityBreeze entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            bindTexture(WIND);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.translate(ageInTicks * 0.02F, 0.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.8F);
            GlStateManager.disableCull();
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.enableCull();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.disableBlend();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
