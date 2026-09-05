package net.dawson.adorablehamsterpets.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Reusable localized gust response for particles with floaty motion.
 *
 * <p>The target supplies velocity access and receives each gust impulse in order. A new gust
 * callback runs before this motion state samples its per-particle coupling and delay, allowing a
 * particle to preserve any event side effects such as ambient sound. The returned strength is
 * suitable for a particle-specific horizontal speed cap.</p>
 */
public final class FloatyParticleMotion {

    // --- Gust Model Constants ---
    private static final int GUST_WINDOW_TICKS = 100;
    private static final float GUST_PROB_PER_WIN = 0.35f;
    private static final int GUST_MIN_LEN = 20;
    private static final int GUST_MAX_LEN = 40;
    private static final int GUST_CELL_BLOCKS = 12;
    private static final float GUST_UP_ACCEL = 0.025f;
    private static final float GUST_HORIZ_ACCEL = 0.06f;
    private static final float GUST_SPIN_IMPULSE = 2.0f;
    private static final float GUST_SPIN_DAMP = 0.85f;
    private static final float COUPLING_MIN = 0.12f;
    private static final float COUPLING_SPAN = 0.78f;
    private static final int PER_PARTICLE_DELAY_MAX = 8;
    private static final float WIND_DRAG = 0.10f;
    private static final float WIND_TARGET_SPEED = 0.09f;
    private static final float GUST_RISE_FRAC = 0.30f;

    // --- Per-Particle State ---
    private long lastAppliedGustKey = Long.MIN_VALUE;
    private float gustSpinVelocity;
    private float gustCoupling;
    private int gustDelayTicks;

    /**
     * Applies the active gust at the target's current position and returns its local strength.
     *
     * <p>Call this after any particle-specific drift or sway acceleration and before applying a
     * particle-specific horizontal cap. The target callback and all velocity changes preserve
     * the gust's original event, coupling, and force ordering.</p>
     */
    public float applyGust(ClientWorld world, double x, double y, double z,
                           float driftDirX, float driftDirZ, Target target) {
        return applyGust(world, world.getTime(), x, y, z, driftDirX, driftDirZ, target);
    }

    /**
     * Applies the active gust using a tick value already sampled by the owning particle.
     *
     * <p>Use this overload when the owner performs other per-tick bookkeeping against the same
     * world time, so gust-start side effects use that exact timestamp.</p>
     */
    public float applyGust(ClientWorld world, long worldTime, double x, double y, double z,
                           float driftDirX, float driftDirZ, Target target) {
        Gust gust = sampleGust(world, x, z);
        if (!gust.active) {
            return 0f;
        }

        // Initialize gust response for this particle if it's a new gust event.
        if (gust.key != this.lastAppliedGustKey) {
            target.onGustStarted(world, x, y, z, gust.key, worldTime);

            Random particleRandom = new Random(mix64(gust.key, System.identityHashCode(target)));
            this.gustCoupling = COUPLING_MIN + particleRandom.nextFloat() * COUPLING_SPAN;
            this.gustCoupling = (float) Math.sqrt(this.gustCoupling);
            this.gustDelayTicks = particleRandom.nextInt(PER_PARTICLE_DELAY_MAX + 1);
            this.lastAppliedGustKey = gust.key;
        }

        int delayedTicksSinceGustStart = Math.max(0, gust.ticksSinceGustStart - this.gustDelayTicks);
        float gustProgress = delayedTicksSinceGustStart / (float) gust.gustDurationTicks;
        float rise = smooth01(Math.min(1f, gustProgress / GUST_RISE_FRAC));
        float decay = (float) Math.exp(-3.0f * gustProgress);
        float gustStrength = rise * decay * this.gustCoupling;

        // Apply one-time spin impulse when particle first feels gust.
        if (delayedTicksSinceGustStart == 0 && gust.ticksSinceGustStart >= this.gustDelayTicks) {
            this.gustSpinVelocity += GUST_SPIN_IMPULSE * this.gustCoupling;
        }

        // Interpolate gust angle.
        float easeOutFactor = 1.0f - (float) Math.pow(1.0f - gustProgress, 3.0);
        float finalGustDirX = MathHelper.lerp(easeOutFactor, driftDirX, gust.gustDirX);
        float finalGustDirZ = MathHelper.lerp(easeOutFactor, driftDirZ, gust.gustDirZ);

        // Apply drag along interpolated wind angle toward wind's target speed.
        float along = (float) (target.velocityX() * finalGustDirX + target.velocityZ() * finalGustDirZ);
        float targetSpeed = WIND_TARGET_SPEED * gustStrength;
        float correction = (targetSpeed - along) * WIND_DRAG;
        target.addVelocityX(finalGustDirX * correction);
        target.addVelocityZ(finalGustDirZ * correction);

        // Apply upward and lateral push from gust using interpolated direction.
        target.addVelocityY(gustStrength * GUST_UP_ACCEL);
        target.addVelocityX(finalGustDirX * gustStrength * GUST_HORIZ_ACCEL);
        target.addVelocityZ(finalGustDirZ * gustStrength * GUST_HORIZ_ACCEL);

        return gustStrength;
    }

