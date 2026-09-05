package net.dawson.adorablehamsterpets.flute;

import net.minecraft.util.math.MathHelper;

/**
 * Pure admission, mode-selection, and binding rules for an Acorn Flute performance.
 */
public final class FlutePerformancePolicy {

    private FlutePerformancePolicy() {}

    public static boolean canStart(
            boolean alreadyActive,
            boolean validInitiatingStack,
            boolean activeNormalRiff,
            boolean antiSpamCooldownActive) {
        return validInitiatingStack
                && !antiSpamCooldownActive
                && (!alreadyActive || activeNormalRiff);
    }

    public static RepeatAction repeatAction(
            long elapsedTicks, long totalDurationTicks, int layeringThresholdPercent) {
        if (totalDurationTicks <= 0L) {
            return RepeatAction.LAYER;
        }

        long clampedElapsed = Math.max(0L, Math.min(elapsedTicks, totalDurationTicks));
        int clampedThreshold = MathHelper.clamp(layeringThresholdPercent, 0, 100);
        return clampedElapsed * 100L >= totalDurationTicks * clampedThreshold
                ? RepeatAction.LAYER
                : RepeatAction.REPLACE;
    }

    public static Mode selectMode(
            boolean hasEligibleTarget, boolean hasAvailableSlot, boolean targetReserved) {
        return hasEligibleTarget && hasAvailableSlot && !targetReserved
                ? Mode.SHOULDER_CALL
                : Mode.NORMAL;
    }

    public static boolean remainsBound(
            boolean playerAlive,
            boolean sameDimension,
            boolean sameStack,
            boolean matchingFluteVariant) {
        return playerAlive && sameDimension && sameStack && matchingFluteVariant;
    }

    public static String sanitizePreviousGoalName(String goalName) {
        if (goalName == null
                || goalName.equals("HamsterLookAtEntityGoal")
                || goalName.equals("HamsterLookAroundGoal")
                || goalName.equals("FluteMountResponse")) {
            return "None";
        }
        return goalName;
    }

    public enum Mode {
        NORMAL,
        SHOULDER_CALL
    }

    public enum RepeatAction {
        REPLACE,
        LAYER
    }
}
