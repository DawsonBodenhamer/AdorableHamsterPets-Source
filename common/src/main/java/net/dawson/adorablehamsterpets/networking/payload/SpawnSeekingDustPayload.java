package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnSeekingDustPayload(
        int hamsterEntityId,
        double particleX,
        double particleY,
        double particleZ
) implements CustomPayload {
    public static final CustomPayload.Id<SpawnSeekingDustPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "spawn_seeking_dust"));

    public static final PacketCodec<RegistryByteBuf, SpawnSeekingDustPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SpawnSeekingDustPayload::hamsterEntityId,
            PacketCodecs.DOUBLE, SpawnSeekingDustPayload::particleX,
            PacketCodecs.DOUBLE, SpawnSeekingDustPayload::particleY,
            PacketCodecs.DOUBLE, SpawnSeekingDustPayload::particleZ,
            SpawnSeekingDustPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}