package net.dawson.adorablehamsterpets.client.particle;

import net.dawson.adorablehamsterpets.flute.AcornFluteVariant;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A colored Acorn Flute note following a player-directed three-dimensional cone.
 */
public final class AcornFluteNoteParticle extends SpriteBillboardParticle {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int MIN_LIFETIME = 40;                     // Higher = notes live longer
    private static final int LIFETIME_VARIANCE = 20;                // Higher = more lifespan variation
    private static final float NOTE_SCALE = 1.0F;                   // Higher = bigger
    private static final float CONE_HALF_ANGLE_DEGREES = 20.0F;     // Higher = wider cone
    private static final float CONE_VERTICAL_MIN_DEGREES = 25.0F;   // 0 = parallel to horizon
    private static final float CONE_VERTICAL_MAX_DEGREES = 90.0F;   // Steepest upward launch pitch
    private static final float INITIAL_SPEED = 0.02F;               // Higher = faster launch
    private static final float INITIAL_SPEED_VARIANCE = 0.07F;      // Higher = wider launch speeds
    private static final float DIRECTIONAL_ACCELERATION = 0.01F;    // Higher = stronger push each tick
    private static final float VELOCITY_DAMPING = 0.96F;            // Closer to 1.0 = glides farther
    private static final float ROTATION_SPEED_MIN = 0.02F;          // Higher = faster
    private static final float ROTATION_SPEED_MAX = 0.06F;          // Higher = faster

    // --- Deterministic Tuning Probes ---
    static float noteScale() {
        return NOTE_SCALE;
    }

    static float initialSpeed(float randomSample) {
        return INITIAL_SPEED
                + MathHelper.clamp(randomSample, 0.0F, 1.0F) * INITIAL_SPEED_VARIANCE;
    }

    static float opacity(int age, int maxAge) {
        if (maxAge <= 0) {
            return 0.0F;
        }

        float fadeStart = maxAge * 0.9F;
        float fadeOut = age < fadeStart
                ? 1.0F
                : MathHelper.clamp(
                        (maxAge - age) / (maxAge - fadeStart),
                        0.0F,
                        1.0F);
        return ParticleAnimationUtil.fadeInOpacity(age) * fadeOut;
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

    private final float baseScale;
    private final double directionX;
    private final double directionY;
    private final double directionZ;
    private float previousScale;
    private float rotationVelocity;

    private AcornFluteNoteParticle(ClientWorld world,
                                   double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   SpriteProvider sprites, AcornFluteVariant variant,
                                   float lookYaw, float lookPitch) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);

        this.setSprite(sprites.getSprite(this.random));
        this.maxAge = MIN_LIFETIME + this.random.nextInt(LIFETIME_VARIANCE);
        this.scale *= NOTE_SCALE;
        this.baseScale = this.scale;
        Vec3d direction = createConeDirection(lookYaw, lookPitch);
        this.directionX = direction.x;
        this.directionY = direction.y;
        this.directionZ = direction.z;
        this.scale = 0.0F;
        this.previousScale = 0.0F;
        this.alpha = 0.0F;
        this.gravityStrength = 0.0F;
        this.velocityMultiplier = VELOCITY_DAMPING;
        this.collidesWithWorld = true;

        this.rotationVelocity = rotationSpeed(this.random.nextFloat() * 2.0F - 1.0F);
        this.angle = this.random.nextFloat() * MathHelper.TAU;
        this.prevAngle = this.angle;

        double launchSpeed = initialSpeed(this.random.nextFloat());
        this.velocityX = this.directionX * launchSpeed;
        this.velocityY = this.directionY * launchSpeed;
        this.velocityZ = this.directionZ * launchSpeed;

        this.setPaletteColor(variant);
    }

    private Vec3d createConeDirection(float lookYaw, float lookPitch) {
        float minimumPitch = (float) Math.toRadians(CONE_VERTICAL_MIN_DEGREES);
        float maximumPitch = (float) Math.toRadians(CONE_VERTICAL_MAX_DEGREES);
        float clampedPitch = MathHelper.clamp(lookPitch, minimumPitch, maximumPitch);
        float cosPitch = MathHelper.cos(clampedPitch);
        Vec3d centerDirection = new Vec3d(
                MathHelper.cos(lookYaw) * cosPitch,
                MathHelper.sin(clampedPitch),
                MathHelper.sin(lookYaw) * cosPitch).normalize();
        Vec3d reference = Math.abs(centerDirection.y) > 0.99D
                ? new Vec3d(1.0D, 0.0D, 0.0D)
                : new Vec3d(0.0D, 1.0D, 0.0D);
        Vec3d tangent = centerDirection.crossProduct(reference).normalize();
        Vec3d bitangent = tangent.crossProduct(centerDirection).normalize();
        double maximumConeAngle = Math.min(
                Math.toRadians(CONE_HALF_ANGLE_DEGREES),
                clampedPitch);
        double coneAngle = maximumConeAngle * this.random.nextFloat();
        double azimuth = MathHelper.TAU * this.random.nextFloat();
        double sinConeAngle = Math.sin(coneAngle);

        return centerDirection.multiply(Math.cos(coneAngle))
                .add(tangent.multiply(sinConeAngle * Math.cos(azimuth)))
                .add(bitangent.multiply(sinConeAngle * Math.sin(azimuth)))
                .normalize();
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
        // --- Global Drift ---
        this.velocityX += this.directionX * DIRECTIONAL_ACCELERATION;
        this.velocityY += this.directionY * DIRECTIONAL_ACCELERATION;
        this.velocityZ += this.directionZ * DIRECTIONAL_ACCELERATION;

        // --- Continuous Rotation ---
        this.prevAngle = this.angle;
        this.angle += this.rotationVelocity;
        this.previousScale = this.scale;
        this.scale = this.baseScale * ParticleAnimationUtil.growInScale(this.age);
        this.alpha = opacity(this.age, this.maxAge);

        super.tick();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ────────────────────────────────────────────────────────────────────────────*/

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getSize(float tickDelta) {
        return ParticleAnimationUtil.interpolateScale(
                this.previousScale,
                this.scale,
                tickDelta);
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
                    effect.variant(),
                    effect.lookYaw(),
                    effect.lookPitch());
        }
    }
}
