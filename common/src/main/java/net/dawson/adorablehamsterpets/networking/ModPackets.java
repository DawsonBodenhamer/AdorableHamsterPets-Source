package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkChannel;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static net.dawson.adorablehamsterpets.AdorableHamsterPets.MOD_ID;

public class ModPackets {

    // --- 1. Create the Network Channel ---
    public static final NetworkChannel CHANNEL = NetworkChannel.create(Identifier.of(MOD_ID, "main"));

    // --- 2. Define Packet Data as Records ---
    // C2S (Client-to-Server)
    public record ThrowHamsterC2SPacket() {}
    public record DismountHamsterC2SPacket() {}
    public record UpdateRenderStateC2SPacket(int entityId, boolean isRendering) {}

    // S2C (Server-to-Client)
    public record StartFlightSoundS2CPacket(int entityId) {}
    public record StartThrowSoundS2CPacket(int entityId) {}

    /**
     * Registers all packet types and their handlers using the NetworkChannel API.
     * This method is safe to call from the common initializer, as the API handles
     * client/server separation internally.
     */
    public static void register() {
        // --- C2S Packet Registrations ---
        CHANNEL.register(ThrowHamsterC2SPacket.class,
                (packet, buf) -> {}, // Encoder (no data)
                (buf) -> new ThrowHamsterC2SPacket(), // Decoder
                (packet, context) -> context.get().queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.get().getPlayer()))
        );

        CHANNEL.register(DismountHamsterC2SPacket.class,
                (packet, buf) -> {}, // Encoder
                (buf) -> new DismountHamsterC2SPacket(), // Decoder
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster(false);
                    }
                })
        );

        CHANNEL.register(UpdateRenderStateC2SPacket.class,
                (packet, buf) -> { // Encoder
                    buf.writeInt(packet.entityId());
                    buf.writeBoolean(packet.isRendering());
                },
                (buf) -> new UpdateRenderStateC2SPacket(buf.readInt(), buf.readBoolean()), // Decoder
                (packet, context) -> context.get().queue(() -> {
                    if (packet.isRendering()) {
                        HamsterRenderTracker.addPlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    } else {
                        HamsterRenderTracker.removePlayer(packet.entityId(), context.get().getPlayer().getUuid());
                    }
                })
        );

        // --- S2C Packet Registrations ---
        CHANNEL.register(StartFlightSoundS2CPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()), // Encoder
                (buf) -> new StartFlightSoundS2CPacket(buf.readInt()), // Decoder
                (packet, context) -> context.get().queue(() -> AdorableHamsterPetsClient.handleStartFlightSound(packet.entityId()))
        );

        CHANNEL.register(StartThrowSoundS2CPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()), // Encoder
                (buf) -> new StartThrowSoundS2CPacket(buf.readInt()), // Decoder
                (packet, context) -> context.get().queue(() -> AdorableHamsterPetsClient.handleStartThrowSound(packet.entityId()))
        );
    }
}