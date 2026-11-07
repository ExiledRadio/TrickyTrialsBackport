package com.exiledradio.trickytrialsbackport.bogged.client;

import com.exiledradio.trickytrialsbackport.bogged.EntityBogged;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.entity.Entity;

/**
 * The bogged: vanilla's skeleton with 1.21's six mushroom planes on its head (64x32 texture), hidden
 * once it has been sheared.
 */
public final class ModelBogged extends ModelSkeleton {

    private final ModelRenderer mushrooms;

    public ModelBogged() {
        super(0.0F, false);
        this.mushrooms = new ModelRenderer(this);
        this.mushrooms.setRotationPoint(0.0F, 0.0F, 0.0F);
        float quarter = (float) (Math.PI / 4);
        float threeQuarters = (float) (Math.PI * 3.0 / 4.0);
        this.mushrooms.addChild(plane(50, 16, -3.0F, 3.0F, -8.0F, 3.0F, 0.0F, quarter, 0.0F));
        this.mushrooms.addChild(plane(50, 16, -3.0F, 3.0F, -8.0F, 3.0F, 0.0F, threeQuarters, 0.0F));
        this.mushrooms.addChild(plane(50, 22, -3.0F, -3.0F, -8.0F, -3.0F, 0.0F, quarter, 0.0F));
        this.mushrooms.addChild(plane(50, 22, -3.0F, -3.0F, -8.0F, -3.0F, 0.0F, threeQuarters, 0.0F));
        this.mushrooms.addChild(plane(50, 28, -4.0F, -2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, quarter));
        this.mushrooms.addChild(plane(50, 28, -4.0F, -2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, threeQuarters));
        this.bipedHead.addChild(this.mushrooms);
    }

    /** One 6x4 flat mushroom: a box with no depth, placed and turned. */
    private ModelRenderer plane(int u, int v, float top, float x, float y, float z, float rx, float ry, float rz) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = rx;
        part.rotateAngleY = ry;
        part.rotateAngleZ = rz;
        part.addBox(-3.0F, top, 0.0F, 6, 4, 0);
        return part;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.mushrooms.isHidden = entity instanceof EntityBogged && ((EntityBogged) entity).isSheared();
    }
}
