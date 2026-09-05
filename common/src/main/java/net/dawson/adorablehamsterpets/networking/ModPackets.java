package net.dawson.adorablehamsterpets.networking;

import dev.architectury.networking.NetworkChannel;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.client.state.ClientShoulderHamsterData;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.mixin.accessor.ValidatedFieldAccessor;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreenHandler;
import net.dawson.adorablehamsterpets.util.GuidebookProgressUtil;
import net.dawson.adorablehamsterpets.util.HamsterInteractionUtil;
import net.dawson.adorablehamsterpets.util.HamsterRenderTracker;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.dawson.adorablehamsterpets.AdorableHamsterPets.MOD_ID;

/**
 * Manages network packet registration and handling for the 1.20.1 version of the mod.
 * Utilizes Architectury API's {@link NetworkChannel} to handle cross-platform networking.
 * <p>
 * Note: In Architectury 9 (1.20.1), packets must be registered on both logical sides
 * to ensure the NetworkChannel knows how to encode/decode them. Client-side handlers
 * are wrapped in {@link EnvExecutor} to prevent classloading crashes on the server.
 */
public class ModPackets {

    // --- 1. Create Network Channel ---
    public static final NetworkChannel CHANNEL = NetworkChannel.create(new Identifier(MOD_ID, "main"));

    // --- 2. Define Packet Data as Records ---
    // C2S (Client-to-Server)
    public record ThrowHamsterC2SPacket() {}
    public record DismountHamsterC2SPacket() {}
    public record UpdateHamsterRenderStateC2SPacket(List<Integer> hamsterEntityIds, boolean isRendering) {}
    public record RequestGuidebookC2SPacket() {}
    public record AcknowledgeGuidebookWarningC2SPacket() {}
    public record RequestGuidebookWarningC2SPacket() {}
    public record RequestHamsterMountC2SPacket(int entityId) {}
    public record ResetHeistHistoryC2SPacket() {}
    public record RequestHamsterRideC2SPacket(int entityId) {}
    public record RequestPetHamsterC2SPacket(int entityId) {}
    public record HamsterInputC2SPacket(boolean jumpHeld, boolean sprintHeld) {}
    public record RenameHamsterC2SPacket(int entityId, String newName) {}
    public record UpdateHamsterArmorVisibilityC2SPacket(int entityId, boolean visible) {}
    public record UpdateCrownThemeC2SPacket(int themeOrdinal) {}
    public record StartCrownTrialC2SPacket(int themeOrdinal) {}
    public record AdjustGeneticsConfigC2SPacket(boolean isVariance, boolean increase) {}
    public record CancelPettingC2SPacket() {}
    public record HamsterAnimationSoundC2SPacket(int hamsterEntityId, String soundId) {}

