package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.coppergolem.BlockCopperGolemStatue;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * 1.21.11's {@code CopperGolemStatueModel}: the copper golem model in one of four poses (standing is the golem's own
 * layer; sitting, running and star are {@code createSittingPoseBodyLayer} and the others), unanimated. The root is
 * turned to the statue's facing and upside down, as the statue renderer draws it without an entity's flip.
 */
public final class ModelCopperGolemStatue extends ModelBase {

    final ModelRenderer root;

    public ModelCopperGolemStatue(BlockCopperGolemStatue.Pose pose) {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.root = new ModelRenderer(this, 0, 0);
        switch (pose) {
            case SITTING:
                sitting();
                break;
            case RUNNING:
                running();
                break;
            case STAR:
                star();
                break;
            default:
                standing();
                break;
        }
    }

    private ModelRenderer part(ModelRenderer parent, float x, float y, float z, float rx, float ry, float rz) {
        ModelRenderer p = new ModelRenderer(this, 0, 0);
        p.setRotationPoint(x, y, z);
        p.rotateAngleX = rx;
        p.rotateAngleY = ry;
        p.rotateAngleZ = rz;
        parent.addChild(p);
        return p;
    }

    private ModelRenderer part(ModelRenderer parent, float x, float y, float z) {
        return part(parent, x, y, z, 0.0F, 0.0F, 0.0F);
    }

    private static void box(ModelRenderer p, int u, int v, float x, float y, float z, int w, int h, int d, float delta) {
        p.setTextureOffset(u, v).addBox(x, y, z, w, h, d, delta);
    }

    private void head(ModelRenderer head, float y, float x2, float face) {
        box(head, 0, 0, -4.0F, -5.0F + y, -5.0F, 8, 5, 10, face);
        box(head, 56, 0, -1.0F + x2, -2.0F + y, -6.0F, 2, 3, 2, 0.0F);
        box(head, 37, 8, -1.0F + x2, -9.0F + y, -1.0F, 2, 4, 2, -0.015F);
        box(head, 37, 0, -2.0F, -13.0F + y, -2.0F, 4, 4, 4, -0.015F);
    }

    /** {@code createBodyLayer}, with the root at 0 (the statue model's {@code root.y = 0}). */
    private void standing() {
        ModelRenderer body = part(this.root, 0.0F, -5.0F, 0.0F);
        box(body, 0, 15, -4.0F, -6.0F, -3.0F, 8, 6, 6, 0.0F);
        head(part(body, 0.0F, -6.0F, 0.0F), 0.0F, 0.0F, 0.015F);
        box(part(body, -4.0F, -6.0F, 0.0F), 36, 16, -3.0F, -1.0F, -2.0F, 3, 10, 4, 0.0F);
        box(part(body, 4.0F, -6.0F, 0.0F), 50, 16, 0.0F, -1.0F, -2.0F, 3, 10, 4, 0.0F);
        box(part(this.root, 0.0F, -5.0F, 0.0F), 0, 27, -4.0F, 0.0F, -2.0F, 4, 5, 4, 0.0F);
        box(part(this.root, 0.0F, -5.0F, 0.0F), 16, 27, 0.0F, 0.0F, -2.0F, 4, 5, 4, 0.0F);
    }

    /** {@code createRunningPoseBodyLayer}. */
    private void running() {
        ModelRenderer body = part(this.root, -1.064F, -5.0F, 0.0F);
        box(part(body, 1.1F, 0.1F, 0.7F, 0.1204F, -0.0064F, -0.0779F), 0, 15, -4.02F, -6.116F, -3.5F, 8, 6, 6, 0.0F);
        ModelRenderer head = part(body, 0.7F, -5.6F, -1.8F);
        box(head, 0, 0, -4.0F, -5.1F, -5.0F, 8, 5, 10, 0.0F);
        box(head, 56, 0, -1.02F, -2.1F, -6.0F, 2, 3, 2, 0.0F);
        box(head, 37, 8, -1.02F, -9.1F, -1.0F, 2, 4, 2, -0.015F);
        box(head, 37, 0, -2.0F, -13.1F, -2.0F, 4, 4, 4, -0.015F);
        ModelRenderer rightArm = part(body, -4.0F, -6.0F, 0.0F);
        box(part(rightArm, 0.7F, -0.248F, -1.62F, 1.0036F, 0.0F, 0.0F), 36, 16, -3.052F, -1.11F, -2.036F, 3, 10, 4, 0.0F);
        ModelRenderer leftArm = part(body, 4.0F, -6.0F, 0.0F);
        box(part(leftArm, 0.732F, 0.0F, 0.0F, -0.8715F, -0.0535F, -0.0449F), 50, 16, 0.032F, -1.1F, -2.0F, 3, 10, 4, 0.0F);
        ModelRenderer rightLeg = part(this.root, -3.064F, -5.0F, 0.0F);
        box(part(rightLeg, 1.048F, 0.0F, -0.9F, -0.8727F, 0.0F, 0.0F), 0, 27, -1.856F, -0.1F, -1.09F, 4, 5, 4, 0.0F);
        ModelRenderer leftLeg = part(this.root, 0.936F, -5.0F, 0.0F);
        box(part(leftLeg, 1.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F), 16, 27, -2.088F, -0.1F, -2.0F, 4, 5, 4, 0.0F);
    }

