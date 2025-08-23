package net.dawson.adorablehamsterpets.entity.client.feature;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.util.math.random.Random;

/**
 * Manages the animation state for a single shoulder-mounted hamster.
 * This class is used exclusively on the client within the HamsterShoulderFeatureRenderer.
 */
public class ShoulderHamsterState {
    private ShoulderAnimationState currentState;
    private int timer; // Ticks remaining in the current state
    private final Random random = Random.create();
    private int sprintTransitionDelay = 0;

    public ShoulderHamsterState() {
        // Start in a random state
        this.currentState = ShoulderAnimationState.values()[random.nextInt(ShoulderAnimationState.values().length)];
        this.timer = getNewRandomDuration();
    }

    /**
     * Ticks the state machine. This should be called once per game tick.
     * @param isPlayerSprinting True if the player is currently sprinting.
     */
    public void tick(boolean isPlayerSprinting, boolean isPlayerWalking) {
        // --- 1. Movement Logic (Highest Priority) ---
        boolean shouldForceLayDown = (Configs.AHP.forceLayDownOnSprint && isPlayerSprinting) ||
                (Configs.AHP.forceLayDownOnWalk && isPlayerWalking);

        if (shouldForceLayDown) {
            // If not already laying down or preparing to, start the transition.
            if (this.currentState != ShoulderAnimationState.LAYING_DOWN && this.sprintTransitionDelay == 0) {
                this.sprintTransitionDelay = random.nextBetween(1, 7);
            }

            // Countdown the sprint delay timer.
            if (this.sprintTransitionDelay > 0) {
                this.sprintTransitionDelay--;
                if (this.sprintTransitionDelay == 0) {
                    // Transition is complete. Force the state and set the temporary timer.
                    this.currentState = ShoulderAnimationState.LAYING_DOWN;
                    this.timer = random.nextBetween(5, 40); // 0.25-2 seconds
                }
            }
            // The state is now either LAYING_DOWN or a previous state during the delay.
            // No other logic should run this tick. A 'return' is unnecessary here, as the last statement in a 'void' method
        }
        // --- 2. Normal State Logic (Runs only if not sprinting) ---
        else {
            // Reset the sprint delay when not sprinting.
            this.sprintTransitionDelay = 0;

            // Countdown the main timer.
            if (this.timer > 0) {
                this.timer--;
            }

            // If the timer has expired, determine the next state.
            if (this.timer <= 0) {
                if (Configs.AHP.enableDynamicShoulderAnimations.get()) {
                    // DYNAMIC: Pick a new random state.
                    ShoulderAnimationState nextState;
                    do {
                        nextState = ShoulderAnimationState.values()[random.nextInt(ShoulderAnimationState.values().length)];
                    } while (nextState == this.currentState);
                    this.currentState = nextState;
                    this.timer = getNewRandomDuration(); // Reset with config duration.
                } else {
                    // NOT DYNAMIC: Snap to the forced state from config.
                    this.currentState = switch (Configs.AHP.forcedShoulderState.get()) {
                        case ALWAYS_SIT -> ShoulderAnimationState.SITTING;
                        case ALWAYS_LAY_DOWN -> ShoulderAnimationState.LAYING_DOWN;
                        default -> ShoulderAnimationState.STANDING;
                    };
                }
            }
        }
    }

    /**
     * Gets the current animation state determined by the state machine.
     * @return The current ShoulderAnimationState.
     */
    public ShoulderAnimationState getCurrentState() {
        return this.currentState;
    }

    /**
     * Updates the dummy hamster's internal DataTrackers and flags based on the current state.
     * This is the crucial link that allows the entity's own animation controller to function correctly.
     * @param hamster The dummy hamster entity to update.
     */
    private void updateHamsterEntityState(HamsterEntity hamster, ShoulderAnimationState stateToApply) {
        // Set the primary shoulder animation state tracker
        hamster.getDataTracker().set(HamsterEntity.SHOULDER_ANIMATION_STATE, stateToApply.ordinal());

        // Also update the core 'isSitting' flag. This is essential for the cleaning animation logic
        // and any other logic that relies on the general sitting state.
        hamster.setSitting(stateToApply == ShoulderAnimationState.SITTING, true);
    }

    private int getNewRandomDuration() {
        int min = Configs.AHP.shoulderMinStateSeconds.get() * 20;
        int max = Configs.AHP.shoulderMaxStateSeconds.get() * 20;
        if (min >= max) return min;
        return this.random.nextBetween(min, max);
    }
}