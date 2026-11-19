package com.exiledradio.trickytrialsbackport.coppergolem.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * 1.21.11's {@code ChestModel} (64x64 per half, the 1.15+ texture layout): bottom, lid and lock, single or one half of
 * a double chest. Drawn in block space with y up and no flip, as 1.21's chest renderer draws it, so 1.21's copper
 * chest textures fit as they are.
 */
public final class ModelCopperChest extends ModelBase {

    public enum Kind {
        SINGLE,
        LEFT,
        RIGHT
    }

    final ModelRenderer bottom;
    final ModelRenderer lid;
    final ModelRenderer lock;

    public ModelCopperChest(Kind kind) {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.bottom = new ModelRenderer(this, 0, 19);
        this.lid = new ModelRenderer(this, 0, 0);
        this.lock = new ModelRenderer(this, 0, 0);
        this.lid.setRotationPoint(0.0F, 9.0F, 1.0F);
        this.lock.setRotationPoint(0.0F, 9.0F, 1.0F);
        switch (kind) {
            case RIGHT:
                this.bottom.addBox(1.0F, 0.0F, 1.0F, 15, 10, 14);
                this.lid.addBox(1.0F, 0.0F, 0.0F, 15, 5, 14);
                this.lock.addBox(15.0F, -2.0F, 14.0F, 1, 4, 1);
                break;
            case LEFT:
                this.bottom.addBox(0.0F, 0.0F, 1.0F, 15, 10, 14);
                this.lid.addBox(0.0F, 0.0F, 0.0F, 15, 5, 14);
                this.lock.addBox(0.0F, -2.0F, 14.0F, 1, 4, 1);
                break;
            default:
                this.bottom.addBox(1.0F, 0.0F, 1.0F, 14, 10, 14);
                this.lid.addBox(1.0F, 0.0F, 0.0F, 14, 5, 14);
                this.lock.addBox(7.0F, -2.0F, 14.0F, 2, 4, 1);
                break;
        }
    }

    /** @param open the eased openness, 0 shut to 1 open */
    void render(float open, float scale) {
        this.lid.rotateAngleX = -(open * (float) (Math.PI / 2.0D));
        this.lock.rotateAngleX = this.lid.rotateAngleX;
        this.bottom.render(scale);
        this.lid.render(scale);
        this.lock.render(scale);
    }
}
