package net.dawson.adorablehamsterpets.fabric;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.fabricmc.api.ModInitializer;

public final class AdorableHamsterPetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AdorableHamsterPets.initRegistries();
        AdorableHamsterPets.initAttributes();
        AdorableHamsterPets.initCommonSetup();
        ModEntitySpawns.initialize();
    }
}
