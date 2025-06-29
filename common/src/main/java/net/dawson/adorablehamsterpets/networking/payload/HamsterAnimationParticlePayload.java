package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HamsterAnimationParticlePayload(int hamsterEntityId, String particleId) implements CustomPayload {
    public static final Id<HamsterAnimationParticlePayload> ID = new Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_animation_particle"));

    public static final PacketCodec<RegistryByteBuf, HamsterAnimationParticlePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, HamsterAnimationParticlePayload::hamsterEntityId,
            PacketCodecs.STRING, HamsterAnimationParticlePayload::particleId,
            HamsterAnimationParticlePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}