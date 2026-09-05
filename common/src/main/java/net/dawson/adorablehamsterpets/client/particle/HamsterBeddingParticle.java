package net.dawson.adorablehamsterpets.client.particle;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.particles.common.HamsterBeddingParticleBehavior;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.util.IndoorOutdoorDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * A particle representing a piece of hamster bedding (a leaf).
 * This particle has two distinct physics behaviors:
 * <p>
 * 1.  <b>Standard Physics:</b> A simple gravity-and-friction model used when particles are spawned
 *     from bed interactions.
 * 2.  <b>Floaty Physics:</b> A complex simulation for particles spawned from the Hamster Bedding item
 *     or a dispenser. This includes a gentle pendulum-like sway and a deterministic, spatially-coherent
 *     wind gust model that creates realistic, synchronized movement among nearby particles.
 */
public class HamsterBeddingParticle extends SpriteBillboardParticle implements FloatyParticleMotion.Target {

    // --- Constants ---
    /** A magic number used in the 'vy' field to signal that this particle should use the "floaty" physics simulation. */
    public static final double BEDDING_ITEM_FLAG = -0.000123;

    // --- Universal Drift ---
    private static final float UNIVERSAL_DRIFT_ACCEL = 0.002f;
    private static final float DRIFT_PERIOD_TICKS = 3 * 60 * 20f; // 3 minutes

    // --- Sway Physics Constants (for floaty mode) ---
    private static final float SWAY_ROTATION_AMPLITUDE = 0.5f;
    private static final float SWAY_ROTATION_SPEED_MOD = 2.8f;
    private static final float UPWARD_BOOST_AT_APEX = 0.004f;
    private static final float HORIZ_SPEED_CAP = 0.08f;

    // --- Gust Physics Constants (for floaty mode) ---
    private static final float EXTRA_HCAP = 0.075f; // Cap for horizontal speed during full gust

    // --- Sound Management ---
    private static final Set<Long> playedGustSoundsThisTick = new HashSet<>();
    private static final Deque<Long> soundStartTimes = new ArrayDeque<>();
    private static final int MAX_CONCURRENT_SOUNDS = 3;
    private static final int SOUND_DURATION_TICKS = 65; // ~3 seconds
    private static long lastTick = -1L;

    // --- Fields ---
    // --- State ---
    private final boolean useFloatyPhysics;
    private final float baseScale;
    private final FloatyParticleMotion floatyMotion;
    private float previousScale;

    // --- Sway Physics ---
    private final float swayFrequency;
    private final float swayAcceleration;
    private final float swayPhaseOffset;
    private final float swayDirectionX, swayDirectionZ;
    private final float constantRollVelocity;

    public HamsterBeddingParticle(ClientWorld world,
                                  double x, double y, double z,
                                  double vx, double vy, double vz,
                                  SpriteProvider sprites) {
        super(world, x, y, z, vx, vy, vz);

        this.floatyMotion = new FloatyParticleMotion();

        // Set size to match leaf textures on bed
        this.scale *= 2.0f;
        this.baseScale = this.scale;
        this.scale = 0.0f;
        this.previousScale = 0.0f;
        this.alpha = 0.0f;

        // Use floaty physics if spawned from the Hamster Bedding item
        this.useFloatyPhysics = (vy == BEDDING_ITEM_FLAG);

        // --- Set Physics based on Spawn Type ---
        if (useFloatyPhysics) {
            // "Floaty" physics for item/dispenser use.
            this.velocityY = 0;
            this.gravityStrength = 0.07f;
            this.velocityMultiplier = 0.92f;
            this.maxAge = 140 + world.random.nextInt(200);

            float theta = world.random.nextFloat() * MathHelper.TAU;
            this.swayDirectionX = MathHelper.cos(theta);
            this.swayDirectionZ = MathHelper.sin(theta);

            this.swayFrequency = 0.09f + world.random.nextFloat() * 0.05f; // Period ~ 2π/ω = 45–80 ticks
            this.swayAcceleration = 0.002f + world.random.nextFloat() * 0.0025f; // Swing radius ≈ swayAcceleration / ω^2  → ~0.15–0.35 blocks
            this.swayPhaseOffset = world.random.nextFloat() * MathHelper.TAU;
            this.constantRollVelocity = (world.random.nextFloat() - 0.5f) * 0.12f; // Slow constant roll
        } else {
            // Standard physics for bed interactions.
            this.gravityStrength = HamsterBeddingParticleBehavior.GRAVITY;
            this.velocityMultiplier = HamsterBeddingParticleBehavior.FRICTION;
            this.maxAge = HamsterBeddingParticleBehavior.LIFETIME_MIN
                    + world.random.nextInt(HamsterBeddingParticleBehavior.LIFETIME_EXTRA);

            this.swayDirectionX = 0f;
            this.swayDirectionZ = 0f;
            this.swayFrequency = 0f;
            this.swayAcceleration = 0f;
            this.swayPhaseOffset = 0f;
            this.constantRollVelocity = 0f;
        }

        // --- Set Visuals ---
        this.setSprite(sprites.getSprite(this.random));
        this.setBoundingBoxSpacing(HamsterBeddingParticleBehavior.SIZE_X, HamsterBeddingParticleBehavior.SIZE_Y);
    }

