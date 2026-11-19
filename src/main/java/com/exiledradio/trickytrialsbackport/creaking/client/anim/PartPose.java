package com.exiledradio.trickytrialsbackport.creaking.client.anim;

/**
 * One model part's pose as plain numbers: 1.21's {@code ModelPart} fields (pivot in pixels, rotations in radians,
 * scale) and its initial pose, which {@link #reset()} returns to. The model copies these onto its parts; tests read them.
 */
public final class PartPose implements Posable {

    public final float initX;
    public final float initY;
    public final float initZ;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;

    public PartPose(float x, float y, float z) {
        this.initX = x;
        this.initY = y;
        this.initZ = z;
        reset();
    }

    /** {@code ModelPart.resetPose}. */
    public void reset() {
        this.x = this.initX;
        this.y = this.initY;
        this.z = this.initZ;
        this.xRot = 0.0F;
        this.yRot = 0.0F;
        this.zRot = 0.0F;
        this.xScale = 1.0F;
        this.yScale = 1.0F;
        this.zScale = 1.0F;
    }

    @Override
    public void offsetPos(Vec3f v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
    }

    @Override
    public void offsetRotation(Vec3f v) {
        this.xRot += v.x;
        this.yRot += v.y;
        this.zRot += v.z;
    }

    @Override
    public void offsetScale(Vec3f v) {
        this.xScale += v.x;
        this.yScale += v.y;
        this.zScale += v.z;
    }
}
