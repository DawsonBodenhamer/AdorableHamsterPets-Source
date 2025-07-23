package net.dawson.adorablehamsterpets.component; // New package

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

// Data-holder record
public record HamsterShoulderData(
        int variantId,
        float health,
        NbtCompound inventoryNbt,
        boolean leftCheekFull,
        boolean rightCheekFull,
        int breedingAge,
        long throwCooldownEndTick,
        long greenBeanBuffEndTick,
        long greenBeanBuffDuration,
        NbtCompound activeEffectsNbt,
        int autoEatCooldownTicks,
        Optional<String> customName,
        int pinkPetalType,
        boolean cheekPouchUnlocked,
        int animationPersonalityId,
        SeekingBehaviorData seekingBehaviorData
) {

    public record SeekingBehaviorData(
            boolean isPrimedToSeekDiamonds,
            long foundOreCooldownEndTick,
            Optional<BlockPos> currentOreTarget,
            boolean isSulking
    ) {
        public static final Codec<SeekingBehaviorData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("isPrimedToSeekDiamonds").orElse(false).forGetter(SeekingBehaviorData::isPrimedToSeekDiamonds),
                        Codec.LONG.fieldOf("foundOreCooldownEndTick").orElse(0L).forGetter(SeekingBehaviorData::foundOreCooldownEndTick),
                        BlockPos.CODEC.optionalFieldOf("currentOreTarget").forGetter(SeekingBehaviorData::currentOreTarget),
                        Codec.BOOL.fieldOf("isSulking").orElse(false).forGetter(SeekingBehaviorData::isSulking)
                ).apply(instance, SeekingBehaviorData::new)
        );
        public static SeekingBehaviorData empty() {
            return new SeekingBehaviorData(false, 0L, Optional.empty(), false);
        }
    }

    public static final Codec<HamsterShoulderData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("variantId").forGetter(HamsterShoulderData::variantId),
                    Codec.FLOAT.fieldOf("health").forGetter(HamsterShoulderData::health),
                    NbtCompound.CODEC.fieldOf("inventoryNbt").forGetter(HamsterShoulderData::inventoryNbt),
                    Codec.BOOL.fieldOf("leftCheekFull").forGetter(HamsterShoulderData::leftCheekFull),
                    Codec.BOOL.fieldOf("rightCheekFull").forGetter(HamsterShoulderData::rightCheekFull),
                    Codec.INT.fieldOf("breedingAge").forGetter(HamsterShoulderData::breedingAge),
                    Codec.LONG.fieldOf("throwCooldownEndTick").forGetter(HamsterShoulderData::throwCooldownEndTick),
                    Codec.LONG.fieldOf("greenBeanBuffEndTick").forGetter(HamsterShoulderData::greenBeanBuffEndTick),
                    Codec.LONG.fieldOf("greenBeanBuffDuration").orElse(0L).forGetter(HamsterShoulderData::greenBeanBuffDuration),
                    NbtCompound.CODEC.fieldOf("activeEffectsNbt").forGetter(HamsterShoulderData::activeEffectsNbt),
                    Codec.INT.fieldOf("autoEatCooldownTicks").forGetter(HamsterShoulderData::autoEatCooldownTicks),
                    Codec.STRING.optionalFieldOf("customName").forGetter(HamsterShoulderData::customName),
                    Codec.INT.fieldOf("pinkPetalType").orElse(0).forGetter(HamsterShoulderData::pinkPetalType),
                    Codec.BOOL.fieldOf("cheekPouchUnlocked").orElse(false).forGetter(HamsterShoulderData::cheekPouchUnlocked),
                    Codec.INT.fieldOf("animationPersonalityId").orElse(1).forGetter(HamsterShoulderData::animationPersonalityId),
                    SeekingBehaviorData.CODEC.fieldOf("seekingBehaviorData").orElse(SeekingBehaviorData.empty()).forGetter(HamsterShoulderData::seekingBehaviorData)
            ).apply(instance, HamsterShoulderData::new)
    );

    /**
     * Serializes this record into an NbtCompound.
     * @return The NbtCompound representation of this data.
     */
    public NbtCompound toNbt() {
        return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(error -> new IllegalStateException("Could not encode HamsterShoulderData: " + error));
    }

    /**
     * Deserializes an NbtCompound into a HamsterShoulderData record.
     * @param nbt The NbtCompound to read from.
     * @return An Optional containing the deserialized data, or empty if deserialization fails.
     */
    public static Optional<HamsterShoulderData> fromNbt(NbtCompound nbt) {
        return CODEC.parse(NbtOps.INSTANCE, nbt)
                .resultOrPartial(AdorableHamsterPets.LOGGER::error);
    }
}