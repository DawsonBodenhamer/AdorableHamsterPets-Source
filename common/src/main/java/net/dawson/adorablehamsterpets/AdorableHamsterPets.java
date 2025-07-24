package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.command.ModCommands;
import net.dawson.adorablehamsterpets.component.ModDataComponentTypes;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItemGroups;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public class AdorableHamsterPets {
	public static final String MOD_ID = "adorablehamsterpets";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);
	public static AhpConfig CONFIG;

	/**
	 * Initializes all DeferredRegister instances.
	 * This must be called during mod construction (e.g., the loader-specific entrypoint's constructor or onInitialize).
	 */
	public static void initRegistries() {
		CONFIG = Configs.AHP;
		ModEntities.register();
		ModDataComponentTypes.registerDataComponentTypes();
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModCriteria.register();
	}

	/**
	 * Initializes common setup logic that needs to run after registries are populated.
	 * This is called from FMLCommonSetupEvent on NeoForge and onInitialize on Fabric.
	 */
	public static void initCommonSetup() {
		// We check if the data generation API is NOT loaded. If it is loaded, we are in a datagen environment
		// and should skip runtime-only logic to prevent crashes.
		if (System.getProperty("fabric-api.datagen") == null) {
			ModRegistries.initialize();
			ModEntitySpawns.parseConfig();
			ModWorldGeneration.parseConfig();

			// --- Networking Client to Server Registration ---
			ModPackets.registerC2SPackets();

			// --- World Gen ---
			ModWorldGeneration.registerBiomeModifications();

			// --- Events ---
			PlayerEvent.PLAYER_JOIN.register(AdorableHamsterPets::onPlayerJoin);
			PlayerEvent.PLAYER_CLONE.register(AdorableHamsterPets::onPlayerClone);
			CommandRegistrationEvent.EVENT.register(ModCommands::register);
			LifecycleEvent.SETUP.register(AdorableHamsterPets::onSetup);
		}
	}

	/**
	 * Initializes entity attributes. This must be called after registries are initialized
	 * but before the main setup event, typically during mod construction.
	 */
	public static void initAttributes() {
		EntityAttributeRegistry.register(ModEntities.HAMSTER, HamsterEntity::createHamsterAttributes);
	}

	/**
	 * This method is called during the SETUP lifecycle event, after all registries are frozen.
	 * It's the safe place to register things that require fully-realized registry objects,
	 * like spawn placements.
	 */
	private static void onSetup() {
		// --- Spawn Restriction Registration ---
		ModSpawnPlacements.register(ModEntities.HAMSTER.get(), SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(type, world, reason, pos, random) -> AnimalEntity.isValidNaturalSpawn(type, world, reason, pos, random) ||
						ModEntitySpawns.VALID_SPAWN_BLOCKS.contains(world.getBlockState(pos.down()).getBlock()));
	}

	/**
	 * An event handler that is called whenever a player joins the server.
	 * <p>
	 * This method is responsible for the one-time delivery of the Hamster Guide Book. It checks if the player
	 * has the {@code adorablehamsterpets:technical/has_received_initial_guidebook} advancement. If they do not,
	 * and if the {@code uiTweaks.enableAutoGuidebookDelivery} config option is enabled, it triggers a custom
	 * criterion. This criterion, in turn, grants another advancement that rewards the player with the
	 * fully-written guide book via a function.
	 *
	 * @param player The ServerPlayerEntity who has just joined the world.
	 */
	private static void onPlayerJoin(ServerPlayerEntity player) {
		if (Configs.AHP.enableAutoGuidebookDelivery) {
			PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
			Identifier flagAdvId = Identifier.of(MOD_ID, "technical/has_received_initial_guidebook");
			net.minecraft.advancement.AdvancementEntry flagAdvancementEntry = player.server.getAdvancementLoader().get(flagAdvId);


			if (flagAdvancementEntry != null) {
				AdvancementProgress flagProgress = advancementTracker.getProgress(flagAdvancementEntry);
				if (!flagProgress.isDone()) {
					ModCriteria.FIRST_JOIN_GUIDEBOOK_CHECK.get().trigger(player);
					for (String criterion : flagAdvancementEntry.value().criteria().keySet()) {
						advancementTracker.grantCriterion(flagAdvancementEntry, criterion);
					}
				}
			} else {
				LOGGER.warn("Could not find flag advancement: {}", flagAdvId);
			}
		}
	}

	/**
	 * An event handler that is called when a player entity is "cloned".
	 * This occurs upon player death and respawn, or when traveling between certain dimensions (like returning from the End).
	 * <p>
	 * This method ensures that a shoulder-mounted hamster is not lost during these events.
	 * <ul>
	 *     <li>If the player died ({@code wasDeath} is true), the hamster is spawned as an entity in the world at the player's death location.</li>
	 *     <li>If the player was cloned for another reason (e.g., dimension travel), the hamster's NBT data is transferred directly to the new player entity instance, keeping it on their shoulder.</li>
	 * </ul>
	 *
	 * @param oldPlayer The player entity instance before the cloning event.
	 * @param newPlayer The new player entity instance created after the cloning event.
	 * @param wasDeath  A boolean flag indicating whether the clone was caused by player death.
	 */
	private static void onPlayerClone(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean wasDeath) {
		// --- 1. Get Shoulder Data from the Old Player ---
		PlayerEntityAccessor oldPlayerAccessor = (PlayerEntityAccessor) oldPlayer;
		NbtCompound shoulderNbt = oldPlayerAccessor.getHamsterShoulderEntity();

		// --- 2. Check if a Hamster Was on the Shoulder ---
		if (shoulderNbt.isEmpty()) {
			return; // No hamster data to process.
		}

		// --- 3. Handle Death vs. Non-Death Scenarios ---
		if (oldPlayer.isDead()) {
			// --- Player Died: Spawn the hamster at the death location ---
			ServerWorld world = oldPlayer.getServerWorld();
			// The 'isDiamondAlertActive' parameter is false as it's not relevant to respawning.
			HamsterEntity.spawnFromNbt(world, oldPlayer, shoulderNbt, false);
			AdorableHamsterPets.LOGGER.debug("Player {} died. Spawning shoulder hamster at death location.", oldPlayer.getName().getString());
		} else {
			// --- Player Cloned (e.g., End Portal): Transfer hamster to the new player instance ---
			PlayerEntityAccessor newPlayerAccessor = (PlayerEntityAccessor) newPlayer;
			newPlayerAccessor.setHamsterShoulderEntity(shoulderNbt);
			AdorableHamsterPets.LOGGER.debug("Player {} was cloned. Transferring shoulder hamster to new entity.", newPlayer.getName().getString());
		}
	}
}
