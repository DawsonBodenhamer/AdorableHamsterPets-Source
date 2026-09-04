package net.dawson.adorablehamsterpets.flute;

import net.dawson.adorablehamsterpets.entity.custom.DanceStyle;

import java.util.List;
import java.util.Locale;

public final class DanceSongMatcher {

    private DanceSongMatcher() {}

    public static DanceStyle classify(
            List<String> slowSearchStrings,
            List<String> fastSearchStrings,
            List<String> searchableText) {
        if (matches(slowSearchStrings, searchableText)) return DanceStyle.SWAYING;
        if (matches(fastSearchStrings, searchableText)) return DanceStyle.BOUNCING;
        return DanceStyle.NONE;
    }

    private static boolean matches(List<String> searchStrings, List<String> searchableText) {
        for (String searchString : searchStrings) {
            String normalizedSearch = searchString.toLowerCase(Locale.ROOT);
            if (normalizedSearch.isBlank()) continue;

            for (String text : searchableText) {
                if (text.toLowerCase(Locale.ROOT).contains(normalizedSearch)) return true;
            }
        }
        return false;
    }
}
