package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAroundGoal extends LookAroundGoal {

    // --- 1. Fields ---
    private final MobEntity hamsterMob; // Store our own reference
    // --- End 1. Fields ---

    // --- 2. Constructor ---
    public HamsterLookAroundGoal(MobEntity mob) {
        super(mob);
        this.hamsterMob = mob; // Initialize our reference
    }
    // --- End 2. Constructor ---

    // --- 3. Overridden Methods ---
    @Override
    public boolean canStart() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()) {
                return false;
            }
        }
        // --- End 1. Check Hamster State ---
        return super.canStart();
    }

    @Override
    public void start() {
        super.start();
        if (this.hamsterMob instanceof HamsterEntity he) {
            he.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking()) {
                return false;
            }
        }
        // --- End 1. Check Hamster State ---
        return super.shouldContinue();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamsterMob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalDebugName("None");
            }
        }
    }
    // --- End 3. Overridden Methods ---
}