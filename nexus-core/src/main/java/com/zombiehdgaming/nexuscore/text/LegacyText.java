package com.zombiehdgaming.nexuscore.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Bridges legacy colour codes (as returned by most PlaceholderAPI expansions) into components.
 */
public final class LegacyText {

    /**
     * Accepts {@code &a}, {@code &#rrggbb} and the {@code &x&r&r&g&g&b&b} form. Section signs are normalised to
     * ampersands first so both styles work.
     */
    private static final LegacyComponentSerializer INPUT = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /** Output format for consumers that only understand strings (PlaceholderAPI). */
    private static final LegacyComponentSerializer OUTPUT = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private LegacyText() {
    }

    public static Component parse(final String value) {
        if (value.indexOf('&') < 0 && value.indexOf(LegacyComponentSerializer.SECTION_CHAR) < 0) {
            return Component.text(value);
        }
        return INPUT.deserialize(value.replace(LegacyComponentSerializer.SECTION_CHAR, '&'));
    }

    public static String serialize(final Component component) {
        return OUTPUT.serialize(component);
    }
}