    /** {@code createSittingPoseBodyLayer}. */
    private void sitting() {
        ModelRenderer body = part(this.root, 0.0F, -3.0F, 2.325F);
        box(body, 3, 19, -3.0F, -4.0F, -4.525F, 6, 1, 6, 0.0F);
        box(body, 0, 15, -4.0F, -3.0F, -3.525F, 8, 6, 6, 0.0F);
        box(part(body, 0.0F, -1.0F, -4.325F, 0.0F, 0.0F, -3.1416F), 3, 18, -4.0F, -3.0F, -2.2F, 8, 6, 3, 0.0F);
        ModelRenderer head = part(body, 0.0F, -6.0F, -0.2F);
        box(head, 37, 8, -1.0F, -7.0F, -3.3F, 2, 4, 2, -0.015F);
        box(head, 37, 0, -2.0F, -11.0F, -4.3F, 4, 4, 4, -0.015F);
        box(head, 0, 0, -4.0F, -3.0F, -7.325F, 8, 5, 10, 0.0F);
        box(head, 56, 0, -1.0F, 0.0F, -8.325F, 2, 3, 2, 0.0F);
        ModelRenderer rightArm = part(body, -4.0F, -5.6F, -1.8F, 0.4363F, 0.0F, 0.0F);
        box(part(rightArm, 0.0F, 0.0893F, 0.1198F, -1.0472F, 0.0F, 0.0F), 36, 16, -3.075F, -0.9733F, -1.9966F, 3, 10, 4, 0.0F);
        ModelRenderer leftArm = part(body, 4.0F, -5.6F, -1.7F, 0.4363F, 0.0F, 0.0F);
        box(part(leftArm, 0.0F, -0.0015F, -0.0808F, -1.0472F, 0.0F, 0.0F), 50, 16, 0.075F, -1.0443F, -1.8997F, 3, 10, 4, 0.0F);
        ModelRenderer rightLeg = part(this.root, -2.1F, -2.1F, -2.075F);
        box(part(rightLeg, 0.05F, -1.9F, 1.075F, -1.5708F, 0.0F, 0.0F), 0, 27, -2.0F, 0.975F, 0.0F, 4, 5, 4, 0.0F);
        ModelRenderer leftLeg = part(this.root, 2.0F, -2.0F, -2.075F);
        box(part(leftLeg, 0.05F, -2.0F, 1.075F, -1.5708F, 0.0F, 0.0F), 16, 27, -2.0F, 0.975F, 0.0F, 4, 5, 4, 0.0F);
    }

    /** {@code createStarPoseBodyLayer}. */
    private void star() {
        ModelRenderer body = part(this.root, 0.0F, -5.0F, 0.0F);
        box(body, 0, 15, -4.0F, -6.0F, -3.0F, 8, 6, 6, 0.0F);
        head(part(body, 0.0F, -6.0F, 0.0F), 0.0F, 0.0F, 0.0F);
        ModelRenderer rightArm = part(body, -4.0F, -6.0F, 0.0F);
        box(part(rightArm, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.9199F), 36, 16, -1.5F, -5.0F, -2.0F, 3, 10, 4, 0.0F);
        ModelRenderer leftArm = part(body, 4.0F, -6.0F, 0.0F);
        box(part(leftArm, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.9199F), 50, 16, -1.5F, -5.0F, -2.0F, 3, 10, 4, 0.0F);
        ModelRenderer rightLeg = part(this.root, -3.0F, -5.0F, 0.0F);
        box(part(rightLeg, 0.35F, 2.0F, 0.01F, 0.0F, 0.0F, 0.2618F), 0, 27, -2.0F, -2.5F, -2.0F, 4, 5, 4, 0.0F);
        ModelRenderer leftLeg = part(this.root, 1.0F, -5.0F, 0.0F);
        box(part(leftLeg, 1.65F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2618F), 16, 27, -2.0F, -2.5F, -2.0F, 4, 5, 4, 0.0F);
    }

    /**
     * {@code setupAnim(direction)}: the root turned to face the given way (its opposite's angle, as 1.21) and flipped
     * upside down; {@code flip} false leaves the flip to the caller (the item renderer's own {@code scale(-1, -1, 1)}).
     */
    void render(float facingOppositeDegrees, boolean flip, float scale) {
        this.root.rotationPointY = 0.0F;
        this.root.rotateAngleY = facingOppositeDegrees * (float) (Math.PI / 180.0D);
        this.root.rotateAngleZ = flip ? (float) Math.PI : 0.0F;
        this.root.render(scale);
    }
}
