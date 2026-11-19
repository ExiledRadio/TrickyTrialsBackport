package com.exiledradio.trickytrialsbackport.ghast.client;

import com.exiledradio.trickytrialsbackport.ghast.EntityHappyGhast;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * 1.21.11's {@code HappyGhastModel} (64 x 64): a 16-pixel body and nine tentacles swaying as the ghast's do
 * ({@code GhastModel.animateTentacles}). Drawn 4 times larger by the renderer (1.21 bakes that scale into the mesh,
 * about the feet), the ghastling 0.2375 of that; the body shrinks to 0.9375 inside a harness.
 */
public class ModelHappyGhast extends ModelBase {

    private static final float BODY_SQUEEZE = 0.9375F;
    private static final float[][] TENTACLES = {
            {-3.75F, 7.0F, -5.0F, 5}, {1.25F, 7.0F, -5.0F, 7}, {6.25F, 7.0F, -5.0F, 4},
            {-6.25F, 7.0F, 0.0F, 5}, {-1.25F, 7.0F, 0.0F, 5}, {3.75F, 7.0F, 0.0F, 7},
            {-3.75F, 7.0F, 5.0F, 8}, {1.25F, 7.0F, 5.0F, 8}, {6.25F, 7.0F, 5.0F, 5}};

    final ModelRenderer body;
    private final ModelRenderer[] tentacles = new ModelRenderer[TENTACLES.length];

    public ModelHappyGhast() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this, 0, 0);
        this.body.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
        this.body.setRotationPoint(0.0F, 16.0F, 0.0F);
        for (int i = 0; i < TENTACLES.length; i++) {
            ModelRenderer t = new ModelRenderer(this, 0, 0);
            t.addBox(-1.0F, 0.0F, -1.0F, 2, (int) TENTACLES[i][3], 2);
            t.setRotationPoint(TENTACLES[i][0], TENTACLES[i][1], TENTACLES[i][2]);
            this.body.addChild(t);
            this.tentacles[i] = t;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i].rotateAngleX = 0.2F * MathHelper.sin(ageInTicks * 0.3F + i) + 0.4F;
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        boolean harnessed = entity instanceof EntityHappyGhast && ((EntityHappyGhast) entity).isWearingHarness();
        if (!harnessed) {
            this.body.render(scale);
            return;
        }
        GlStateManager.pushMatrix();
        float py = this.body.rotationPointY * scale;
        GlStateManager.translate(0.0F, py, 0.0F);
        GlStateManager.scale(BODY_SQUEEZE, BODY_SQUEEZE, BODY_SQUEEZE);
        GlStateManager.translate(0.0F, -py, 0.0F);
        this.body.render(scale);
        GlStateManager.popMatrix();
    }
}
