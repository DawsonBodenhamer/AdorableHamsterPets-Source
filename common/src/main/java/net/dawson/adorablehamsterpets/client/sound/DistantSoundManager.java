package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.networking.ModPackets;
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
    public void handlePlay(MinecraftClient client, ModPackets.PlayDistantSoundS2CPacket packet) {
        if (client.world == null) {
            return;
        }

        this.resetForWorldChange(client);

        if (!packet.sessionKey().isEmpty()) {
            this.replaceSession(client, packet);
        } else if (packet.position().isPresent()) {
            client.getSoundManager().play(new DistantSoundInstance(packet));
        } else if (client.player != null) {
            // Positionless packets play at the listener position
            client.world.playSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvent.of(packet.soundId()),
                    packet.category(),
                    packet.volume(),
                    packet.pitch(),
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

    private void replaceSession(MinecraftClient client, ModPackets.PlayDistantSoundS2CPacket packet) {
        this.stopSession(client, packet.sessionKey());

        DistantSoundInstance sound = new DistantSoundInstance(packet);
        this.activeSessions.put(packet.sessionKey(), sound);
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
