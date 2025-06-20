package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartHamsterThrowSoundPayload(int hamsterEntityId) implements CustomPayload {
    // Ensure this ID is unique
    public static final CustomPayload.Id<StartHamsterThrowSoundPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "start_hamster_throw_sound"));
    public static final PacketCodec<RegistryByteBuf, StartHamsterThrowSoundPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StartHamsterThrowSoundPayload::hamsterEntityId,
            StartHamsterThrowSoundPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}