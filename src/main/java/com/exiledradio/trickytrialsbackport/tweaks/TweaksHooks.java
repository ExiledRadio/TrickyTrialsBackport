package com.exiledradio.trickytrialsbackport.tweaks;

import com.exiledradio.trickytrialsbackport.ModConfig;

/**
 * Hooks the coremod's tweaks patches call (docs/PATCHES.md). Each keeps 1.12.2's behavior while {@code tweaks} is off.
 */
public final class TweaksHooks {

    private TweaksHooks() {
    }

    /**
     * Patch P4, {@code EntityMoveHelper}'s strafe where the side step is not walkable. 1.12.2 then walks forward at the
     * mob's full movement speed ({@code f1 = f}), four times its strafing speed: a strafing skeleton darts at its
     * target. Today's {@code MoveControl} walks forward at the strafing speed.
     *
     * @param full     1.12.2's speed there: the movement speed attribute
     * @param strafing the strafing speed already worked out: {@code speed * attribute}
     * @return the speed to walk forward at
     */
    public static float blockedStrafeSpeed(float full, float strafing) {
        return ModConfig.tweaks.enabled ? strafing : full;
    }
}
