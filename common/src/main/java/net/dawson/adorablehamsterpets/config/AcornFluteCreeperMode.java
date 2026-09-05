package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum AcornFluteCreeperMode implements EnumTranslatable {
    ALL,
    CHARGED_ONLY;

    @NotNull
    @Override
    public String prefix() {
        return "config.adorablehamsterpets.enum.acorn_flute_creeper_mode";
    }

    @NotNull
    @Override
    public String translationKey() {
        return prefix() + "." + this.name().toLowerCase(Locale.ROOT);
    }
}
