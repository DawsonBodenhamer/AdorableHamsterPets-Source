package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static net.dawson.adorablehamsterpets.AdorableHamsterPets.MOD_ID;

public class ModPackets {

    // --- Packet Identifiers ---
    public static final Identifier THROW_HAMSTER_ID = Identifier.of(MOD_ID, "throw_hamster");
    public static final Identifier UPDATE_HAMSTER_RENDER_STATE_ID = Identifier.of(MOD_ID, "update_hamster_render_state");
    public static final Identifier START_HAMSTER_FLIGHT_SOUND_ID = Identifier.of(MOD_ID, "start_hamster_flight_sound");
    public static final Identifier START_HAMSTER_THROW_SOUND_ID = Identifier.of(MOD_ID, "start_hamster_throw_sound");

    /**
     * Registers all C2S (Client-to-Server) packet receivers.
     * This method should be called from the common initializer.
     */
    public static void registerC2SPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, THROW_HAMSTER_ID,
                (buf, context) -> context.queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.getPlayer()))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UPDATE_HAMSTER_RENDER_STATE_ID,
                (buf, context) -> {
                    int entityId = buf.readInt();
                    boolean isRendering = buf.readBoolean();
                    context.queue(() -> {
                        if (isRendering) {
                            HamsterRenderTracker.addPlayer(entityId, context.getPlayer().getUuid());
                        } else {
                            HamsterRenderTracker.removePlayer(entityId, context.getPlayer().getUuid());
                        }
                    });
                }
        );
    }

    /**
     * Registers all S2C (Server-to-Client) packet receivers.
     * This method MUST be called from a client-only initializer.
     */
    public static void registerS2CPackets() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, START_HAMSTER_FLIGHT_SOUND_ID,
                (buf, context) -> {
                    int entityId = buf.readInt();
                    context.queue(() -> AdorableHamsterPetsClient.handleStartFlightSound(entityId));
                }
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, START_HAMSTER_THROW_SOUND_ID,
                (buf, context) -> {
                    int entityId = buf.readInt();
                    context.queue(() -> AdorableHamsterPetsClient.handleStartThrowSound(entityId));
                }
        );
    }
}