    // S2C (Server-to-Client)
    public record PlayGuidebookEffectsS2CPacket(boolean closeScreen) {}
    public record ShowGuidebookWarningS2CPacket() {}
    public record SpawnBeddingParticlesS2CPacket(BlockPos pos, Direction direction, WoodVariant variant) {}
    public record SyncHamsterStateS2CPacket(int entityId, NbtCompound data) {}
    public record StopDistantSoundS2CPacket(String sessionKey) {
        public StopDistantSoundS2CPacket(UUID sessionId) {
            this(sessionId != null ? sessionId.toString() : "");
        }
    }
    public record PlayDistantSoundS2CPacket(
            Identifier soundId,
            float volume,
            float pitch,
            float audioRange,
            Optional<Vec3d> position,
            int sourceEntityId,
            String sessionKey,
            SoundCategory category
    ) {
        public PlayDistantSoundS2CPacket(Identifier soundId, float volume, float pitch) {
            this(soundId, volume, pitch, 0.0F, Optional.empty(), 0, "", SoundCategory.NEUTRAL);
        }

        public PlayDistantSoundS2CPacket {
            if (position == null) {
                position = Optional.empty();
            }
            if (sessionKey == null) {
                sessionKey = "";
            }
            if (category == null) {
                category = SoundCategory.NEUTRAL;
            }
        }

        public static PlayDistantSoundS2CPacket positioned(
                Identifier soundId,
                Vec3d position,
                float baseVolume,
                float pitch,
                float audioRange,
                SoundCategory category
        ) {
            return new PlayDistantSoundS2CPacket(
                    soundId,
                    baseVolume,
                    pitch,
                    audioRange,
                    Optional.of(position),
                    0,
                    "",
                    category
            );
        }

        public static PlayDistantSoundS2CPacket positioned(
                Identifier soundId,
                Vec3d position,
                float baseVolume,
                float pitch,
                float audioRange
        ) {
            return positioned(soundId, position, baseVolume, pitch, audioRange, SoundCategory.NEUTRAL);
        }

        public static PlayDistantSoundS2CPacket startSession(
                Identifier soundId,
                Vec3d position,
                int sourceEntityId,
                float baseVolume,
                float pitch,
                float audioRange,
                String sessionKey,
                SoundCategory category
        ) {
            if (sessionKey == null || sessionKey.isBlank()) {
                throw new IllegalArgumentException("Distant sound session key cannot be blank");
            }
            return new PlayDistantSoundS2CPacket(
                    soundId,
                    baseVolume,
                    pitch,
                    audioRange,
                    Optional.of(position),
                    sourceEntityId,
                    sessionKey,
                    category
            );
        }

        public static PlayDistantSoundS2CPacket startSession(
                Identifier soundId,
                Vec3d position,
                float baseVolume,
                float pitch,
                float audioRange,
                String sessionKey,
                SoundCategory category
        ) {
            return startSession(soundId, position, 0, baseVolume, pitch, audioRange, sessionKey, category);
        }
    }
    public record SyncPettingStateS2CPacket(boolean isPetting) {}
    public record PlayMountSoundS2CPacket(Identifier soundId, float pitch, int delay) {}
    public record PlayerKnockbackS2CPacket(double velocityX, double velocityY, double velocityZ) {}

    /**
     * Registers all packet definitions and their handlers.
     * This method must be called during common setup on both client and server.
     */
    public static void registerCommonPackets() {

        // --- Client to Server (C2S) ---
        CHANNEL.register(ThrowHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new ThrowHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> HamsterEntity.tryThrowFromShoulder((ServerPlayerEntity) context.get().getPlayer()))
        );

