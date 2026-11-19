package com.exiledradio.trickytrialsbackport.nautilus.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * 1.21.11's {@code NautilusModel} and its four relatives, one class with a mesh per {@link Mesh}: the adult (128 x 128),
 * the baby (64 x 64), the armor layer (the shell 0.01 larger), the saddle layer (the shell's top 0.2 larger, nothing
 * else of the shell) and the zombie nautilus's coral-grown shell ({@code ZombieNautilusCoralModel}).
 *
 * <p>Animation: the head ({@code body}) turns with the look, up to 10 degrees each way, and {@code NautilusAnimation
 * .SWIMMING} plays as a walk animation (time {@code (limbSwing + age / 5) x 100} ms, strength {@code min((swing amount +
 * 0.2) x 3, 1)}). Its scale channels need 1.21's part scale, which 1.12.2's {@link ModelRenderer} lacks: {@link Part}
 * adds it.
 */
public class ModelNautilus extends ModelBase {

    public enum Mesh {
        ADULT, BABY, ARMOR, SADDLE, CORAL
    }

    private static final float DEG = (float) (Math.PI / 180.0D);

    final Part root;
    final Part shell;
    final Part body;
    final Part upperMouth;
    final Part innerMouth;
    final Part lowerMouth;
    /** The coral mesh's corals, hidden when the zombie nautilus wears armor. */
    Part corals;

