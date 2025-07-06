package net.dawson.adorablehamsterpets.world.fabric;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.world.gen.feature.ModPlacedFeatures;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.function.Predicate;

public class ModWorldGenerationImpl {
    public static void registerBiomeModifications() {
        // --- Sunflower Replacement Logic ---
        Predicate<BiomeModifications.BiomeContext> sunflowerSelector = context -> context.getKey()
                .map(key -> key.equals(BiomeKeys.SUNFLOWER_PLAINS.getValue()))
                .orElse(false);

        BiomeModifications.removeProperties(sunflowerSelector,
                (context, props) -> props.getGenerationProperties().removeFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        VegetationPlacedFeatures.PATCH_SUNFLOWER
                )
        );
        BiomeModifications.addProperties(sunflowerSelector,
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.CUSTOM_SUNFLOWER_PLACED_KEY
                )
        );

        // --- Wild Green Bean Bush Generation ---
        Predicate<BiomeModifications.BiomeContext> greenBeanSelector = context -> context.getKey()
                .map(key -> key.equals(BiomeKeys.SWAMP.getValue()) ||
                        key.equals(BiomeKeys.MANGROVE_SWAMP.getValue()) ||
                        key.equals(BiomeKeys.LUSH_CAVES.getValue()) ||
                        key.equals(BiomeKeys.FLOWER_FOREST.getValue()))
                .orElse(false);

        BiomeModifications.addProperties(greenBeanSelector,
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.WILD_GREEN_BEAN_BUSH_PLACED_KEY
                )
        );

        // --- Wild Cucumber Bush Generation ---
        Predicate<BiomeModifications.BiomeContext> cucumberSelector = context ->
                context.hasTag(BiomeTags.IS_JUNGLE) || context.getKey().map(key ->
                                key.equals(BiomeKeys.PLAINS.getValue()) ||
                                        key.equals(BiomeKeys.SUNFLOWER_PLAINS.getValue()) ||
                                        key.equals(BiomeKeys.SAVANNA.getValue()) ||
                                        key.equals(BiomeKeys.SAVANNA_PLATEAU.getValue()) ||
                                        key.equals(BiomeKeys.FOREST.getValue()) ||
                                        key.equals(BiomeKeys.BIRCH_FOREST.getValue()) ||
                                        key.equals(BiomeKeys.MEADOW.getValue()) ||
                                        key.equals(BiomeKeys.WOODED_BADLANDS.getValue()))
                        .orElse(false);

        BiomeModifications.addProperties(cucumberSelector,
                (context, props) -> props.getGenerationProperties().addFeature(
                        GenerationStep.Feature.VEGETAL_DECORATION,
                        ModPlacedFeatures.WILD_CUCUMBER_BUSH_PLACED_KEY
                )
        );
    }
}