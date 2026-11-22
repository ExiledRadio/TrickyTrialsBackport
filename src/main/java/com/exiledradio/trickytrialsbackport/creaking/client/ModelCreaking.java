package com.exiledradio.trickytrialsbackport.creaking.client;

import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.AnimatedPart;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.PartPose;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.21.11's {@code CreakingModel} (64x64): root, upper body (head, body, arms) and legs. The pose is worked out by
 * {@link CreakingPoser} (the head, the walk while it can move, and the attack, invulnerable and death keyframe
 * animations) and copied onto the boxes here.
 */
public final class ModelCreaking extends ModelBase {

    final AnimatedPart root;
    final AnimatedPart upperBody;
    final AnimatedPart head;
    final AnimatedPart body;
    final AnimatedPart rightArm;
    final AnimatedPart leftArm;
    final AnimatedPart leftLeg;
    final AnimatedPart rightLeg;
    private final Map<String, AnimatedPart> parts = new LinkedHashMap<String, AnimatedPart>();
    private final CreakingPoser poser = new CreakingPoser();

    public ModelCreaking() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.root = part("root");
        this.upperBody = part("upper_body");
        this.root.addChild(this.upperBody);

        this.head = part("head");
        this.head.setTextureOffset(0, 0).addBox(-3.0F, -10.0F, -3.0F, 6, 10, 6);
        this.head.setTextureOffset(28, 31).addBox(-3.0F, -13.0F, -3.0F, 6, 3, 6);
        this.head.setTextureOffset(12, 40).addBox(3.0F, -13.0F, 0.0F, 9, 14, 0);
        this.head.setTextureOffset(34, 12).addBox(-12.0F, -14.0F, 0.0F, 9, 14, 0);
        this.upperBody.addChild(this.head);

        this.body = part("body");
        this.body.setTextureOffset(0, 16).addBox(0.0F, -3.0F, -3.0F, 6, 13, 5);
        this.body.setTextureOffset(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6, 7, 5);
        this.upperBody.addChild(this.body);

        this.rightArm = part("right_arm");
        this.rightArm.setTextureOffset(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3, 21, 3);
        this.rightArm.setTextureOffset(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3, 4, 3);
        this.upperBody.addChild(this.rightArm);

        this.leftArm = part("left_arm");
        this.leftArm.setTextureOffset(30, 40).addBox(0.0F, -1.0F, -1.5F, 3, 16, 3);
        this.leftArm.setTextureOffset(52, 12).addBox(0.0F, -5.0F, -1.5F, 3, 4, 3);
        this.leftArm.setTextureOffset(52, 19).addBox(0.0F, 15.0F, -1.5F, 3, 4, 3);
        this.upperBody.addChild(this.leftArm);

        this.leftLeg = part("left_leg");
        this.leftLeg.setTextureOffset(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3, 16, 3);
        this.leftLeg.setTextureOffset(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5, 0, 9);
        this.root.addChild(this.leftLeg);

        this.rightLeg = part("right_leg");
        this.rightLeg.setTextureOffset(0, 34).addBox(-3.0F, -1.5F, -1.5F, 3, 19, 3);
        this.rightLeg.setTextureOffset(45, 46).addBox(-5.0F, 17.2F, -4.5F, 5, 0, 9);
        this.rightLeg.setTextureOffset(12, 34).addBox(-3.0F, -4.5F, -1.5F, 3, 3, 3);
        this.root.addChild(this.rightLeg);
    }

    /** A part at the poser's pivot for that name. */
    private AnimatedPart part(String name) {
        PartPose pose = this.poser.part(name);
        AnimatedPart p = new AnimatedPart(this, 0, 0).pose(pose.initX, pose.initY, pose.initZ);
        this.parts.put(name, p);
        return p;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (entity instanceof EntityCreaking) {
            EntityCreaking c = (EntityCreaking) entity;
            this.poser.pose(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, c.canMove(),
                    c.attackAnimationStart, c.invulnerabilityAnimationStart, c.deathAnimationStart);
        } else {
            this.poser.pose(0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch, false,
                    Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        for (Map.Entry<String, AnimatedPart> e : this.parts.entrySet()) {
            PartPose from = this.poser.part(e.getKey());
            AnimatedPart to = e.getValue();
            to.rotationPointX = from.x;
            to.rotationPointY = from.y;
            to.rotationPointZ = from.z;
            to.rotateAngleX = from.xRot;
            to.rotateAngleY = from.yRot;
            to.rotateAngleZ = from.zRot;
            to.xScale = from.xScale;
            to.yScale = from.yScale;
            to.zScale = from.zScale;
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.root.render(scale);
    }

    /** The eyes layer: the head alone ({@code createEyesLayer} keeps only the head), in the pose just set. */
    void renderHeadOnly(float scale) {
        AnimatedPart[] others = {this.body, this.rightArm, this.leftArm, this.leftLeg, this.rightLeg};
        for (AnimatedPart p : others) {
            p.skipDraw = true;
        }
        this.root.render(scale);
        for (AnimatedPart p : others) {
            p.skipDraw = false;
        }
    }
}
