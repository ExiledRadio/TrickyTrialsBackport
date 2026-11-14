package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.asm.StrafeSpeedTransformer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Stream 25 (tweaks): patch P4, a strafing mob that cannot step aside walks on at its strafing speed (1.21.11's MoveControl). */
public final class TweaksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "tweaks";
    }

    @Override
    public void run(Check c) {
        c.ok(StrafeSpeedTransformer.applied, "patch P4 applied to EntityMoveHelper");
        BlockPos area = c.area();
        // A skeleton against a wall on its +x side (yaw 0: the strafe's forward part is an x step in 1.12.2's check).
        BlockPos at = area.add(8, 0, 8);
        for (int y = 0; y < 3; y++) {
            for (int z = -2; z <= 2; z++) {
                c.world.setBlockState(at.add(1, y, z), Blocks.STONE.getDefaultState(), 2);
            }
        }
        EntitySkeleton skeleton = new EntitySkeleton(c.world);
        skeleton.setLocationAndAngles(at.getX() + 0.98, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        c.world.spawnEntity(skeleton);
        float attribute = (float) skeleton.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        c.ok(Math.abs(attribute - 0.25F) < 1e-6F, "skeleton movement speed 0.25 (1.21.11's too): " + attribute);

        boolean was = ModConfig.tweaks.enabled;
        try {
            ModConfig.tweaks.enabled = true;
            float blocked = strafe(skeleton, 0.5F, 0.5F);
            c.ok(Math.abs(blocked - 0.25F * 0.25F) < 1e-6F, "blocked strafe walks on at 0.25 x 0.25 = 0.0625, as 1.21.11: " + blocked);
            c.eq(1.0F, skeleton.moveForward, "and walks forward, as both versions");
            ModConfig.tweaks.enabled = false;
            float old = strafe(skeleton, 0.5F, 0.5F);
            c.ok(Math.abs(old - 0.25F) < 1e-6F, "tweaks off: 1.12.2's full speed, 0.25: " + old);
            ModConfig.tweaks.enabled = true;
            skeleton.setLocationAndAngles(at.getX() - 3.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            float open = strafe(skeleton, 0.5F, 0.5F);
            c.ok(Math.abs(open - 0.25F * 0.25F) < 1e-6F, "an open strafe is 0.0625 in both versions: " + open);
        } finally {
            ModConfig.tweaks.enabled = was;
            skeleton.setDead();
        }
    }

    private static float strafe(EntitySkeleton skeleton, float forward, float right) {
        skeleton.getMoveHelper().strafe(forward, right);
        skeleton.getMoveHelper().onUpdateMoveHelper();
        return skeleton.getAIMoveSpeed();
    }
}
