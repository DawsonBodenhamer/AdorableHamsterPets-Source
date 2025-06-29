package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ModPackets {

    /**
     * Registers all C2S (Client-to-Server) and S2C (Server-to-Client) packet receivers
     * for the mod. This method should be called once during the common mod initialization phase.
     * It uses the Architectury Networking API to set up handlers for each custom payload,
     * ensuring that packet logic is executed on the correct thread (server or client)
     * for safe interaction with the game world.
     */
    public static void register() {
        // --- C2S Receivers (Server-side handlers) ---

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ThrowHamsterPayload.ID, ThrowHamsterPayload.CODEC,
                (payload, context) -> context.queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.getPlayer()))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SpawnAttackParticlesPayload.ID, SpawnAttackParticlesPayload.CODEC,
                (payload, context) -> context.queue(() -> handleSpawnAttackParticles(payload, context))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SpawnSeekingDustPayload.ID, SpawnSeekingDustPayload.CODEC,
                (payload, context) -> context.queue(() -> handleSpawnSeekingDust(payload, context))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UpdateHamsterRenderStatePayload.ID, UpdateHamsterRenderStatePayload.CODEC,
                (payload, context) -> context.queue(() -> handleUpdateRenderState(payload, context))
        );

        // --- S2C Receivers (Client-side handlers) ---
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterFlightSoundPayload.ID, StartHamsterFlightSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartFlightSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, StartHamsterThrowSoundPayload.ID, StartHamsterThrowSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleStartThrowSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HamsterAnimationSoundPayload.ID, HamsterAnimationSoundPayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleAnimationSound(payload))
        );

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HamsterAnimationParticlePayload.ID, HamsterAnimationParticlePayload.CODEC,
                (payload, context) -> context.queue(() -> AdorableHamsterPetsClient.handleAnimationParticle(payload))
        );
    }

    // --- Server-Side Packet Handling Logic ---
    private static void handleSpawnAttackParticles(SpawnAttackParticlesPayload payload, NetworkManager.PacketContext context) {
        ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
        ServerWorld world = player.getServerWorld();
        if (world != null) {
            world.spawnParticles(ParticleTypes.WHITE_SMOKE, payload.x(), payload.y(), payload.z(), 10, 0.1, 0.2, 0.1, 0.05);
        }
    }

    private static void handleSpawnSeekingDust(SpawnSeekingDustPayload payload, NetworkManager.PacketContext context) {
        ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
        ServerWorld world = player.getServerWorld();
        if (world == null) return;

        Entity entity = world.getEntityById(payload.hamsterEntityId());
        if (!(entity instanceof HamsterEntity hamster)) return;

        BlockPos blockBelowHamster = hamster.getBlockPos().down();
        BlockState stateForParticles = world.getBlockState(blockBelowHamster);
        if (stateForParticles.isAir() || stateForParticles.isReplaceable()) {
            stateForParticles = world.getBlockState(blockBelowHamster.down());
        }
        if (stateForParticles.getCollisionShape(world, blockBelowHamster).isEmpty()) {
            stateForParticles = Blocks.DIRT.getDefaultState();
        }

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, stateForParticles),
                payload.particleX(), payload.particleY(), payload.particleZ(), 12, 0.2, 0.03, 0.2, 0.0);
    }

    private static void handleUpdateRenderState(UpdateHamsterRenderStatePayload payload, NetworkManager.PacketContext context) {
        if (payload.isRendering()) {
            HamsterRenderTracker.addPlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        } else {
            HamsterRenderTracker.removePlayer(payload.hamsterEntityId(), context.getPlayer().getUuid());
        }
    }
}