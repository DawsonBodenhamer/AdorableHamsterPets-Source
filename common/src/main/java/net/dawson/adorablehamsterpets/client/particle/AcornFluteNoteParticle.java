package net.dawson.adorablehamsterpets.client.particle;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.flute.AcornFluteVariant;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

/**
 * A colored Acorn Flute note with the same localized gust response as floaty bedding.
 */
public final class AcornFluteNoteParticle extends SpriteBillboardParticle
        implements FloatyParticleMotion.Target {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int MIN_LIFETIME = 48;
    private static final int LIFETIME_VARIANCE = 24;
    private static final float NOTE_SCALE = 0.891F;
    private static final float INITIAL_UPWARD_SPEED = 0.0115F;
    private static final float INITIAL_UPWARD_SPEED_VARIANCE = 0.0046F;
    private static final float UPWARD_ACCELERATION = 0.0002875F;
    private static final float UNIVERSAL_DRIFT_ACCELERATION = 0.002F;
    private static final float DRIFT_PERIOD_TICKS = 3.0F * 60.0F * 20.0F;
    private static final float HORIZONTAL_SPEED_CAP = 0.12F;
    private static final float GUST_SPEED_ALLOWANCE = 0.10F;
    private static final float VELOCITY_DAMPING = 0.96F;
    private static final float ROTATION_SPEED_MIN = 0.012F;
    private static final float ROTATION_SPEED_MAX = 0.060F;

    // --- Deterministic Tuning Probes ---
    static float noteScale() {
        return NOTE_SCALE;
    }

    static float initialVerticalSpeed(float randomSample) {
        return INITIAL_UPWARD_SPEED
                + MathHelper.clamp(randomSample, 0.0F, 1.0F) * INITIAL_UPWARD_SPEED_VARIANCE;
    }

    static double baselineVerticalDisplacement(int ticks, float randomSample) {
        double displacement = 0.0D;
        double velocity = initialVerticalSpeed(randomSample);
        for (int tick = 0; tick < Math.max(0, ticks); tick++) {
            displacement += velocity;
            velocity = (velocity + UPWARD_ACCELERATION) * VELOCITY_DAMPING;
        }
        return displacement;
    }

    static float opacity(int age, int maxAge) {
        if (maxAge <= 0) {
            return 0.0F;
        }

        float fadeStart = maxAge * 0.9F;
        return age < fadeStart
                ? 1.0F
                : MathHelper.clamp(
                        (maxAge - age) / (maxAge - fadeStart),
                        0.0F,
                        1.0F);
    }

    static float rotationSpeed(float signedRandomSample) {
        float sample = MathHelper.clamp(signedRandomSample, -1.0F, 1.0F);
        float magnitude = ROTATION_SPEED_MIN
                + Math.abs(sample) * (ROTATION_SPEED_MAX - ROTATION_SPEED_MIN);
        return sample < 0.0F ? -magnitude : magnitude;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        State
     * ────────────────────────────────────────────────────────────────────────────*/

    private final FloatyParticleMotion floatyMotion = new FloatyParticleMotion();
    private float rotationVelocity;

    private AcornFluteNoteParticle(ClientWorld world,
                                   double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   SpriteProvider sprites, AcornFluteVariant variant) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);

        this.setSprite(sprites.getSprite(this.random));
        this.maxAge = MIN_LIFETIME + this.random.nextInt(LIFETIME_VARIANCE);
        this.scale *= NOTE_SCALE;
        this.gravityStrength = 0.0F;
        this.velocityMultiplier = VELOCITY_DAMPING;
        this.collidesWithWorld = false;

        this.rotationVelocity = rotationSpeed(this.random.nextFloat() * 2.0F - 1.0F);
        this.angle = this.random.nextFloat() * MathHelper.TAU;
        this.prevAngle = this.angle;

        // Ignore packet launch velocity so every note acquires the same global drift direction
        this.velocityX = 0.0D;
        this.velocityY = initialVerticalSpeed(this.random.nextFloat());
        this.velocityZ = 0.0D;

        this.setPaletteColor(variant);
    }

    // --- Palette ---
    private void setPaletteColor(AcornFluteVariant variant) {
        // Runtime sprites retain each source mask's alpha; white RGB lets particle shader apply this tint
        int[] palette = variant.palette();
        int color = palette[this.random.nextInt(palette.length)];
        this.setColor(
                (color >> 16 & 0xFF) / 255.0F,
                (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public void tick() {
        long worldTime = this.world.getTime();

        // --- Global Drift ---
        this.velocityY += UPWARD_ACCELERATION;
        float tickDelta = MinecraftClient.getInstance().getTickDelta();
        float driftAngle = globalDriftAngle(
                Configs.AHP_UI.enableDynamicDriftAngle.get(),
                worldTime,
                tickDelta,
                Configs.AHP_UI.staticDriftAngle.get());
        float driftDirectionX = MathHelper.cos(driftAngle);
        float driftDirectionZ = MathHelper.sin(driftAngle);
        this.velocityX += driftDirectionX * UNIVERSAL_DRIFT_ACCELERATION;
        this.velocityZ += driftDirectionZ * UNIVERSAL_DRIFT_ACCELERATION;

        // --- Shared Gust Response ---
        float gustStrength = this.floatyMotion.applyGust(
                this.world,
                worldTime,
                this.x,
                this.y,
                this.z,
                driftDirectionX,
                driftDirectionZ,
                this);
        this.floatyMotion.capHorizontalVelocity(
                this,
                HORIZONTAL_SPEED_CAP,
                GUST_SPEED_ALLOWANCE,
                gustStrength);

        // --- Continuous Rotation ---
        this.prevAngle = this.angle;
        this.angle += this.rotationVelocity;
        this.alpha = opacity(this.age, this.maxAge);

        super.tick();
    }

    static float globalDriftAngle(
            boolean dynamic, long worldTime, float tickDelta, int staticAngleDegrees) {
        if (dynamic) {
            float timeWithPartial = worldTime + tickDelta;
            return timeWithPartial / DRIFT_PERIOD_TICKS * MathHelper.TAU;
        }
        return (float) Math.toRadians(staticAngleDegrees);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    // --- Floaty Particle Target ---
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

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Factory
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final class Factory implements ParticleFactory<AcornFluteNoteParticleEffect> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(AcornFluteNoteParticleEffect effect,
                                       ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new AcornFluteNoteParticle(
                    world,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    this.sprites,
                    effect.variant());
        }
    }
}
