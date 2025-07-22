package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.LookAtEntityGoalAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;

public class HamsterLookAtEntityGoal extends LookAtEntityGoal {

    // --- 1. Fields ---
    private final MobEntity hamsterMob; // Store our own reference
    private final float chance; // Initialize with default vanilla chance
    // --- End 1. Fields ---

    // --- 2. Constructors ---
    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range) {
        super(mob, targetType, range);
        this.hamsterMob = mob; // Initialize our reference
        this.chance = 0.02F; // Initialize the chance
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        super(mob, targetType, range, chance);
        this.hamsterMob = mob;
        this.chance = chance; // Store the chance
    }

    public HamsterLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean lookForward) {
        super(mob, targetType, range, chance, lookForward);
        this.hamsterMob = mob; // Initialize our reference
        this.chance = chance; // Initialize the chance
    }
    // --- End 2. Constructors ---

    // --- 3. Overridden Methods ---
    @Override
    public boolean canStart() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking() || hamster.isCelebratingChase() || hamster.isStealingDiamond()) {
                return false;
            }
        }
        // The vanilla probability check
        if (this.mob.getRandom().nextFloat() >= this.chance) {
            return false;
        }
        return super.canStart();
    }

    @Override
    public void start() {
        super.start();
        if (this.mob instanceof HamsterEntity he) {
            he.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
            AdorableHamsterPets.LOGGER.info("[AI Goal Start] Hamster {} started LookAtEntityGoal.", he.getId());
        }
    }

    @Override
    public boolean shouldContinue() {
        // --- 1. Check Hamster State ---
        // Use our stored 'hamsterMob' reference
        if (this.hamsterMob instanceof HamsterEntity hamster) {
            if (hamster.isSitting() || hamster.isSleeping() || hamster.isKnockedOut() || hamster.isSulking() || hamster.isStealingDiamond()) {
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

    @Override
    public void tick() {
        LookAtEntityGoalAccessor accessor = (LookAtEntityGoalAccessor) this;
        Entity target = accessor.getTarget();

        if (target != null && target.isAlive()) {
            double targetY = accessor.getLookForward() ? this.mob.getEyeY() : target.getEyeY();
            // Use our centralized constants for rotation speed
            this.mob.getLookControl().lookAt(target.getX(), targetY, target.getZ(), HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
            accessor.setLookTime(accessor.getLookTime() - 1);
        }
    }
    // --- End 3. Overridden Methods ---
}