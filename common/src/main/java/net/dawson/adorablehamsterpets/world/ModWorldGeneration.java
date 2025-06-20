package net.dawson.adorablehamsterpets.world;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.dawson.adorablehamsterpets.world.gen.feature.ModPlacedFeatures;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.function.Predicate;

public class ModWorldGeneration {

    public static void generateModWorldGen() {
        AdorableHamsterPets.LOGGER.info("Registering Biome Modifications for " + AdorableHamsterPets.MOD_ID);

        // --- Sunflower Replacement Logic ---
        // This uses a predicate to select all biomes and then removes the vanilla sunflower feature.
        BiomeModifications.removeProperties(
                context -> true, // Select all biomes
                (context, props) -> props.getGenerationProperties().removeFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        VegetationPlacedFeatures.PATCH_SUNFLOWER
                )
        );

        // This adds our custom sunflower feature only to the sunflower plains biome.
        BiomeModifications.addProperties(
                context -> context.getKey().map(key -> key.equals(BiomeKeys.SUNFLOWER_PLAINS.getValue())).orElse(false),
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY
                )
        );

        // --- Wild Bush Generation ---
        addWildGreenBeanBushGeneration();
        addWildCucumberBushGeneration();
    }

    private static void addWildGreenBeanBushGeneration() {
        // --- Spawns in focused "Lush/Wet" biomes ---
        Predicate<BiomeModifications.BiomeContext> greenBeanBiomeSelector = context -> context.getKey().map(id ->
                ModEntitySpawns.matchesAnyBiomeKey(id,
                        BiomeKeys.SWAMP,
                        BiomeKeys.MANGROVE_SWAMP,
                        BiomeKeys.LUSH_CAVES,
                        BiomeKeys.FLOWER_FOREST
                )
        ).orElse(false);

        BiomeModifications.addProperties(
                greenBeanBiomeSelector,
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY
                )
        );
    }


    private static void addWildCucumberBushGeneration() {
        // --- Spawns in a wide variety of common "Warm/Temperate" biomes ---
        Predicate<BiomeModifications.BiomeContext> cucumberBiomeSelector = context ->
                context.hasTag(BiomeTags.IS_JUNGLE) || context.getKey().map(id ->
                        ModEntitySpawns.matchesAnyBiomeKey(id,
                                BiomeKeys.PLAINS,
                                BiomeKeys.SUNFLOWER_PLAINS,
                                BiomeKeys.SAVANNA,
                                BiomeKeys.SAVANNA_PLATEAU,
                                BiomeKeys.FOREST,
                                BiomeKeys.BIRCH_FOREST,
                                BiomeKeys.MEADOW,
                                BiomeKeys.WOODED_BADLANDS
                        )
                ).orElse(false);

        BiomeModifications.addProperties(
                cucumberBiomeSelector,
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY
                )
        );
    }
}