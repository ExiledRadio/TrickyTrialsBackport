package com.exiledradio.trickytrialsbackport.animals.client;

import com.exiledradio.trickytrialsbackport.animals.FarmVariant;

import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;

/**
 * 1.21.5's {@code CowModel}, {@code WarmCowModel} and {@code ColdCowModel} (64x64), part for part, on 1.12.2's
 * quadruped: {@code leg1}-{@code leg4} are the right hind, left hind, right front and left front legs, which 1.12.2
 * animates as 1.21.5 does. A calf is drawn as 1.21.5's {@code BABY_TRANSFORMER} draws it (head unscaled, 8 down and 6
 * forward; the rest at half size).
 */
public final class ModelFarmCow extends ModelQuadruped {

    public ModelFarmCow(FarmVariant variant) {
        super(12, 0.0F);
        this.boxList.clear();
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.childYOffset = 8.0F;
        this.childZOffset = 6.0F;

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 4.0F, -8.0F);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8, 8, 6, 0.0F);
        switch (variant) {
            case WARM:
                this.head.setTextureOffset(1, 33).addBox(-3.0F, 1.0F, -7.0F, 6, 3, 1, 0.0F);
                this.head.setTextureOffset(27, 0).addBox(-8.0F, -3.0F, -5.0F, 4, 2, 2, 0.0F);
                this.head.setTextureOffset(39, 0).addBox(-8.0F, -5.0F, -5.0F, 2, 2, 2, 0.0F);
                this.head.mirror = true;
                this.head.setTextureOffset(27, 0).addBox(4.0F, -3.0F, -5.0F, 4, 2, 2, 0.0F);
                this.head.setTextureOffset(39, 0).addBox(6.0F, -5.0F, -5.0F, 2, 2, 2, 0.0F);
                this.head.mirror = false;
                break;
            case COLD:
                this.head.setTextureOffset(9, 33).addBox(-3.0F, 1.0F, -7.0F, 6, 3, 1, 0.0F);
                ModelRenderer right = new ModelRenderer(this, 0, 40);
                right.addBox(-1.5F, -4.5F, -0.5F, 2, 6, 2, 0.0F);
                right.setRotationPoint(-4.5F, -2.5F, -3.5F);
                right.rotateAngleX = 1.5708F;
                this.head.addChild(right);
                ModelRenderer left = new ModelRenderer(this, 0, 32);
                left.addBox(-1.5F, -3.0F, -0.5F, 2, 6, 2, 0.0F);
                left.setRotationPoint(5.5F, -2.5F, -5.0F);
                left.rotateAngleX = 1.5708F;
                this.head.addChild(left);
                break;
            default:
                this.head.setTextureOffset(1, 33).addBox(-3.0F, 1.0F, -7.0F, 6, 3, 1, 0.0F);
                this.head.setTextureOffset(22, 0).addBox(-5.0F, -5.0F, -5.0F, 1, 3, 1, 0.0F);
                this.head.setTextureOffset(22, 0).addBox(4.0F, -5.0F, -5.0F, 1, 3, 1, 0.0F);
                break;
        }

        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 5.0F, 2.0F);
        if (variant == FarmVariant.COLD) {
            this.body.setTextureOffset(20, 32).addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.5F);
        }
        this.body.setTextureOffset(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F);
        this.body.setTextureOffset(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4, 6, 1, 0.0F);

        this.leg1 = leg(false, -4.0F, 7.0F);
        this.leg2 = leg(true, 4.0F, 7.0F);
        this.leg3 = leg(false, -4.0F, -5.0F);
        this.leg4 = leg(true, 4.0F, -5.0F);
    }

    private ModelRenderer leg(boolean mirror, float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 0, 16);
        leg.mirror = mirror;
        leg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        leg.setRotationPoint(x, 12.0F, z);
        return leg;
    }
}
