package net.dawson.adorablehamsterpets.util;

import dev.architectury.networking.NetworkManager;
import net.dawson.adorablehamsterpets.networking.payload.PlayDistantSoundPayload;
import net.dawson.adorablehamsterpets.networking.payload.StopDistantSoundPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Sends server-authoritative distant audio with explicit range and session semantics.
 *
 * <p>Audio range is intentionally an argument to this service. Callers should keep
 * gameplay effect radii separate and calculate those independently.</p>
 */
public final class DistantSoundUtil {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Constants
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final float MINIMUM_VOLUME = 0.0F;
    private static final float MAXIMUM_VOLUME = 1.0F;

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Static Utilities
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Returns a linear source-to-listener falloff for the supplied audio range.
     * A listener at the edge of the range receives no packet.
     */
    public static float calculateVolumeFalloff(double distance, double audioRange) {
        if (!Double.isFinite(distance)
                || !Double.isFinite(audioRange)
                || distance < 0.0
                || audioRange <= 0.0) {
            return MINIMUM_VOLUME;
        }

        return MathHelper.clamp((float) (1.0 - distance / audioRange), MINIMUM_VOLUME, MAXIMUM_VOLUME);
    }

    /**
     * Sends one positioned sound to listeners inside the configured range.
     * The default category is neutral.
     */
    public static void playOneShot(
            ServerWorld world,
            Vec3d sourcePosition,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange
    ) {
        playOneShot(world, sourcePosition, sound, baseVolume, pitch, audioRange, SoundCategory.NEUTRAL);
    }

    /**
     * Sends one positioned sound to listeners inside the configured range.
     */
    public static void playOneShot(
            ServerWorld world,
            Vec3d sourcePosition,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange,
            SoundCategory category
    ) {
        broadcastStart(
                world,
                sourcePosition,
                0,
                sound,
                baseVolume,
                pitch,
                audioRange,
                category,
                ""
        );
    }

    /**
     * Starts one positioned keyed session. The default category matches vanilla
     * Goat Horn audio and is suitable for Acorn Flute performances.
     */
    public static void startSession(
            ServerWorld world,
            UUID sessionId,
            Vec3d sourcePosition,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange
    ) {
        startSession(
                world,
                sessionId,
                sourcePosition,
                sound,
                baseVolume,
                pitch,
                audioRange,
                SoundCategory.RECORDS
        );
    }

    /**
     * Starts one positioned keyed session with an explicit category.
     * Reusing a session UUID replaces that identity on each receiving client.
     */
    public static void startSession(
            ServerWorld world,
            UUID sessionId,
            Vec3d sourcePosition,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange,
            SoundCategory category
    ) {
        startSession(
                world,
                sessionId,
                sourcePosition,
                0,
                sound,
                baseVolume,
                pitch,
                audioRange,
                category);
    }

    /**
     * Starts a positioned keyed session that follows a living source entity on clients.
     * The supplied position remains the fallback if the source is not yet client-tracked.
     */
    public static void startSession(
            ServerWorld world,
            UUID sessionId,
            Vec3d sourcePosition,
            int sourceEntityId,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange,
            SoundCategory category
    ) {
        broadcastStart(
                world,
                sourcePosition,
                sourceEntityId,
                sound,
                baseVolume,
                pitch,
                audioRange,
                category,
                sessionId.toString()
        );
    }

    /**
     * Explicitly stops a keyed session for every player in this dimension.
     * Stop packets are not range-limited so listeners who moved away still clean up.
     */
    public static void stopSession(ServerWorld world, UUID sessionId) {
        StopDistantSoundPayload payload = new StopDistantSoundPayload(sessionId);
        for (ServerPlayerEntity player : world.getPlayers()) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private static void broadcastStart(
            ServerWorld world,
            Vec3d sourcePosition,
            int sourceEntityId,
            SoundEvent sound,
            float baseVolume,
            float pitch,
            double audioRange,
            SoundCategory category,
            String sessionKey
    ) {
        if (!isValidPosition(sourcePosition)
                || !Float.isFinite(baseVolume)
                || baseVolume <= MINIMUM_VOLUME
                || !Double.isFinite(audioRange)
                || audioRange <= 0.0) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            double distance = player.getPos().distanceTo(sourcePosition);
            float volume = calculateVolumeFalloff(distance, audioRange) * baseVolume;
            if (volume > MINIMUM_VOLUME) {
                volume = MathHelper.clamp(volume, MINIMUM_VOLUME, MAXIMUM_VOLUME);
                PlayDistantSoundPayload payload = sessionKey.isEmpty()
                        ? PlayDistantSoundPayload.positioned(
                                sound.getId(),
                                sourcePosition,
                                baseVolume,
                                pitch,
                                (float) audioRange,
                                category)
                        : PlayDistantSoundPayload.startSession(
                                sound.getId(),
                                sourcePosition,
                                sourceEntityId,
                                baseVolume,
                                pitch,
                                (float) audioRange,
                                sessionKey,
                                category);
                NetworkManager.sendToPlayer(player, payload);
            }
        }
    }

    private static boolean isValidPosition(Vec3d position) {
        return position != null
                && Double.isFinite(position.x)
                && Double.isFinite(position.y)
                && Double.isFinite(position.z);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private DistantSoundUtil() {}
}
