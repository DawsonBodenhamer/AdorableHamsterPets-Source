package net.dawson.adorablehamsterpets.client.particle;

import net.minecraft.util.math.MathHelper;

/**
 * Shared visual entrance animation for particles that grow into view.
 *
 * <p>Callers multiply their stable base scale by {@link #growInScale(int)} and assign
 * {@link #fadeInOpacity(int)} directly to particle opacity. Both curves use particle age, so
 * related effects begin together without sharing mutable animation state.</p>
 */
final class ParticleAnimationUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int SPAWN_ANIMATION_TICKS = 20;           // Time to reach full size
    private static final int FADE_IN_TICKS = 10;                   // Time to reach full opacity
    private static final float SPAWN_OVERSHOOT = 0.001F;           // Higher = more pronounced
    private static final float MIN_START_SCALE = 0.8F;             // 1.0 = full size
    private static final float SETTLE_START = 0.02F;               // 0 = immediate, 0.5 = halfway through intro anim
    private static final float SETTLE_SPAN = 0.4F - SETTLE_START;  // Fraction of intro anim

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    static float growInScale(int age) {
        float progress = MathHelper.clamp(age / (float) SPAWN_ANIMATION_TICKS, 0.0F, 1.0F);
        float minimumStartScale = MathHelper.clamp(MIN_START_SCALE, 0.0F, 1.0F);
        if (progress <= 0.0F) {
            return minimumStartScale;
        }
        if (progress >= 1.0F) {
            return 1.0F;
        }

        // Start gently, accelerate through middle
        float easedProgress = exponentialEaseInOut(progress);

        // Small damped overshoot near end
        float settleProgress = MathHelper.clamp(
                (progress - SETTLE_START) / SETTLE_SPAN,
                0.0F,
                1.0F);
        float overshoot = SPAWN_OVERSHOOT
                * (float) Math.sin(settleProgress * Math.PI)
                * (float) Math.exp(-1.6D * settleProgress);
        float animatedScale = Math.max(0.0F, easedProgress + overshoot);
        return minimumStartScale + (1.0F - minimumStartScale) * animatedScale;
    }

    static float interpolateScale(float previousScale, float currentScale, float tickDelta) {
        // Render between the previous and current tick samples
        float clampedTickDelta = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        return MathHelper.lerp(clampedTickDelta, previousScale, currentScale);
    }

    static float fadeInOpacity(int age) {
        // Invisible at age zero, full opacity at 5 ticks
        return MathHelper.clamp(age / (float) FADE_IN_TICKS, 0.0F, 1.0F);
    }

    private static float exponentialEaseInOut(float progress) {
        if (progress <= 0.0F || progress >= 1.0F) {
            return progress;
        }

        // Mirrored exponential halves so both ends settle without hard slope change
        return progress < 0.5F
                ? 0.5F * (float) Math.exp(20.0D * progress - 10.0D)
                : 1.0F - 0.5F * (float) Math.exp(-20.0D * progress + 10.0D);
    }

    private ParticleAnimationUtil() {
    }
}
