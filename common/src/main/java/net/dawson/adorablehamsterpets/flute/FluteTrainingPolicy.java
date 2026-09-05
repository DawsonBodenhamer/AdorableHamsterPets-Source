package net.dawson.adorablehamsterpets.flute;

import net.minecraft.util.math.MathHelper;

public final class FluteTrainingPolicy {

    public static final int UNTRAINED_DELAY_TICKS = 60;
    public static final int MASTERED_DELAY_TICKS = 20;
    private static final int DELAY_RANGE_TICKS = UNTRAINED_DELAY_TICKS - MASTERED_DELAY_TICKS;

    private FluteTrainingPolicy() {}

    public static int responseDelayTicks(int successfulMounts, int masteryTarget) {
        int safeTarget = Math.max(1, masteryTarget);
        int progress = MathHelper.clamp(successfulMounts, 0, safeTarget);
        int improvement = (int) Math.round((double) progress * DELAY_RANGE_TICKS / safeTarget);
        return MathHelper.clamp(UNTRAINED_DELAY_TICKS - improvement, MASTERED_DELAY_TICKS, UNTRAINED_DELAY_TICKS);
    }

    public static Tier tier(int successfulMounts, int masteryTarget) {
        if (successfulMounts <= 0) return Tier.UNTRAINED;

        int safeTarget = Math.max(1, masteryTarget);
        if (successfulMounts >= safeTarget) return Tier.ATTUNED;

        double progress = (double) successfulMounts / safeTarget;
        if (progress >= 0.5D) return Tier.RESPONSIVE;
        if (progress >= 0.25D) return Tier.LEARNING;
        return Tier.UNTRAINED;
    }

    public enum Tier {
        UNTRAINED,
        LEARNING,
        RESPONSIVE,
        ATTUNED
    }
}