    /**
     * Applies a cap after {@link #applyGust}, allowing each floaty particle to choose its own
     * baseline and gust allowance.
     */
    public void capHorizontalVelocity(Target target, float baseCap, float gustAllowance, float gustStrength) {
        float dynamicHorizontalCap = baseCap + gustStrength * gustAllowance;
        float horizontalSpeedSquared = (float) (target.velocityX() * target.velocityX()
                + target.velocityZ() * target.velocityZ());
        if (horizontalSpeedSquared > dynamicHorizontalCap * dynamicHorizontalCap) {
            float scale = dynamicHorizontalCap / MathHelper.sqrt(horizontalSpeedSquared);
            target.multiplyVelocityX(scale);
            target.multiplyVelocityZ(scale);
        }
    }

    /**
     * Damps and returns accumulated gust spin for the particle's rotation phase.
     */
    public float dampAndGetSpinVelocity() {
        this.gustSpinVelocity *= GUST_SPIN_DAMP;
        return this.gustSpinVelocity;
    }

    /**
     * Mutable particle surface used by the gust component without coupling it to one particle
     * superclass or changing how a particle stores its velocity.
     */
    public interface Target {
        double velocityX();

        double velocityZ();

        void addVelocityX(double amount);

        void addVelocityY(double amount);

        void addVelocityZ(double amount);

        void multiplyVelocityX(float factor);

        void multiplyVelocityZ(float factor);

        default void onGustStarted(ClientWorld world, double x, double y, double z,
                                    long gustKey, long worldTime) {
        }
    }

    // --- Deterministic Wind Model ---
    private record Gust(boolean active, long key, int ticksSinceGustStart, int gustDurationTicks,
                        float gustDirX, float gustDirZ) {
    }

    /**
     * Samples a gust deterministically from world time and spatial cell.
     */
    private static Gust sampleGust(ClientWorld world, double x, double z) {
        long worldTime = world.getTime();
        long gustWindowIndex = Math.floorDiv(worldTime, GUST_WINDOW_TICKS);

        int cellX = MathHelper.floor((float) x) / GUST_CELL_BLOCKS;
        int cellZ = MathHelper.floor((float) z) / GUST_CELL_BLOCKS;

        long baseSeed = mix64(cellX, cellZ, gustWindowIndex);
        Random random = new Random(baseSeed);

        if (random.nextFloat() >= GUST_PROB_PER_WIN) {
            return new Gust(false, baseSeed, 0, 0, 0f, 0f);
        }

        int tickInCurrentWindow = (int) (worldTime % GUST_WINDOW_TICKS);
        int maxStartTickInWindow = Math.max(1, GUST_WINDOW_TICKS - GUST_MAX_LEN - 1);
        int gustStartTick = 1 + random.nextInt(maxStartTickInWindow);
        int gustDurationTicks = GUST_MIN_LEN + random.nextInt(GUST_MAX_LEN - GUST_MIN_LEN + 1);

        if (tickInCurrentWindow < gustStartTick || tickInCurrentWindow > gustStartTick + gustDurationTicks) {
            long key = baseSeed ^ gustStartTick;
            return new Gust(false, key, 0, gustDurationTicks, 0f, 0f);
        }

        float theta = random.nextFloat() * MathHelper.TAU;
        float gustDirX = MathHelper.cos(theta);
        float gustDirZ = MathHelper.sin(theta);
        long key = baseSeed ^ gustStartTick;
        int ticksSinceGustStart = tickInCurrentWindow - gustStartTick;

        return new Gust(true, key, ticksSinceGustStart, gustDurationTicks, gustDirX, gustDirZ);
    }

    /**
     * Small, fast mixing function for stable randomness per cell/window.
     */
    private static long mix64(long seedA, long seedB) {
        long x = seedA * 0x9E3779B97F4A7C15L + seedB + 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    private static long mix64(long currentSwayAcceleration, long b, long c) {
        return mix64(mix64(currentSwayAcceleration, b), c);
    }

    /**
     * Smoothing function (ease-in, ease-out) for gust strength envelopes.
     */
    private static float smooth01(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
