package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ThrowHamsterPayload() implements CustomPayload {
    // 1. Define a unique ID (can reuse the one from AdorableHamsterPets)
    public static final CustomPayload.Id<ThrowHamsterPayload> ID = new CustomPayload.Id<>(AdorableHamsterPets.THROW_HAMSTER_PACKET_ID);

    // 2. Define a codec. Since there's no data, it does nothing but return the instance.
    public static final PacketCodec<RegistryByteBuf, ThrowHamsterPayload> CODEC = PacketCodec.unit(new ThrowHamsterPayload());

    // 3. Implement the getId method
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}