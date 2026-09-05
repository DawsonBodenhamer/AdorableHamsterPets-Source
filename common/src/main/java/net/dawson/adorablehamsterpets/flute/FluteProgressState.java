package net.dawson.adorablehamsterpets.flute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FluteProgressState {

    private int successfulMounts;
    private final Set<UUID> feverPerformers = new LinkedHashSet<>();
    @Nullable private UUID qualifiedRescuerUuid;
    private boolean rewardAvailable;

    public synchronized int getSuccessfulMounts() {
        return this.successfulMounts;
    }

    public synchronized void recordSuccessfulMount() {
        if (this.successfulMounts < Integer.MAX_VALUE) this.successfulMounts++;
    }

    public synchronized Set<UUID> getFeverPerformers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.feverPerformers));
    }

    public synchronized void recordFeverPerformer(UUID performerUuid) {
        this.feverPerformers.add(Objects.requireNonNull(performerUuid, "performerUuid"));
    }

    public synchronized boolean hasFeverPerformer(@Nullable UUID performerUuid) {
        return this.feverPerformers.contains(performerUuid);
    }

    public synchronized void clearFeverPerformers() {
        this.feverPerformers.clear();
    }

    @Nullable
    public synchronized UUID getQualifiedRescuerUuid() {
        return this.qualifiedRescuerUuid;
    }

    /**
     * Qualifies one reward for a rescuer who actually performed during this fever episode.
     *
     * <p>A pending qualification is never replaced. This keeps a repeated cure or a duplicate
     * callback from transferring an earned reward to a different player.
     */
    public synchronized boolean qualifyReward(@Nullable UUID rescuerUuid) {
        if (rescuerUuid == null || !this.feverPerformers.contains(rescuerUuid)) return false;
        if (this.qualifiedRescuerUuid != null) return false;

        this.qualifiedRescuerUuid = rescuerUuid;
        this.rewardAvailable = true;
        return true;
    }

    public synchronized void clearRewardQualification() {
        this.qualifiedRescuerUuid = null;
        this.rewardAvailable = false;
    }

    public synchronized boolean hasAvailableRewardFor(@Nullable UUID playerUuid) {
        return this.rewardAvailable && playerUuid != null && playerUuid.equals(this.qualifiedRescuerUuid);
    }

    /**
     * Atomically consumes the one-use reward for its qualified rescuer.
     */
    public synchronized boolean consumeReward(@Nullable UUID playerUuid) {
        if (!this.rewardAvailable
                || playerUuid == null
                || !playerUuid.equals(this.qualifiedRescuerUuid)) {
            return false;
        }

        this.rewardAvailable = false;
        return true;
    }

    public synchronized TransferData createTransferData() {
        return new TransferData(
                this.successfulMounts,
                List.copyOf(this.feverPerformers),
                Optional.ofNullable(this.qualifiedRescuerUuid),
                this.rewardAvailable);
    }

    public synchronized void applyTransferData(TransferData data) {
        Objects.requireNonNull(data, "data");
        this.successfulMounts = data.successfulMounts();
        this.feverPerformers.clear();
        this.feverPerformers.addAll(data.feverPerformers());
        this.qualifiedRescuerUuid = data.qualifiedRescuerUuid().orElse(null);
        this.rewardAvailable = data.rewardAvailable() && this.qualifiedRescuerUuid != null;
    }

    public void writeNbt(NbtCompound nbt) {
        this.createTransferData().writeNbt(nbt);
    }

    public void readNbt(NbtCompound nbt) {
        this.applyTransferData(TransferData.fromNbt(nbt));
    }

    public record TransferData(
            int successfulMounts,
            List<UUID> feverPerformers,
            Optional<UUID> qualifiedRescuerUuid,
            boolean rewardAvailable) {

        private static final String SUCCESSFUL_MOUNTS_KEY = "AcornFluteSuccessfulMounts";
        private static final String FEVER_PERFORMERS_KEY = "AcornFluteFeverPerformers";
        private static final String QUALIFIED_RESCUER_KEY = "AcornFluteQualifiedRescuer";
        private static final String REWARD_AVAILABLE_KEY = "AcornFluteRewardAvailable";

        public static final Codec<TransferData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf(SUCCESSFUL_MOUNTS_KEY, 0)
                                .forGetter(TransferData::successfulMounts),
                        Uuids.CODEC.listOf().optionalFieldOf(FEVER_PERFORMERS_KEY, List.of())
                                .forGetter(TransferData::feverPerformers),
                        Uuids.CODEC.optionalFieldOf(QUALIFIED_RESCUER_KEY)
                                .forGetter(TransferData::qualifiedRescuerUuid),
                        Codec.BOOL.optionalFieldOf(REWARD_AVAILABLE_KEY, false)
                                .forGetter(TransferData::rewardAvailable)
                ).apply(instance, TransferData::new));

        public TransferData {
            successfulMounts = Math.max(0, successfulMounts);
            LinkedHashSet<UUID> uniquePerformers = new LinkedHashSet<>();
            if (feverPerformers != null) {
                for (UUID performer : feverPerformers) {
                    if (performer != null) uniquePerformers.add(performer);
                }
            }
            feverPerformers = List.copyOf(uniquePerformers);
            qualifiedRescuerUuid = qualifiedRescuerUuid == null ? Optional.empty() : qualifiedRescuerUuid;
            rewardAvailable = rewardAvailable && qualifiedRescuerUuid.isPresent();
        }

        public static TransferData empty() {
            return new TransferData(0, List.of(), Optional.empty(), false);
        }

        public static TransferData fromNbt(NbtCompound nbt) {
            LinkedHashSet<UUID> performers = new LinkedHashSet<>();
            NbtList performerList = nbt.getList(FEVER_PERFORMERS_KEY, NbtElement.STRING_TYPE);
            for (int index = 0; index < performerList.size(); index++) {
                if (!(performerList.get(index) instanceof NbtString)) continue;
                try {
                    performers.add(UUID.fromString(performerList.getString(index)));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed entries while retaining valid rescue credit
                }
            }
            Optional<UUID> qualifiedRescuer = nbt.containsUuid(QUALIFIED_RESCUER_KEY)
                    ? Optional.of(nbt.getUuid(QUALIFIED_RESCUER_KEY))
                    : Optional.empty();
            return new TransferData(
                    nbt.getInt(SUCCESSFUL_MOUNTS_KEY),
                    List.copyOf(performers),
                    qualifiedRescuer,
                    nbt.getBoolean(REWARD_AVAILABLE_KEY));
        }

        public void writeNbt(NbtCompound nbt) {
            nbt.remove(QUALIFIED_RESCUER_KEY);
            nbt.putInt(SUCCESSFUL_MOUNTS_KEY, this.successfulMounts);
            NbtList performers = new NbtList();
            for (UUID performer : this.feverPerformers) {
                performers.add(NbtString.of(performer.toString()));
            }
            nbt.put(FEVER_PERFORMERS_KEY, performers);
            this.qualifiedRescuerUuid.ifPresent(uuid -> nbt.putUuid(QUALIFIED_RESCUER_KEY, uuid));
            nbt.putBoolean(REWARD_AVAILABLE_KEY, this.rewardAvailable);
        }
    }
}
