package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAtEntityGoal extends LookAtEntityGoal {

    // --- 1. Fields ---
    private final MobEntity hamsterMob; // Store our own reference
    // --- End 1. Fields ---

    // --- 2. Constructors ---
    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range) {
        super(mob, targetType, range);
        this.hamsterMob = mob; // Initialize our reference
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.hamsterMob = mob; // Initialize our reference
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean lookForward) {
        super(mob, targetType, range, chance, lookForward);
        this.hamsterMob = mob; // Initialize our reference
    }
    // --- End 2. Constructors ---

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
        if (this.mob instanceof HamsterEntity he) {
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
        if (this.mob instanceof HamsterEntity he) {
            if (he.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
                he.setActiveCustomGoalDebugName("None");
            }
        }
    }
    // --- End 3. Overridden Methods ---
}