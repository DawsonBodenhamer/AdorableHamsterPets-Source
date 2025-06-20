package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;


public class HamsterWanderAroundFarGoal extends WanderAroundFarGoal {
    private final HamsterEntity hamster;

    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed) {
        super(hamster, speed);
        this.hamster = hamster;
    }

    // Constructor for speed and probability
    public HamsterWanderAroundFarGoal(HamsterEntity hamster, double speed, float probability) {
        super(hamster, speed, probability);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (this.mob instanceof HamsterEntity he && (he.isSitting() || he.isSleeping() || he.isKnockedOut() || he.isSulking() || he.isCelebratingDiamond())) {
            return false;
        }
        return super.canStart(); // Call the original MobEntity's canStart logic
    }

    @Override
    public void start() {
        super.start(); // Call vanilla logic first
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop(); // Call vanilla logic first
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}