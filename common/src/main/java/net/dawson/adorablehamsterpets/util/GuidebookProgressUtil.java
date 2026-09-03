package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GuidebookProgressUtil {

    private static final Identifier RECEIVED_GUIDEBOOK =
            Identifier.of(AdorableHamsterPets.MOD_ID, "technical/has_received_initial_guidebook");
    private static final Identifier SAW_MISSING_GUIDEBOOK_WARNING =
            Identifier.of(AdorableHamsterPets.MOD_ID, "technical/has_seen_missing_guidebook_warning");

    private GuidebookProgressUtil() {}

    public static boolean hasReceivedGuidebook(ServerPlayerEntity player) {
        return isComplete(player, RECEIVED_GUIDEBOOK);
    }

    public static boolean hasSeenMissingGuidebookWarning(ServerPlayerEntity player) {
        return isComplete(player, SAW_MISSING_GUIDEBOOK_WARNING);
    }

    public static boolean markGuidebookReceived(ServerPlayerEntity player) {
        return grant(player, RECEIVED_GUIDEBOOK);
    }

    public static boolean markMissingGuidebookWarningSeen(ServerPlayerEntity player) {
        return grant(player, SAW_MISSING_GUIDEBOOK_WARNING);
    }

    private static boolean isComplete(ServerPlayerEntity player, Identifier id) {
        AdvancementEntry advancement = player.server.getAdvancementLoader().get(id);
        return advancement != null && player.getAdvancementTracker().getProgress(advancement).isDone();
    }

    private static boolean grant(ServerPlayerEntity player, Identifier id) {
        AdvancementEntry advancement = player.server.getAdvancementLoader().get(id);
        if (advancement == null) {
            AdorableHamsterPets.LOGGER.warn("Could not find technical advancement: {}", id);
            return false;
        }

        for (String criterion : advancement.value().criteria().keySet()) {
            player.getAdvancementTracker().grantCriterion(advancement, criterion);
        }
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }
}
