package net.dawson.adorablehamsterpets.forge;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.forge.client.AdorableHamsterPetsForgeClient;
import net.dawson.adorablehamsterpets.world.forge.ModBiomeModifiers;
import net.dawson.adorablehamsterpets.world.forge.ModSpawnPlacementsImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;


@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsForge {

    /**
     * The main entrypoint for the mod on the NeoForge platform.
     * NeoForge injects the mod-specific event bus into this constructor.
     * @param modEventBus The event bus for this mod, provided by NeoForge.
     */
    public AdorableHamsterPetsForge(IEventBus modEventBus) {
        // --- Register Event Handlers ---
        modEventBus.register(this);
        modEventBus.register(ModSpawnPlacementsImpl.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsForgeClient.class);
        }

        // --- Initialize Registries & Attributes during Construction ---
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();

        // --- Register custom biome modifier ---
        ModBiomeModifiers.register(modEventBus);
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