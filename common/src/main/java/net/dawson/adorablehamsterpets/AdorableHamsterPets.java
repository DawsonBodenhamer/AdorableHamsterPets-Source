package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.advancement.criterion.ModCriteria;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.command.ModCommands;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItemGroups;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.dawson.adorablehamsterpets.tag.ModItemTags;
import net.dawson.adorablehamsterpets.world.ModSpawnPlacements;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.SpawnRestriction;
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
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModCriteria.register();
	}

	/**
	 * Initializes common setup logic that needs to run after registries are populated.
	 * This is called from FMLCommonSetupEvent on Forge and onInitialize on Fabric.
	 */
	public static void initCommonSetup() {
		// We check if the data generation API is NOT loaded. If it is loaded, we are in a datagen environment
		// and should skip runtime-only logic to prevent crashes.
		if (System.getProperty("fabric-api.datagen") == null) {
			ModRegistries.registerCompostables();
			ModEntitySpawns.parseConfig();
			ModWorldGeneration.parseConfig();
			ModItemTags.parseConfig();

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
		ModSpawnPlacements.register(ModEntities.HAMSTER.get(), SpawnRestriction.Location.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(type, world, reason, pos, random) -> (world.getBlockState(pos.down()).isIn(net.minecraft.registry.tag.BlockTags.ANIMALS_SPAWNABLE_ON) ||
						ModEntitySpawns.VALID_SPAWN_BLOCKS.contains(world.getBlockState(pos.down()).getBlock())));
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
			Advancement flagAdvancement = player.server.getAdvancementLoader().get(flagAdvId);


			if (flagAdvancement != null) {
				AdvancementProgress flagProgress = advancementTracker.getProgress(flagAdvancement);
				if (!flagProgress.isDone()) {
					ModCriteria.FIRST_JOIN_GUIDEBOOK_CHECK.trigger(player);
					for (String criterion : flagAdvancement.getCriteria().keySet()) {
						advancementTracker.grantCriterion(flagAdvancement, criterion);
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
		PlayerEntityAccessor oldPlayerAccessor = (PlayerEntityAccessor) oldPlayer;
		PlayerEntityAccessor newPlayerAccessor = (PlayerEntityAccessor) newPlayer;

		for (ShoulderLocation location : ShoulderLocation.values()) {
			NbtCompound shoulderNbt = oldPlayerAccessor.getShoulderHamster(location);
			if (shoulderNbt.isEmpty()) {
				continue;
			}

			if (wasDeath) {
				// Player Died: Spawn the hamster at the death location
				ServerWorld world = oldPlayer.getServerWorld();
				HamsterEntity.spawnFromNbt(world, oldPlayer, shoulderNbt, false, null);
				AdorableHamsterPets.LOGGER.debug("Player {} died. Spawning {} hamster at death location.", oldPlayer.getName().getString(), location);
			} else {
				// Player Cloned (e.g., End Portal): Transfer hamster to the new player instance
				newPlayerAccessor.setShoulderHamster(location, shoulderNbt);
				AdorableHamsterPets.LOGGER.debug("Player {} was cloned. Transferring {} hamster to new entity.", newPlayer.getName().getString(), location);
			}
		}
	}
}
