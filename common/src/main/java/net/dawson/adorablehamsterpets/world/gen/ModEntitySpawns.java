package net.dawson.adorablehamsterpets.world.gen;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.SpawnSettings;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the registration of entity spawns within specific biomes using the Architectury API.
 */
public class ModEntitySpawns {

    public static final Set<Block> VALID_SPAWN_BLOCKS = new HashSet<>();

    static {
        VALID_SPAWN_BLOCKS.add(Blocks.SAND);
        VALID_SPAWN_BLOCKS.add(Blocks.RED_SAND);
        VALID_SPAWN_BLOCKS.add(Blocks.TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.WHITE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.ORANGE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.MAGENTA_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIGHT_BLUE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.YELLOW_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIME_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.PINK_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.GRAY_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.LIGHT_GRAY_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.CYAN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.PURPLE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BLUE_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BROWN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.GREEN_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.RED_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.BLACK_TERRACOTTA);
        VALID_SPAWN_BLOCKS.add(Blocks.STONE);
        VALID_SPAWN_BLOCKS.add(Blocks.ANDESITE);
        VALID_SPAWN_BLOCKS.add(Blocks.DIORITE);
        VALID_SPAWN_BLOCKS.add(Blocks.GRANITE);
        VALID_SPAWN_BLOCKS.add(Blocks.GRAVEL);
        VALID_SPAWN_BLOCKS.add(Blocks.DIRT);
        VALID_SPAWN_BLOCKS.add(Blocks.COARSE_DIRT);
        VALID_SPAWN_BLOCKS.add(Blocks.PODZOL);
        VALID_SPAWN_BLOCKS.add(Blocks.SNOW_BLOCK);
        VALID_SPAWN_BLOCKS.add(Blocks.MYCELIUM);
        VALID_SPAWN_BLOCKS.add(Blocks.MUD);
        VALID_SPAWN_BLOCKS.add(Blocks.PACKED_MUD);
    }

    /**
     * Registers the biome modification for adding hamster spawns.
     * This should be called from the common initializer.
     */
    public static void initialize() {
        AdorableHamsterPets.LOGGER.info("[AHP Spawn Debug] Initializing biome modifications for hamster spawns.");
        BiomeModifications.addProperties(
                ModEntitySpawns::shouldSpawnInBiome,
                (context, props) -> {
                    context.getKey().ifPresent(key -> AdorableHamsterPets.LOGGER.info("[AHP Spawn Debug] Applying spawn entry to biome: {}", key.toString()));
                    props.getSpawnProperties().addSpawn(
                            SpawnGroup.CREATURE,
                            new SpawnSettings.SpawnEntry(
                                    ModEntities.HAMSTER.get(),
                                    Configs.AHP.spawnWeight.get(),
                                    1,
                                    Configs.AHP.maxGroupSize.get()
                            )
                    );
                }
        );
    }

    /**
     * Determines if a hamster should spawn in the given biome context.
     * This is used as the predicate for the BiomeModifications call.
     * @param ctx The biome context provided by Architectury.
     * @return True if hamsters should spawn, false otherwise.
     */
    public static boolean shouldSpawnInBiome(BiomeModifications.BiomeContext ctx) {
        boolean shouldSpawn = ctx.getKey().filter(ModEntitySpawns::isKeyInSpawnList).isPresent();
        ctx.getKey().ifPresent(key -> AdorableHamsterPets.LOGGER.info("[AHP Spawn Debug] Checking biome [{}]: Should spawn? -> {}", key.toString(), shouldSpawn));
        return shouldSpawn;
    }

