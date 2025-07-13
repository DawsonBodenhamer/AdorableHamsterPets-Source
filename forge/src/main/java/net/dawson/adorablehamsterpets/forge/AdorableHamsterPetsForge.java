package net.dawson.adorablehamsterpets.forge;

import dev.architectury.platform.forge.EventBuses;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.forge.client.AdorableHamsterPetsForgeClient;
import net.dawson.adorablehamsterpets.world.forge.ModBiomeModifiers;
import net.dawson.adorablehamsterpets.world.forge.ModSpawnPlacementsImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsForge {

    public AdorableHamsterPetsForge() {
        // Submit our event bus to let architectury handle the registration of DeferredRegisters
        EventBuses.registerModEventBus(AdorableHamsterPets.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // --- Initialization Order ---
        // 1. Call the method that calls .register() on all DeferredRegister instances.
        //    This populates the registries.
        AdorableHamsterPets.initRegistries();

        // 2. Get the event bus for listening to other events.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 3. Register event listeners.
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.register(ModSpawnPlacementsImpl.class);
        ModBiomeModifiers.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(AdorableHamsterPetsForgeClient.class);
        }
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // 4. Defer the rest of the setup until the FMLCommonSetupEvent.
        //    By this point, all registries are guaranteed to be populated.
        event.enqueueWork(() -> {
            AdorableHamsterPets.initAttributes();
            AdorableHamsterPets.initCommonSetup();
        });
    }
}