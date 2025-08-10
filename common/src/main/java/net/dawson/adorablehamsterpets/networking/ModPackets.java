package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModPackets {

    /**
     * Registers all C2S (Client-to-Server) packet receivers.
     * This method should be called from the common initializer, as the server
     * needs to be aware of these packets.
     */
    public static void registerC2SPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ThrowHamsterPayload.ID, ThrowHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.getPlayer()))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UpdateHamsterRenderStatePayload.ID, UpdateHamsterRenderStatePayload.CODEC,
                (payload, context) -> context.queue(() -> handleUpdateRenderState(payload, context))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DismountHamsterPayload.ID, DismountHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster();
                    }
                })
        );
    }

    /**
     * Registers all S2C (Server-to-Client) packet receivers.
     * This method MUST be called from a client-only initializer to prevent
     * server-side crashes.
     */
    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterFlightSoundPayload.ID, StartHamsterFlightSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartFlightSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterThrowSoundPayload.ID, StartHamsterThrowSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartThrowSound(payload))
        );
    }

    private static void handleUpdateRenderState(UpdateHamsterRenderStatePayload payload, NetworkManager.PacketContext context) {
        if (payload.isRendering()) {
            HamsterRenderTracker.addPlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        } else {
            HamsterRenderTracker.removePlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        }
    }
}