        CHANNEL.register(HamsterAnimationSoundC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.hamsterEntityId());
                    buf.writeString(packet.soundId());
                },
                (buf) -> new HamsterAnimationSoundC2SPacket(buf.readInt(), buf.readString()),
                (packet, context) -> context.get().queue(() -> {
                    Entity entity = context.get().getPlayer().getWorld().getEntityById(packet.hamsterEntityId());
                    if (entity instanceof HamsterEntity hamster) {
                        // Ensure sender is >16 blocks away (squared)
                        if (hamster.squaredDistanceTo(context.get().getPlayer()) > 256.0) {
                            if ("hamster_thump_sound".equals(packet.soundId())) {

                                // Ignore duplicate packets
                                if (hamster.interactionCooldown <= 0) {
                                    float thumpPitch = 1.0F + hamster.getRandom().nextFloat() * 0.4F;
                                    net.dawson.adorablehamsterpets.util.HamsterPhysicsUtil.broadcastImpactSound(hamster, net.dawson.adorablehamsterpets.sound.ModSounds.HAMSTER_THUMP.get(), thumpPitch);

                                    // 5-tick cooldown
                                    hamster.interactionCooldown = 5;
                                }
                            }
                        }
                    }
                })
        );

        CHANNEL.register(DismountHamsterC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new DismountHamsterC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        ((PlayerEntityAccessor) player).adorablehamsterpets$dismountShoulderHamster(false);
                    }
                })
        );

        CHANNEL.register(UpdateHamsterRenderStateC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.hamsterEntityIds().size());
                    for (int id : packet.hamsterEntityIds()) {
                        buf.writeInt(id);
                    }
                    buf.writeBoolean(packet.isRendering());
                },
                (buf) -> {
                    int size = buf.readInt();
                    List<Integer> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        ids.add(buf.readInt());
                    }
                    return new UpdateHamsterRenderStateC2SPacket(ids, buf.readBoolean());
                },
                (packet, context) -> context.get().queue(() -> {
                    for (int id : packet.hamsterEntityIds()) {
                        if (packet.isRendering()) {
                            HamsterRenderTracker.addPlayer(id, context.get().getPlayer().getUuid());
                        } else {
                            HamsterRenderTracker.removePlayer(id, context.get().getPlayer().getUuid());
                        }
                    }
                })
        );

        CHANNEL.register(RequestGuidebookC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new RequestGuidebookC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) context.get().getPlayer();

                    // Deliver guidebook: no advancement, no fallback message, play effect, close the config screen
                    AdorableHamsterPets.deliverGuidebook(player, false, false, true, true);

                    // Set cache
                    ((PlayerEntityAccessor) player).ahp$initGuideBookTracking(true);
                })
        );

        CHANNEL.register(AcknowledgeGuidebookWarningC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new AcknowledgeGuidebookWarningC2SPacket(),
                (packet, context) -> context.get().queue(() ->
                        GuidebookProgressUtil.markMissingGuidebookWarningSeen((ServerPlayerEntity) context.get().getPlayer()))
        );

        CHANNEL.register(RequestGuidebookWarningC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new RequestGuidebookWarningC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) context.get().getPlayer();
                    if (Configs.AHP_UI.playersWhoHaveSeenGuidebookWarning.contains("john_wayne")) return;
                    if (Configs.AHP_UI.enableAutoGuidebookDelivery) return;
                    if (GuidebookProgressUtil.hasReceivedGuidebook(player)) return;
                    if (GuidebookProgressUtil.hasSeenMissingGuidebookWarning(player)) return;

                    PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;
                    if (accessor.ahp$computeHasGuideBook(player)) {
                        GuidebookProgressUtil.markGuidebookReceived(player);
                        return;
                    }

                    if (GuidebookProgressUtil.markMissingGuidebookWarningSeen(player)) {
                        CHANNEL.sendToPlayer(player, new ShowGuidebookWarningS2CPacket());
                    }
                })
        );

        CHANNEL.register(RequestHamsterMountC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()),
                (buf) -> new RequestHamsterMountC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    PlayerEntity player = context.get().getPlayer();
                    net.minecraft.entity.Entity entity = player.getWorld().getEntityById(packet.entityId());
                    if (entity instanceof HamsterEntity hamster && hamster.isOwner(player)) {
                        // Distance check for security
                        if (hamster.squaredDistanceTo(player) < 64.0) {
                            HamsterInteractionUtil.executeShoulderMount(hamster, player, ItemStack.EMPTY); // Pass empty stack for force-mount
                        }
                    }
                })
        );

        CHANNEL.register(ResetHeistHistoryC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new ResetHeistHistoryC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$clearHeistHistory();
                    }
                })
        );

        CHANNEL.register(RequestHamsterRideC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()),
                (buf) -> new RequestHamsterRideC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    if (!Configs.AHP_MAIN.enableMountableHamsters.get()) {
                        return;
                    }

                    PlayerEntity player = context.get().getPlayer();
                    net.minecraft.entity.Entity entity = player.getWorld().getEntityById(packet.entityId());

                    if (entity instanceof HamsterEntity hamster) {
                        if (hamster.squaredDistanceTo(player) < 64.0) {
                            hamster.putPlayerOnBack(player);
                        }
                    }
                })
        );

        CHANNEL.register(RequestPetHamsterC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.entityId()),
                (buf) -> new RequestPetHamsterC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$startPettingHamster(packet.entityId());
                    }
                })
        );

        CHANNEL.register(HamsterInputC2SPacket.class,
                (packet, buf) -> {
                    buf.writeBoolean(packet.jumpHeld());
                    buf.writeBoolean(packet.sprintHeld());
                },
                (buf) -> new HamsterInputC2SPacket(buf.readBoolean(), buf.readBoolean()),
                (packet, context) -> context.get().queue(() -> {
                    if (!Configs.AHP_MAIN.enableMountableHamsters.get()) return;

                    PlayerEntity player = context.get().getPlayer();
                    if (player.getVehicle() instanceof HamsterEntity hamster) {
                        if (hamster.getControllingPassenger() == player) {
                            hamster.setRiderInput(packet.jumpHeld(), packet.sprintHeld());
                        }
                    }
                })
        );

        CHANNEL.register(RenameHamsterC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeString(packet.newName());
                },
                (buf) -> new RenameHamsterC2SPacket(buf.readInt(), buf.readString()),
                (packet, context) -> context.get().queue(() -> {
                    if (!Configs.AHP_MAIN.enableGuiRenaming) return;

                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        net.minecraft.entity.Entity entity = player.getWorld().getEntityById(packet.entityId());

                        // Ensure player owns hamster and is close enough
                        if (entity instanceof HamsterEntity hamster && hamster.isOwner(player) && hamster.squaredDistanceTo(player) < 64.0) {
                            String newName = packet.newName().trim();
                            boolean canRename = true;

                            // Process name tag sacrifice if configured
                            if (Configs.AHP_UI.consumeNameTagForGuiRename) {
                                canRename = HamsterInteractionUtil.consumeNameTag(player, hamster);
                            }

                            if (canRename) {
                                if (newName.isEmpty()) {
                                    hamster.setCustomName(null);
                                } else {
                                    hamster.setCustomName(Text.literal(newName));

                                    // Trigger Sweet Potato easter egg effects after GUI closes
                                    if (hamster.isSweetPotato()) {
                                        Runnable easterEggTask = new Runnable() {
                                            @Override
                                            public void run() {
                                                if (player.currentScreenHandler != player.playerScreenHandler) {
                                                    hamster.scheduleTask(hamster.getWorld().getTime() + 5, "sweet_potato_delay", this);
                                                } else {
                                                    player.server.getCommandManager().executeWithPrefix(player.getCommandSource(), "function adorablehamsterpets:technical/sweet_potato_effects");
                                                }
                                            }
                                        };
                                        easterEggTask.run();
                                    }
                                }
                            }
                        }
                    }
                })
        );

        CHANNEL.register(UpdateHamsterArmorVisibilityC2SPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeBoolean(packet.visible());
                },
                (buf) -> new UpdateHamsterArmorVisibilityC2SPacket(
                        buf.readInt(), buf.readBoolean()),
                (packet, context) -> context.get().queue(() -> {
                    if (!(context.get().getPlayer() instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    Entity entity = player.getWorld().getEntityById(packet.entityId());
                    if (!(entity instanceof HamsterEntity hamster)
                            || !hamster.isAlive()
                            || hamster.isRemoved()
                            || !hamster.isOwner(player)
                            || hamster.squaredDistanceTo(player) >= 64.0) {
                        return;
                    }

                    if (!(player.currentScreenHandler instanceof HamsterInventoryScreenHandler handler)
                            || handler.getHamsterEntity() != hamster) {
                        return;
                    }

                    hamster.setArmorVisible(packet.visible());
                })
        );

        CHANNEL.register(UpdateCrownThemeC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.themeOrdinal()),
                (buf) -> new UpdateCrownThemeC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof PlayerEntityAccessor player) {
                        player.ahp$setSupporterCrownTheme(packet.themeOrdinal());
                    }
                })
        );

        CHANNEL.register(StartCrownTrialC2SPacket.class,
                (packet, buf) -> buf.writeInt(packet.themeOrdinal()),
                (buf) -> new StartCrownTrialC2SPacket(buf.readInt()),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        PlayerEntityAccessor accessor = (PlayerEntityAccessor) player;

                        // Prevent users from requesting multiple trials per server by checking nbt flag
                        if (!accessor.ahp$hasUsedSupporterCrownTrial()) {
                            accessor.ahp$setHasUsedSupporterCrownTrial(true);
                            accessor.ahp$setSupporterCrownTrialTicks(600); // 30 seconds
                            accessor.ahp$setSupporterCrownTheme(packet.themeOrdinal());
                        }
                    }
                })
        );

        CHANNEL.register(AdjustGeneticsConfigC2SPacket.class,
                (packet, buf) -> {
                    buf.writeBoolean(packet.isVariance());
                    buf.writeBoolean(packet.increase());
                },
                (buf) -> new AdjustGeneticsConfigC2SPacket(buf.readBoolean(), buf.readBoolean()),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof ServerPlayerEntity player) {
                        if (player.hasPermissionLevel(2)) {
                            // OP required to modify server config
                            if (packet.isVariance()) {
                                double current = Configs.AHP_MAIN.geneticVariance.get();
                                double next = MathHelper.clamp(current + (packet.increase() ? 0.05 : -0.05), 0.0, 1.0);
                                @SuppressWarnings("unchecked")
                                ValidatedFieldAccessor<Double> accessor = (ValidatedFieldAccessor<Double>) (Object) Configs.AHP_MAIN.geneticVariance;
                                accessor.adorablehamsterpets$set(next);
                                player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.genetic_variance_updated", String.format("%.2f", next)).formatted(Formatting.WHITE), true);
                            } else {
                                double current = Configs.AHP_MAIN.geneticMutationRate.get();
                                double next = MathHelper.clamp(current + (packet.increase() ? 0.1 : -0.1), 0.0, 2.0);
                                @SuppressWarnings("unchecked")
                                ValidatedFieldAccessor<Double> accessor = (ValidatedFieldAccessor<Double>) (Object) Configs.AHP_MAIN.geneticMutationRate;
                                accessor.adorablehamsterpets$set(next);
                                player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.genetics_mutation_rate_updated", String.format("%.1f", next)).formatted(Formatting.WHITE), true);
                            }
                            Configs.AHP_MAIN.save();
                        } else {
                            player.sendMessage(Text.translatable("message.adorablehamsterpets.breeding.genetics_visualization.no_permission").formatted(Formatting.RED), true);
                        }
                    }
                })
        );

        CHANNEL.register(CancelPettingC2SPacket.class,
                (packet, buf) -> {},
                (buf) -> new CancelPettingC2SPacket(),
                (packet, context) -> context.get().queue(() -> {
                    if (context.get().getPlayer() instanceof PlayerEntityAccessor accessor) {
                        accessor.ahp$cancelPettingHamster();
                    }
                })
        );

        // --- Server to Client (S2C) ---
        CHANNEL.register(SpawnBeddingParticlesS2CPacket.class,
                (packet, buf) -> {
                    buf.writeBlockPos(packet.pos());
                    buf.writeEnumConstant(packet.direction());
                    buf.writeEnumConstant(packet.variant());
                },
                (buf) -> new SpawnBeddingParticlesS2CPacket(
                        buf.readBlockPos(),
                        buf.readEnumConstant(Direction.class),
                        buf.readEnumConstant(WoodVariant.class)
                ),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handleSpawnBeddingParticles(packet))
                )
        );

        CHANNEL.register(PlayGuidebookEffectsS2CPacket.class,
                (packet, buf) -> buf.writeBoolean(packet.closeScreen()),
                (buf) -> new PlayGuidebookEffectsS2CPacket(buf.readBoolean()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.queueGuidebookEffects(packet))
                )
        );

        CHANNEL.register(ShowGuidebookWarningS2CPacket.class,
                (packet, buf) -> {},
                (buf) -> new ShowGuidebookWarningS2CPacket(),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> AdorableHamsterPetsClient::handleGuidebookWarningApproval)
                )
        );

        CHANNEL.register(SyncHamsterStateS2CPacket.class,
                (packet, buf) -> {
                    buf.writeInt(packet.entityId());
                    buf.writeNbt(packet.data());
                },
                (buf) -> new SyncHamsterStateS2CPacket(buf.readInt(), buf.readNbt()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.world != null) {
                                Entity entity = client.world.getEntityById(packet.entityId());
                                if (entity instanceof PlayerEntity player && entity instanceof PlayerEntityAccessor accessor) {
                                    accessor.adorablehamsterpets$setRawHamsterState(packet.data());
                                    ClientShoulderHamsterData.REPLAY_CACHE.put(player.getUuid(), packet.data());
                                }
                            }
                        })
                )
        );

        CHANNEL.register(PlayDistantSoundS2CPacket.class,
                (packet, buf) -> {
                    buf.writeIdentifier(packet.soundId());
                    buf.writeFloat(packet.volume());
                    buf.writeFloat(packet.pitch());
                    buf.writeFloat(packet.audioRange());
                    buf.writeBoolean(packet.position().isPresent());
                    if (packet.position().isPresent()) {
                        buf.writeDouble(packet.position().get().x);
                        buf.writeDouble(packet.position().get().y);
                        buf.writeDouble(packet.position().get().z);
                    }
                    buf.writeVarInt(packet.sourceEntityId());
                    buf.writeString(packet.sessionKey());
                    buf.writeEnumConstant(packet.category());
                },
                (buf) -> {
                    Identifier soundId = buf.readIdentifier();
                    float volume = buf.readFloat();
                    float pitch = buf.readFloat();
                    float audioRange = buf.readFloat();
                    Optional<Vec3d> position = buf.readBoolean()
                            ? Optional.of(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()))
                            : Optional.empty();
                    int sourceEntityId = buf.readVarInt();
                    String sessionKey = buf.readString();
                    SoundCategory category = buf.readEnumConstant(SoundCategory.class);
                    return new PlayDistantSoundS2CPacket(soundId, volume, pitch, audioRange, position, sourceEntityId, sessionKey, category);
                },
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handlePlayDistantSound(packet))
                )
        );

        CHANNEL.register(StopDistantSoundS2CPacket.class,
                (packet, buf) -> buf.writeString(packet.sessionKey()),
                (buf) -> new StopDistantSoundS2CPacket(buf.readString()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handleStopDistantSound(packet))
                )
        );

        CHANNEL.register(SyncPettingStateS2CPacket.class,
                (packet, buf) -> buf.writeBoolean(packet.isPetting()),
                (buf) -> new SyncPettingStateS2CPacket(buf.readBoolean()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                            if (packet.isPetting()) {
                                AdorableHamsterPetsClient.clientPettingTicks = 200; // Sync client state for 10 seconds
                            } else {
                                AdorableHamsterPetsClient.clientPettingTicks = 0;
                            }
                        })
                )
        );

        CHANNEL.register(PlayMountSoundS2CPacket.class,
                (packet, buf) -> {
                    buf.writeIdentifier(packet.soundId());
                    buf.writeFloat(packet.pitch());
                    buf.writeInt(packet.delay());
                },
                (buf) -> new PlayMountSoundS2CPacket(buf.readIdentifier(), buf.readFloat(), buf.readInt()),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AdorableHamsterPetsClient.handlePlayMountSound(packet.soundId(), packet.pitch(), packet.delay()))
                )
        );

        CHANNEL.register(PlayerKnockbackS2CPacket.class,
                (packet, buf) -> {
                    buf.writeDouble(packet.velocityX());
                    buf.writeDouble(packet.velocityY());
                    buf.writeDouble(packet.velocityZ());
                },
                (buf) -> new PlayerKnockbackS2CPacket(
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble()
                ),
                (packet, context) -> context.get().queue(() ->
                        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null) {
                                client.player.setVelocity(packet.velocityX(), packet.velocityY(), packet.velocityZ());
                            }
                        })
                )
        );
    }
}
