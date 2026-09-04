package net.dawson.adorablehamsterpets.particles;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.custom.WoodVariant;
import net.dawson.adorablehamsterpets.client.particle.AcornFluteNoteParticleEffect;
import net.dawson.adorablehamsterpets.client.particle.PixieDustParticleTheme;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKeys;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Holds all particle type registrations.
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.PARTICLE_TYPE);

    // Map to link each particle type back to its wood variant
    public static final Map<WoodVariant, RegistrySupplier<SimpleParticleType>> BEDDING_PARTICLES = new EnumMap<>(WoodVariant.class);

    // Map to link each Pixie Dust theme to a particle type
    public static final Map<PixieDustParticleTheme, RegistrySupplier<SimpleParticleType>> PIXIE_DUST = new EnumMap<>(PixieDustParticleTheme.class);

    // Particle color palette matches flute palette
    public static final RegistrySupplier<ParticleType<AcornFluteNoteParticleEffect>> ACORN_FLUTE_NOTE =
            PARTICLE_TYPES.register("acorn_flute_note", () -> new ParticleType<AcornFluteNoteParticleEffect>(false) {
                @Override
                public MapCodec<AcornFluteNoteParticleEffect> getCodec() {
                    return AcornFluteNoteParticleEffect.CODEC;
                }

                @Override
                public PacketCodec<? super RegistryByteBuf, AcornFluteNoteParticleEffect> getPacketCodec() {
                    return AcornFluteNoteParticleEffect.PACKET_CODEC;
                }
            });

    static {
        for (WoodVariant variant : WoodVariant.values()) {
            String id = "hamster_bedding_" + variant.asString();
            BEDDING_PARTICLES.put(variant, PARTICLE_TYPES.register(id, () -> new SimpleParticleType(false) {}));
        }

        for (PixieDustParticleTheme theme : PixieDustParticleTheme.values()) {
            String id = "pixie_dust_" + theme.name().toLowerCase(Locale.ROOT);
            PIXIE_DUST.put(theme, PARTICLE_TYPES.register(id, () -> new SimpleParticleType(false) {}));
        }
    }

    public static void register() {
        PARTICLE_TYPES.register();
    }

    /**
     * Gets the appropriate particle type for a given wood variant.
     * @param variant The wood variant.
     * @return The corresponding SimpleParticleType.
     */
    public static SimpleParticleType getForVariant(WoodVariant variant) {
        return BEDDING_PARTICLES.getOrDefault(variant, BEDDING_PARTICLES.get(WoodVariant.OAK)).get();
    }
}
