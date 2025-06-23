package net.dawson.adorablehamsterpets.networking.payload;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnAttackParticlesPayload(double x, double y, double z) implements CustomPayload {
    public static final CustomPayload.Id<SpawnAttackParticlesPayload> ID = new CustomPayload.Id<>(Identifier.of(AdorableHamsterPets.MOD_ID, "spawn_attack_particles"));

    public static final PacketCodec<RegistryByteBuf, SpawnAttackParticlesPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, SpawnAttackParticlesPayload::x,
            PacketCodecs.DOUBLE, SpawnAttackParticlesPayload::y,
            PacketCodecs.DOUBLE, SpawnAttackParticlesPayload::z,
            SpawnAttackParticlesPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}