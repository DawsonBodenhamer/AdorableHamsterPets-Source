package net.dawson.adorablehamsterpets.client.state;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderHamsterState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A client-side data holder attached to each player to manage the state of their shoulder pets.
 * This ensures that all state management is isolated per-player and occurs on the main client thread.
 */
public class ClientShoulderHamsterData {
    private final Map<ShoulderLocation, ShoulderHamsterState> hamsterStates = new EnumMap<>(ShoulderLocation.class);
    private final Map<ShoulderLocation, Integer> animationAges = new EnumMap<>(ShoulderLocation.class);

    /**
     * Ticks the state machines and animation clocks for all active shoulder pets on this player.
     * This method should be called once per client tick.
     *
     * @param player The client player entity this data is attached to.
     */
    public void clientTick(ClientPlayerEntity player) {
        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) player;

        // Determine player movement state once
        boolean isSprinting = player.isSprinting();
        // Player is "walking" if they are moving but not sprinting.
        boolean isWalking = player.getVelocity().horizontalLengthSquared() > 1.0E-7 && !isSprinting;

        // Iterate through all possible locations to update states
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);

            if (!shoulderNbt.isEmpty()) {
                // Get or create the state machine for this slot
                ShoulderHamsterState state = this.hamsterStates.computeIfAbsent(location, l -> new ShoulderHamsterState());

                // Tick the state machine
                state.tick(isSprinting, isWalking);

                // Increment the unique animation age for this slot
                int currentAge = this.animationAges.getOrDefault(location, 0);
                this.animationAges.put(location, currentAge + 1);

            } else {
                // If a slot becomes empty, remove its state and age data to prevent memory leaks
                this.hamsterStates.remove(location);
                this.animationAges.remove(location);
            }
        }
    }

    /**
     * Gets the current animation state for a specific shoulder location.
     *
     * @param location The shoulder location.
     * @return The ShoulderHamsterState, or null if no pet is in that slot.
     */
    @Nullable
    public ShoulderHamsterState getHamsterState(ShoulderLocation location) {
        return this.hamsterStates.get(location);
    }

    /**
     * Gets the unique, persistent animation age for a specific shoulder location.
     *
     * @param location The shoulder location.
     * @return The animation age for that slot.
     */
    public int getAnimationAge(ShoulderLocation location) {
        return this.animationAges.getOrDefault(location, 0);
    }
}