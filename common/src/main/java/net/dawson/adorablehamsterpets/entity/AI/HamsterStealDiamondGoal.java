package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HamsterStealDiamondGoal extends Goal {

    private final HamsterEntity hamster;
    private final World world;
    @Nullable
    private ItemEntity targetItem;
    @Nullable
    private PlayerEntity owner;

    private enum State {
        SCANNING,
        MOVING_TO_DIAMOND,
        POUNCING,
        FLEEING,
        TAUNTING
    }

    private State currentState = State.SCANNING;
    private int pounceTimer;
    private int stealDurationTimer;

    public HamsterStealDiamondGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (!Configs.AHP.enableDiamondStealing || this.hamster.isStealingDiamond() || this.hamster.isSitting()) {
            return false;
        }

        if (this.hamster.getRandom().nextFloat() >= Configs.AHP.diamondPounceChance.get()) {
            return false;
        }

        if (!(this.hamster.getOwner() instanceof PlayerEntity playerOwner)) {
            return false;
        }
        this.owner = playerOwner;

        // Access the list and convert string IDs to Item objects
        List<Item> stealableItems = Configs.AHP.stealableItems.stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(Registries.ITEM::get)
                .filter(item -> item != Items.AIR)
                .toList();

        if (stealableItems.isEmpty()) {
            return false; // No valid items to steal were configured
        }

        List<ItemEntity> nearbyItems = this.world.getEntitiesByClass(
                ItemEntity.class,
                this.hamster.getBoundingBox().expand(10.0),
                itemEntity -> stealableItems.contains(itemEntity.getStack().getItem()) && itemEntity.isOnGround()
        );

        Optional<ItemEntity> closestItem = nearbyItems.stream()
                .filter(item -> this.hamster.getNavigation().findPathTo(item, 0) != null)
                .min((item1, item2) -> Float.compare(item1.distanceTo(this.hamster), item2.distanceTo(this.hamster)));

        if (closestItem.isPresent()) {
            this.targetItem = closestItem.get();
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (this.hamster.isSitting() || this.targetItem == null || !this.targetItem.isAlive() || this.owner == null || !this.owner.isAlive()) {
            return false;
        }
        return this.stealDurationTimer > 0;
    }

    @Override
    public void start() {
        this.currentState = State.MOVING_TO_DIAMOND;
        this.hamster.getNavigation().startMovingTo(this.targetItem, 1.2D);
        this.stealDurationTimer = this.hamster.getRandom().nextBetween(
                Configs.AHP.minStealDurationSeconds.get() * 20,
                Configs.AHP.maxStealDurationSeconds.get() * 20
        );
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName());
    }

    @Override
    public void stop() {
        if (this.hamster.isStealingDiamond()) {
            // Drop the item if the timer ran out
            if (this.targetItem != null) {
                this.world.spawnEntity(new ItemEntity(this.world, this.hamster.getX(), this.hamster.getY(), this.hamster.getZ(), this.targetItem.getStack()));
                this.hamster.playSound(ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_HURT_SOUNDS, this.hamster.getRandom()), 1.0f, 1.0f);
            }
        }
        this.hamster.setStealingDiamond(false);
        this.hamster.getNavigation().stop();
        this.targetItem = null;
        this.owner = null;
        this.currentState = State.SCANNING;
        if (this.hamster.getActiveCustomGoalDebugName().equals(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
    }

    @Override
    public void tick() {
        if (this.targetItem == null || this.owner == null) {
            return;
        }

        this.stealDurationTimer--;

        switch (this.currentState) {
            case MOVING_TO_DIAMOND:
                this.hamster.getLookControl().lookAt(this.targetItem);
                if (this.hamster.distanceTo(this.targetItem) < 1.5) {
                    this.currentState = State.POUNCING;
                    this.pounceTimer = 20; // 1 second for the pounce animation
                    this.hamster.getNavigation().stop();
                    this.hamster.triggerAnimOnServer("mainController", "anim_hamster_diamond_pounce");
                    this.hamster.playSound(ModSounds.HAMSTER_BOUNCE.get(), 0.8f, this.hamster.getSoundPitch() * 1.2f);
                }
                break;

            case POUNCING:
                this.pounceTimer--;
                if (this.pounceTimer <= 0) {
                    this.targetItem.discard();
                    this.hamster.setStealingDiamond(true);

                    // Play composite sound
                    this.world.playSound(null, this.hamster.getBlockPos(), ModSounds.HAMSTER_DIAMOND_POUNCE.get(), SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    SoundEvent celebrateSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CELEBRATE_SOUNDS, this.hamster.getRandom());
                    if (celebrateSound != null) {
                        this.world.playSound(null, this.hamster.getBlockPos(), celebrateSound, SoundCategory.NEUTRAL, 0.7f, 1.2f);
                    }

                    this.currentState = State.FLEEING;
                }
                break;

            case FLEEING:
                if (this.hamster.distanceTo(this.owner) < Configs.AHP.minFleeDistance.get()) {
                    Vec3d fleePos = FuzzyTargeting.findFrom(this.hamster, Configs.AHP.maxFleeDistance.get(), 7, this.owner.getPos());
                    // First, check if a valid flee position was found.
                    if (fleePos != null) {
                        // Then, attempt to start moving to it.
                        this.hamster.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.4D);
                    }
                } else {
                    this.currentState = State.TAUNTING;
                    this.hamster.getNavigation().stop();
                }
                break;

            case TAUNTING:
                this.hamster.getLookControl().lookAt(this.owner, 10.0f, (float)this.hamster.getMaxLookPitchChange());
                if (this.hamster.distanceTo(this.owner) < Configs.AHP.minFleeDistance.get()) {
                    this.currentState = State.FLEEING;
                }
                break;
        }
    }
}