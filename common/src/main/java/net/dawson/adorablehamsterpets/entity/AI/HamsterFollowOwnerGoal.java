package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.util.math.Vec3d;

public class HamsterFollowOwnerGoal extends FollowOwnerGoal {
    private final HamsterEntity hamster;
    private static final double BUFFED_FOLLOW_SPEED = 1.5D;

    public HamsterFollowOwnerGoal(HamsterEntity hamster, double speed, float minDistance, float maxDistance) {
        super(hamster, speed, minDistance, maxDistance, false);
        this.hamster = hamster;
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }

        // Use accessor to get the base minimum distance
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false; // Should be handled by super, but good practice

        // Dynamically adjust min distance for buffed state
        if (this.hamster.hasGreenBeanBuff()) {
            minDist += 5.0F;
        }

        // Re-check the distance condition with the potentially modified value
        return !(this.hamster.squaredDistanceTo(owner) < (double)(minDist * minDist));
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingChase()) {
            return false;
        }

        // Use accessor to get the base maximum distance
        float maxDist = ((FollowOwnerGoalAccessor) this).getMaxDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false;

        // Dynamically adjust max distance for buffed state
        if (this.hamster.hasGreenBeanBuff()) {
            maxDist += 5.0F;
        }

        // The rest of the vanilla logic
        return !this.hamster.getNavigation().isIdle() && this.hamster.squaredDistanceTo(owner) > (double)(maxDist * maxDist);
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (this.hamster.hasGreenBeanBuff() ? " (Zoomies)" : ""));
        AdorableHamsterPets.LOGGER.info("[FollowGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Override
    public void tick() {
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return;

        this.hamster.getLookControl().lookAt(owner, 10.0F, (float)this.hamster.getMaxLookPitchChange());

        if (this.hamster.hasGreenBeanBuff()) {
            // "Zoomies" follow logic
            if (this.hamster.getNavigation().isIdle() || this.hamster.getRandom().nextInt(20) == 0) {
                Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 4, Vec3d.ofCenter(owner.getBlockPos()));
                if (targetPos != null) {
                    this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, BUFFED_FOLLOW_SPEED);
                }
            }
        } else {
            // Normal follow logic
            super.tick();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }
}