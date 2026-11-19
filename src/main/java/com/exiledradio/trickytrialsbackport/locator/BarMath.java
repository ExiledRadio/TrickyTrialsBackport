package com.exiledradio.trickytrialsbackport.locator;

import net.minecraft.util.math.MathHelper;

/**
 * The locator bar's drawing numbers as plain functions ({@code LocatorBarRenderer}, {@code TrackedWaypoint},
 * {@code WaypointStyle}, {@code GameRenderer.projectHorizonToScreen}), so the headless self-test checks the same code
 * the client draws with. Angles in degrees unless named otherwise.
 */
public final class BarMath {

    /** {@code LocatorBarRenderer.VISIBLE_DEGREE_RANGE}: dots are shown within 60 degrees either side. */
    public static final int VISIBLE_DEGREES = 60;
    /** {@code WaypointStyle.DEFAULT_NEAR_DISTANCE} and {@code DEFAULT_FAR_DISTANCE}. */
    public static final int NEAR = 128;
    public static final int FAR = 332;
    /** The default style's sprites, {@code default_0} to {@code default_3}. */
    public static final int SPRITES = 4;

    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;

    private BarMath() {
    }

    /**
     * {@code Vec3iWaypoint} / {@code ChunkWaypoint.yawAngleToCamera}: the angle from the camera's look to the point.
     *
     * @return degrees, -180 to 180; negative is to the left
     */
    public static double yawToPoint(double camX, double camZ, float camYaw, double x, double z) {
        double dx = camX - x;
        double dz = camZ - z;
        // (camera - point).rotateClockwise90() = (-dz, dy, dx); atan2 of z over x.
        float angle = (float) MathHelper.atan2(dx, -dz) * (180.0F / (float) Math.PI);
        return MathHelper.wrapDegrees(angle - MathHelper.wrapDegrees(camYaw));
    }

    /** {@code AzimuthWaypoint.yawAngleToCamera}. */
    public static double yawToAzimuth(float camYaw, float radians) {
        return MathHelper.wrapDegrees(radians * (180.0F / (float) Math.PI) - MathHelper.wrapDegrees(camYaw));
    }

    /** @return true when a dot at this angle is drawn */
    public static boolean visible(double yaw) {
        return !Double.isNaN(yaw) && yaw > -VISIBLE_DEGREES && yaw <= VISIBLE_DEGREES;
    }

    /** The dot's x: {@code ceil((guiWidth - 9) / 2) + floor(yaw * 173 / 2 / 60)}. */
    public static int dotX(int guiWidth, double yaw) {
        return MathHelper.ceil((guiWidth - 9) / 2.0F) + MathHelper.floor(yaw * 173.0D / 2.0D / VISIBLE_DEGREES);
    }

    /** {@code WaypointStyle.sprite} for the default style: 0 nearer than 128, 3 from 332, 1 or 2 between. */
    public static int sprite(float distance) {
        if (distance < NEAR) {
            return 0;
        }
        if (distance >= FAR) {
            return SPRITES - 1;
        }
        return 1 + MathHelper.floor((distance - NEAR) / (float) (FAR - NEAR) * (SPRITES - 2));
    }

    /** {@code GameRenderer.projectHorizonToScreen}: tan(pitch) / tan(fov / 2). */
    public static double horizon(float pitch, float fov) {
        if (pitch <= -90.0F) {
            return Double.NEGATIVE_INFINITY;
        }
        if (pitch >= 90.0F) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.tan(pitch * (Math.PI / 180.0D)) / Math.tan(fov / 2.0F * (Math.PI / 180.0D));
    }

    /** The chunk and angle waypoints' arrow: down when the horizon is below the screen's bottom edge, up above the top. */
    public static int pitchFromHorizon(double horizon) {
        if (horizon < -1.0D) {
            return DOWN;
        }
        return horizon > 1.0D ? UP : NONE;
    }

    /**
     * {@code Vec3iWaypoint.pitchDirectionToCamera}: the point projected to the screen ({@code projectPointToScreen});
     * off the top or bottom edge an arrow points to it, and behind the camera it always points up or down.
     *
     * @param dx, dy, dz the point minus the camera
     */
    public static int pitchToPoint(double dx, double dy, double dz, float camYaw, float camPitch, float fov) {
        double yaw = camYaw * (Math.PI / 180.0D);
        double pitch = camPitch * (Math.PI / 180.0D);
        // The look and the screen's up, 1.12.2's getVectorForRotation(pitch, yaw) and (pitch - 90, yaw).
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double ux = -Math.sin(yaw) * Math.sin(pitch);
        double uy = Math.cos(pitch);
        double uz = Math.cos(yaw) * Math.sin(pitch);
        double depth = dx * fx + dy * fy + dz * fz;
        double up = dx * ux + dy * uy + dz * uz;
        double y = up / (depth * Math.tan(fov / 2.0F * (Math.PI / 180.0D)));
        boolean behind = depth < 0.0D;
        double edge = behind ? -y : y;
        if (edge < -1.0D) {
            return DOWN;
        }
        if (edge > 1.0D) {
            return UP;
        }
        if (behind) {
            if (y > 0.0D) {
                return UP;
            }
            if (y < 0.0D) {
                return DOWN;
            }
        }
        return NONE;
    }
}
