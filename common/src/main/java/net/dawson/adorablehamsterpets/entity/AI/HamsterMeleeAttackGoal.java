package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
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
    protected void attack(LivingEntity target) {
        // --- Add the crucial canAttack check back ---
        // This check includes isCooledDown(), isInAttackRange(), and canSee()
        if (this.canAttack(target)) {
            // --- Code inside this block only runs if cooldown is ready AND target is in range/visible ---

            // Reset cooldown using the custom duration
            this.resetCooldown();
            AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Attack condition met (cooldown {}, in range), attacking target {}. Cooldown reset to {}.",
                    this.hamster.getId(), this.hamster.getWorld().getTime(), this.getCooldown(), // Log cooldown *before* reset for clarity
                    target.getId(), this.getMaxCooldown()); // Log the value it's being reset to

            // Play Sound
            SoundEvent attackSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_ATTACK_SOUNDS, this.hamster.getRandom());
            if (attackSound != null) {
                this.hamster.playSound(attackSound, 1.2F, this.hamster.getSoundPitch());
                AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Played attack sound: {}", this.hamster.getId(), this.hamster.getWorld().getTime(), attackSound.getId());
            }

            // Trigger Attack Animation (Server-Side)
            this.hamster.triggerAnimOnServer("mainController", "attack");

            // Deal Damage
            this.mob.tryAttack(target);
            AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Called tryAttack() on target {}.", this.hamster.getId(), this.hamster.getWorld().getTime(), target.getId());

        }
    }

    @Override
    protected int getMaxCooldown() {
        return CUSTOM_ATTACK_COOLDOWN_TICKS;
    }

    @Override
    protected void resetCooldown() {
        this.cooldown = this.getMaxCooldown();
        // Logging moved to attack() method for better context
    }


    @Override
    public void start() {
        super.start();
        AdorableHamsterPets.LOGGER.debug("[AttackGoal {} Tick {}] Goal started.", this.hamster.getId(), this.hamster.getWorld().getTime());
        this.cooldown = 0;
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
            this.attack(target); // Call attack logic check every tick
        }
    }
}