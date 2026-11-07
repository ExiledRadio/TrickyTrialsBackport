package com.exiledradio.trickytrialsbackport.breeze.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/** 1.21's wind charge: a small core inside a spinning ring of wind (64x32 texture). */
public final class ModelWindCharge extends ModelBase {

    final ModelRenderer wind;
    final ModelRenderer core;

    public ModelWindCharge() {
        this.textureWidth = 64;
        this.textureHeight = 32;
        this.wind = new ModelRenderer(this);
        this.wind.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.wind.setTextureOffset(15, 20).addBox(-4.0F, -1.0F, -1.0F, 8, 2, 2);
        this.wind.setTextureOffset(0, 9).addBox(-3.0F, -2.0F, -3.0F, 6, 4, 6);
        this.core = new ModelRenderer(this);
        this.core.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.core.setTextureOffset(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.wind.rotateAngleY = -0.7854F + ageInTicks * 0.3F;
        this.core.rotateAngleY = -ageInTicks * 0.2F;
        this.wind.render(scale);
        this.core.render(scale);
    }
}
