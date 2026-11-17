package com.zombiehdgaming.nexuscore.scoreboard;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * What appears on the right-hand side of sidebar lines that do not define their own {@code score} text.
 */
public enum ScoreNumbers {
    /** Nothing; the classic clean sidebar. */
    HIDDEN,
    /** Vanilla-style red numbers counting down to 1 at the bottom line. */
    SHOWN;

    /**
     * @param raw the config value
     * @return the matching mode, or {@code null} if unrecognised
     */
    public static @Nullable ScoreNumbers parse(final @Nullable String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "hidden", "hide", "false", "off" -> HIDDEN;
            case "shown", "show", "true", "on" -> SHOWN;
            default -> null;
        };
    }
}
