package net.dawson.adorablehamsterpets.neoforge;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.neoforge.client.AdorableHamsterPetsNeoForgeClient;
import net.dawson.adorablehamsterpets.world.neoforge.ModSpawnPlacementsImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsNeoForge {
    /**
     * The main entrypoint for the mod on the NeoForge platform.
     * NeoForge injects the mod-specific event bus into this constructor.
     * @param modEventBus The event bus for this mod, provided by NeoForge.
     */
    public AdorableHamsterPetsNeoForge(IEventBus modEventBus) {
        // Register the class containing @SubscribeEvent methods to the mod event bus.
        modEventBus.register(ModSpawnPlacementsImpl.class);

        // Register client events only on the client side
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsNeoForgeClient.class);
        }

        // Run common setup.
        AdorableHamsterPets.init();
    }
}