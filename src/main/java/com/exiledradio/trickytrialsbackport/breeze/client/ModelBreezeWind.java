package com.exiledradio.trickytrialsbackport.breeze.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/** The breeze's tornado: three stacked layers on the 128x128 wind texture, each swaying its own way. */
public final class ModelBreezeWind extends ModelBase {

    final ModelRenderer bottom;
    final ModelRenderer mid;
    final ModelRenderer top;

    public ModelBreezeWind() {
        this.textureWidth = 128;
        this.textureHeight = 128;

        this.bottom = new ModelRenderer(this);
        this.bottom.setRotationPoint(0.0F, 24.0F, 0.0F);
        this.bottom.setTextureOffset(1, 83).addBox(-2.5F, -7.0F, -2.5F, 5, 7, 5);

        this.mid = new ModelRenderer(this);
        this.mid.setRotationPoint(0.0F, -7.0F, 0.0F);
        this.mid.setTextureOffset(74, 28).addBox(-6.0F, -6.0F, -6.0F, 12, 6, 12);
        this.mid.setTextureOffset(78, 32).addBox(-4.0F, -6.0F, -4.0F, 8, 6, 8);
        this.mid.setTextureOffset(49, 71).addBox(-2.5F, -6.0F, -2.5F, 5, 6, 5);
        this.bottom.addChild(this.mid);

        this.top = new ModelRenderer(this);
        this.top.setRotationPoint(0.0F, -6.0F, 0.0F);
        this.top.setTextureOffset(0, 0).addBox(-9.0F, -8.0F, -9.0F, 18, 8, 18);
        this.top.setTextureOffset(6, 6).addBox(-6.0F, -8.0F, -6.0F, 12, 8, 12);
        this.top.setTextureOffset(105, 57).addBox(-2.5F, -8.0F, -2.5F, 5, 8, 5);
        this.mid.addChild(this.top);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.bottom.rotateAngleY = ageInTicks * 0.3F;
        this.mid.rotationPointX = (float) Math.cos(ageInTicks * 0.2F) * 0.6F;
        this.mid.rotationPointZ = (float) Math.sin(ageInTicks * 0.2F) * 0.6F;
        this.top.rotationPointX = (float) Math.cos(ageInTicks * 0.15F + 1.0F) * 0.9F;
        this.top.rotationPointZ = (float) Math.sin(ageInTicks * 0.15F + 1.0F) * 0.9F;
        this.bottom.render(scale);
    }
}