    public ModelNautilus(Mesh mesh) {
        boolean baby = mesh == Mesh.BABY;
        this.textureWidth = baby ? 64 : 128;
        this.textureHeight = baby ? 64 : 128;
        if (baby) {
            this.root = part(null, -0.5F, 28.0F, -0.5F);
            this.shell = part(this.root, 3.0F, -8.0F, -2.0F);
            box(this.shell, 0, 0, -6.0F, -4.0F, -1.0F, 7, 4, 7, 0.0F);
            box(this.shell, 0, 11, -6.0F, 0.0F, -1.0F, 7, 4, 9, 0.0F);
            box(this.shell, 23, 11, -6.0F, 0.0F, 5.0F, 7, 4, 0, 0.0F);
            this.body = part(this.root, 0.5F, -5.0F, 3.0F);
            box(this.body, 0, 24, -2.5F, -3.01F, -1.0F, 5, 4, 7, 0.0F);
            box(this.body, 0, 35, -2.5F, -3.01F, 4.1F, 5, 4, 0, 0.0F);
            this.upperMouth = part(this.body, 0.0F, -2.01F, 3.9F);
            box(this.upperMouth, 24, 24, -2.5F, -1.0F, 0.0F, 5, 2, 2, -0.001F);
            this.innerMouth = part(this.body, 0.0F, -1.01F, 4.9F);
            box(this.innerMouth, 24, 32, -1.5F, -1.0F, -1.0F, 3, 2, 2, 0.0F);
            this.lowerMouth = part(this.body, 0.0F, -0.01F, 3.9F);
            box(this.lowerMouth, 24, 28, -2.5F, -1.0F, 0.0F, 5, 2, 2, -0.001F);
            return;
        }
        this.root = part(null, 0.0F, 29.0F, -6.0F);
        this.shell = part(this.root, 0.0F, -13.0F, 5.0F);
        if (mesh == Mesh.SADDLE) {
            box(this.shell, 0, 0, -7.0F, -10.0F, -7.0F, 14, 10, 16, 0.2F);
        } else {
            float grow = mesh == Mesh.ARMOR ? 0.01F : 0.0F;
            box(this.shell, 0, 0, -7.0F, -10.0F, -7.0F, 14, 10, 16, grow);
            box(this.shell, 0, 26, -7.0F, 0.0F, -7.0F, 14, 8, 20, grow);
            box(this.shell, 48, 26, -7.0F, 0.0F, 6.0F, 14, 8, 0, 0.0F);
        }
        this.body = part(this.root, 0.0F, -8.5F, 12.3F);
        box(this.body, 0, 54, -5.0F, -4.51F, -3.0F, 10, 8, 14, 0.0F);
        box(this.body, 0, 76, -5.0F, -4.51F, 7.0F, 10, 8, 0, 0.0F);
        this.upperMouth = part(this.body, 0.0F, -2.51F, 7.0F);
        box(this.upperMouth, 54, 54, -5.0F, -2.0F, 0.0F, 10, 4, 4, -0.001F);
        this.innerMouth = part(this.body, 0.0F, -0.51F, 7.5F);
        box(this.innerMouth, 54, 70, -3.0F, -2.0F, -0.5F, 6, 4, 4, 0.0F);
        this.lowerMouth = part(this.body, 0.0F, 1.49F, 7.0F);
        box(this.lowerMouth, 54, 62, -5.0F, -1.98F, 0.0F, 10, 4, 4, -0.001F);
        if (mesh == Mesh.CORAL) {
            this.corals = part(this.shell, 8.0F, 4.5F, -8.0F);
            Part yellow = part(this.corals, 0.0F, -11.0F, 11.0F);
            box(turned(part(yellow, 0.0F, 0.0F, 2.0F), 0.0F, -0.7854F, 0.0F), 0, 85, -4.5F, -3.5F, 0.0F, 6, 8, 0, 0.0F);
            box(turned(part(yellow, 0.0F, 0.0F, 0.0F), 0.0F, 0.7854F, 0.0F), 0, 85, -4.5F, -3.5F, 0.0F, 6, 8, 0, 0.0F);
            Part pink = part(this.corals, -12.5F, -18.0F, 11.0F);
            box(pink, -8, 94, -4.5F, 4.5F, 0.0F, 6, 0, 8, 0.0F);
            box(turned(part(pink, -1.5F, 4.5F, 4.0F), 0.0F, 0.0F, 1.5708F), -8, 94, -3.0F, 0.0F, -4.0F, 6, 0, 8, 0.0F);
            Part blue = part(this.corals, -14.0F, 0.0F, 5.5F);
            box(turned(part(blue, 0.0F, 0.0F, -2.0F), 0.0F, 0.7854F, 0.0F), 0, 102, -3.5F, -5.5F, 0.0F, 5, 10, 0, 0.0F);
            box(turned(part(blue, 0.0F, 0.0F, 0.0F), 0.0F, -0.7854F, 0.0F), 0, 102, -3.5F, -5.5F, 0.0F, 5, 10, 0, 0.0F);
            Part red = part(this.corals, 0.0F, 0.0F, 0.0F);
            box(turned(part(red, -0.5F, -1.0F, 1.5F), 0.0F, -0.829F, 0.0F), 0, 112, -2.5F, -5.5F, 0.0F, 4, 10, 0, 0.0F);
            box(turned(part(red, 0.0F, 0.0F, 0.0F), 0.0F, 0.7854F, 0.0F), 0, 112, -4.5F, -5.5F, 0.0F, 6, 10, 0, 0.0F);
        }
    }

    private Part part(Part parent, float x, float y, float z) {
        Part p = new Part(this);
        p.setRotationPoint(x, y, z);
        if (parent != null) {
            parent.addChild(p);
        }
        return p;
    }

    private static Part turned(Part p, float x, float y, float z) {
        p.rotateAngleX = x;
        p.rotateAngleY = y;
        p.rotateAngleZ = z;
        return p;
    }

    private static void box(Part p, int u, int v, float x, float y, float z, int w, int h, int d, float grow) {
        p.cubeList.add(new ModelBox(p, u, v, x, y, z, w, h, d, grow, false));
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.root.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
            Entity entity) {
        for (Part p : new Part[] {this.body, this.upperMouth, this.innerMouth, this.lowerMouth}) {
            p.rotateAngleX = 0.0F;
            p.rotateAngleY = 0.0F;
            p.rotateAngleZ = 0.0F;
            p.scaleX = 1.0F;
            p.scaleY = 1.0F;
            p.scaleZ = 1.0F;
        }
        this.body.rotateAngleY = MathHelper.clamp(netHeadYaw, -10.0F, 10.0F) * DEG;
        this.body.rotateAngleX = MathHelper.clamp(headPitch, -10.0F, 10.0F) * DEG;
        long millis = (long) ((limbSwing + ageInTicks / 5.0F) * 50.0F * 2.0F);
        float strength = Math.min((limbSwingAmount + 0.2F) * 3.0F, 1.0F);
        swim(millis, strength);
    }

