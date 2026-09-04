package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Stops one keyed distant sound session on the receiving client.
 */
public record StopDistantSoundPayload(String sessionKey) implements CustomPayload {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int SESSION_KEY_MAX_LENGTH = 64;

    public static final CustomPayload.Id<StopDistantSoundPayload> ID =
            new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "stop_distant_sound"));

    public static final PacketCodec<RegistryByteBuf, StopDistantSoundPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(SESSION_KEY_MAX_LENGTH), StopDistantSoundPayload::sessionKey,
            StopDistantSoundPayload::new
    );

    /* ──────────────────────────────────────────────────────────────────────────────
     *                              Constructors
     * ────────────────────────────────────────────────────────────────────────────*/

    public StopDistantSoundPayload(UUID sessionId) {
        this(sessionId.toString());
    }

    public StopDistantSoundPayload {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new IllegalArgumentException("Distant sound session key cannot be blank");
        }
        if (sessionKey.length() > SESSION_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("Distant sound session key is too long");
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
