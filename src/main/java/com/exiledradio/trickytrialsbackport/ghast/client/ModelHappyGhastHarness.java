package com.exiledradio.trickytrialsbackport.ghast.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 1.21.11's {@code HappyGhastHarnessModel} (64 x 64): the harness around the body and the goggles, raised and tipped
 * up 45 degrees until someone rides, then lowered over the eyes.
 */
public class ModelHappyGhastHarness extends ModelBase {

    private final ModelRenderer harness;
    private final ModelRenderer goggles;

    public ModelHappyGhastHarness() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.harness = new ModelRenderer(this, 0, 0);
        this.harness.addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16);
        this.harness.setRotationPoint(0.0F, 24.0F, 0.0F);
        this.goggles = new ModelRenderer(this, 0, 32);
        this.goggles.addBox(-8.0F, -2.5F, -2.5F, 16, 5, 5, 0.15F);
        this.goggles.setRotationPoint(0.0F, 14.0F, -5.5F);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (entity.isBeingRidden()) {
            this.goggles.rotateAngleX = 0.0F;
            this.goggles.rotationPointY = 14.0F;
        } else {
            this.goggles.rotateAngleX = -0.7854F;
            this.goggles.rotationPointY = 9.0F;
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.harness.render(scale);
        this.goggles.render(scale);
    }
}
