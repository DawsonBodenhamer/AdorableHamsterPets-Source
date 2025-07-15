package net.dawson.adorablehamsterpets.world.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.world.gen.ModEntitySpawns;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * A custom biome modifier that adds hamster spawns to biomes based on runtime configuration.
 * This modifier is triggered by a simple JSON file and performs its logic in Java, allowing
 * spawn weights and counts to be controlled by the user's config file.
 */
public class ConfigurableHamsterSpawnModifier implements BiomeModifier {
    /**
     * The codec for this biome modifier. A "unit" codec is used because this modifier
     * does not read any data from its JSON definition; all logic is self-contained.
     */
    public static final MapCodec<ConfigurableHamsterSpawnModifier> CODEC =
            MapCodec.unit(ConfigurableHamsterSpawnModifier::new);

    /**
     * Called by NeoForge for each biome that this modifier is applied to. This method
     * checks if the biome is in the valid spawn list and, if so, adds the hamster
     * spawn entry with settings read directly from the mod's configuration file.
     *
     * @param biome   The biome being modified.
     * @param phase   The current modification phase. This modifier only acts during the {@link Phase#ADD}.
     * @param builder The mutable biome information builder to apply changes to.
     */
    @Override
    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }

        String biomeName = biome.getKey().map(k -> k.getValue().toString()).orElse("unknown");
        AdorableHamsterPets.LOGGER.info("[AHP Spawn Modifier] Running modify for biome: {}", biomeName);

        if (biome.getKey().map(key -> ModEntitySpawns.isKeyInSpawnList(key.getValue())).orElse(false)) {
            var spawnBuilder = builder.getMobSpawnSettings();
            var spawnEntry = new SpawnSettings.SpawnEntry(
                    ModEntities.HAMSTER.get(),
                    Configs.AHP.spawnWeight.get(),
                    1,
                    Configs.AHP.maxGroupSize.get()
            );
            spawnBuilder.spawn(SpawnGroup.CREATURE, spawnEntry);
            AdorableHamsterPets.LOGGER.info("    -> SUCCESS: Added hamster spawn to biome '{}'", biomeName);
        } else {
            AdorableHamsterPets.LOGGER.info("    -> SKIPPED: Biome '{}' is not in the hamster spawn list.", biomeName);
        }
    }

    /**
     * Returns the registered codec that serializes this modifier.
     * This is required by the {@link BiomeModifier} interface.
     *
     * @return The unique {@link Codec} for this modifier type.
     */
    @Override
    public Codec<? extends BiomeModifier> codec() {
        // Return the Codec representation of our MapCodec
        return ModBiomeModifiers.CONFIGURABLE_HAMSTER_SPAWN.get();
    }
}