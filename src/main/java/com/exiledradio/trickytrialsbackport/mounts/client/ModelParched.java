package com.exiledradio.trickytrialsbackport.mounts.client;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSkeleton;

/**
 * 1.21.11's {@code SkeletonModel.createSingleModelDualBodyLayer} (the parched's 64 x 64 model): a skeleton whose head,
 * body, arms and legs each carry a second, wider box in the lower half of the texture (the parched's wraps).
 */
public final class ModelParched extends ModelSkeleton {

    public ModelParched() {
        super(0.0F, false);
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);
        this.bipedHead.setTextureOffset(0, 32).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.2F);
        this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedHeadwear = new ModelRenderer(this, 32, 0);
        this.bipedHeadwear.isHidden = true;

        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        this.bipedBody.setTextureOffset(28, 0).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, 0.0F);
        this.bipedBody.setTextureOffset(16, 48).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.025F);
        this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, 0.0F);
        this.bipedRightArm.setTextureOffset(42, 33).addBox(-1.55F, -2.025F, -1.5F, 3, 12, 3, 0.0F);
        this.bipedRightArm.setRotationPoint(-5.5F, 2.0F, 0.0F);

        this.bipedLeftArm = new ModelRenderer(this, 56, 16);
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, 0.0F);
        this.bipedLeftArm.setTextureOffset(40, 48).addBox(-1.45F, -2.025F, -1.5F, 3, 12, 3, 0.0F);
        this.bipedLeftArm.setRotationPoint(5.5F, 2.0F, 0.0F);

        this.bipedRightLeg = new ModelRenderer(this, 0, 16);
        this.bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, 0.0F);
        this.bipedRightLeg.setTextureOffset(0, 49).addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, 0.0F);
        this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);

        this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
        this.bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, 0.0F);
        this.bipedLeftLeg.setTextureOffset(4, 49).addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, 0.0F);
        this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
    }
}
