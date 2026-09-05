package net.dawson.adorablehamsterpets.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.flute.AcornFluteVariant;
import net.dawson.adorablehamsterpets.particles.ModParticles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

import java.util.Locale;
import java.util.Objects;

/**
 * Server-spawnable parameters for an Acorn Flute note.
 *
 * <p>The variant travels with the particle effect so every client can choose the same flute
 * palette while the particle factory remains free to select one of the supplied note sprites.</p>
 */
public final class AcornFluteNoteParticleEffect implements ParticleEffect {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Serialization
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final Codec<AcornFluteVariant> VARIANT_CODEC = Codec.STRING.xmap(
            value -> AcornFluteVariant.valueOf(value.toUpperCase(Locale.ROOT)),
            variant -> variant.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<AcornFluteNoteParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    VARIANT_CODEC.fieldOf("variant").forGetter(AcornFluteNoteParticleEffect::variant),
                    Codec.FLOAT.fieldOf("look_yaw").forGetter(AcornFluteNoteParticleEffect::lookYaw),
                    Codec.FLOAT.fieldOf("look_pitch").forGetter(AcornFluteNoteParticleEffect::lookPitch)
            ).apply(instance, AcornFluteNoteParticleEffect::new));

    public static final PacketCodec<RegistryByteBuf, AcornFluteNoteParticleEffect> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            effect -> effect.variant.ordinal(),
            PacketCodecs.FLOAT,
            AcornFluteNoteParticleEffect::lookYaw,
            PacketCodecs.FLOAT,
            AcornFluteNoteParticleEffect::lookPitch,
            AcornFluteNoteParticleEffect::fromOrdinal);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        State
     * ────────────────────────────────────────────────────────────────────────────*/

    private final AcornFluteVariant variant;
    private final float lookYaw;
    private final float lookPitch;

    public AcornFluteNoteParticleEffect(AcornFluteVariant variant) {
        this(variant, 0.0F, 0.0F);
    }

    public AcornFluteNoteParticleEffect(AcornFluteVariant variant, float lookYaw, float lookPitch) {
        this.variant = Objects.requireNonNull(variant, "variant");
        this.lookYaw = lookYaw;
        this.lookPitch = lookPitch;
    }

    public AcornFluteVariant variant() {
        return this.variant;
    }

    public float lookYaw() {
        return this.lookYaw;
    }

    public float lookPitch() {
        return this.lookPitch;
    }

    @Override
    public ParticleType<AcornFluteNoteParticleEffect> getType() {
        return ModParticles.ACORN_FLUTE_NOTE.get();
    }

    // --- Serialization Helpers ---
    private static AcornFluteNoteParticleEffect fromOrdinal(int ordinal, float lookYaw, float lookPitch) {
        return new AcornFluteNoteParticleEffect(
                AcornFluteVariant.values()[ordinal],
                lookYaw,
                lookPitch);
    }
}
