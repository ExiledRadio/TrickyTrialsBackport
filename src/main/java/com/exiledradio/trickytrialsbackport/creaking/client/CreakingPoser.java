package com.exiledradio.trickytrialsbackport.creaking.client;

import com.exiledradio.trickytrialsbackport.creaking.client.anim.AnimationDefinition;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.KeyframeAnimations;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.PartPose;
import com.exiledradio.trickytrialsbackport.creaking.client.anim.Vec3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.21.11's {@code CreakingModel.setupAnim} as plain math: the eight parts' poses (pivots from
 * {@code createMesh}) for a render state. {@link ModelCreaking} copies the result onto its boxes; the self-test
 * samples it on a server, since it names no client type.
 */
public final class CreakingPoser {

    public final Map<String, PartPose> parts = new LinkedHashMap<String, PartPose>();
    private final Vec3f cache = new Vec3f(0.0F, 0.0F, 0.0F);

    public CreakingPoser() {
        this.parts.put("root", new PartPose(0.0F, 24.0F, 0.0F));
        this.parts.put("upper_body", new PartPose(-1.0F, -19.0F, 0.0F));
        this.parts.put("head", new PartPose(-3.0F, -11.0F, 0.0F));
        this.parts.put("body", new PartPose(0.0F, -7.0F, 1.0F));
        this.parts.put("right_arm", new PartPose(-7.0F, -9.5F, 1.5F));
        this.parts.put("left_arm", new PartPose(6.0F, -9.0F, 0.5F));
        this.parts.put("left_leg", new PartPose(1.5F, -16.0F, 0.5F));
        this.parts.put("right_leg", new PartPose(-1.0F, -17.5F, 0.5F));
    }

    public PartPose part(String name) {
        return this.parts.get(name);
    }

    /**
     * {@code setupAnim}: reset every part, turn the head, then the walk (only while it can move, by the walk
     * position and speed) and the attack, invulnerable and death animations (each from the tick it started, or
     * {@link Integer#MIN_VALUE} when stopped).
     *
     * @param walkPos 1.21's {@code walkAnimationPos} (1.12.2's interpolated {@code limbSwing})
     * @param walkSpeed 1.21's {@code walkAnimationSpeed} (1.12.2's interpolated {@code limbSwingAmount}, at most 1)
     */
    public void pose(float walkPos, float walkSpeed, float ageInTicks, float headYaw, float headPitch, boolean canMove,
                     int attackStart, int invulnerableStart, int deathStart) {
        for (PartPose p : this.parts.values()) {
            p.reset();
        }
        PartPose head = this.parts.get("head");
        head.xRot = headPitch * (float) (Math.PI / 180.0D);
        head.yRot = headYaw * (float) (Math.PI / 180.0D);
        if (canMove) {
            // KeyframeAnimation.applyWalk(pos, speed, 1, 1)
            KeyframeAnimations.animate(this.parts, CreakingAnimations.CREAKING_WALK, (long) (walkPos * 50.0F * 1.0F),
                    Math.min(walkSpeed * 1.0F, 1.0F), this.cache);
        }
        apply(CreakingAnimations.CREAKING_ATTACK, attackStart, ageInTicks);
        apply(CreakingAnimations.CREAKING_INVULNERABLE, invulnerableStart, ageInTicks);
        apply(CreakingAnimations.CREAKING_DEATH, deathStart, ageInTicks);
    }

    /** {@code KeyframeAnimation.apply(AnimationState, ageInTicks)}: the time since it started, at 50 ms a tick. */
    private void apply(AnimationDefinition def, int start, float ageInTicks) {
        if (start != Integer.MIN_VALUE) {
            KeyframeAnimations.animate(this.parts, def, (long) ((ageInTicks - start) * 50.0F), 1.0F, this.cache);
        }
    }
}
