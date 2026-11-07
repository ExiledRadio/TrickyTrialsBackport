package com.exiledradio.trickytrialsbackport.breeze.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * The breeze's head and its three rods (the body of 1.21's breeze model, 32x32 texture). The rods turn
 * about the body; the head bobs. The tornado below is {@link ModelBreezeWind}, on its own texture.
 */
public final class ModelBreeze extends ModelBase {

    final ModelRenderer head;
    final ModelRenderer rods;

    public ModelBreeze() {
        this.textureWidth = 32;
        this.textureHeight = 32;

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.head.setTextureOffset(4, 24).addBox(-5.0F, -5.0F, -4.2F, 10, 3, 4);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8);

        this.rods = new ModelRenderer(this);
        this.rods.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.rods.addChild(rod(2.5981F, -3.0F, 1.5F, -2.7489F, -1.0472F, 3.1416F));
        this.rods.addChild(rod(-2.5981F, -3.0F, 1.5F, -2.7489F, 1.0472F, 3.1416F));
        this.rods.addChild(rod(0.0F, -3.0F, -3.0F, 0.3927F, 0.0F, 0.0F));
    }

    private ModelRenderer rod(float x, float y, float z, float rx, float ry, float rz) {
        ModelRenderer rod = new ModelRenderer(this);
        rod.setRotationPoint(x, y, z);
        rod.rotateAngleX = rx;
        rod.rotateAngleY = ry;
        rod.rotateAngleZ = rz;
        rod.setTextureOffset(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2, 8, 2);
        return rod;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotationPointY = 4.0F + (float) Math.sin(ageInTicks * 0.1F) * 0.5F;
        this.rods.rotateAngleY = ageInTicks * 0.2F;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.rods.render(scale);
        this.head.render(scale);
    }

    /** The head alone, for the eyes layer. */
    void renderHead(float scale) {
        this.head.render(scale);
    }
}
