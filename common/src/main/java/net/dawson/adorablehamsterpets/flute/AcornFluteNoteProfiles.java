package net.dawson.adorablehamsterpets.flute;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads immutable, server-safe Acorn Flute note profiles from the mod classpath.
 */
public final class AcornFluteNoteProfiles {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Constants and Static State
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final double NOTE_PARTICLE_NOISE_FLOOR = 0.15D; // Only spawn particles for intensities above 15%

    private static final String RESOURCE_PATH = "/data/adorablehamsterpets/sound_to_particle_conversion_profiles.json";
    private static final int SCHEMA_VERSION = 2;
    private static final int SAMPLE_RATE_HZ = 48_000;
    private static final int SAMPLES_PER_TICK = 2_400;
    private static final int QUANTIZED_MAX = 255;
    private static final List<String> RIFF_KEYS = List.of(
            "acorn_flute_riff1",
            "acorn_flute_riff2",
            "acorn_flute_riff3",
            "acorn_flute_riff4",
            "acorn_flute_riff5",
            "acorn_flute_riff6",
            "acorn_flute_riff7",
            "acorn_flute_riff8");
    private static final String CHIFF_KEY = "acorn_flute_chiff";
    private static final Map<String, AcornFluteNoteProfile> PROFILES = loadProfiles();

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Public Lookup API
     * ────────────────────────────────────────────────────────────────────────────*/

    @Nullable
    public static AcornFluteNoteProfile forRiffIndex(int riffIndex) {
        if (riffIndex < 0 || riffIndex >= RIFF_KEYS.size()) {
            return null;
        }
        return PROFILES.get(RIFF_KEYS.get(riffIndex));
    }

    @Nullable
    public static AcornFluteNoteProfile chiff() {
        return PROFILES.get(CHIFF_KEY);
    }

    @Nullable
    public static AcornFluteNoteProfile find(String key) {
        return PROFILES.get(key);
    }

    public static double applyParticleNoiseFloor(double normalizedIntensity) {
        if (normalizedIntensity <= NOTE_PARTICLE_NOISE_FLOOR) {
            return 0.0D;
        }
        return (normalizedIntensity - NOTE_PARTICLE_NOISE_FLOOR) / (1.0D - NOTE_PARTICLE_NOISE_FLOOR);
    }

    private static Map<String, AcornFluteNoteProfile> loadProfiles() {
        return loadResource(RESOURCE_PATH);
    }

