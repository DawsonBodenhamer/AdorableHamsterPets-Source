package net.dawson.adorablehamsterpets.entity.AI;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    @Nullable private Path path;

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

        List<BlockPos> exposedDiamondOres = new ArrayList<>();
        List<BlockPos> buriedDiamondOres = new ArrayList<>();
        List<BlockPos> buriedGoldOres = new ArrayList<>(); // Only track buried gold for the "mistake"
        int radius = Configs.AHP.diamondSeekRadius.get();

        for (BlockPos pos : BlockPos.iterateOutwards(hamster.getBlockPos(), radius, radius, radius)) {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                if (isOreExposed(pos, this.world)) {
                    exposedDiamondOres.add(pos.toImmutable());
                } else {
                    buriedDiamondOres.add(pos.toImmutable());
                }
            } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
                if (isOreExposed(pos, this.world)) { // Only consider gold if it's hidden
                    buriedGoldOres.add(pos.toImmutable());
                }
            }
        }

        // --- Prioritized Target Selection ---
        boolean targetIsGold = !buriedGoldOres.isEmpty() && this.world.random.nextFloat() < Configs.AHP.goldMistakeChance.get();

        if (targetIsGold) {
            buriedGoldOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
            this.targetOrePos = buriedGoldOres.get(0);
            this.isSeekingGold = true;
        } else {
            if (!exposedDiamondOres.isEmpty()) {
                exposedDiamondOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
                this.targetOrePos = exposedDiamondOres.get(0);
            } else if (!buriedDiamondOres.isEmpty()) {
                buriedDiamondOres.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(hamster.getPos())));
                this.targetOrePos = buriedDiamondOres.get(0);
            }
        }

        if (this.targetOrePos != null) {
            this.hamster.currentOreTarget = this.targetOrePos;
            this.currentState = SeekingState.SCANNING;
            return true; // A target was selected
        }

        return false; // No valid target found
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
            this.currentState = SeekingState.IDLE;
            return;
        }
        // --- Store the Path ---
        this.path = this.hamster.getNavigation().findPathTo(
                this.targetOrePos.getX() + 0.5,
                this.targetOrePos.getY(),
                this.targetOrePos.getZ() + 0.5,
                0
        );

        if (this.path != null) {
            this.hamster.getNavigation().startMovingAlong(this.path, 0.5D);
            this.currentState = SeekingState.MOVING_TO_ORE;
            this.soundTimer = SNIFF_SOUND_INTERVAL_MOVING / 2;
        } else {
            this.currentState = SeekingState.WAITING_FOR_PATH;
            this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
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
            return isTargetGold; // Target gold ore was broken or changed
        } else {
            return isTargetDiamond; // Target diamond ore was broken or changed
        }
    }

    @Override
    public void tick() {
        if (this.targetOrePos == null) {
            stop();
            return;
        }

        this.hamster.getLookControl().lookAt(this.targetOrePos.getX() + 0.5, this.targetOrePos.getY() + 0.5, this.targetOrePos.getZ() + 0.5, HamsterEntity.FAST_YAW_CHANGE, HamsterEntity.FAST_PITCH_CHANGE);

        if (this.soundTimer > 0) {
            this.soundTimer--;
        }

        switch (this.currentState) {
            case MOVING_TO_ORE:

                // --- Particle Breadcrumb Logic ---
                if (this.path != null && !this.world.isClient()) {
                    int currentNodeIndex = this.path.getCurrentNodeIndex();
                    int pathLength = this.path.getLength();

                    // Iterate from the current node to the end of the path
                    for (int i = currentNodeIndex; i < pathLength; i++) {
                        PathNode node = this.path.getNode(i);
                        Vec3d directionVector = Vec3d.ZERO; // Default to no direction

                        // 1. Determine the direction to the next node in the path.
                        if (i + 1 < pathLength) {
                            PathNode nextNode = this.path.getNode(i + 1);
                            // Create a normalized (length of 1) vector pointing from the current node to the next.
                            directionVector = new Vec3d(nextNode.x - node.x, 0, nextNode.z - node.z).normalize();
                        }
                        // For the very last node, directionVector will remain (0,0,0), so particles will cluster around it.

                        // Loop to spawn multiple particles with randomized origins
                        for (int p = 0; p < 3; p++) {
                            // 2. Calculate a random distance to spread the particle along the direction vector.
                            double distanceAlongPath = this.world.random.nextDouble(); // Random value from 0.0 to 1.0
                            Vec3d pathOffset = directionVector.multiply(distanceAlongPath);

                            // 3. Calculate limited vertical offset.
                            double offsetY = (this.world.random.nextDouble() - 0.5) * 0.1;

                            ((ServerWorld)this.world).spawnParticles(
                                    ParticleTypes.MYCELIUM,
                                    node.x + 0.5 + pathOffset.x,      // Center X + directional offset X
                                    (node.y + 0.5) - 0.38 + offsetY,     // Center Y + limited vertical offset
                                    node.z + 0.5 + pathOffset.z,         // Center Z + directional offset Z
                                    1,                                   // Count is 1
                                    0.2, 0.0, 0.2,          // Vertical Spread is 0
                                    3                                    // Speed
                            );
                        }
                    }
                }

                if (this.hamster.getNavigation().isIdle() || this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                    if (this.hamster.getBlockPos().isWithinDistance(this.targetOrePos, 1.5)) {
                        onOreReached();
                    } else {
                        this.path = null; // Clear old path
                        this.currentState = SeekingState.WAITING_FOR_PATH;
                        this.pathingTickTimer = PATHING_RECHECK_INTERVAL;
                        this.soundTimer = SNIFF_SOUND_INTERVAL_WAITING / 2;
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
            AdorableHamsterPets.LOGGER.trace("Hamster {} reached CELEBRATING_DIAMOND state for ore at {}", this.hamster.getId(), this.targetOrePos);

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
        this.path = null; // Clear the path when the goal stops
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
            AdorableHamsterPets.LOGGER.error("[GoldMessage] CRITICAL: Could not find advancement '{}'. Message will not be sent. Check file path and JSON validity.", advId);
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

    /**
     * Checks if an ore block is "exposed" by having at least one adjacent air-like block.
     *
     * @param orePos The position of the ore block.
     * @return True if the ore is exposed, false otherwise.
     */
    public static boolean isOreExposed(BlockPos orePos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = orePos.offset(direction);
            // A block is considered "exposed" if the adjacent block has no collision shape (e.g., air, water, grass).
            if (world.getBlockState(adjacentPos).getCollisionShape(world, adjacentPos, ShapeContext.absent()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}