    // NautilusAnimation.SWIMMING: one second, looping, linear between keyframes at 0, 0.5, 0.75, 0.875 and 1.
    private static final float[] TIMES = {0.0F, 0.5F, 0.75F, 0.875F, 1.0F};
    private static final float[] BODY_SCALE_Z = {0.0F, 0.2F, -0.1F, 0.0F, 0.0F};
    private static final float[] UPPER_ROT_X = {0.0F, 30.0F, 0.0F, 0.0F, 0.0F};
    private static final float[] LOWER_ROT_X = {0.0F, -30.0F, 0.0F, 0.0F, 0.0F};
    private static final float[] MOUTH_SCALE_Z = {0.0F, 0.4F, -0.1F, 0.0F, 0.0F};
    private static final float[] INNER_SCALE_XY = {0.0F, -0.2F, 0.0F, 0.0F, 0.0F};
    private static final float[] INNER_SCALE_Z = {0.0F, 0.0F, -0.1F, 0.0F, 0.0F};

    void swim(long millis, float strength) {
        float t = (millis / 1000.0F) % 1.0F;
        this.body.scaleZ += sample(BODY_SCALE_Z, t) * strength;
        this.upperMouth.rotateAngleX += sample(UPPER_ROT_X, t) * DEG * strength;
        this.upperMouth.scaleZ += sample(MOUTH_SCALE_Z, t) * strength;
        float inner = sample(INNER_SCALE_XY, t) * strength;
        this.innerMouth.scaleX += inner;
        this.innerMouth.scaleY += inner;
        this.innerMouth.scaleZ += sample(INNER_SCALE_Z, t) * strength;
        this.lowerMouth.rotateAngleX += sample(LOWER_ROT_X, t) * DEG * strength;
        this.lowerMouth.scaleZ += sample(MOUTH_SCALE_Z, t) * strength;
    }

    /** Linear interpolation of a channel at a time in [0, 1). */
    static float sample(float[] values, float t) {
        for (int i = 1; i < TIMES.length; i++) {
            if (t <= TIMES[i]) {
                float a = (t - TIMES[i - 1]) / (TIMES[i] - TIMES[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * a;
            }
        }
        return values[values.length - 1];
    }

    /** A {@link ModelRenderer} with 1.21's part scale, applied after its rotation, to its boxes and children. */
    static final class Part extends ModelRenderer {
        float scaleX = 1.0F;
        float scaleY = 1.0F;
        float scaleZ = 1.0F;

        Part(ModelBase model) {
            super(model);
        }

        @Override
        public void render(float scale) {
            if (this.isHidden || !this.showModel) {
                return;
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.offsetX, this.offsetY, this.offsetZ);
            GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
            if (this.rotateAngleZ != 0.0F) {
                GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
            }
            if (this.rotateAngleY != 0.0F) {
                GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
            }
            if (this.rotateAngleX != 0.0F) {
                GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
            }
            if (this.scaleX != 1.0F || this.scaleY != 1.0F || this.scaleZ != 1.0F) {
                GlStateManager.scale(this.scaleX, this.scaleY, this.scaleZ);
            }
            for (ModelBox box : this.cubeList) {
                box.render(Tessellator.getInstance().getBuffer(), scale);
            }
            if (this.childModels != null) {
                for (ModelRenderer child : this.childModels) {
                    child.render(scale);
                }
            }
            GlStateManager.popMatrix();
        }
    }
}
