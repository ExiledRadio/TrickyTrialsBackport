package com.exiledradio.trickytrialsbackport.coppergolem.client;

import com.exiledradio.trickytrialsbackport.coppergolem.EntityCopperGolem;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.AnimatedPart;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.AnimationDefinition;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.KeyframeAnimations;
import com.exiledradio.trickytrialsbackport.coppergolem.client.anim.Vec3f;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.11's {@code CopperGolemModel} (64x64): body with head and arms, two legs, and its animations
 * ({@link CopperGolemAnimations}): walking (with or without an item), the idle head spin and the four chest
 * interactions, each started where the entity's client tick started it.
 */
public final class ModelCopperGolem extends ModelBase {

    private static final float DEG = (float) (Math.PI / 180.0D);

    final AnimatedPart root;
    final AnimatedPart body;
    final AnimatedPart head;
    final AnimatedPart rightArm;
    final AnimatedPart leftArm;
    final AnimatedPart rightLeg;
    final AnimatedPart leftLeg;
    private final AnimatedPart[] all;
    private final Map<String, AnimatedPart> parts = new HashMap<String, AnimatedPart>();
    private final Vec3f cache = new Vec3f(0.0F, 0.0F, 0.0F);

    public ModelCopperGolem() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.root = part("root", 0.0F, 24.0F, 0.0F);
        this.body = part("body", 0.0F, -5.0F, 0.0F);
        this.body.setTextureOffset(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8, 6, 6);
        this.root.addChild(this.body);
        this.head = part("head", 0.0F, -6.0F, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -5.0F, -5.0F, 8, 5, 10, 0.015F);
        this.head.setTextureOffset(56, 0).addBox(-1.0F, -2.0F, -6.0F, 2, 3, 2);
        this.head.setTextureOffset(37, 8).addBox(-1.0F, -9.0F, -1.0F, 2, 4, 2, -0.015F);
        this.head.setTextureOffset(37, 0).addBox(-2.0F, -13.0F, -2.0F, 4, 4, 4, -0.015F);
        this.body.addChild(this.head);
        this.rightArm = part("right_arm", -4.0F, -6.0F, 0.0F);
        this.rightArm.setTextureOffset(36, 16).addBox(-3.0F, -1.0F, -2.0F, 3, 10, 4);
        this.body.addChild(this.rightArm);
        this.leftArm = part("left_arm", 4.0F, -6.0F, 0.0F);
        this.leftArm.setTextureOffset(50, 16).addBox(0.0F, -1.0F, -2.0F, 3, 10, 4);
        this.body.addChild(this.leftArm);
        this.rightLeg = part("right_leg", 0.0F, -5.0F, 0.0F);
        this.rightLeg.setTextureOffset(0, 27).addBox(-4.0F, 0.0F, -2.0F, 4, 5, 4);
        this.root.addChild(this.rightLeg);
        this.leftLeg = part("left_leg", 0.0F, -5.0F, 0.0F);
        this.leftLeg.setTextureOffset(16, 27).addBox(0.0F, 0.0F, -2.0F, 4, 5, 4);
        this.root.addChild(this.leftLeg);
        this.all = new AnimatedPart[] {this.root, this.body, this.head, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg};
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
        this.head.rotateAngleX = headPitch * DEG;
        this.head.rotateAngleY = netHeadYaw * DEG;
        if (!(entity instanceof EntityCopperGolem)) {
            return;
        }
        EntityCopperGolem golem = (EntityCopperGolem) entity;
        long walkTime = (long) (limbSwing * 50.0F * 2.0F);
        float walkScale = Math.min(limbSwingAmount * 2.5F, 1.0F);
        if (golem.getHeldItemMainhand().isEmpty() && golem.getHeldItemOffhand().isEmpty()) {
            KeyframeAnimations.animate(this.parts, CopperGolemAnimations.COPPER_GOLEM_WALK, walkTime, walkScale, this.cache);
        } else {
            KeyframeAnimations.animate(this.parts, CopperGolemAnimations.COPPER_GOLEM_WALK_ITEM, walkTime, walkScale, this.cache);
            poseHeldItemArmsIfStill();
        }
        apply(CopperGolemAnimations.COPPER_GOLEM_IDLE, golem.idleAnimationStart, ageInTicks);
        if (golem.interactionAnimationStart != Integer.MIN_VALUE) {
            switch (golem.interactionAnimationState) {
                case EntityCopperGolem.STATE_GETTING_ITEM:
                    apply(CopperGolemAnimations.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_GET, golem.interactionAnimationStart, ageInTicks);
                    break;
                case EntityCopperGolem.STATE_GETTING_NO_ITEM:
                    apply(CopperGolemAnimations.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_NOGET, golem.interactionAnimationStart, ageInTicks);
                    break;
                case EntityCopperGolem.STATE_DROPPING_ITEM:
                    apply(CopperGolemAnimations.COPPER_GOLEM_CHEST_INTERACTION_ITEM_DROP, golem.interactionAnimationStart, ageInTicks);
                    break;
                case EntityCopperGolem.STATE_DROPPING_NO_ITEM:
                    apply(CopperGolemAnimations.COPPER_GOLEM_CHEST_INTERACTION_ITEM_NODROP, golem.interactionAnimationStart, ageInTicks);
                    break;
                default:
                    break;
            }
        }
    }

    private void apply(AnimationDefinition def, int start, float ageInTicks) {
        if (start != Integer.MIN_VALUE) {
            KeyframeAnimations.animate(this.parts, def, (long) ((ageInTicks - start) * 50.0F), 1.0F, this.cache);
        }
    }

    private void poseHeldItemArmsIfStill() {
        this.rightArm.rotateAngleX = Math.min(this.rightArm.rotateAngleX, -0.87266463F);
        this.leftArm.rotateAngleX = Math.min(this.leftArm.rotateAngleX, -0.87266463F);
        this.rightArm.rotateAngleY = Math.min(this.rightArm.rotateAngleY, -0.1134464F);
        this.leftArm.rotateAngleY = Math.max(this.leftArm.rotateAngleY, 0.1134464F);
        this.rightArm.rotateAngleZ = Math.min(this.rightArm.rotateAngleZ, -0.064577185F);
        this.leftArm.rotateAngleZ = Math.max(this.leftArm.rotateAngleZ, 0.064577185F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.root.render(scale);
    }

    /** Draws again in the pose just set (the eyes layer). */
    void renderAgain(float scale) {
        this.root.render(scale);
    }

    /** {@code ModelPart.translateAndRotate} for a chain of parts, as {@link AnimatedPart#render} moves to each. */
    static void transform(float scale, AnimatedPart... chain) {
        for (AnimatedPart p : chain) {
            GlStateManager.translate(p.rotationPointX * scale, p.rotationPointY * scale, p.rotationPointZ * scale);
            if (p.rotateAngleZ != 0.0F) {
                GlStateManager.rotate(p.rotateAngleZ / DEG, 0.0F, 0.0F, 1.0F);
            }
            if (p.rotateAngleY != 0.0F) {
                GlStateManager.rotate(p.rotateAngleY / DEG, 0.0F, 1.0F, 0.0F);
            }
            if (p.rotateAngleX != 0.0F) {
                GlStateManager.rotate(p.rotateAngleX / DEG, 1.0F, 0.0F, 0.0F);
            }
            if (p.xScale != 1.0F || p.yScale != 1.0F || p.zScale != 1.0F) {
                GlStateManager.scale(p.xScale, p.yScale, p.zScale);
            }
        }
    }

    /** 1.21's {@code translateToHand}. */
    void translateToHand(EntityCopperGolem golem, boolean right, float scale) {
        transform(scale, this.root, this.body, right ? this.rightArm : this.leftArm);
        if (golem.getGolemState() == EntityCopperGolem.STATE_IDLE) {
            GlStateManager.rotate(right ? -90.0F : 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, 0.125F);
        } else {
            GlStateManager.scale(0.55F, 0.55F, 0.55F);
            GlStateManager.translate(-0.125F, 0.3125F, -0.1875F);
        }
    }

    /** 1.21's {@code applyBlockOnAntennaTransform}. */
    void applyBlockOnAntennaTransform(float scale) {
        transform(scale, this.root, this.body, this.head);
        GlStateManager.translate(0.0F, -2.25F, 0.0F);
    }
}
