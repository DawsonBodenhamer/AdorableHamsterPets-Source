package net.dawson.adorablehamsterpets.neoforge;

import net.neoforged.fml.common.Mod;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;

@Mod(AdorableHamsterPets.MOD_ID)
public final class AdorableHamsterPetsNeoForge {
    public AdorableHamsterPetsNeoForge() {
        // Run our common setup.
        AdorableHamsterPets.init();
    }
}