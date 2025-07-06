package net.dawson.adorablehamsterpets.world;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class ModWorldGeneration {

    public static void generateModWorldGen() {
        AdorableHamsterPets.LOGGER.info("Registering Biome Modifications for " + AdorableHamsterPets.MOD_ID);
        registerBiomeModifications();
    }

    @ExpectPlatform
    public static void registerBiomeModifications() {
        throw new AssertionError();
    }

    // --- Biome Helper Predicates (Centralized Logic) ---
    public static boolean isSunflowerPlains(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.SUNFLOWER_PLAINS);
    }

    public static boolean isGreenBeanBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.SWAMP)
                || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP)
                || biome.matchesKey(BiomeKeys.LUSH_CAVES)
                || biome.matchesKey(BiomeKeys.FLOWER_FOREST);
    }

    public static boolean isCucumberBiome(RegistryEntry<Biome> biome) {
        return biome.isIn(BiomeTags.IS_JUNGLE)
                || biome.matchesKey(BiomeKeys.PLAINS)
                || biome.matchesKey(BiomeKeys.SUNFLOWER_PLAINS)
                || biome.matchesKey(BiomeKeys.SAVANNA)
                || biome.matchesKey(BiomeKeys.SAVANNA_PLATEAU)
                || biome.matchesKey(BiomeKeys.FOREST)
                || biome.matchesKey(BiomeKeys.BIRCH_FOREST)
                || biome.matchesKey(BiomeKeys.MEADOW)
                || biome.matchesKey(BiomeKeys.WOODED_BADLANDS);
    }
}