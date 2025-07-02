package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityAccessor {

    // --- 1. DataTracker Definition ---
    @Unique
    private static final TrackedData<NbtCompound> HAMSTER_SHOULDER_ENTITY = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

    // --- Constants and Static Utilities ---
    @Unique
    private static final int CHECK_INTERVAL_TICKS = 20;
    @Unique
    private static final List<String> DISMOUNT_MESSAGE_KEYS = Arrays.asList(
            "message.adorablehamsterpets.dismount.1", "message.adorablehamsterpets.dismount.2",
            "message.adorablehamsterpets.dismount.3", "message.adorablehamsterpets.dismount.4",
            "message.adorablehamsterpets.dismount.5", "message.adorablehamsterpets.dismount.6"
    );

    // --- Fields ---
    @Unique
    private int adorablehamsterpets$diamondCheckTimer = 0;
    @Unique
    private int adorablehamsterpets$creeperCheckTimer = 0;
    @Unique
    private int adorablehamsterpets$diamondSoundCooldownTicks = 0;
    @Unique
    private int adorablehamsterpets$creeperSoundCooldownTicks = 0;
    @Unique
    private String adorablehamsterpets$lastDismountMessageKey = "";
    @Unique
    private boolean adorablehamsterpets$isDiamondAlertConditionMet = false;
    @Unique
    private int adorablehamsterpets$lastGoldMessageIndex = -1;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- 2. DataTracker Registration ---
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void adorablehamsterpets$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        // --- Diagnostic Logging ---
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] PlayerEntityMixin initDataTracker is RUNNING for entity {}.", this.getId());
        builder.add(HAMSTER_SHOULDER_ENTITY, new NbtCompound());
    }

    // --- 3. NBT Read/Write ---
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void adorablehamsterpets$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- Diagnostic Logging ---
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] PlayerEntityMixin writeNbt is RUNNING for entity {}.", this.getId());
        if (!this.getHamsterShoulderEntity().isEmpty()) {
            nbt.put("ShoulderHamster", this.getHamsterShoulderEntity());
        }
        if (this.adorablehamsterpets$lastGoldMessageIndex != -1) {
            nbt.putInt("LastGoldMessageIndex", this.adorablehamsterpets$lastGoldMessageIndex);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void adorablehamsterpets$readNbt(NbtCompound nbt, CallbackInfo ci) {
        // --- Diagnostic Logging ---
        AdorableHamsterPets.LOGGER.debug("[AHP Mixin] PlayerEntityMixin readNbt is RUNNING for entity {}.", this.getId());
        if (nbt.contains("ShoulderHamster", 10)) {
            this.setHamsterShoulderEntity(nbt.getCompound("ShoulderHamster"));
        }
        if (nbt.contains("LastGoldMessageIndex", NbtElement.INT_TYPE)) {
            this.adorablehamsterpets$lastGoldMessageIndex = nbt.getInt("LastGoldMessageIndex");
        } else {
            this.adorablehamsterpets$lastGoldMessageIndex = -1;
        }
    }

    // --- Player Removal Cleanup ---
    /**
     * Injects into the entity's remove method.
     * When a player is removed from the world (e.g., disconnects), this ensures they are
     * cleaned up from the server-side render tracker to prevent memory leaks.
     */
    @Inject(method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void adorablehamsterpets$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!this.getWorld().isClient()) {
            net.dawson.adorablehamsterpets.util.HamsterRenderTracker.onPlayerDisconnect(this.getUuid());
        }
    }

    // --- 4. Public Accessors for the DataTracker ---

    /**
     * Retrieves the NBT data for the hamster currently on the player's shoulder.
     * This data is stored in a custom {@link TrackedData} field and synced from server to client.
     *
     * @return An {@link NbtCompound} containing the shoulder hamster's data. Returns an empty compound if no hamster is on the shoulder.
     */
    @Unique
    public NbtCompound getHamsterShoulderEntity() {
        return this.getDataTracker().get(HAMSTER_SHOULDER_ENTITY);
    }

    /**
     * Sets the NBT data for the hamster on the player's shoulder.
     * This updates the custom {@link TrackedData} field, which is then synced to clients.
     *
     * @param nbt The {@link NbtCompound} to set. Use an empty compound to remove the shoulder hamster.
     */
    @Unique
    public void setHamsterShoulderEntity(NbtCompound nbt) {
        this.getDataTracker().set(HAMSTER_SHOULDER_ENTITY, nbt);
    }

    // --- 5. Tick Logic ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        // --- 1. Initial Setup and Server-Side Check ---
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) {
            return;
        }
        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        // --- End 1. Initial Setup and Server-Side Check ---

        // --- 2. Cooldown Decrement ---
        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;
        // --- End 2. Cooldown Decrement ---

        NbtCompound shoulderNbt = this.getHamsterShoulderEntity();
        if (!shoulderNbt.isEmpty()) {
            // --- 3. Handle Player Sneaking for Dismount ---
            if (self.isSneaking()) {
                HamsterEntity.spawnFromNbt((ServerWorld) world, self, shoulderNbt, this.adorablehamsterpets$isDiamondAlertConditionMet);
                this.adorablehamsterpets$isDiamondAlertConditionMet = false;

                this.setHamsterShoulderEntity(new NbtCompound()); // Clear the data

                world.playSound(null, self.getBlockPos(), ModSounds.HAMSTER_DISMOUNT.get(), SoundCategory.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);

                if (config.enableShoulderDismountMessages && !DISMOUNT_MESSAGE_KEYS.isEmpty()) {
                    String chosenKey;
                    if (DISMOUNT_MESSAGE_KEYS.size() == 1) {
                        chosenKey = DISMOUNT_MESSAGE_KEYS.get(0);
                    } else {
                        List<String> availableKeys = new ArrayList<>(DISMOUNT_MESSAGE_KEYS);
                        availableKeys.remove(this.adorablehamsterpets$lastDismountMessageKey);
                        chosenKey = availableKeys.isEmpty() ? this.adorablehamsterpets$lastDismountMessageKey : availableKeys.get(random.nextInt(availableKeys.size()));
                    }
                    self.sendMessage(Text.translatable(chosenKey), true);
                    this.adorablehamsterpets$lastDismountMessageKey = chosenKey;
                }
                return;
            }
            // --- End 3. Handle Player Sneaking for Dismount ---

            // --- 4. Shoulder Diamond Detection ---
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    if (isDiamondNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true;
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 2.5f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.diamond_nearby").formatted(Formatting.AQUA), true);
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextBetween(140, 200);
                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.get().trigger((ServerPlayerEntity) self);
                        }
                    } else {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = false;
                    }
                }
            }
            // --- End 4. Shoulder Diamond Detection ---

            // --- 5. Shoulder Creeper Detection ---
            if (config.enableShoulderCreeperDetection) {
                adorablehamsterpets$creeperCheckTimer++;
                if (adorablehamsterpets$creeperCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$creeperCheckTimer = 0;
                    if (creeperSeesPlayer(self, config.shoulderCreeperDetectionRadius.get())) {
                        if (adorablehamsterpets$creeperSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_CREEPER_DETECT_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 1.0f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.creeper_detected").formatted(Formatting.RED), true);
                            adorablehamsterpets$creeperSoundCooldownTicks = random.nextBetween(100, 160);
                            ModCriteria.HAMSTER_CREEPER_ALERT_TRIGGERED.get().trigger((ServerPlayerEntity) self);
                        }
                    }
                }
            }
            // --- End 5. Shoulder Creeper Detection ---
        }
    }

    /**
     * Scans a spherical area around the player for diamond ore blocks.
     * This check is performed periodically when a hamster is on the player's shoulder.
     *
     * @param player The player to check around.
     * @param radius The radius of the sphere to scan, in blocks.
     * @return {@code true} if {@link Blocks#DIAMOND_ORE} or {@link Blocks#DEEPSLATE_DIAMOND_ORE} is found within the radius, otherwise {@code false}.
     */
    @Unique
    private boolean isDiamondNearby(PlayerEntity player, double radius) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();
        int intRadius = (int) Math.ceil(radius);

        for (BlockPos checkPos : BlockPos.iterate(center.add(-intRadius, -intRadius, -intRadius), center.add(intRadius, intRadius, intRadius))) {
            if (checkPos.getSquaredDistance(center) <= radius * radius) {
                BlockState state = world.getBlockState(checkPos);
                if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks for nearby creepers that are actively targeting the player.
     * This is used for the shoulder hamster's creeper alert feature.
     *
     * @param player The player being targeted.
     * @param radius The search radius for creepers.
     * @return {@code true} if at least one creeper is found with the player as its current attack target, otherwise {@code false}.
     */
    @Unique
    private boolean creeperSeesPlayer(PlayerEntity player, double radius) {
        World world = player.getWorld();
        Box searchBox = new Box(player.getPos().subtract(radius, radius, radius), player.getPos().add(radius, radius, radius));
        List<CreeperEntity> nearbyCreepers = world.getEntitiesByClass(
                CreeperEntity.class,
                searchBox,
                creeper -> creeper.isAlive() && creeper.getTarget() == player && EntityPredicates.VALID_ENTITY.test(creeper)
        );
        return !nearbyCreepers.isEmpty();
    }

    @Unique
    @Override
    public int ahp_getLastGoldMessageIndex() {
        return this.adorablehamsterpets$lastGoldMessageIndex;
    }

    @Unique
    @Override
    public void ahp_setLastGoldMessageIndex(int index) {
        this.adorablehamsterpets$lastGoldMessageIndex = index;
    }
}