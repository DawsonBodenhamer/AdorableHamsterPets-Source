package net.dawson.adorablehamsterpets;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public class AdorableHamsterPets {
	public static final String MOD_ID = "adorablehamsterpets";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	public static AhpConfig CONFIG;

	/**
	 * The main initialization method for the mod.
	 * This method is called by platform-specific entrypoints to register all common features.
	 */
	public static void init() {
		CONFIG = Configs.AHP;


		// --- Core Registries (Needed for both runtime and datagen) ---
		ModEntities.register();
		ModDataComponentTypes.registerDataComponentTypes();
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModScreenHandlers.register();
		ModCriteria.register();


		// --- Runtime-only Initializations ---
		// We check if the data generation API is NOT loaded. If it is loaded, we are in a datagen environment
		// and should skip runtime-only logic to prevent crashes.
		if (System.getProperty("fabric-api.datagen") == null) {
			ModRegistries.initialize();


			// --- Networking Client to Server Registration ---
			ModPackets.registerC2SPackets();


			// --- World Gen & Spawns ---
			ModWorldGeneration.generateModWorldGen();
			ModEntitySpawns.initialize();


			// --- Entity Attribute Registration ---
			EntityAttributeRegistry.register(ModEntities.HAMSTER, HamsterEntity::createHamsterAttributes);


			// --- Events ---
			PlayerEvent.PLAYER_JOIN.register(AdorableHamsterPets::onPlayerJoin);
			CommandRegistrationEvent.EVENT.register(ModCommands::register);
			LifecycleEvent.SETUP.register(AdorableHamsterPets::onSetup);
		}
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

		// --- Creative Tab Population (Moved here for NeoForge) ---
		// By the time this runs, all items and item groups are guaranteed to be registered.
		CreativeTabRegistry.modify(ModItemGroups.ADORABLE_HAMSTER_PETS_GROUP, (featureSet, output, hasPermissions) -> {
			output.add(ModItems.CHEESE.get());
			output.add(ModItems.HAMSTER_FOOD_MIX.get());
			output.add(ModItems.CUCUMBER.get());
			output.add(ModItems.CUCUMBER_SEEDS.get());
			output.add(ModItems.SLICED_CUCUMBER.get());
			output.add(ModItems.GREEN_BEANS.get());
			output.add(ModItems.GREEN_BEAN_SEEDS.get());
			output.add(ModItems.STEAMED_GREEN_BEANS.get());
			output.add(ModItems.SUNFLOWER_SEEDS.get());
			output.add(ModItems.HAMSTER_SPAWN_EGG.get());
			output.add(ModItems.HAMSTER_GUIDE_BOOK.get());
			output.add(ModItems.SUNFLOWER_BLOCK_ITEM.get());
			output.add(ModItems.WILD_GREEN_BEAN_BUSH_ITEM.get());
			output.add(ModItems.WILD_CUCUMBER_BUSH_ITEM.get());
		});
	}

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
}