    @Override
    public void tick() {
        long worldTime = this.world.getTime();
        // --- Sound Management ---
        // Prune old sounds and clear per-tick tracker
        if (worldTime != lastTick) {
            playedGustSoundsThisTick.clear();
            // Remove any sound start times that are older than the sound's duration
            while (!soundStartTimes.isEmpty() && worldTime - soundStartTimes.peekFirst() > SOUND_DURATION_TICKS) {
                soundStartTimes.pollFirst();
            }
            lastTick = worldTime;
        }

        // --- Particle Physics ---
        if (useFloatyPhysics) {
            // --- Universal Drift When Outdoors ---
            float universalDriftAngle;
            if (Configs.AHP_UI.enableDynamicDriftAngle.get()) {
                // Dynamic, time-based rotation
                float timeWithPartial = worldTime + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
                universalDriftAngle = (timeWithPartial / DRIFT_PERIOD_TICKS) * MathHelper.TAU;
            } else {
                // Static angle from config
                universalDriftAngle = (float) Math.toRadians(Configs.AHP_UI.staticDriftAngle.get());
            }

            float driftDirX = MathHelper.cos(universalDriftAngle);
            float driftDirZ = MathHelper.sin(universalDriftAngle);

            boolean isOutdoor = IndoorOutdoorDetector.isOutdoor(this.world, this.x, this.y, this.z);

            // Apply drift only if outdoors
            if (isOutdoor) {
                this.velocityX += driftDirX * UNIVERSAL_DRIFT_ACCEL;
                this.velocityZ += driftDirZ * UNIVERSAL_DRIFT_ACCEL;
            }

            // --- Sway Physics ---
            float phase = (this.age + this.swayPhaseOffset) * this.swayFrequency;
            float sinPhase = MathHelper.sin(phase);
            float cosPhase = MathHelper.cos(phase);
            float positionInSwing = Math.abs(cosPhase);
            // Swing faster in the middle and slower at the edges
            float speedMultiplier = 0.5f + 1.5f * positionInSwing;
            float modifiedAcceleration = sinPhase * this.swayAcceleration * speedMultiplier;

            this.velocityX += this.swayDirectionX * modifiedAcceleration;
            this.velocityZ += this.swayDirectionZ * modifiedAcceleration;

            // Add a gradual upward boost that is strongest at the apex of the swing to create a 'U' shape motion.
            // The boost is proportional to the fourth power of the particle's position in its swing.
            this.velocityY += UPWARD_BOOST_AT_APEX * (float)Math.pow(positionInSwing, 4);

            // --- Gust Simulation ---
            float gustStrengthLocal = isOutdoor
                    ? this.floatyMotion.applyGust(this.world, worldTime, this.x, this.y, this.z, driftDirX, driftDirZ, this)
                    : 0f;

            // --- Velocity & Rotation Update with Dynamic Horizontal Cap ---
            this.floatyMotion.capHorizontalVelocity(this, HORIZ_SPEED_CAP, EXTRA_HCAP, gustStrengthLocal);

            // --- Roll update ---
            this.prevAngle = this.angle;
            float gustSpinVelocity = this.floatyMotion.dampAndGetSpinVelocity();
            // Angular velocity from sway is now based on cos(phase) to make it fastest at the apex of the swing, creating a 'twist'.
            float swayAngularVelocity = cosPhase * this.swayFrequency * SWAY_ROTATION_AMPLITUDE * SWAY_ROTATION_SPEED_MOD;
            this.angle += this.constantRollVelocity + swayAngularVelocity + gustSpinVelocity;
        }

        this.previousScale = this.scale;
        this.scale = this.baseScale * ParticleAnimationUtil.growInScale(this.age);
        this.alpha = ParticleAnimationUtil.fadeInOpacity(this.age);

        super.tick();

        // --- Finalization ---
        // Settle quickly after landing on a block.
        if (this.onGround) {
            this.maxAge = Math.min(this.maxAge, this.age + 10);
        }
    }

    @Override
    public float getSize(float tickDelta) {
        return ParticleAnimationUtil.interpolateScale(
                this.previousScale,
                this.scale,
                tickDelta);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    // --- Gust Event Hook ---
    @Override
    public double velocityX() {
        return this.velocityX;
    }

    @Override
    public double velocityZ() {
        return this.velocityZ;
    }

    @Override
    public void addVelocityX(double amount) {
        this.velocityX += amount;
    }

    @Override
    public void addVelocityY(double amount) {
        this.velocityY += amount;
    }

    @Override
    public void addVelocityZ(double amount) {
        this.velocityZ += amount;
    }

    @Override
    public void multiplyVelocityX(float factor) {
        this.velocityX *= factor;
    }

    @Override
    public void multiplyVelocityZ(float factor) {
        this.velocityZ *= factor;
    }

    @Override
    public void onGustStarted(ClientWorld world, double x, double y, double z, long gustKey, long worldTime) {
        // Play sound once per unique gust event per tick, up to MAX_CONCURRENT_SOUNDS simultaneously
        if (soundStartTimes.size() < MAX_CONCURRENT_SOUNDS && playedGustSoundsThisTick.add(gustKey)) {
            float vol = Configs.AHP_UI.leafGustVolume.get();
            if (vol > 0) {
                world.playSound(x, y, z, ModSounds.GENTLE_BREEZE.get(), SoundCategory.AMBIENT, vol, 1.0f, false);
                soundStartTimes.addLast(worldTime);
            }
        }
    }

    // --- Factory ---
    /**
     * The factory for creating instances of {@link HamsterBeddingParticle}.
     */
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new HamsterBeddingParticle(world, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
