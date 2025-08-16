package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.tag.ModItemTags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Ingredient;

public class HamsterTemptGoal extends TemptGoal {

    // --- 1. Fields ---
    private final HamsterEntity hamster;
    private int recheckTimer = 0; // Frequency of begging state updates

    // --- 2. Constructors ---
    public HamsterTemptGoal(HamsterEntity hamster, double speed, boolean canBeScared) {
        // Pass an empty ingredient to the super constructor. Then override the check.
        super(hamster, speed, Ingredient.EMPTY, canBeScared);
        this.hamster = hamster;
    }

    // --- 3. Public Methods (Overrides from TemptGoal/Goal) ---
    /**
     * Overrides the vanilla item check to use our dynamic, config-driven item tags.
     * This is the core of the backport, allowing the goal to function without a static Ingredient.
     */
    private boolean isTemptedBy(LivingEntity entity) {
        return ModItemTags.isTamingFood(entity.getMainHandStack()) || ModItemTags.isTamingFood(entity.getOffHandStack());
    }

    @Override
    public void start() {
        super.start();
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public boolean canStart() {
        // --- 1. Initial State Checks ---
        if (this.hamster.isSitting() || this.hamster.isCelebratingDiamond()) {
            return false;
        }

        // --- 2. Superclass Logic ---
        // The super.canStart() will call our overridden isTemptedBy method.
        if (!super.canStart()) {
            return false;
        }

        // --- 3. Ownership Check ---
        if (this.hamster.isTamed()) {
            return this.hamster.isOwner(this.closestPlayer);
        }

        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isSitting() || this.hamster.isCelebratingDiamond()) {
            return false;
        }
        return super.shouldContinue();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.recheckTimer > 0) {
            this.recheckTimer--;
            return;
        }
        this.recheckTimer = 5;

        PlayerEntity temptingPlayer = this.closestPlayer;

        if (temptingPlayer != null && temptingPlayer.isAlive() && this.hamster.squaredDistanceTo(temptingPlayer) < 64.0) {
            this.hamster.setBegging(isHoldingTemptItem(temptingPlayer));
        } else {
            this.hamster.setBegging(false);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.hamster.setBegging(false);
        this.recheckTimer = 0;
    }

    // --- 4. Private Helper Methods ---
    private boolean isHoldingTemptItem(PlayerEntity player) {
        // This check is slightly redundant with isTemptedBy in 1.20.1, but keeping it so I don't have to refactor the tick() method.
        return ModItemTags.isTamingFood(player.getMainHandStack()) || ModItemTags.isTamingFood(player.getOffHandStack());
    }
}