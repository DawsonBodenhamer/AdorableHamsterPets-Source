package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.FollowOwnerGoalAccessor;
import net.dawson.adorablehamsterpets.mixin.accessor.LandPathNodeMakerInvoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
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
        // --- 1. Let the vanilla logic run first ---
        // This is crucial because super.canStart() finds and sets the 'owner' field.
        if (!super.canStart()) {
            return false;
        }

        // --- 2. Apply  custom conditions ---
        if (this.hamster.isSitting() ||
                this.hamster.isSleeping() ||
                this.hamster.isKnockedOut() ||
                this.hamster.isSulking() ||
                this.hamster.isCelebratingDiamond() ||
                this.hamster.isCelebratingChase()) {
            return false;
        }

        // --- 3. Re-check distance with buff modification ---
        // Use accessor to get the base minimum distance
        float minDist = ((FollowOwnerGoalAccessor) this).getMinDistance();
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return false; // Should be handled by super, but good practice

        // Dynamically adjust min distance for buffed state
        if (this.hamster.hasGreenBeanBuff()) {
            minDist += 5.0F;
        }

        // Re-check the distance condition with the potentially modified value
        return !(hamster.squaredDistanceTo(owner) < (double) (minDist * minDist));
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
        AdorableHamsterPets.LOGGER.debug("[FollowGoal-{}] start: Goal has started. IsBuffed: {}", this.hamster.getId(), this.hamster.hasGreenBeanBuff());
    }

    @Override
    public void tick() {
        // --- 1. Get Owner and Check Teleport Condition ---
        // This logic is now shared for both buffed and non-buffed states.
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = ((FollowOwnerGoalAccessor) this).getOwner();
        if (owner == null) return; // Safety check

        // --- 2. Handle Looking ---
       this.hamster.getLookControl().lookAt(owner, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);

        // --- 4. Use Vanilla Update Timer via Accessor ---
        int currentTicks = accessor.getUpdateCountdownTicks() - 1;
        accessor.setUpdateCountdownTicks(currentTicks);

        if (currentTicks <= 0) {
            accessor.setUpdateCountdownTicks(this.getTickCount(10));

            // --- Replicated 1.20.1 Teleport Logic ---
            float maxDist = accessor.getMaxDistance();
            if (this.hamster.hasGreenBeanBuff()) {
                maxDist += 5.0f; // Apply buff offset to max distance as well
            }

            if (this.hamster.squaredDistanceTo(owner) >= (double)(maxDist * maxDist)) {
                this.teleportToOwner(); // Call the new helper method
            } else {
                // --- Pathfinding Logic ---
                if (this.hamster.hasGreenBeanBuff()) {
                    Vec3d targetPos = FuzzyTargeting.findTo(this.hamster, 8, 5, Vec3d.ofCenter(owner.getBlockPos()));
                    if (targetPos != null) {
                        this.hamster.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, BUFFED_FOLLOW_SPEED);
                    }
                } else {
                    this.hamster.getNavigation().startMovingTo(owner, accessor.getSpeed());
                }
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    // --- Private Helper Methods ---
    /**
     * Replicates the teleportation logic from the 1.20.1 FollowOwnerGoal.
     */
    private void teleportToOwner() {
        FollowOwnerGoalAccessor accessor = (FollowOwnerGoalAccessor) this;
        LivingEntity owner = accessor.getOwner();
        if (owner == null) return;

        BlockPos blockPos = owner.getBlockPos();

        for(int i = 0; i < 10; ++i) {
            int j = this.getRandomInt(-3, 3);
            int k = this.getRandomInt(-1, 1);
            int l = this.getRandomInt(-3, 3);
            boolean success = this.tryTeleportTo(blockPos.getX() + j, blockPos.getY() + k, blockPos.getZ() + l);
            if (success) {
                return;
            }
        }
    }

    /**
     * Helper for teleportToOwner, replicates vanilla logic.
     */
    private boolean tryTeleportTo(int x, int y, int z) {
        if (Math.abs((double)x - ((FollowOwnerGoalAccessor)this).getOwner().getX()) < 2.0 && Math.abs((double)z - ((FollowOwnerGoalAccessor)this).getOwner().getZ()) < 2.0) {
            return false;
        } else if (!this.isTeleportPositionClear(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.hamster.refreshPositionAndAngles((double)x + 0.5, (double)y, (double)z + 0.5, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
            this.hamster.getNavigation().stop();
            return true;
        }
    }

    /**
     * Helper for teleportToOwner, replicates vanilla logic.
     */
    private boolean isTeleportPositionClear(BlockPos pos) {
        // Use the invoker to safely access the vanilla path node type logic.
        PathNodeType pathNodeType = LandPathNodeMakerInvoker.callGetCommonNodeType(this.hamster.getWorld(), pos.mutableCopy());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        } else {
            BlockPos floorPos = pos.down();
            BlockState floorState = this.hamster.getWorld().getBlockState(floorPos);
            // Check that the block below has a collision shape, ensuring it's a valid surface to stand on.
            if (floorState.getCollisionShape(this.hamster.getWorld(), floorPos, ShapeContext.absent()).isEmpty()) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(this.hamster.getBlockPos());
                return this.hamster.getWorld().isSpaceEmpty(this.hamster, this.hamster.getBoundingBox().offset(blockPos));
            }
        }
    }

    /**
     * Helper for teleportToOwner, replicates vanilla logic.
     */
    private int getRandomInt(int min, int max) {
        return this.hamster.getRandom().nextInt(max - min + 1) + min;
    }
}