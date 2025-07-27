package net.dawson.adorablehamsterpets.world.fabric;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.dawson.adorablehamsterpets.world.gen.feature.ModPlacedFeatures;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.function.Predicate;

public class ModWorldGenerationImpl {
    public static void registerBiomeModifications() {
        // A universal predicate that allows our decider logic to run for every biome.
        Predicate<BiomeModifications.BiomeContext> universalSelector = context -> true;

        // --- Feature Addition ---
        // This block handles adding our custom features to the world.
        BiomeModifications.addProperties(universalSelector, (context, props) -> {
            // Check and add each of our custom features by calling the central decider method.
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY, context)) {
                props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY);
            }
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY, context)) {
                props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY);
            }
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY, context)) {
                props.getGenerationProperties().addFeature(GenerationStep.Feature.VEGETAL_DECORATION, ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY);
            }
        });

        // --- Feature Removal ---
        // This block handles removing the vanilla sunflower where our custom one is added.
        BiomeModifications.removeProperties(universalSelector, (context, props) -> {
            // Check if the vanilla sunflower should be removed from this biome.
            // The common decider method checks both the user's config AND if the vanilla feature is actually present in the biome.
            if (ModWorldGeneration.shouldFeatureSpawnInBiome(VegetationPlacedFeatures.PATCH_SUNFLOWER, context)) {
                props.getGenerationProperties().removeFeature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUNFLOWER);
            }
        });
    }
}