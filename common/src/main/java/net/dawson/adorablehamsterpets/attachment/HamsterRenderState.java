package net.dawson.adorablehamsterpets.attachment;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Holds transient state about how a Hamster is being rendered by clients.
 * This is NOT persisted to NBT.
 */
public class HamsterRenderState {

    // --- Server-Side State ---
    private final Set<UUID> renderingPlayers = new HashSet<>();

    // --- Client-Side State ---
    private boolean isClientRendering = false;
    private int soundDelayTicks = 0;

    // --- Server-Side Methods ---
    public void addPlayer(PlayerEntity player) {
        this.renderingPlayers.add(player.getUuid());
    }

    public void removePlayer(PlayerEntity player) {
        this.renderingPlayers.remove(player.getUuid());
    }

    public boolean isBeingRenderedByAnyPlayer() {
        return !this.renderingPlayers.isEmpty();
    }

    // --- Client-Side Methods ---
    public boolean isClientRendering() {
        return this.isClientRendering;
    }

    public void setClientRendering(boolean rendering) {
        this.isClientRendering = rendering;
    }

    public void startSoundDelay() {
        this.soundDelayTicks = 10; // 10-tick delay
    }

    public boolean isSoundDelayed() {
        return this.soundDelayTicks > 0;
    }

    public void tickClient() {
        if (this.soundDelayTicks > 0) {
            this.soundDelayTicks--;
        }
    }
}