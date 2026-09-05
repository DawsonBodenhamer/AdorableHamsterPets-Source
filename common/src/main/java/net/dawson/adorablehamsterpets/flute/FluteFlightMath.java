package net.dawson.adorablehamsterpets.flute;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Deterministic motion curve and orientation math for flute-assisted shoulder recall.
 */
public final class FluteFlightMath {

    private static final double EASING_STRENGTH = 3.0D;
    private static final double LINEAR_BLEND = 0.5D;

    public static double exponentialEaseOut(double progress) {
        double clampedProgress = MathHelper.clamp(progress, 0.0D, 1.0D);
        double exponentialProgress;
        if (clampedProgress <= 0.5D) {
            exponentialProgress = 0.5D * oneSidedEaseOut(clampedProgress * 2.0D);
        } else {
            exponentialProgress = 0.5D + 0.5D * oneSidedEaseIn((clampedProgress - 0.5D) * 2.0D);
        }
        return clampedProgress * LINEAR_BLEND
                + exponentialProgress * (1.0D - LINEAR_BLEND);
    }

    private static double oneSidedEaseOut(double progress) {
        return -Math.expm1(-EASING_STRENGTH * progress)
                / -Math.expm1(-EASING_STRENGTH);
    }

    private static double oneSidedEaseIn(double progress) {
        return Math.expm1(EASING_STRENGTH * progress)
                / Math.expm1(EASING_STRENGTH);
    }

    public static float pitchFromVelocity(double velocityX, double velocityY, double velocityZ) {
        double horizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        return (float) Math.atan2(velocityY, horizontalSpeed);
    }

    public static float yawFromDirection(double directionX, double directionZ) {
        return (float) Math.toDegrees(Math.atan2(-directionX, directionZ));
    }

    static Vec3d positionAlongArc(
            Vec3d start, Vec3d destination, double progress, double arcHeight) {
        double easedProgress = exponentialEaseOut(progress);
        return start.lerp(destination, easedProgress)
                .add(0.0D, Math.sin(Math.PI * easedProgress) * arcHeight, 0.0D);
    }

    static Vec3d directionAlongArc(
            Vec3d start,
            Vec3d destination,
            double progress,
            double sampleStep,
            double arcHeight) {
        double clampedProgress = MathHelper.clamp(progress, 0.0D, 1.0D);
        double previousProgress = Math.max(0.0D, clampedProgress - sampleStep);
        double nextProgress = Math.min(1.0D, clampedProgress + sampleStep);
        Vec3d previous = positionAlongArc(start, destination, previousProgress, arcHeight);
        Vec3d next = positionAlongArc(start, destination, nextProgress, arcHeight);
        return next.subtract(previous);
    }

    private FluteFlightMath() {}
}
