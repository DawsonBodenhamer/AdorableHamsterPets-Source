package net.dawson.adorablehamsterpets.fabric.client;


import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.fabricmc.api.ClientModInitializer;


public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
        ModKeyBindings.init();
        KeyMappingRegistry.register(ModKeyBindings.THROW_HAMSTER_KEY); // Register keybind for Fabric
        AdorableHamsterPetsClient.initScreenHandlers();
        AdorableHamsterPetsClient.initEntityRenderers();
        AdorableHamsterPetsClient.initModelLayers();
    }
}