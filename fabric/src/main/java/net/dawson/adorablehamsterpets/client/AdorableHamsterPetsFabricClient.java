package net.dawson.adorablehamsterpets.client;


import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.fabricmc.api.ClientModInitializer;


public final class AdorableHamsterPetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AdorableHamsterPetsClient.init();
    }
}