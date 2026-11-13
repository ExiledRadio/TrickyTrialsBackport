package com.exiledradio.trickytrialsbackport.palegarden.boat.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.MathHelper;

/**
 * 1.20's boat, chest boat, raft and chest raft models in 1.12.2 model parts.
 *
 * <p>The boat's hull and paddles are vanilla 1.12.2's own (1.20's boat model is the same); 1.20's raft is a
 * flat two-layer deck with its paddles further in; the chest parts (bottom, lid, lock) sit at the back.
 * Chest models use a 128x128 texture, the others 128x64.
 */
public class ModelWoodBoat extends ModelBase {

    private final boolean raft;
    private final ModelRenderer[] hull;
    private final ModelRenderer[] paddles = new ModelRenderer[2];
    private final ModelRenderer[] chest;
    final ModelRenderer noWater;

    public ModelWoodBoat(boolean raft, boolean withChest) {
        this.raft = raft;
        this.textureWidth = 128;
        this.textureHeight = withChest ? 128 : 64;
        if (raft) {
            ModelRenderer bottom = part(0, 0);
            bottom.addBox(-14.0F, -11.0F, -4.0F, 28, 20, 4);
            bottom.setTextureOffset(0, 0);
            bottom.addBox(-14.0F, -9.0F, -8.0F, 28, 16, 4);
            bottom.setRotationPoint(0.0F, -2.1F, 1.0F);
            bottom.rotateAngleX = (float) Math.PI / 2F;
            this.hull = new ModelRenderer[] {bottom};
            this.paddles[0] = paddle(0, 24, true, 3.0F, -4.0F, 9.0F);
            this.paddles[1] = paddle(40, 24, false, 3.0F, -4.0F, -9.0F);
            this.noWater = null;
        } else {
            ModelRenderer[] sides = new ModelRenderer[5];
            sides[0] = part(0, 0);
            sides[0].addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3);
            sides[0].setRotationPoint(0.0F, 3.0F, 1.0F);
            sides[0].rotateAngleX = (float) Math.PI / 2F;
            sides[1] = part(0, 19);
            sides[1].addBox(-13.0F, -7.0F, -1.0F, 18, 6, 2);
            sides[1].setRotationPoint(-15.0F, 4.0F, 4.0F);
            sides[1].rotateAngleY = (float) Math.PI * 3F / 2F;
            sides[2] = part(0, 27);
            sides[2].addBox(-8.0F, -7.0F, -1.0F, 16, 6, 2);
            sides[2].setRotationPoint(15.0F, 4.0F, 0.0F);
            sides[2].rotateAngleY = (float) Math.PI / 2F;
            sides[3] = part(0, 35);
            sides[3].addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2);
            sides[3].setRotationPoint(0.0F, 4.0F, -9.0F);
            sides[3].rotateAngleY = (float) Math.PI;
            sides[4] = part(0, 43);
            sides[4].addBox(-14.0F, -7.0F, -1.0F, 28, 6, 2);
            sides[4].setRotationPoint(0.0F, 4.0F, 9.0F);
            this.hull = sides;
            this.paddles[0] = paddle(62, 0, true, 3.0F, -5.0F, 9.0F);
            this.paddles[1] = paddle(62, 20, false, 3.0F, -5.0F, -9.0F);
            this.noWater = part(0, 0);
            this.noWater.addBox(-14.0F, -9.0F, -3.0F, 28, 16, 3);
            this.noWater.setRotationPoint(0.0F, -3.0F, 1.0F);
            this.noWater.rotateAngleX = (float) Math.PI / 2F;
        }
        if (withChest) {
            float y = raft ? -5.1F : 0.0F;
            ModelRenderer bottom = part(0, 76);
            bottom.addBox(0.0F, 0.0F, 0.0F, 12, 8, 12);
            bottom.setRotationPoint(-2.0F, -5.0F + y, -6.0F);
            bottom.rotateAngleY = -(float) Math.PI / 2F;
            ModelRenderer lid = part(0, 59);
            lid.addBox(0.0F, 0.0F, 0.0F, 12, 4, 12);
            lid.setRotationPoint(-2.0F, -9.0F + y, -6.0F);
            lid.rotateAngleY = -(float) Math.PI / 2F;
            ModelRenderer lock = part(0, 59);
            lock.addBox(0.0F, 0.0F, 0.0F, 2, 4, 1);
            lock.setRotationPoint(-1.0F, -6.0F + y, -1.0F);
            lock.rotateAngleY = -(float) Math.PI / 2F;
            this.chest = new ModelRenderer[] {bottom, lid, lock};
        } else {
            this.chest = new ModelRenderer[0];
        }
        this.paddles[1].rotateAngleY = (float) Math.PI;
        this.paddles[0].rotateAngleZ = 0.19634955F;
        this.paddles[1].rotateAngleZ = 0.19634955F;
    }

    private ModelRenderer part(int u, int v) {
        return new ModelRenderer(this, u, v).setTextureSize(this.textureWidth, this.textureHeight);
    }

    private ModelRenderer paddle(int u, int v, boolean left, float x, float y, float z) {
        ModelRenderer r = part(u, v);
        r.addBox(-1.0F, 0.0F, -5.0F, 2, 2, 18);
        r.addBox(left ? -1.001F : 0.001F, -3.0F, 8.0F, 1, 6, 7);
        r.setRotationPoint(x, y, z);
        return r;
    }

    public void render(EntityBoat boat, float partialTicks, float scale) {
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        for (ModelRenderer r : this.hull) {
            r.render(scale);
        }
        for (ModelRenderer r : this.chest) {
            r.render(scale);
        }
        for (int side = 0; side < 2; side++) {
            float rowing = boat.getRowingTime(side, partialTicks);
            ModelRenderer r = this.paddles[side];
            r.rotateAngleX = (float) MathHelper.clampedLerp(-Math.PI / 3, -Math.PI / 12, (MathHelper.sin(-rowing) + 1.0F) / 2.0F);
            r.rotateAngleY = (float) MathHelper.clampedLerp(-Math.PI / 4, Math.PI / 4, (MathHelper.sin(-rowing + 1.0F) + 1.0F) / 2.0F);
            if (side == 1) {
                r.rotateAngleY = (float) Math.PI - r.rotateAngleY;
            }
            r.render(scale);
        }
    }

    public boolean raft() {
        return this.raft;
    }
}
