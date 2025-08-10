package net.dawson.adorablehamsterpets.fabric.client;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.fabricmc.api.ClientModInitializer;

public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
        AdorableHamsterPetsClient.initScreenHandlers();
        AdorableHamsterPetsClient.initEntityRenderers();
        AdorableHamsterPetsClient.initModelLayers();

        // --- Register keybindings for Fabric ---
        ModKeyBindings.init();
        KeyMappingRegistry.register(ModKeyBindings.THROW_HAMSTER_KEY);
        KeyMappingRegistry.register(ModKeyBindings.DISMOUNT_HAMSTER_KEY);
    }
}