package com.exiledradio.trickytrialsbackport.bogged.client;

import com.exiledradio.trickytrialsbackport.bogged.EntityBogged;
import com.exiledradio.trickytrialsbackport.common.Content;

import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/** The bogged: skeleton armor and held items, its own texture, and the mossy overlay over the body. */
public final class RenderBogged extends RenderBiped<EntityBogged> {

    static final ResourceLocation TEXTURE = Content.id("textures/entity/skeleton/bogged.png");
    static final ResourceLocation OVERLAY = Content.id("textures/entity/skeleton/bogged_overlay.png");

    public RenderBogged(RenderManager manager) {
        super(manager, new ModelBogged(), 0.5F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelSkeleton(0.5F, true);
                this.modelArmor = new ModelSkeleton(1.0F, true);
            }
        });
        addLayer(new Overlay());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBogged entity) {
        return TEXTURE;
    }

    @Override
    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.09375F, 0.1875F, 0.0F);
    }

    /** As the stray's clothing layer: a slightly larger skeleton in the overlay texture. */
    private final class Overlay implements LayerRenderer<EntityBogged> {
        private final ModelSkeleton model = new ModelSkeleton(0.25F, true);

        @Override
        public void doRenderLayer(EntityBogged entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            this.model.setModelAttributes(getMainModel());
            this.model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            bindTexture(OVERLAY);
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
