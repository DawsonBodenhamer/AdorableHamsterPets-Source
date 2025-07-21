package net.dawson.adorablehamsterpets.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Supplier; // Import Supplier

public record HamsterShoulderData(
        int variantId,
        float health,
        NbtCompound inventoryNbt,
        boolean leftCheekFull,
        boolean rightCheekFull,
        int breedingAge,
        long throwCooldownEndTick,
        long greenBeanBuffEndTick,
        NbtList activeEffectsNbt, // Changed from NbtCompound to NbtList
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

    public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            (dynamic) -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtCompound compound) {
                    return DataResult.success(compound);
                }
                return DataResult.error(() -> "Not a compound NBT: " + element);
            },
            (nbt) -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    public static final Codec<NbtList> NBT_LIST_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            (dynamic) -> {
                NbtElement element = dynamic.convert(NbtOps.INSTANCE).getValue();
                if (element instanceof NbtList list) {
                    return DataResult.success(list);
                }
                return DataResult.error(() -> "Not a list NBT: " + element);
            },
            (nbt) -> new Dynamic<>(NbtOps.INSTANCE, nbt)
    );

    public static final Codec<HamsterShoulderData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("variantId").forGetter(HamsterShoulderData::variantId),
                    Codec.FLOAT.fieldOf("health").forGetter(HamsterShoulderData::health),
                    NBT_COMPOUND_CODEC.fieldOf("inventoryNbt").forGetter(HamsterShoulderData::inventoryNbt),
                    Codec.BOOL.fieldOf("leftCheekFull").forGetter(HamsterShoulderData::leftCheekFull),
                    Codec.BOOL.fieldOf("rightCheekFull").forGetter(HamsterShoulderData::rightCheekFull),
                    Codec.INT.fieldOf("breedingAge").forGetter(HamsterShoulderData::breedingAge),
                    Codec.LONG.fieldOf("throwCooldownEndTick").forGetter(HamsterShoulderData::throwCooldownEndTick),
                    Codec.LONG.fieldOf("greenBeanBuffEndTick").forGetter(HamsterShoulderData::steamedBeansCooldownEndTick),
                    NBT_LIST_CODEC.fieldOf("activeEffectsNbt").forGetter(HamsterShoulderData::activeEffectsNbt), // Now expects a list
                    Codec.INT.fieldOf("autoEatCooldownTicks").forGetter(HamsterShoulderData::autoEatCooldownTicks),
                    Codec.STRING.optionalFieldOf("customName").forGetter(HamsterShoulderData::customName),
                    Codec.INT.fieldOf("pinkPetalType").orElse(0).forGetter(HamsterShoulderData::pinkPetalType),
                    Codec.BOOL.fieldOf("cheekPouchUnlocked").orElse(false).forGetter(HamsterShoulderData::cheekPouchUnlocked),
                    Codec.INT.fieldOf("animationPersonalityId").orElse(1).forGetter(HamsterShoulderData::animationPersonalityId),
                    SeekingBehaviorData.CODEC.fieldOf("seekingBehaviorData").orElse(SeekingBehaviorData.empty()).forGetter(HamsterShoulderData::seekingBehaviorData)
            ).apply(instance, HamsterShoulderData::new)
    );

    /**
     * Deserializes an NbtCompound into a HamsterShoulderData record.
     * @param nbt The NbtCompound to deserialize.
     * @return An Optional containing the HamsterShoulderData, or empty if deserialization fails.
     */
    public static Optional<HamsterShoulderData> fromNbt(NbtCompound nbt) {
        return CODEC.parse(NbtOps.INSTANCE, nbt).result();
    }

    /**
     * Serializes this record into an NbtCompound.
     * @return The NbtCompound representation of this data.
     */
    public NbtCompound toNbt() {
        // Use the 1.20.1 getOrThrow signature which takes a Consumer<String>
        return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(false, error -> {
                    throw new IllegalStateException("Could not encode HamsterShoulderData: " + error);
                });
    }

    @Override
    public String toString() {
        return "HamsterShoulderData[variantId=" + variantId +
                ", health=" + health +
                ", inventoryNbt=" + inventoryNbt.toString().substring(0, Math.min(inventoryNbt.toString().length(), 50)) + "..." +
                ", leftFull=" + leftCheekFull +
                ", rightFull=" + rightCheekFull +
                ", age=" + breedingAge +
                ", throwCooldownEnd=" + throwCooldownEndTick +
                ", beansCooldownEnd=" + greenBeanBuffEndTick +
                ", effectsNbtCount=" + activeEffectsNbt.size() +
                ", autoEatCooldown=" + autoEatCooldownTicks +
                ", customName=" + customName.orElse("None") +
                ", pinkPetalType=" + pinkPetalType +
                ", cheekPouchUnlocked=" + cheekPouchUnlocked +
                ", animationPersonalityId=" + animationPersonalityId +
                ", seekingBehaviorData=" + seekingBehaviorData.toString() +
                "]";
    }

    public static HamsterShoulderData empty() {
        return new HamsterShoulderData(0, 8.0f, new NbtCompound(), false, false, 0, 0L, 0L, new NbtList(), 0, Optional.empty(), 0, false, 1,
                SeekingBehaviorData.empty()
        );
    }
}