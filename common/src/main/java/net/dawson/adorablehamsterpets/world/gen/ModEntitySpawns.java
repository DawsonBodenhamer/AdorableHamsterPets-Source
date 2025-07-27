package net.dawson.adorablehamsterpets.world.gen;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the registration of entity spawns within specific biomes using the Architectury API.
 */
public class ModEntitySpawns {

    public static final Set<Block> VALID_SPAWN_BLOCKS = new HashSet<>();

    // --- Caches for Parsed Config Values ---
    private static final Set<TagKey<Biome>> PARSED_TAGS = new HashSet<>();
    private static final Set<Identifier> PARSED_INCLUDES = new HashSet<>();
    private static final Set<Identifier> PARSED_EXCLUDES = new HashSet<>();

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
     * Parses the biome lists from the config file into Sets for efficient lookup.
     * This should be called once during mod initialization.
     */
    public static void parseConfig() {
        // Clear existing sets to allow for config reloading
        PARSED_TAGS.clear();
        PARSED_INCLUDES.clear();
        PARSED_EXCLUDES.clear();

        // Parse Tags
        for (String tagStr : Configs.AHP.spawnBiomeTags) {
            try {
                PARSED_TAGS.add(TagKey.of(RegistryKeys.BIOME, Identifier.of(tagStr)));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.info("[BiomeConfig] Invalid biome tag identifier in config: '{}'", tagStr);
            }
        }

        // Parse Includes
        for (String biomeIdStr : Configs.AHP.includeBiomes) {
            try {
                PARSED_INCLUDES.add(Identifier.of(biomeIdStr));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeConfig] Invalid biome identifier in include list: '{}'", biomeIdStr);
            }
        }

        // Parse Excludes
        for (String biomeIdStr : Configs.AHP.excludeBiomes) {
            try {
                PARSED_EXCLUDES.add(Identifier.of(biomeIdStr));
            } catch (Exception e) {
                AdorableHamsterPets.LOGGER.warn("[BiomeConfig] Invalid biome identifier in exclude list: '{}'", biomeIdStr);
            }
        }

        AdorableHamsterPets.LOGGER.info("[BiomeConfig] Parsed {} tags, {} included biomes, and {} excluded biomes.",
                PARSED_TAGS.size(), PARSED_INCLUDES.size(), PARSED_EXCLUDES.size());
    }

    /**
     * The universal decider for Fabric, driven by the parsed config.
     *
     * @param ctx The biome context provided by Architectury.
     * @return True if hamsters should spawn in this biome, false otherwise.
     */
    public static boolean shouldSpawnInBiome(BiomeModifications.BiomeContext ctx) {
        // Get the Identifier directly from the Optional.
        Identifier biomeId = ctx.getKey().orElse(null);
        if (biomeId == null) return false;

        // 1. Exclusion check (highest priority)
        if (PARSED_EXCLUDES.contains(biomeId)) {
            return false;
        }
        // 2. Inclusion check (specific biomes)
        if (PARSED_INCLUDES.contains(biomeId)) {
            return true;
        }
        // 3. Tag check
        for (TagKey<Biome> tag : PARSED_TAGS) {
            if (ctx.hasTag(tag)) {
                return true;
            }
        }
        // 4. Default to false if no rules match
        return false;
    }

    /**
     * A NeoForge-specific decider that works directly with a RegistryEntry.
     *
     * @param biomeEntry The biome entry from the NeoForge modifier.
     * @return True if hamsters should spawn, false otherwise.
     */
    public static boolean shouldSpawnInBiomeNeoForge(RegistryEntry<Biome> biomeEntry) {
        Identifier biomeId = biomeEntry.getKey().map(RegistryKey::getValue).orElse(null);
        if (biomeId == null) return false;

        // 1. Exclusion check
        if (PARSED_EXCLUDES.contains(biomeId)) {
            return false;
        }
        // 2. Inclusion check
        if (PARSED_INCLUDES.contains(biomeId)) {
            return true;
        }
        // 3. Tag check
        for (TagKey<Biome> tag : PARSED_TAGS) {
            if (biomeEntry.isIn(tag)) {
                return true;
            }
        }
        // 4. Default to false
        return false;
    }
}