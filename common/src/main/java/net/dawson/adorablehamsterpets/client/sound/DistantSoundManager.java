package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.networking.payload.PlayDistantSoundPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Owns positioned distant sounds and keyed session replacement/cancellation.
 */
public final class DistantSoundManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *                          Static Access and State
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final DistantSoundManager INSTANCE = new DistantSoundManager();

    private final Map<String, DistantSoundInstance> activeSessions = new HashMap<>();
    private Object activeWorld;

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Constructor
     * ────────────────────────────────────────────────────────────────────────────*/

    private DistantSoundManager() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Packet Handling
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Handles listener-position one-shots, positioned one-shots, and keyed
     * session starts.
     */
    public void handlePlay(MinecraftClient client, PlayDistantSoundPayload payload) {
        if (client.world == null) {
            return;
        }

        this.resetForWorldChange(client);

        if (!payload.sessionKey().isEmpty()) {
            this.replaceSession(client, payload);
        } else if (payload.position().isPresent()) {
            client.getSoundManager().play(new DistantSoundInstance(payload));
        } else if (client.player != null) {
            // Positionless payloads play at the listener position
            client.world.playSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvent.of(payload.soundId()),
                    payload.category(),
                    payload.volume(),
                    payload.pitch(),
                    false
            );
        }
    }

    /**
     * Stops a keyed session. Unknown keys are harmless, which makes cancellation
     * idempotent across reconnects and late packets.
     */
    public void stopSession(MinecraftClient client, String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) {
            return;
        }

        DistantSoundInstance sound = this.activeSessions.remove(sessionKey);
        if (sound != null) {
            this.stop(client, sound);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                  Tick Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Removes naturally completed sessions and clears sounds across world changes.
     */
    public void tick(MinecraftClient client) {
        if (client.world == null) {
            this.reset(client);
            return;
        }

        this.resetForWorldChange(client);

        SoundManager soundManager = client.getSoundManager();
        Iterator<Map.Entry<String, DistantSoundInstance>> iterator = this.activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            DistantSoundInstance sound = iterator.next().getValue();
            if (sound.isDone() || !soundManager.isPlaying(sound)) {
                sound.markDone();
                soundManager.stop(sound);
                iterator.remove();
            }
        }
    }

    /**
     * Stops and forgets all sessions, including when the client leaves a world.
     */
    public void reset(MinecraftClient client) {
        for (DistantSoundInstance sound : this.activeSessions.values()) {
            this.stop(client, sound);
        }
        this.activeSessions.clear();
        this.activeWorld = null;
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *                                Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void replaceSession(MinecraftClient client, PlayDistantSoundPayload payload) {
        this.stopSession(client, payload.sessionKey());

        DistantSoundInstance sound = new DistantSoundInstance(payload);
        this.activeSessions.put(payload.sessionKey(), sound);
        client.getSoundManager().play(sound);
    }

    private void resetForWorldChange(MinecraftClient client) {
        if (this.activeWorld != null && this.activeWorld != client.world) {
            this.reset(client);
        }
        this.activeWorld = client.world;
    }

    private void stop(MinecraftClient client, DistantSoundInstance sound) {
        sound.markDone();
        client.getSoundManager().stop(sound);
    }

}