    static Map<String, AcornFluteNoteProfile> loadResource(String resourcePath) {
        try (InputStream stream = AcornFluteNoteProfiles.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                AdorableHamsterPets.LOGGER.error(
                        "Sound-to-particle conversion profile resource '{}' is missing; note emission is disabled for all clips.",
                        resourcePath);
                return Map.of();
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parseProfiles(reader);
            }
        } catch (IOException | RuntimeException exception) {
            AdorableHamsterPets.LOGGER.error(
                    "Sound-to-particle conversion profile resource '{}' is malformed; note emission is disabled for all clips.",
                    resourcePath,
                    exception);
            return Map.of();
        }
    }

    static Map<String, AcornFluteNoteProfile> parseProfiles(Reader reader) {
        try {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IllegalArgumentException("Root value must be an object");
            }
            JsonObject root = rootElement.getAsJsonObject();
            validateEnvelopeSpec(root);
            JsonObject profileObjects = requireObject(root, "profiles");
            Map<String, AcornFluteNoteProfile> profiles = new LinkedHashMap<>();
            for (String key : allProfileKeys()) {
                JsonElement profileElement = profileObjects.get(key);
                try {
                    profiles.put(key, parseProfile(key, profileElement));
                } catch (RuntimeException exception) {
                    AdorableHamsterPets.LOGGER.error(
                            "Acorn Flute note profile '{}' is invalid; note emission is disabled for that clip: {}",
                            key,
                            exception.getMessage());
                }
            }
            for (Map.Entry<String, JsonElement> entry : profileObjects.entrySet()) {
                if (RIFF_KEYS.contains(entry.getKey()) || CHIFF_KEY.equals(entry.getKey())) {
                    continue;
                }
                try {
                    profiles.put(entry.getKey(), parseProfile(entry.getKey(), entry.getValue()));
                } catch (RuntimeException exception) {
                    AdorableHamsterPets.LOGGER.error(
                            "Sound-to-particle conversion profile '{}' is invalid; that profile is disabled: {}",
                            entry.getKey(),
                            exception.getMessage());
                }
            }
            return Collections.unmodifiableMap(profiles);
        } catch (RuntimeException exception) {
            AdorableHamsterPets.LOGGER.error(
                    "Acorn Flute note profile data is malformed; note emission is disabled for all clips.",
                    exception);
            return Map.of();
        }
    }

    private static void validateEnvelopeSpec(JsonObject root) {
        requireInt(root, "schema_version", SCHEMA_VERSION);
        requireInt(root, "sample_rate_hz", SAMPLE_RATE_HZ);
        requireInt(root, "samples_per_tick", SAMPLES_PER_TICK);
        requireInt(root, "quantized_max", QUANTIZED_MAX);
    }

    private static AcornFluteNoteProfile parseProfile(String key, @Nullable JsonElement profileElement) {
        if (profileElement == null || !profileElement.isJsonObject()) {
            throw new IllegalArgumentException("Profile object is missing");
        }
        JsonObject profile = profileElement.getAsJsonObject();
        String sourceSha256 = requireString(profile, "source_sha256");
        if (!sourceSha256.matches("[0-9A-Fa-f]{64}")) {
            throw new IllegalArgumentException("Source SHA-256 must contain 64 hexadecimal characters");
        }
        int durationTicks = requirePositiveInt(profile, "duration_ticks");
        Integer expectedDurationTicks = expectedDurationTicks(key);
        if (expectedDurationTicks != null && durationTicks != expectedDurationTicks) {
            throw new IllegalArgumentException(
                    "Duration ticks " + durationTicks + " do not match expected " + expectedDurationTicks);
        }
        double peakRms = profile.has("peak_rms")
                ? requireFiniteDouble(profile, "peak_rms")
                : requireFiniteDouble(profile, "smoothed_peak_dbfs");
        JsonArray intensityArray = requireArray(profile, "intensities");
        if (intensityArray.size() != durationTicks) {
            throw new IllegalArgumentException("Intensity count does not match duration ticks");
        }
        int[] intensities = new int[durationTicks];
        boolean reachesMaximum = false;
        for (int index = 0; index < intensityArray.size(); index++) {
            JsonElement value = intensityArray.get(index);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("Intensity values must be integers");
            }
            double numericValue = value.getAsDouble();
            int intensity = value.getAsInt();
            if (!Double.isFinite(numericValue) || numericValue != intensity || intensity < 0 || intensity > QUANTIZED_MAX) {
                throw new IllegalArgumentException("Intensity values must be integers in 0..255");
            }
            intensities[index] = intensity;
            reachesMaximum |= intensity == QUANTIZED_MAX;
        }
        if (!reachesMaximum) {
            throw new IllegalArgumentException("Profile never reaches the quantized maximum");
        }
        return new AcornFluteNoteProfile(
                key,
                sourceSha256,
                durationTicks,
                peakRms,
                intensities);
    }

    @Nullable
    private static Integer expectedDurationTicks(String key) {
        double durationSeconds;
        if (CHIFF_KEY.equals(key)) {
            durationSeconds = AcornFluteSoundSpec.CHIFF_DURATION_SECONDS;
        } else {
            int riffIndex = RIFF_KEYS.indexOf(key);
            if (riffIndex < 0) {
                return null;
            }
            durationSeconds = AcornFluteSoundSpec.RIFF_DURATIONS_SECONDS.get(riffIndex);
        }
        return Math.max(1, (int) Math.ceil(durationSeconds * 20.0D));
    }

    private static List<String> allProfileKeys() {
        List<String> keys = new ArrayList<>(RIFF_KEYS);
        keys.add(CHIFF_KEY);
        return keys;
    }

    private static JsonObject requireObject(JsonObject object, String memberName) {
        JsonElement element = requireMember(object, memberName);
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject object, String memberName) {
        JsonElement element = requireMember(object, memberName);
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be an array");
        }
        return element.getAsJsonArray();
    }

    private static String requireString(JsonObject object, String memberName) {
        JsonElement element = requireMember(object, memberName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be a string");
        }
        return element.getAsString();
    }

    private static int requirePositiveInt(JsonObject object, String memberName) {
        int value = requireInt(object, memberName);
        if (value <= 0) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be positive");
        }
        return value;
    }

    private static int requireInt(JsonObject object, String memberName) {
        JsonElement element = requireMember(object, memberName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be an integer");
        }
        double numericValue = element.getAsDouble();
        int value = element.getAsInt();
        if (!Double.isFinite(numericValue) || numericValue != value) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be an integer");
        }
        return value;
    }

    private static void requireInt(JsonObject object, String memberName, int expectedValue) {
        if (requireInt(object, memberName) != expectedValue) {
            throw new IllegalArgumentException(
                    "Member '" + memberName + "' does not match the locked extraction constants");
        }
    }

    private static double requireFiniteDouble(JsonObject object, String memberName) {
        JsonElement element = requireMember(object, memberName);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be numeric");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Member '" + memberName + "' must be finite");
        }
        return value;
    }

    private static JsonElement requireMember(JsonObject object, String memberName) {
        JsonElement element = object.get(memberName);
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing member '" + memberName + "'");
        }
        return element;
    }

    private AcornFluteNoteProfiles() {}
}
