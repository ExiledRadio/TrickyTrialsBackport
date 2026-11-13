package com.exiledradio.trickytrialsbackport.creaking.client;

import com.exiledradio.trickytrialsbackport.creaking.EntityCreaking;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.AnimatedPart;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.AnimationDefinition;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.KeyframeAnimations;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.Vec3f;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.11's {@code CreakingModel} (64x64): root, upper body (head, body, arms) and legs, with the walk, attack,
 * invulnerable and death keyframe animations ({@link CreakingAnimations}). The walk plays only while the creaking
 * can move.
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
    private final AnimatedPart[] all;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();
    private final Vec3f cache = new Vec3f(0.0F, 0.0F, 0.0F);

    public ModelCreaking() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.root = part("root", 0.0F, 24.0F, 0.0F);
        this.upperBody = part("upper_body", -1.0F, -19.0F, 0.0F);
        this.root.addChild(this.upperBody);

        this.head = part("head", -3.0F, -11.0F, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-3.0F, -10.0F, -3.0F, 6, 10, 6);
        this.head.setTextureOffset(28, 31).addBox(-3.0F, -13.0F, -3.0F, 6, 3, 6);
        this.head.setTextureOffset(12, 40).addBox(3.0F, -13.0F, 0.0F, 9, 14, 0);
        this.head.setTextureOffset(34, 12).addBox(-12.0F, -14.0F, 0.0F, 9, 14, 0);
        this.upperBody.addChild(this.head);

        this.body = part("body", 0.0F, -7.0F, 1.0F);
        this.body.setTextureOffset(0, 16).addBox(0.0F, -3.0F, -3.0F, 6, 13, 5);
        this.body.setTextureOffset(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6, 7, 5);
        this.upperBody.addChild(this.body);

        this.rightArm = part("right_arm", -7.0F, -9.5F, 1.5F);
        this.rightArm.setTextureOffset(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3, 21, 3);
        this.rightArm.setTextureOffset(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3, 4, 3);
        this.upperBody.addChild(this.rightArm);

        this.leftArm = part("left_arm", 6.0F, -9.0F, 0.5F);
        this.leftArm.setTextureOffset(30, 40).addBox(0.0F, -1.0F, -1.5F, 3, 16, 3);
        this.leftArm.setTextureOffset(52, 12).addBox(0.0F, -5.0F, -1.5F, 3, 4, 3);
        this.leftArm.setTextureOffset(52, 19).addBox(0.0F, 15.0F, -1.5F, 3, 4, 3);
        this.upperBody.addChild(this.leftArm);

        this.leftLeg = part("left_leg", 1.5F, -16.0F, 0.5F);
        this.leftLeg.setTextureOffset(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3, 16, 3);
        this.leftLeg.setTextureOffset(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5, 0, 9);
        this.root.addChild(this.leftLeg);

        this.rightLeg = part("right_leg", -1.0F, -17.5F, 0.5F);
        this.rightLeg.setTextureOffset(0, 34).addBox(-3.0F, -1.5F, -1.5F, 3, 19, 3);
        this.rightLeg.setTextureOffset(45, 46).addBox(-5.0F, 17.2F, -4.5F, 5, 0, 9);
        this.rightLeg.setTextureOffset(12, 34).addBox(-3.0F, -4.5F, -1.5F, 3, 3, 3);
        this.root.addChild(this.rightLeg);

        this.all = new AnimatedPart[]{this.root, this.upperBody, this.head, this.body, this.rightArm, this.leftArm, this.leftLeg, this.rightLeg};
    }

    private AnimatedPart part(String name, float x, float y, float z) {
        AnimatedPart p = new AnimatedPart(this, 0, 0).pose(x, y, z);
        this.parts.put(name, p);
        return p;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        for (AnimatedPart p : this.all) {
            p.resetPose();
        }
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        if (!(entity instanceof EntityCreaking)) {
            return;
        }
        EntityCreaking c = (EntityCreaking) entity;
        if (c.canMove()) {
            // applyWalk(position, speed, 1, 1)
            KeyframeAnimations.animate(this.parts, CreakingAnimations.CREAKING_WALK, (long) (limbSwing * 50.0F),
                    Math.min(limbSwingAmount, 1.0F), this.cache);
        }
        apply(CreakingAnimations.CREAKING_ATTACK, c.attackAnimationStart, ageInTicks);
        apply(CreakingAnimations.CREAKING_INVULNERABLE, c.invulnerabilityAnimationStart, ageInTicks);
        apply(CreakingAnimations.CREAKING_DEATH, c.deathAnimationStart, ageInTicks);
    }

    private void apply(AnimationDefinition def, int start, float ageInTicks) {
        if (start != Integer.MIN_VALUE) {
            KeyframeAnimations.animate(this.parts, def, (long) ((ageInTicks - start) * 50.0F), 1.0F, this.cache);
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
