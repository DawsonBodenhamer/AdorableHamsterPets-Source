package net.dawson.adorablehamsterpets.flute;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.client.particle.AcornFluteNoteParticleEffect;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.AI.HamsterLookAtEntityGoal;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.custom.AcornFluteItem;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.sound.ModSounds.TimedSound;
import net.dawson.adorablehamsterpets.util.DistantSoundUtil;
import net.dawson.adorablehamsterpets.util.EntityTargetingUtil;
import net.dawson.adorablehamsterpets.util.HamsterInteractionUtil;
import net.dawson.adorablehamsterpets.util.HamsterMovementUtil;
import net.dawson.adorablehamsterpets.util.HamsterNbtUtil;
import net.dawson.adorablehamsterpets.util.HamsterState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns server-authoritative Acorn Flute performances and shoulder-call flights.
 *
 * <p>A performance belongs to the exact mainhand stack object used to start it. This
 * deliberately makes inventory changes a cancellation boundary without adding fragile
 * inventory or swap mixins.</p>
 */
public final class FlutePerformanceManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static State
     * ────────────────────────────────────────────────────────────────────────────*/

    private static final int FLIGHT_DURATION_TICKS = 24;
    private static final int ARRIVAL_PRESENTATION_TICKS = 2;
    private static final double NOTES_PER_TICK = 0.23D;
    private static final double FLIGHT_ARC_HEIGHT = 2.55D;
    private static final double SHOULDER_OFFSET = 0.36D;
    private static final double HEAD_OFFSET_Y = 0.15D;
    // Detection-only bound for the too-far action-bar response; the actual mount radius remains configurable.
    private static final double MOUNT_FEEDBACK_RADIUS = 64.0D;
    private static final Set<UUID> RESPONDING_HAMSTERS = new HashSet<>();
    private static final Map<UUID, List<Performance>> ACTIVE_PERFORMANCES = new HashMap<>();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public Performance API
     * ────────────────────────────────────────────────────────────────────────────*/

    /**
     * Starts a normal riff or the selected hamster's shoulder call for one player.
     */
    public static boolean startPerformance(
            ServerPlayerEntity player, ItemStack initiatingStack, AcornFluteVariant variant) {
        boolean validInitiatingStack = !initiatingStack.isEmpty()
                && initiatingStack.getItem() instanceof AcornFluteItem flute
                && flute.variant() == variant;
        ServerWorld world = player.getServerWorld();
        long startTick = world.getTime();
        List<Performance> performances = ACTIVE_PERFORMANCES.computeIfAbsent(
                player.getUuid(), ignored -> new ArrayList<>());
        Performance activePerformance = performances.isEmpty()
                ? null
                : performances.get(performances.size() - 1);
        int antiSpamCooldownTicks = Math.max(0, Configs.AHP_MAIN.acornFluteAntiSpamCooldownTicks.get());
        boolean antiSpamCooldownActive = antiSpamCooldownTicks > 0
                && player.getItemCooldownManager().isCoolingDown(initiatingStack.getItem());
        if (!FlutePerformancePolicy.canStart(
                activePerformance != null,
                validInitiatingStack,
                activePerformance != null && activePerformance.mode == Mode.NORMAL,
                antiSpamCooldownActive)) {
            if (performances.isEmpty()) {
                ACTIVE_PERFORMANCES.remove(player.getUuid());
            }
            return false;
        }

        if (activePerformance != null) {
            FlutePerformancePolicy.RepeatAction repeatAction =
                    !isBoundAndAlive(player, activePerformance)
                            ? FlutePerformancePolicy.RepeatAction.REPLACE
                            : FlutePerformancePolicy.repeatAction(
                                    startTick - activePerformance.startTick,
                                    activePerformance.soundEndTick - activePerformance.startTick,
                                    Configs.AHP_MAIN.acornFluteRiffLayeringThresholdPercent.get());
            if (repeatAction == FlutePerformancePolicy.RepeatAction.REPLACE) {
                for (Performance performance : performances) {
                    terminate(player.getServer(), player, performance);
                }
                performances.clear();
            }
        }

        double mountRadius = Math.max(0.0D, Configs.AHP_MAIN.acornFluteMountTargetingRadius.get());
        HamsterEntity target = findMountTarget(player, mountRadius);
        if (target == null) {
            HamsterEntity distantTarget = findMountTarget(
                    player, Math.max(mountRadius, MOUNT_FEEDBACK_RADIUS));
            if (distantTarget != null
                    && distantTarget.squaredDistanceTo(player) > mountRadius * mountRadius) {
                player.sendMessage(
                        Text.translatable("message.adorablehamsterpets.flute_move_closer"), true);
                return true;
            }
        }

        ShoulderLocation slot = HamsterInteractionUtil.getNextAvailableSlot(player);
        if (target != null && slot == null) {
            player.sendMessage(Text.translatable("message.adorablehamsterpets.shoulder_occupied"), true);
            return true;
        }

        // Set after the bail-outs so feedback-only attempts don't eat the cooldown
        if (antiSpamCooldownTicks > 0) {
            player.getItemCooldownManager().set(initiatingStack.getItem(), antiSpamCooldownTicks);
        }

        FlutePerformancePolicy.Mode selectedMode = FlutePerformancePolicy.selectMode(
                target != null,
                slot != null,
                target != null && RESPONDING_HAMSTERS.contains(target.getUuid()));
        if (selectedMode == FlutePerformancePolicy.Mode.SHOULDER_CALL) {
            TimedSound sound = ModSounds.ACORN_FLUTE_CHIFF_TIMED;
            Performance performance = Performance.shoulderCall(
                    player,
                    player.getUuid(),
                    initiatingStack,
                    variant,
                    player.getWorld().getRegistryKey(),
                    player.getPos(),
                    startTick,
                    sound,
                    target,
                    slot,
                    FluteTrainingPolicy.responseDelayTicks(
                            target.getFluteProgressState().getSuccessfulMounts(),
                            Configs.AHP_MAIN.acornFluteMountsToMastery.get()),
                    target.isFrozenMovement(),
                    target.hasNoGravity(),
                    target.getHamsterFlag(HamsterEntity.SITTING_FLAG),
                    target.getActiveCustomGoalName());
            performances.add(performance);
            RESPONDING_HAMSTERS.add(target.getUuid());
            prepareShoulderResponse(target);
            playSound(world, performance);
            return true;
        }

        TimedSound sound = ModSounds.ACORN_FLUTE_RIFFS.get(player.getRandom().nextInt(ModSounds.ACORN_FLUTE_RIFFS.size()));
        Performance performance = Performance.normal(
                player,
                player.getUuid(),
                initiatingStack,
                variant,
                player.getWorld().getRegistryKey(),
                player.getPos(),
                startTick,
                sound);
        performances.add(performance);
        playSound(world, performance);
        return true;
    }

    /**
     * Cancels the player's current performance and sends an explicit stop packet.
     */
    public static void cancel(ServerPlayerEntity player) {
        List<Performance> performances = ACTIVE_PERFORMANCES.remove(player.getUuid());
        if (performances != null) {
            for (Performance performance : performances) {
                terminate(player.getServer(), player, performance);
            }
        }
    }

    /**
     * Ticks all sessions once on the server thread.
     */
    public static void onServerTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, List<Performance>>> iterator = ACTIVE_PERFORMANCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<Performance>> entry = iterator.next();
            List<Performance> performances = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null
                    || performances.stream().anyMatch(performance -> !isBoundAndAlive(player, performance))) {
                iterator.remove();
                for (Performance performance : performances) {
                    terminate(server, player, performance);
                }
                continue;
            }

            long currentTick = player.getWorld().getTime();
            Iterator<Performance> performanceIterator = performances.iterator();
            while (performanceIterator.hasNext()) {
                Performance performance = performanceIterator.next();
                tickPresentationAndProgress(player, performance, currentTick);
                if (!performance.soundStopped && currentTick >= performance.soundEndTick) {
                    stopSoundEverywhere(server, performance.sessionId);
                    performance.soundStopped = true;
                }

                if (performance.mode == Mode.NORMAL) {
                    if (currentTick >= performance.soundEndTick) {
                        performanceIterator.remove();
                    }
                    continue;
                }

                if (!tickShoulderCall(server, player, performance, currentTick)) {
                    performanceIterator.remove();
                    terminate(server, player, performance);
                }
            }

            if (performances.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns true when any active normal riff affects the supplied entity.
     */
    public static boolean isAffectedByNormalRiff(Entity entity) {
        return !getNormalRiffPerformersAffecting(entity).isEmpty();
    }

    /**
     * Returns immutable UUIDs for all active normal-riff performers affecting a hamster.
     * Dimension and configured effect-radius checks are applied at query time.
     */
    public static Set<UUID> getNormalRiffPerformersAffecting(Entity entity) {
        if (entity == null || entity.getWorld().isClient()) {
            return Set.of();
        }

        double radius = Math.max(0.0D, Configs.AHP_MAIN.acornFluteEffectRadius.get());
        double radiusSquared = radius * radius;
        Set<UUID> performers = new HashSet<>();
        for (List<Performance> performances : ACTIVE_PERFORMANCES.values()) {
            for (Performance performance : performances) {
                if (performance.mode == Mode.NORMAL
                        && isBoundAndAlive(performance.player, performance)
                        && performance.dimension.equals(entity.getWorld().getRegistryKey())
                        && performance.sourcePosition.squaredDistanceTo(entity.getPos()) <= radiusSquared) {
                    performers.add(performance.playerUuid);
                }
            }
        }
        return Set.copyOf(performers);
    }

    /**
     * Returns true when any active normal riff affects the supplied hamster.
     */
    public static boolean isAffectedByAnyActiveNormalRiff(HamsterEntity hamster) {
        return isAffectedByNormalRiff(hamster);
    }

    /**
     * Compatibility alias for integrations that prefer an explicit active-session name.
     */
    public static Set<UUID> getActiveNormalRiffPerformers(HamsterEntity hamster) {
        return getNormalRiffPerformersAffecting(hamster);
    }

    /**
     * Returns whether a specific player currently supplies a qualifying normal riff.
     */
    public static boolean isNormalRiffActive(UUID performerUuid) {
        List<Performance> performances = ACTIVE_PERFORMANCES.get(performerUuid);
        return performances != null && performances.stream().anyMatch(performance ->
                performance.mode == Mode.NORMAL && isBoundAndAlive(performance.player, performance));
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Shoulder Response Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    private static boolean tickShoulderCall(
            MinecraftServer server,
            ServerPlayerEntity player,
            Performance performance,
            long currentTick) {
        HamsterEntity hamster = performance.target;
        if (!isValidMountTarget(hamster, player)
                || HamsterInteractionUtil.getNextAvailableSlot(player) != performance.slot) {
            return false;
        }

        if (!performance.flightStarted) {
            HamsterMovementUtil.faceEntity(hamster, player);
            hamster.getNavigation().stop();
            hamster.setVelocity(Vec3d.ZERO);
            hamster.setActiveCustomGoalName(HamsterLookAtEntityGoal.class.getSimpleName());
            if (currentTick - performance.startTick < performance.responseDelayTicks) {
                return true;
            }

            hamster.triggerAnimOnServer("mainController", "anim_hamster_high_jump");
            hamster.setNoGravity(true);
            hamster.setFluteMountFlight(true);
            hamster.setFluteMountPitch(0.0F);
            performance.flightStarted = true;
            performance.flightStartTick = currentTick;
            performance.flightStartPosition = hamster.getPos();
            faceAlongFlight(
                    hamster,
                    FluteFlightMath.directionAlongArc(
                            performance.flightStartPosition,
                            shoulderAnchor(player, performance.slot),
                            0.0D,
                            1.0D / FLIGHT_DURATION_TICKS,
                            FLIGHT_ARC_HEIGHT));
            hamster.setVelocity(Vec3d.ZERO);
            return true;
        }

        double progress = Math.min(
                1.0D,
                (double) (currentTick - performance.flightStartTick) / FLIGHT_DURATION_TICKS);
        Vec3d previousPosition = hamster.getPos();
        Vec3d destination = shoulderAnchor(player, performance.slot);
        Vec3d nextPosition = FluteFlightMath.positionAlongArc(
                performance.flightStartPosition, destination, progress, FLIGHT_ARC_HEIGHT);
        Vec3d flightDirection = FluteFlightMath.directionAlongArc(
                performance.flightStartPosition,
                destination,
                progress,
                1.0D / FLIGHT_DURATION_TICKS,
                FLIGHT_ARC_HEIGHT);

        hamster.setFluteMountPitch(FluteFlightMath.pitchFromVelocity(
                flightDirection.x, flightDirection.y, flightDirection.z));
        float flightYaw = faceAlongFlight(hamster, flightDirection);
        hamster.setVelocity(Vec3d.ZERO);
        hamster.refreshPositionAndAngles(
                nextPosition.x,
                nextPosition.y,
                nextPosition.z,
                flightYaw,
                hamster.getPitch());
        if (!hamster.getWorld().isSpaceEmpty(hamster)) {
            hamster.refreshPositionAndAngles(
                    previousPosition.x,
                    previousPosition.y,
                    previousPosition.z,
                    hamster.getYaw(),
                    hamster.getPitch());
            return false;
        }
        hamster.velocityDirty = true;

        if (progress < 1.0D
                || performance.arrivalPresentationTicks++ < ARRIVAL_PRESENTATION_TICKS) {
            return true;
        }

        return finishShoulderMount(server, player, performance);
    }

    private static void tickPresentationAndProgress(
            ServerPlayerEntity player, Performance performance, long currentTick) {
        ServerWorld world = player.getServerWorld();
        if (currentTick < performance.soundEndTick) {
            performance.noteSpawnAccumulator += NOTES_PER_TICK;
        }
        if (performance.noteSpawnAccumulator >= 1.0D) {
            performance.noteSpawnAccumulator -= 1.0D;
            Vec3d hand = mainHandPosition(player);
            world.spawnParticles(
                    new AcornFluteNoteParticleEffect(performance.variant),
                    hand.x,
                    hand.y,
                    hand.z,
                    1,
                    0.32D,
                    0.12D,
                    0.32D,
                    0.025D);
        }

        if (performance.mode != Mode.NORMAL) {
            return;
        }

        double radius = Math.max(0.0D, Configs.AHP_MAIN.acornFluteEffectRadius.get());
        if (radius <= 0.0D) {
            return;
        }

        Box affectedArea = new Box(performance.sourcePosition, performance.sourcePosition).expand(radius);
        double radiusSquared = radius * radius;
        for (HamsterEntity hamster : world.getEntitiesByClass(
                HamsterEntity.class,
                affectedArea,
                candidate -> candidate.hasRedstoneFever()
                        && candidate.squaredDistanceTo(performance.sourcePosition) <= radiusSquared)) {
            hamster.getFluteProgressState().recordFeverPerformer(performance.playerUuid);
        }
    }

    private static boolean finishShoulderMount(
            MinecraftServer server, ServerPlayerEntity player, Performance performance) {
        HamsterEntity hamster = performance.target;
        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
        if (HamsterInteractionUtil.getNextAvailableSlot(player) != performance.slot) {
            return false;
        }

        hamster.setFluteMountFlight(false);
        hamster.setFluteMountPitch(0.0F);
        hamster.setFluteMountResponseActive(false);
        hamster.setNoGravity(performance.previousNoGravity);
        hamster.setFrozenMovement(performance.previousFrozenMovement);
        hamster.setActiveCustomGoalName(performance.previousGoalName);
        hamster.setVelocity(Vec3d.ZERO);

        HamsterInteractionUtil.executeShoulderMount(
                hamster,
                player,
                performance.initiatingStack,
                HamsterInteractionUtil.ShoulderMountSoundTiming.IMMEDIATE);
        if (!hamster.isRemoved() || accessor.getShoulderHamster(performance.slot).isEmpty()) {
            return false;
        }

        hamster.getFluteProgressState().recordSuccessfulMount();
        HamsterState updatedState = HamsterNbtUtil.saveToHamsterState(hamster);
        accessor.setShoulderHamster(performance.slot, updatedState.toNbt());
        RESPONDING_HAMSTERS.remove(hamster.getUuid());
        return true;
    }

    private static void prepareShoulderResponse(HamsterEntity hamster) {
        hamster.setSitting(false, true);
        hamster.setFluteMountResponseActive(true);
        hamster.setActiveCustomGoalName(HamsterLookAtEntityGoal.class.getSimpleName());
        hamster.getDataTracker().set(
                HamsterEntity.CURRENT_LOOK_UP_ANIM_ID,
                Math.floorMod(hamster.getUuid().hashCode(), 3) + 1);
        hamster.getNavigation().stop();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Validation and Audio Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    @Nullable
    private static HamsterEntity findMountTarget(ServerPlayerEntity player, double radius) {
        if (radius <= 0.0D) {
            return null;
        }

        Box searchBox = player.getBoundingBox().expand(radius);
        HamsterEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (HamsterEntity hamster : player.getWorld().getEntitiesByClass(
                HamsterEntity.class, searchBox, entity -> isValidMountTarget(entity, player))) {
            if (RESPONDING_HAMSTERS.contains(hamster.getUuid())
                    || hamster.getWorld().isClient()
                    || !EntityTargetingUtil.isLookingAt(player, hamster, radius, 0.0D)
                    || !player.canSee(hamster)) {
                continue;
            }

            double distance = hamster.squaredDistanceTo(player);
            if (distance < nearestDistance) {
                nearest = hamster;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isValidMountTarget(HamsterEntity hamster, PlayerEntity player) {
        return hamster != null
                && hamster.isAlive()
                && !hamster.isRemoved()
                && hamster.isTamed()
                && player.getUuid().equals(hamster.getOwnerUuid())
                && hamster.getWorld() == player.getWorld()
                && !hamster.isSleeping()
                && !hamster.isKnockedOut()
                && !hamster.isSulking()
                && !hamster.isShoulderPet()
                && !hamster.isWanderModeActive()
                && !hamster.hasVehicle()
                && !hamster.hasPassengers()
                && !hamster.isLeashed()
                && !hamster.isAiDisabled();
    }

    private static boolean isBoundAndAlive(ServerPlayerEntity player, Performance performance) {
        ItemStack mainhand = player.getMainHandStack();
        boolean matchingVariant = mainhand.getItem() instanceof AcornFluteItem flute
                && flute.variant() == performance.variant;
        return FlutePerformancePolicy.remainsBound(
                player.isAlive(),
                player.getWorld().getRegistryKey().equals(performance.dimension),
                mainhand == performance.initiatingStack,
                matchingVariant);
    }

    private static Vec3d shoulderAnchor(PlayerEntity player, ShoulderLocation slot) {
        double yaw = Math.toRadians(player.getBodyYaw());
        Vec3d side = new Vec3d(Math.cos(yaw), 0.0D, Math.sin(yaw));
        double sideOffset = switch (slot) {
            case RIGHT_SHOULDER -> -SHOULDER_OFFSET;
            case LEFT_SHOULDER -> SHOULDER_OFFSET;
            case HEAD -> 0.0D;
        };
        double y = player.getY() + player.getHeight() * 0.8D
                + (slot == ShoulderLocation.HEAD ? HEAD_OFFSET_Y : 0.0D);
        Vec3d horizontal = player.getPos().add(side.multiply(sideOffset));
        return new Vec3d(horizontal.x, y, horizontal.z);
    }

    private static float faceAlongFlight(HamsterEntity hamster, Vec3d direction) {
        float yaw = FluteFlightMath.yawFromDirection(direction.x, direction.z);
        hamster.setYaw(yaw);
        hamster.bodyYaw = yaw;
        hamster.headYaw = yaw;
        hamster.prevYaw = yaw;
        hamster.prevBodyYaw = yaw;
        hamster.prevHeadYaw = yaw;
        return yaw;
    }

    private static Vec3d mainHandPosition(PlayerEntity player) {
        double yaw = Math.toRadians(player.getYaw());
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0D, Math.sin(yaw));
        double side = player.getMainArm() == Arm.RIGHT ? -0.24D : 0.24D;
        return player.getEyePos()
                .add(player.getRotationVec(1.0F).multiply(0.62D))
                .add(right.multiply(side))
                .add(0.0D, -0.32D, 0.0D);
    }

    private static void playSound(ServerWorld world, Performance performance) {
        DistantSoundUtil.startSession(
                world,
                performance.sessionId,
                performance.sourcePosition,
                performance.player.getId(),
                performance.sound.sound().get(),
                1.0F,
                1.0F,
                Math.max(1.0D, Configs.AHP_MAIN.acornFluteAudioRange.get()),
                SoundCategory.RECORDS);
    }

    private static void terminate(
            @Nullable MinecraftServer server,
            @Nullable ServerPlayerEntity player,
            Performance performance) {
        if (server != null) {
            stopSoundEverywhere(server, performance.sessionId);
        } else if (player != null && player.getWorld() instanceof ServerWorld world) {
            DistantSoundUtil.stopSession(world, performance.sessionId);
        }

        HamsterEntity hamster = performance.target;
        if (hamster != null) {
            RESPONDING_HAMSTERS.remove(hamster.getUuid());
            if (!hamster.isRemoved()) {
                hamster.setFluteMountFlight(false);
                hamster.setFluteMountPitch(0.0F);
                hamster.setFluteMountResponseActive(false);
                hamster.setNoGravity(performance.previousNoGravity);
                hamster.setFrozenMovement(performance.previousFrozenMovement);
                hamster.setActiveCustomGoalName(performance.previousGoalName);
                hamster.setVelocity(Vec3d.ZERO);
                if (performance.previousSitting) {
                    hamster.setSitting(true, true);
                }
            }
        }
    }

    private static void stopSoundEverywhere(MinecraftServer server, UUID sessionId) {
        for (ServerWorld world : server.getWorlds()) {
            DistantSoundUtil.stopSession(world, sessionId);
        }
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constructor and Nested Types
     * ────────────────────────────────────────────────────────────────────────────*/

    private FlutePerformanceManager() {}

    private enum Mode {
        NORMAL,
        SHOULDER_CALL
    }

    private static final class Performance {
        private final UUID playerUuid;
        private final ServerPlayerEntity player;
        private final UUID sessionId = UUID.randomUUID();
        private final ItemStack initiatingStack;
        private final AcornFluteVariant variant;
        private final RegistryKey<World> dimension;
        private final Vec3d sourcePosition;
        private final long startTick;
        private final TimedSound sound;
        private final long soundEndTick;
        private final Mode mode;
        @Nullable private final HamsterEntity target;
        @Nullable private final ShoulderLocation slot;
        private final int responseDelayTicks;
        private final boolean previousFrozenMovement;
        private final boolean previousNoGravity;
        private final boolean previousSitting;
        private final String previousGoalName;
        private boolean soundStopped;
        private boolean flightStarted;
        private long flightStartTick;
        private Vec3d flightStartPosition = Vec3d.ZERO;
        private int arrivalPresentationTicks;
        private double noteSpawnAccumulator;

        private Performance(
                ServerPlayerEntity player,
                UUID playerUuid,
                ItemStack initiatingStack,
                AcornFluteVariant variant,
                RegistryKey<World> dimension,
                Vec3d sourcePosition,
                long startTick,
                TimedSound sound,
                Mode mode,
                @Nullable HamsterEntity target,
                @Nullable ShoulderLocation slot,
                int responseDelayTicks,
                boolean previousFrozenMovement,
                boolean previousNoGravity,
                boolean previousSitting,
                String previousGoalName) {
            this.player = player;
            this.playerUuid = playerUuid;
            this.initiatingStack = initiatingStack;
            this.variant = variant;
            this.dimension = dimension;
            this.sourcePosition = sourcePosition;
            this.startTick = startTick;
            this.sound = sound;
            this.soundEndTick = startTick + Math.max(1L, (long) Math.ceil(sound.durationSeconds() * 20.0D));
            this.mode = mode;
            this.target = target;
            this.slot = slot;
            this.responseDelayTicks = responseDelayTicks;
            this.previousFrozenMovement = previousFrozenMovement;
            this.previousNoGravity = previousNoGravity;
            this.previousSitting = previousSitting;
            this.previousGoalName = previousGoalName;
        }

        private static Performance normal(
                ServerPlayerEntity player,
                UUID playerUuid,
                ItemStack initiatingStack,
                AcornFluteVariant variant,
                RegistryKey<World> dimension,
                Vec3d sourcePosition,
                long startTick,
                TimedSound sound) {
            return new Performance(
                    player,
                    playerUuid,
                    initiatingStack,
                    variant,
                    dimension,
                    sourcePosition,
                    startTick,
                    sound,
                    Mode.NORMAL,
                    null,
                    null,
                    0,
                    false,
                    false,
                    false,
                    "None");
        }

        private static Performance shoulderCall(
                ServerPlayerEntity player,
                UUID playerUuid,
                ItemStack initiatingStack,
                AcornFluteVariant variant,
                RegistryKey<World> dimension,
                Vec3d sourcePosition,
                long startTick,
                TimedSound sound,
                HamsterEntity target,
                ShoulderLocation slot,
                int responseDelayTicks,
                boolean previousFrozenMovement,
                boolean previousNoGravity,
                boolean previousSitting,
                String previousGoalName) {
            return new Performance(
                    player,
                    playerUuid,
                    initiatingStack,
                    variant,
                    dimension,
                    sourcePosition,
                    startTick,
                    sound,
                    Mode.SHOULDER_CALL,
                    target,
                    slot,
                    responseDelayTicks,
                    previousFrozenMovement,
                    previousNoGravity,
                    previousSitting,
                    previousGoalName);
        }

    }
}
