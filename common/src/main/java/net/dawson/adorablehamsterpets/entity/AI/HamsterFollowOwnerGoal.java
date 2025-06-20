package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {
    private final HamsterEntity hamster;

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        super(hamster, speed, minDistance, maxDistance);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond()) {
            return false;
        }
        return super.canStart(); // Defer to vanilla logic if no custom conditions met
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond()) {
            return false;
        }
        return super.shouldContinue(); // Defer to vanilla logic
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}