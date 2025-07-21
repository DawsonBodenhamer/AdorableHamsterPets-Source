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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.util.Formatting;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        if (this.hamster.isSitting() || this.hamster.isSleeping() || this.hamster.isKnockedOut() || this.hamster.isCelebratingChase()|| this.hamster.isSulking()) {
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

        if (!goldOres.isEmpty() && this.world.random.nextFloat() < Configs.AHP.goldMistakeChance.get()) {
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

        this.hamster.getLookControl().lookAt(this.targetOrePos.getX() + 0.5, this.targetOrePos.getY() + 0.5, this.targetOrePos.getZ() + 0.5, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);

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
            if (this.hamster.getOwner() instanceof ServerPlayerEntity owner) {
                if (this.hamster.squaredDistanceTo(owner) < 36.0) {
                    this.hamster.getLookControl().lookAt(owner, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);
                }
                // Send message to the owner
                sendMessageToOwner(owner);
            }

            // --- Startled Jump & Sound Logic ---
            // Calculate a vector pointing away from the target ore
            Vec3d awayFromOre = this.hamster.getPos().subtract(Vec3d.ofCenter(this.targetOrePos)).normalize();
            // Apply a small backward and upward velocity
            this.hamster.setVelocity(awayFromOre.x * 0.1, 0.5, awayFromOre.z * 0.1);
            this.hamster.velocityDirty = true; // Mark velocity for client sync
            // Play a random bounce sound at the hamster's location
            SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, this.hamster.getRandom());
            if (bounceSound != null) {
                this.world.playSound(null, this.hamster.getBlockPos(), bounceSound, SoundCategory.NEUTRAL, 0.6f, this.hamster.getSoundPitch());
            }
            // --- End Startled Jump & Sound Logic ---

            this.hamster.setSulking(true);
            this.hamster.triggerAnimOnServer("mainController", "anim_hamster_sulk");

        } else {
            this.currentState = SeekingState.CELEBRATING_DIAMOND;
            this.hamster.setCelebratingDiamond(true); // Triggers begging animation
            AdorableHamsterPets.LOGGER.debug("Hamster {} reached CELEBRATING_DIAMOND state for ore at {}", this.hamster.getId(), this.targetOrePos);

            if (this.hamster.getOwner() instanceof ServerPlayerEntity serverPlayerOwner) {
                ModCriteria.HAMSTER_LED_TO_DIAMOND.get().trigger(serverPlayerOwner, this.hamster, this.targetOrePos);
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

    /**
     * Selects and sends a humorous message to the hamster's owner about finding gold.
     * <p>
     * This method implements specific logic to enhance the player experience:
     * <ul>
     *     <li><b>First-Time Experience:</b> It checks if the player has the
     *     {@code adorablehamsterpets:technical/hamster_found_gold_first_time} advancement.
     *     If not, it sends a specific, predetermined message (index 0) and grants the
     *     advancement to ensure this "first-time" message is only seen once per player.</li>
     *     <li><b>Subsequent Experiences:</b> For all subsequent times, it retrieves the index of the
     *     last message shown from the player's persistent NBT data (via the {@link PlayerEntityAccessor}).
     *     It then randomly selects a new message from the available pool, guaranteeing it will not be the
     *     same as the one shown immediately prior.</li>
     *     <li><b>State Persistence:</b> The index of the newly displayed message is saved back to the
     *     player's NBT data, ensuring the "don't repeat" logic works across game sessions.</li>
     * </ul>
     * The method also triggers the {@link ModCriteria#HAMSTER_FOUND_GOLD} criterion on every execution.
     *
     * @param owner The player who owns the hamster and will receive the message.
     */
    private void sendMessageToOwner(ServerPlayerEntity owner) {
        PlayerAdvancementTracker tracker = owner.getAdvancementTracker();
        Identifier advId = Identifier.of(AdorableHamsterPets.MOD_ID, "technical/hamster_found_gold_first_time");
        AdvancementEntry advancement = owner.server.getAdvancementLoader().get(advId);

        if (advancement == null) {
            AdorableHamsterPets.LOGGER.debug("[GoldMessage] CRITICAL: Could not find advancement '{}'. Message will not be sent. Check file path and JSON validity.", advId);
            return;
        }

        AdvancementProgress progress = tracker.getProgress(advancement);
        int messageIndex;

        if (!progress.isDone()) {
            // First time ever for this player
            messageIndex = 0;
            // Grant the advancement so this block doesn't run again
            for (String criterion : advancement.value().criteria().keySet()) {
                tracker.grantCriterion(advancement, criterion);
            }
        } else {
            // Subsequent times
            PlayerEntityAccessor accessor = (PlayerEntityAccessor) owner;
            int lastIndex = accessor.ahp_getLastGoldMessageIndex();

            List<Integer> possibleIndices = IntStream.range(0, 7).boxed().collect(Collectors.toList());
            if (lastIndex >= 0 && lastIndex < 7) {
                possibleIndices.remove(Integer.valueOf(lastIndex));
            }

            messageIndex = possibleIndices.get(this.world.random.nextInt(possibleIndices.size()));
        }

        // Save the new index and send the message
        ((PlayerEntityAccessor) owner).ahp_setLastGoldMessageIndex(messageIndex);
        String messageKey = "message.adorablehamsterpets.found_gold_mistake." + (messageIndex + 1);
        owner.sendMessage(Text.translatable(messageKey).formatted(Formatting.GOLD), true);

        // Trigger the criterion for any other potential uses
        ModCriteria.HAMSTER_FOUND_GOLD.get().trigger(owner);
    }
}