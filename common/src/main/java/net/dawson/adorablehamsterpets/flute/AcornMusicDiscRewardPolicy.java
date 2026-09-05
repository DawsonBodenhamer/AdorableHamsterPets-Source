package net.dawson.adorablehamsterpets.flute;

import java.util.UUID;

public final class AcornMusicDiscRewardPolicy {

    private AcornMusicDiscRewardPolicy() {}

    public static Outcome evaluate(
            boolean creeperDied,
            boolean chargedCreeper,
            boolean calmedByNormalRiff,
            UUID throwerUuid,
            UUID qualifiedRescuerUuid,
            boolean rewardAvailable) {
        if (!creeperDied || !chargedCreeper) return Outcome.NONE;
        if (calmedByNormalRiff
                && rewardAvailable
                && throwerUuid != null
                && throwerUuid.equals(qualifiedRescuerUuid)) {
            return Outcome.ACORN_DISC;
        }
        return Outcome.CHEESE_DISC;
    }

    public enum Outcome {
        NONE,
        CHEESE_DISC,
        ACORN_DISC
    }
}
