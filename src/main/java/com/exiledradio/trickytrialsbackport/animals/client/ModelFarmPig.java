package com.exiledradio.trickytrialsbackport.animals.client;

import com.exiledradio.trickytrialsbackport.animals.FarmVariant;

import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;

/**
 * 1.21.5's {@code PigModel} and {@code ColdPigModel} (64x64; the warm pig uses the plain model), part for part, on
 * 1.12.2's quadruped. A piglet is drawn as 1.21.5's {@code BABY_TRANSFORMER} draws it (head unscaled, 4 down and 4
 * forward; the rest at half size).
 */
public final class ModelFarmPig extends ModelQuadruped {

    public ModelFarmPig(FarmVariant variant) {
        super(6, 0.0F);
        this.boxList.clear();
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.childYOffset = 4.0F;
        this.childZOffset = 4.0F;

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 12.0F, -6.0F);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, 0.0F);
        this.head.setTextureOffset(16, 16).addBox(-2.0F, 0.0F, -9.0F, 4, 3, 1, 0.0F);

        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 11.0F, 2.0F);
        this.body.setTextureOffset(28, 8).addBox(-5.0F, -10.0F, -7.0F, 10, 16, 8, 0.0F);
        if (variant == FarmVariant.COLD) {
            this.body.setTextureOffset(28, 32).addBox(-5.0F, -10.0F, -7.0F, 10, 16, 8, 0.5F);
        }

        // QuadrupedModel.createBodyMesh(6, true, false, ...): the left legs mirrored.
        this.leg1 = leg(false, -3.0F, 7.0F);
        this.leg2 = leg(true, 3.0F, 7.0F);
        this.leg3 = leg(false, -3.0F, -5.0F);
        this.leg4 = leg(true, 3.0F, -5.0F);
    }

    private ModelRenderer leg(boolean mirror, float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 0, 16);
        leg.mirror = mirror;
        leg.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, 0.0F);
        leg.setRotationPoint(x, 18.0F, z);
        return leg;
    }
}
