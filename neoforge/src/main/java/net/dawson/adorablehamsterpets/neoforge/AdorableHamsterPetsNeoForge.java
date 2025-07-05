package net.dawson.adorablehamsterpets.neoforge;


import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.dawson.adorablehamsterpets.neoforge.client.AdorableHamsterPetsNeoForgeClient;
import net.dawson.adorablehamsterpets.world.neoforge.ModSpawnPlacementsImpl;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;


@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsNeoForge {
    /**
     * The main entrypoint for the mod on the NeoForge platform.
     * NeoForge injects the mod-specific event bus into this constructor.
     * @param modEventBus The event bus for this mod, provided by NeoForge.
     */
    public AdorableHamsterPetsNeoForge(IEventBus modEventBus) {
        // --- Register Event Handlers ---
        modEventBus.register(this);
        modEventBus.register(ModSpawnPlacementsImpl.class);


        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsNeoForgeClient.class);
        }


        // --- Initialize Registries during Construction ---
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes(); // Register attributes right after registries

        // --- Handle Biome Modifications during Construction ---
        // This needs to run here so Architectury can register its own event listeners correctly.
        AdorableHamsterPets.LOGGER.info("[AHP Spawn Debug] Initializing biome modifications for hamster spawns (NeoForge-specific path).");
        BiomeModifications.addProperties(
                ModEntitySpawns::shouldSpawnInBiome,
                (context, props) -> {
                    context.getKey().ifPresent(key -> AdorableHamsterPets.LOGGER.info("[AHP Spawn Debug] Applying spawn entry to biome: {}", key.toString()));
                    props.getSpawnProperties().addSpawn(
                            SpawnGroup.CREATURE,
                            new SpawnSettings.SpawnEntry(
                                    ModEntities.HAMSTER.get(),
                                    Configs.AHP.spawnWeight.get(),
                                    1,
                                    Configs.AHP.maxGroupSize.get()
                            )
                    );
                }
        );
    }


    /**
     * Listens for the FMLCommonSetupEvent to run logic that must occur after
     * registries are populated.
     * @param event The common setup event.
     */
    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Run the common setup logic, which now includes spawn registration.
        // This runs synchronously within the event handler, not deferred.
        AdorableHamsterPets.initCommonSetup();
    }
}