    /**
     * Checks if a given biome identifier is in our master list of spawnable biomes.
     * @param id The Identifier of the biome to check.
     * @return True if the key corresponds to a valid spawn biome.
     */
    private static boolean isKeyInSpawnList(Identifier id) {
        // Pass the identifier to the key-matching helper
        return matchesAnyBiomeKey(id,
                // Snowy Biomes
                BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_TAIGA, BiomeKeys.SNOWY_SLOPES,
                BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS, BiomeKeys.GROVE,
                BiomeKeys.FROZEN_RIVER, BiomeKeys.SNOWY_BEACH, BiomeKeys.FROZEN_OCEAN,
                BiomeKeys.DEEP_FROZEN_OCEAN, BiomeKeys.ICE_SPIKES,
                // Other Biomes
                BiomeKeys.CHERRY_GROVE, BiomeKeys.LUSH_CAVES, BiomeKeys.DRIPSTONE_CAVES,
                BiomeKeys.DEEP_DARK, BiomeKeys.SWAMP, BiomeKeys.MANGROVE_SWAMP,
                BiomeKeys.DESERT, BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                BiomeKeys.MEADOW, BiomeKeys.OLD_GROWTH_BIRCH_FOREST, BiomeKeys.WINDSWEPT_HILLS,
                BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, BiomeKeys.WINDSWEPT_FOREST,
                BiomeKeys.WINDSWEPT_SAVANNA, BiomeKeys.STONY_PEAKS, BiomeKeys.JUNGLE,
                BiomeKeys.SPARSE_JUNGLE, BiomeKeys.BAMBOO_JUNGLE, BiomeKeys.STONY_SHORE, BiomeKeys.MUSHROOM_FIELDS,
                // Include keys for biomes that are also covered by tags, for completeness
                BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.DARK_FOREST,
                BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
                BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.BADLANDS,
                BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS, BiomeKeys.BEACH
        );
    }

    // --- Biome Helper Methods ---
    @SafeVarargs
    public static boolean matchesAnyBiomeKey(Identifier id, RegistryKey<Biome>... keysToMatch) {
        if (id == null) return false;
        for (RegistryKey<Biome> k : keysToMatch) {
            // Compare the identifier from the context to the identifier of the key
            if (k.getValue().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSnowyBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey()
                .map(key -> matchesAnyBiomeKey(key.getValue(),
                        BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_TAIGA, BiomeKeys.SNOWY_SLOPES,
                        BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS, BiomeKeys.GROVE,
                        BiomeKeys.FROZEN_RIVER, BiomeKeys.SNOWY_BEACH, BiomeKeys.FROZEN_OCEAN,
                        BiomeKeys.DEEP_FROZEN_OCEAN))
                .orElse(false);
    }

    public static boolean isIceSpikesBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.matchesKey(BiomeKeys.ICE_SPIKES);
    }

    public static boolean isCherryGroveBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.matchesKey(BiomeKeys.CHERRY_GROVE);
    }

    public static boolean isDesertBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.matchesKey(BiomeKeys.DESERT);
    }

    public static boolean isPlainsBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey()
                .map(key -> matchesAnyBiomeKey(key.getValue(),
                        BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.MEADOW))
                .orElse(false);
    }

    public static boolean isSwampBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey()
                .map(key -> matchesAnyBiomeKey(key.getValue(),
                        BiomeKeys.SWAMP, BiomeKeys.MANGROVE_SWAMP))
                .orElse(false);
    }

    public static boolean isCaveBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey()
                .map(key -> matchesAnyBiomeKey(key.getValue(),
                        BiomeKeys.LUSH_CAVES, BiomeKeys.DRIPSTONE_CAVES, BiomeKeys.DEEP_DARK))
                .orElse(false);
    }

    public static boolean isOldGrowthBirchForest(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.matchesKey(BiomeKeys.OLD_GROWTH_BIRCH_FOREST);
    }

    public static boolean isWindsweptOrStonyPeaks(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey()
                .map(key -> matchesAnyBiomeKey(key.getValue(),
                        BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.WINDSWEPT_GRAVELLY_HILLS,
                        BiomeKeys.WINDSWEPT_FOREST, BiomeKeys.WINDSWEPT_SAVANNA,
                        BiomeKeys.STONY_PEAKS))
                .orElse(false);
    }

    public static boolean isJungleBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.isIn(BiomeTags.IS_JUNGLE);
    }
}