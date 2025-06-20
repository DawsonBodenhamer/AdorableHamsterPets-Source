package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Payload sent from Server to Client
public record StartHamsterFlightSoundPayload(int hamsterEntityId) implements CustomPayload {
    public static final CustomPayload.Id<StartHamsterFlightSoundPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "start_hamster_flight_sound"));

    public static final PacketCodec<RegistryByteBuf, StartHamsterFlightSoundPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StartHamsterFlightSoundPayload::hamsterEntityId,
            StartHamsterFlightSoundPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}