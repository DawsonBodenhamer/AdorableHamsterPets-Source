package net.dawson.adorablehamsterpets.entity.AI;


import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.MeleeAttackGoalAccessor;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.sound.SoundEvent;


public class HamsterMeleeAttackGoal extends MeleeAttackGoal {
    private final HamsterEntity hamster;
    private static final int CUSTOM_ATTACK_COOLDOWN_TICKS = 35;


    public HamsterMeleeAttackGoal(HamsterEntity hamster, double speed, boolean pauseWhenMobIdle) {
        super(hamster, speed, pauseWhenMobIdle);
        this.hamster = hamster;
    }


    @Override
    protected void attack(LivingEntity target, double squaredDistance) {
        // --- Gatekeeper Logic ---
        // Check 1: Is the cooldown ready?
        // Check 2: Is the target in the custom attack range?
        if (this.getCooldown() <= 0 && this.hamster.isInAttackRange(target)) {

            // --- Attack Action ---
            this.resetCooldown();
            this.mob.tryAttack(target);

            // --- Sound and Animation ---
            SoundEvent attackSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_ATTACK_SOUNDS, this.hamster.getRandom());
            if (attackSound != null) {
                this.hamster.playSound(attackSound, 1.2F, this.hamster.getSoundPitch());
            }
            this.hamster.triggerAnimOnServer("mainController", "attack");

            AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Attack performed on target {}. Cooldown reset to {}.",
                    this.hamster.getId(), this.hamster.getWorld().getTime(),
                    target.getId(), this.getMaxCooldown());
        }
    }


    @Override
    protected int getMaxCooldown() {
        return CUSTOM_ATTACK_COOLDOWN_TICKS;
    }


    @Override
    protected void resetCooldown() {
        // Cast 'this' to the accessor interface and call the public setter method.
        ((MeleeAttackGoalAccessor) this).setCooldown(this.getMaxCooldown());
    }


    @Override
    public boolean canStart() {
        // Check the master sitting state
        if (this.hamster.isSitting()) {
            return false;
        }
        return super.canStart();
    }


    @Override
    public void start() {
        super.start();
        AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Goal started.", this.hamster.getId(), this.hamster.getWorld().getTime());
        // Use the accessor to set the cooldown to 0, making the hamster able to attack immediately.
        ((MeleeAttackGoalAccessor) this).setCooldown(0);
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }


    @Override
    public void stop() {
        super.stop();
        AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Goal stopped.", this.hamster.getId(), this.hamster.getWorld().getTime());
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        super.tick(); // Handles pathing updates and cooldown decrementing
        // We need to call attack() every tick because the superclass doesn't call it automatically
        // if we override tick() without calling super.tick() *first*.
        // However, the actual attack logic is now correctly gated by canAttack().
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.attack(target, this.mob.squaredDistanceTo(target));
        }
    }
}
