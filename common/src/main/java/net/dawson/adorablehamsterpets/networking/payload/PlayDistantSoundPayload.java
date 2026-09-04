package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Starts a distant sound as a listener-position one-shot, a positioned one-shot
 * with distance falloff, or a keyed session.
 *
 * <p>An empty session key selects a one-shot, and an absent position plays the
 * sound at the receiving client player's own position. The three-argument
 * constructor builds that form for the Hamster Yeet impact audio.</p>
 */
public record PlayDistantSoundPayload(
        Identifier soundId,
        Attenuation attenuation,
        float pitch,
        Optional<Vec3d> position,
        int sourceEntityId,
        String sessionKey,
        SoundCategory category
) implements CustomPayload {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int SESSION_KEY_MAX_LENGTH = 64;

    public static final CustomPayload.Id<PlayDistantSoundPayload> ID =
            new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "play_distant_sound"));

    private static final PacketCodec<RegistryByteBuf, Vec3d> POSITION_CODEC = PacketCodec.ofStatic(
            (buf, position) -> {
                buf.writeDouble(position.x);
                buf.writeDouble(position.y);
                buf.writeDouble(position.z);
            },
            buf -> new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    private static final PacketCodec<RegistryByteBuf, Attenuation> ATTENUATION_CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, Attenuation::volume,
            PacketCodecs.FLOAT, Attenuation::audioRange,
            Attenuation::new
    );

    public static final PacketCodec<RegistryByteBuf, PlayDistantSoundPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                Identifier.PACKET_CODEC.encode(buf, payload.soundId());
                ATTENUATION_CODEC.encode(buf, payload.attenuation());
                PacketCodecs.FLOAT.encode(buf, payload.pitch());
                PacketCodecs.optional(POSITION_CODEC).encode(buf, payload.position());
                PacketCodecs.VAR_INT.encode(buf, payload.sourceEntityId());
                PacketCodecs.string(SESSION_KEY_MAX_LENGTH).encode(buf, payload.sessionKey());
                PacketCodecs.indexed(
                        index -> SoundCategory.values()[index],
                        SoundCategory::ordinal
                ).encode(buf, payload.category());
            },
            buf -> new PlayDistantSoundPayload(
                    Identifier.PACKET_CODEC.decode(buf),
                    ATTENUATION_CODEC.decode(buf),
                    PacketCodecs.FLOAT.decode(buf),
                    PacketCodecs.optional(POSITION_CODEC).decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.string(SESSION_KEY_MAX_LENGTH).decode(buf),
                    PacketCodecs.indexed(
                            index -> SoundCategory.values()[index],
                            SoundCategory::ordinal
                    ).decode(buf))
    );

    /* ──────────────────────────────────────────────────────────────────────────────
     *                              Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Creates a listener-position one-shot with neutral audio, used by the
     * Hamster Yeet impact and armor sounds.
     */
    public PlayDistantSoundPayload(Identifier soundId, float volume, float pitch) {
        this(
                soundId,
                new Attenuation(volume, 0.0F),
                pitch,
                Optional.empty(),
                0,
                "",
                SoundCategory.NEUTRAL);
    }

    public PlayDistantSoundPayload {
        if (attenuation == null) {
            attenuation = new Attenuation(0.0F, 0.0F);
        }
        if (position == null) {
            position = Optional.empty();
        }
        if (sessionKey == null) {
            sessionKey = "";
        }
        if (sessionKey.length() > SESSION_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Distant sound session key is too long");
        }
        if (category == null) {
            category = SoundCategory.NEUTRAL;
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                              Static Factories
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Creates a positioned one-shot sound with client-updated distance falloff.
     */
    public static PlayDistantSoundPayload positioned(
            Identifier soundId,
            Vec3d position,
            float baseVolume,
            float pitch,
            float audioRange,
            SoundCategory category
    ) {
        return new PlayDistantSoundPayload(
                soundId,
                new Attenuation(baseVolume, audioRange),
                pitch,
                Optional.of(position),
                0,
                "",
                category);
    }

    /**
     * Creates a positioned one-shot using neutral audio.
     */
    public static PlayDistantSoundPayload positioned(
            Identifier soundId,
            Vec3d position,
            float baseVolume,
            float pitch,
            float audioRange
    ) {
        return positioned(soundId, position, baseVolume, pitch, audioRange, SoundCategory.NEUTRAL);
    }

    /**
     * Creates a positioned session start. Reusing the same key replaces the
     * currently playing session with that identity on each client.
     */
    public static PlayDistantSoundPayload startSession(
            Identifier soundId,
            Vec3d position,
            int sourceEntityId,
            float baseVolume,
            float pitch,
            float audioRange,
            String sessionKey,
            SoundCategory category
    ) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new IllegalArgumentException("Distant sound session key cannot be blank");
        }

        return new PlayDistantSoundPayload(
                soundId,
                new Attenuation(baseVolume, audioRange),
                pitch,
                Optional.of(position),
                sourceEntityId,
                sessionKey,
                category);
    }

    public static PlayDistantSoundPayload startSession(
            Identifier soundId,
            Vec3d position,
            float baseVolume,
            float pitch,
            float audioRange,
            String sessionKey,
            SoundCategory category
    ) {
        return startSession(
                soundId,
                position,
                0,
                baseVolume,
                pitch,
                audioRange,
                sessionKey,
                category);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public float volume() {
        return this.attenuation.volume();
    }

    public float audioRange() {
        return this.attenuation.audioRange();
    }

    public record Attenuation(float volume, float audioRange) {}
}
