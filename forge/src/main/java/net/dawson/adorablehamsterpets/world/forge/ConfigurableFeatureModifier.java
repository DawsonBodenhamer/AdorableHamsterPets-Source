package net.dawson.adorablehamsterpets.world.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.world.ModWorldGeneration;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.Optional;

public record ConfigurableFeatureModifier(
        RegistryEntryList<Biome> biomes,
        Optional<RegistryEntryList<PlacedFeature>> featuresToAdd,
        Optional<RegistryEntryList<PlacedFeature>> featuresToRemove
) implements BiomeModifier {

    public static final MapCodec<ConfigurableFeatureModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    RegistryCodecs.entryList(RegistryKeys.BIOME).fieldOf("biomes").forGetter(ConfigurableFeatureModifier::biomes),
                    RegistryCodecs.entryList(RegistryKeys.PLACED_FEATURE).optionalFieldOf("features_to_add").forGetter(ConfigurableFeatureModifier::featuresToAdd),
                    RegistryCodecs.entryList(RegistryKeys.PLACED_FEATURE).optionalFieldOf("features_to_remove").forGetter(ConfigurableFeatureModifier::featuresToRemove)
            ).apply(instance, ConfigurableFeatureModifier::new)
    );

    @Override
    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        String biomeName = biome.getKey().map(k -> k.getValue().toString()).orElse("unknown");
        AdorableHamsterPets.LOGGER.debug("[AHP Feature Modifier] Running modify for biome: {} in phase: {}", biomeName, phase);

        // --- ADD Phase ---
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            this.featuresToAdd.ifPresent(additions -> {
                for (RegistryEntry<PlacedFeature> feature : additions) {
                    String featureName = feature.getKey().map(k -> k.getValue().toString()).orElse("unknown");
                    AdorableHamsterPets.LOGGER.debug("[AHP Feature Modifier] Considering ADDING feature '{}' to biome '{}'", featureName, biomeName);
                    if (shouldFeatureBeInBiome(feature, biome)) {
                        builder.getGenerationSettings().feature(GenerationStep.Feature.VEGETAL_DECORATION, feature);
                        AdorableHamsterPets.LOGGER.debug("    -> SUCCESS: Added feature '{}' to biome '{}'", featureName, biomeName);
                    } else {
                        AdorableHamsterPets.LOGGER.debug("    -> SKIPPED: Feature '{}' is not valid for biome '{}'", featureName, biomeName);
                    }
                }
            });
        }
        // --- REMOVE Phase ---
        if (phase == Phase.REMOVE && this.biomes.contains(biome)) {
            this.featuresToRemove.ifPresent(removals -> {
                for (RegistryEntry<PlacedFeature> feature : removals) {
                    String featureName = feature.getKey().map(k -> k.getValue().toString()).orElse("unknown");
                    AdorableHamsterPets.LOGGER.debug("[AHP Feature Modifier] Considering REMOVING feature '{}' from biome '{}'", featureName, biomeName);
                    if (shouldFeatureBeInBiome(feature, biome)) {
                        boolean removed = builder.getGenerationSettings().getFeatures(GenerationStep.Feature.VEGETAL_DECORATION).remove(feature);
                        AdorableHamsterPets.LOGGER.debug("    -> SUCCESS: Removed feature '{}' from biome '{}'. Was present: {}", featureName, biomeName, removed);
                    } else {
                        AdorableHamsterPets.LOGGER.debug("    -> SKIPPED: Feature '{}' is not valid for biome '{}'", featureName, biomeName);
                    }
                }
            });
        }
    }

    private boolean shouldFeatureBeInBiome(RegistryEntry<PlacedFeature> feature, RegistryEntry<Biome> biome) {
        // This is where we check against our centralized logic
        String featurePath = feature.getKey().map(key -> key.getValue().getPath()).orElse("");
        return switch (featurePath) {
            case "custom_sunflower_placed", "patch_sunflower" -> ModWorldGeneration.isSunflowerPlains(biome);
            case "wild_green_bean_bush_placed" -> ModWorldGeneration.isGreenBeanBiome(biome);
            case "wild_cucumber_bush_placed" -> ModWorldGeneration.isCucumberBiome(biome);
            default -> true; // Default to true if no specific logic, allows for future simple additions
        };
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        // Return the Codec representation of our MapCodec
        return ModBiomeModifiers.CONFIGURABLE_FEATURE_MODIFIER.get();
    }
}