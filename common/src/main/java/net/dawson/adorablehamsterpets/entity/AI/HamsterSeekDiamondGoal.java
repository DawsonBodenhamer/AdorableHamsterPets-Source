package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HamsterSeekDiamondGoal extends Goal {

    private final HamsterEntity hamster;
    private final World world;
    private BlockPos targetOrePos; // The specific ore block being targeted
    private boolean isSeekingGold; // True if the current target is gold ore

    private enum SeekingState {
        IDLE,
        SCANNING,
        MOVING_TO_ORE,
        WAITING_FOR_PATH,
        CELEBRATING_DIAMOND,
        SULKING_AT_GOLD
    }

    private SeekingState currentState = SeekingState.IDLE;
    private int pathingTickTimer;
    private int soundTimer;

    private static final int PATHING_RECHECK_INTERVAL = 20; // Ticks (1 second)
    private static final int SNIFF_SOUND_INTERVAL_MOVING = 30; // Less than 2 seconds
    private static final int SNIFF_SOUND_INTERVAL_WAITING = 160; // Approx 8 seconds

    public HamsterSeekDiamondGoal(HamsterEntity hamster) {
        this.hamster = hamster;
        this.world = hamster.getWorld();
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (world.isClient || !Configs.AHP.enableIndependentDiamondSeeking) {
            return false;
        }
        // Check the isPrimedToSeekDiamonds flag directly
        if (!this.hamster.isPrimedToSeekDiamonds) {
            return false;
        }
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) {
            return false;
        }
        if (this.hamster.getTarget() != null) { // In combat
            return false;
        }
        if (Configs.AHP.enableIndependentDiamondSeekCooldown &&
                this.hamster.foundOreCooldownEndTick > this.world.getTime()) {
            return false;
        }
        // Attempt to find a target only if all above conditions pass
        return findNewTargetOreAndSetState();
    }

    private boolean findNewTargetOreAndSetState() {
        this.targetOrePos = null; // Reset before scan
        this.isSeekingGold = false;
        this.hamster.currentOreTarget = null; // Clear entity's direct target tracker initially

        List<BlockPos> diamondOres = new ArrayList<>();
        List<BlockPos> goldOres = new ArrayList<>();
        int radius = Configs.AHP.diamondSeekRadius.get();

        for (BlockPos pos : BlockPos.iterateOutwards(hamster.getBlockPos(), radius, radius, radius)) {
            Block block = world.getBlockState(pos).getBlock();
            if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                diamondOres.add(pos.toImmutable());
            } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
                goldOres.add(pos.toImmutable());
            }
        }

        if (diamondOres.isEmpty()) {
            // No diamond ore found. isPrimedToSeekDiamonds remains true, goal just won't start this tick.
            return false;
        }

        diamondOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
        goldOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));

        if (!goldOres.isEmpty() && this.world.random.nextFloat() < 0.33f) {
            this.targetOrePos = goldOres.get(0);
            this.isSeekingGold = true;
        } else {
            this.targetOrePos = diamondOres.get(0);
            this.isSeekingGold = false;
        }
        this.hamster.currentOreTarget = this.targetOrePos; // Store in entity for persistence/debug
        this.currentState = SeekingState.SCANNING; // Mark that we have a target and are ready to attempt pathing
        return true; // A target was selected
    }

    @Override
    public void start() {
        this.hamster.setActiveCustomGoalDebugName(this.getClass().getSimpleName() + (isSeekingGold ? "_Gold" : "_Diamond"));
        this.pathingTickTimer = 0;
        this.soundTimer = 0;
        // currentState is already SCANNING from canStart/findNewTargetOreAndSetState
        attemptPathToTarget();
    }

    private void attemptPathToTarget() {
        if (this.targetOrePos == null) {
            // This might happen if the goal was started but then the target became invalid before first tick
            this.currentState = SeekingState.IDLE; // Go to idle to allow canStart to re-evaluate
            return;
        }
        boolean pathFound = this.hamster.getNavigation().startMovingTo(
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY(), // Target the ore's Y level
                this.targetOrePos.getZ() + 0.5,
                0.7D // 70% speed
        );

        if (pathFound) {
            this.currentState = SeekingState.MOVING_TO_ORE;
            this.soundTimer = SNIFF_SOUND_INTERVAL_MOVING / 2;
        } else {
            this.currentState = SeekingState.WAITING_FOR_PATH;
            this.pathingTickTimer = PATHING_RECHECK_INTERVAL; // Start timer to recheck path
            this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING / 2;
        }
    }

    @Override
    public boolean shouldContinue() {
        // Terminal states for this goal instance
        if (this.currentState == SeekingState.IDLE || this.currentState == SeekingState.CELEBRATING_DIAMOND || this.currentState == SeekingState.SULKING_AT_GOLD) {
            return false;
        }
        // Interruptions
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isSulking()) {
            return false;
        }
        if (this.hamster.getTarget() != null) { // Combat
            return false;
        }
        // Target validity
        if (this.targetOrePos == null) return false; // Should be caught by IDLE state, but good check

        Block targetBlock = world.getBlockState(this.targetOrePos).getBlock();
        boolean isTargetDiamond = targetBlock == Blocks.DIAMOND_ORE || targetBlock == Blocks.DEEPSLATE_DIAMOND_ORE;
        boolean isTargetGold = targetBlock == Blocks.GOLD_ORE || targetBlock == Blocks.DEEPSLATE_GOLD_ORE;

        if (this.isSeekingGold) {
            if (!isTargetGold) return false; // Target gold ore was broken or changed
        } else {
            if (!isTargetDiamond) return false; // Target diamond ore was broken or changed
        }
        return true;
    }

    @Override
    public void tick() {
        if (this.targetOrePos == null) {
            stop(); // Should ensure goal stops if target becomes null
            return;
        }

        this.hamster.getLookControl().lookAt(this.targetOrePos.getX() + 0.5, this.targetOrePos.getY() + 0.5, this.targetOrePos.getZ() + 0.5, 10.0f, (float) this.hamster.getMaxLookPitchChange());

        if (this.soundTimer > 0) {
            this.soundTimer--;
        }

        switch (this.currentState) {
            case MOVING_TO_ORE:
                if (this.hamster.getNavigation().isIdle() || this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                    if (this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                        onOreReached();
                    } else {
                        // Path failed or hamster got stuck, switch to waiting to re-evaluate
                        this.currentState = SeekingState.WAITING_FOR_PATH;
                        this.pathingTickTimer = PATHING_RECHECK_INTERVAL; // Start timer to recheck path
                        this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING / 2; // Reset sound timer for waiting state
                    }
                } else {
                    if (this.soundTimer <= 0) {
                        playSniffSound();
                        this.soundTimer = SNIFF_SOUND_INTERVAL_MOVING;
                    }
                }
                break;
            case WAITING_FOR_PATH:
                if (this.pathingTickTimer > 0) {
                    this.pathingTickTimer--;
                } else {
                    attemptPathToTarget();
                }
                if (this.soundTimer <= 0) {
                    playSniffSound();
                    this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING;
                }
                break;
        }
    }

    private void onOreReached() {
        this.hamster.getNavigation().stop();
        this.hamster.isPrimedToSeekDiamonds = false;

        if (Configs.AHP.enableIndependentDiamondSeekCooldown) {
            this.hamster.foundOreCooldownEndTick = this.world.getTime() + Configs.AHP.independentOreSeekCooldownTicks.get();
        }

        if (this.isSeekingGold) {
            this.currentState = SeekingState.SULKING_AT_GOLD;
            // Attempt to face owner if nearby
            if (this.hamster.getOwner() instanceof ServerPlayerEntity owner && this.hamster.squaredDistanceTo(owner) < 36.0) { // Within 6 blocks
                this.hamster.getLookControl().lookAt(owner, 30.0f, 30.0f);
            }
            // The actual setSulking and triggerAnimOnServer will happen immediately after.
            // Not sure if the "sulk" triggerable anim will allow the hamster to turn or not, we'll have to test
            this.hamster.setSulking(true);
            this.hamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");
        } else {
            this.currentState = SeekingState.CELEBRATING_DIAMOND;
            this.hamster.setCelebratingDiamond(true); // Triggers begging animation
            AdorableHamsterPets.LOGGER.info("Hamster {} reached CELEBRATING_DIAMOND state for ore at {}", this.hamster.getId(), this.targetOrePos);

            if (this.hamster.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
                ModCriteria.HAMSTER_LED_TO_DIAMOND.trigger(serverPlayerOwner, this.hamster, this.targetOrePos);
            }
        }
    }

    private void playSniffSound() {
        SoundEvent sniffSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, this.hamster.getRandom());
        if (sniffSound != null) {
            this.world.playSound(null, this.hamster.getBlockPos(), sniffSound, SoundCategory.NEUTRAL, 3.0F, this.hamster.getSoundPitch());
        }
    }

    @Override
    public void stop() {
        this.hamster.getNavigation().stop();
        boolean targetOreStillExists = false;
        if (this.targetOrePos != null) {
            Block targetBlock = world.getBlockState(this.targetOrePos).getBlock();
            boolean isTargetDiamond = targetBlock == Blocks.DIAMOND_ORE || targetBlock == Blocks.DEEPSLATE_DIAMOND_ORE;
            boolean isTargetGold = targetBlock == Blocks.GOLD_ORE || targetBlock == Blocks.DEEPSLATE_GOLD_ORE;
            if (this.isSeekingGold && isTargetGold) targetOreStillExists = true;
            if (!this.isSeekingGold && isTargetDiamond) targetOreStillExists = true;
        }

        if (this.currentState != SeekingState.CELEBRATING_DIAMOND && this.currentState != SeekingState.SULKING_AT_GOLD && !targetOreStillExists) {
            this.hamster.isPrimedToSeekDiamonds = false;
        }

        if (this.hamster.isCelebratingDiamond() && (this.currentState != SeekingState.CELEBRATING_DIAMOND || !targetOreStillExists)) {
            this.hamster.setCelebratingDiamond(false);
        }

        if (this.hamster.getActiveCustomGoalDebugName().startsWith(this.getClass().getSimpleName())) {
            this.hamster.setActiveCustomGoalDebugName("None");
        }
        this.currentState = SeekingState.IDLE;
        this.targetOrePos = null;
    }
}