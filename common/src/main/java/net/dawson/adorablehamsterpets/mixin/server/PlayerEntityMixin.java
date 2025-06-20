package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.attachment.HamsterShoulderData;
import net.dawson.adorablehamsterpets.attachment.ModEntityAttachments;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
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
public abstract class PlayerEntityMixin extends LivingEntity {

    // --- Constants and Static Utilities ---
    @Unique
    private static final int CHECK_INTERVAL_TICKS = 20; // How often to check for diamonds/creepers
    @Unique
    private static final List<String> DISMOUNT_MESSAGE_KEYS = Arrays.asList(
            "message.adorablehamsterpets.dismount.1",
            "message.adorablehamsterpets.dismount.2",
            "message.adorablehamsterpets.dismount.3",
            "message.adorablehamsterpets.dismount.4",
            "message.adorablehamsterpets.dismount.5",
            "message.adorablehamsterpets.dismount.6"
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

    /**
     * Constructor for the mixin.
     * @param entityType The entity type.
     * @param world The world.
     */
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Injects logic into the player's tick method to handle shoulder hamster features.
     * This includes dismounting, diamond detection, and creeper detection.
     * @param ci CallbackInfo for the injection.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void adorablehamsterpets$onTick(CallbackInfo ci) {
        // --- Initial Setup and Server-Side Check ---
        PlayerEntity self = (PlayerEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient) {
            return;
        }
        Random random = world.getRandom();
        final AhpConfig config = AdorableHamsterPets.CONFIG;
        // --- End Initial Setup ---

        // --- Cooldown Decrement ---
        if (adorablehamsterpets$diamondSoundCooldownTicks > 0) adorablehamsterpets$diamondSoundCooldownTicks--;
        if (adorablehamsterpets$creeperSoundCooldownTicks > 0) adorablehamsterpets$creeperSoundCooldownTicks--;
        // --- End Cooldown Decrement ---

        HamsterShoulderData shoulderData = self.getAttached(ModEntityAttachments.HAMSTER_SHOULDER_DATA);
        if (shoulderData != null) {
            // --- Handle Player Sneaking for Dismount ---
            if (self.isSneaking()) {
                AdorableHamsterPets.LOGGER.debug("[PlayerTickMixin] Player {} is sneaking. Dismounting hamster.", self.getName().getString());
                // Pass the flag to spawnFromShoulderData or handle priming here
                HamsterEntity.spawnFromShoulderData((ServerWorld) world, self, shoulderData, this.adorablehamsterpets$isDiamondAlertConditionMet);
                this.adorablehamsterpets$isDiamondAlertConditionMet = false; // Reset player's flag after use

                self.removeAttached(ModEntityAttachments.HAMSTER_SHOULDER_DATA);

                // Play dismount sound at player's location
                world.playSound(null, self.getBlockPos(), ModSounds.HAMSTER_DISMOUNT.get(), SoundCategory.PLAYERS, 0.7f, 1.0f + random.nextFloat() * 0.2f);

                // Send dismount message if enabled
                if (config.enableShoulderDismountMessages && !DISMOUNT_MESSAGE_KEYS.isEmpty()) {
                    String chosenKey;
                    if (DISMOUNT_MESSAGE_KEYS.size() == 1) {
                        chosenKey = DISMOUNT_MESSAGE_KEYS.get(0);
                    } else {
                        List<String> availableKeys = new ArrayList<>(DISMOUNT_MESSAGE_KEYS);
                        availableKeys.remove(this.adorablehamsterpets$lastDismountMessageKey); // Avoid immediate repeat
                        chosenKey = availableKeys.isEmpty() ? this.adorablehamsterpets$lastDismountMessageKey : availableKeys.get(random.nextInt(availableKeys.size()));
                    }
                    self.sendMessage(Text.translatable(chosenKey), true);
                    this.adorablehamsterpets$lastDismountMessageKey = chosenKey;
                }

                // Reset detection timers and cooldowns as hamster is no longer on shoulder
                adorablehamsterpets$diamondCheckTimer = 0;
                adorablehamsterpets$creeperCheckTimer = 0;
                adorablehamsterpets$diamondSoundCooldownTicks = 0;
                adorablehamsterpets$creeperSoundCooldownTicks = 0;
                return; // Interaction handled, no further shoulder checks needed this tick
            }
            // --- End Handle Player Sneaking for Dismount ---

            // --- Shoulder Diamond Detection ---
            if (config.enableShoulderDiamondDetection) {
                adorablehamsterpets$diamondCheckTimer++;
                if (adorablehamsterpets$diamondCheckTimer >= CHECK_INTERVAL_TICKS) {
                    adorablehamsterpets$diamondCheckTimer = 0;
                    if (isDiamondNearby(self, config.shoulderDiamondDetectionRadius.get())) {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = true; // SET FLAG
                        if (adorablehamsterpets$diamondSoundCooldownTicks == 0) {
                            world.playSound(null, self.getBlockPos(),
                                    ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_DIAMOND_SNIFF_SOUNDS, random),
                                    SoundCategory.NEUTRAL, 2.5f, 1.0f);
                            self.sendMessage(Text.translatable("message.adorablehamsterpets.diamond_nearby").formatted(Formatting.AQUA), true);
                            adorablehamsterpets$diamondSoundCooldownTicks = random.nextBetween(140, 200);

                            ModCriteria.HAMSTER_DIAMOND_ALERT_TRIGGERED.trigger((ServerPlayerEntity) self);
                        }
                    } else {
                        this.adorablehamsterpets$isDiamondAlertConditionMet = false; // RESET FLAG
                    }
                }
            }
            // --- End Shoulder Diamond Detection ---

            // --- Shoulder Creeper Detection ---
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
                            adorablehamsterpets$creeperSoundCooldownTicks = random.nextBetween(100, 160); // 5-8 seconds

                            // --- Trigger Advancement Criterion ---
                            ModCriteria.HAMSTER_CREEPER_ALERT_TRIGGERED.trigger((ServerPlayerEntity) self);
                            // --- End Trigger ---
                        }
                    }
                }
            }
            // --- End Shoulder Creeper Detection ---
        }
    }

    /**
     * Checks if diamond ore is nearby the player.
     * @param player The player to check around.
     * @param radius The radius to check within.
     * @return True if diamond ore is found, false otherwise.
     */
    @Unique
    private boolean isDiamondNearby(PlayerEntity player, double radius) {
        World world = player.getWorld();
        BlockPos center = player.getBlockPos();
        int intRadius = (int) Math.ceil(radius); // Ensure radius covers partial blocks

        for (BlockPos checkPos : BlockPos.iterate(center.add(-intRadius, -intRadius, -intRadius), center.add(intRadius, intRadius, intRadius))) {
            // More accurate distance check for a sphere
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
     * Checks if any nearby creepers are targeting the player.
     * @param player The player to check.
     * @param radius The radius to check for creepers within.
     * @return True if a creeper is targeting the player, false otherwise.
     */
    @Unique
    private boolean creeperSeesPlayer(PlayerEntity player, double radius) {
        World world = player.getWorld();
        Box searchBox = new Box(player.getPos().subtract(radius, radius, radius), player.getPos().add(radius, radius, radius));
        List<CreeperEntity> nearbyCreepers = world.getEntitiesByClass(
                CreeperEntity.class,
                searchBox,
                // Creeper must be alive, targeting the player, and generally valid
                creeper -> creeper.isAlive() && creeper.getTarget() == player && EntityPredicates.VALID_ENTITY.test(creeper)
        );
        return !nearbyCreepers.isEmpty();
    }
}