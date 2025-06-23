package net.dawson.adorablehamsterpets.client;

import net.dawson.adorablehamsterpets.client.sound.HamsterCleaningSoundInstance;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client-side tickable sound instances for hamsters.
 * This class is annotated to only load on the client, preventing server-side crashes.
 */
@Environment(EnvType.CLIENT)
public class HamsterSoundManager {

    // A map to track the currently playing cleaning sound for each hamster entity ID.
    private static final Map<Integer, HamsterCleaningSoundInstance> activeCleaningSounds = new ConcurrentHashMap<>();

    /**
     * Called every client tick from HamsterEntity.tick().
     * Manages the starting and stopping of the continuous cleaning sound.
     * @param hamster The hamster entity to manage sounds for.
     */
    public static void tick(HamsterEntity hamster) {
        boolean shouldBeCleaning = hamster.getDataTracker().get(HamsterEntity.IS_CLEANING);
        // Check if currently tracking a sound for this hamster.
        // The sound instance itself will handle when it's "done".
        boolean isSoundTracked = activeCleaningSounds.containsKey(hamster.getId());

        if (shouldBeCleaning && !isSoundTracked) {
            // If it should be cleaning but we aren't tracking a sound, start one.
            HamsterCleaningSoundInstance newSound = new HamsterCleaningSoundInstance(hamster);
            activeCleaningSounds.put(hamster.getId(), newSound);
            MinecraftClient.getInstance().getSoundManager().play(newSound);
        } else if (!shouldBeCleaning && isSoundTracked) {
            // If it shouldn't be cleaning but we are still tracking a sound,
            // the sound instance will stop itself. We just need to remove it from our map.
            activeCleaningSounds.remove(hamster.getId());
        }
    }

    /**
     * Cleans up any tracked sounds for an entity that is being removed.
     * @param hamsterId The ID of the hamster being removed.
     */
    public static void onHamsterRemoved(int hamsterId) {
        activeCleaningSounds.remove(hamsterId);
    }
}